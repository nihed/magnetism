/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-connection.h"
#include "hippo-data-cache-internal.h"
#include "hippo-data-model-internal.h"
#include "hippo-data-resource-internal.h"
#include "hippo-data-query-internal.h"
#include "hippo-common-marshal.h"
#include "hippo-external-account.h"
#include "hippo-title-pattern.h"
#include "hippo-xml-utils.h"
#include <loudmouth/loudmouth.h>
#include <string.h>
#include <stdlib.h>

#ifdef HAVE_RES_INIT
#include <resolv.h>
#endif

/* === CONSTANTS === */

static const int KEEP_ALIVE_RATE = 60;                  /* 1 minute; 0 disables */

/* retrying _authentication_ */
static const int SIGN_IN_INITIAL_TIMEOUT = 5000;        /* 5 seconds */
static const int SIGN_IN_INITIAL_COUNT = 60;            /* 5 minutes of fast retry */
static const int SIGN_IN_SUBSEQUENT_TIMEOUT = 30000;    /* 30 seconds retry after INITIAL_COUNT tries*/

/* retrying _connection_ */
static const int RETRY_TIMEOUT = 60*1000;               /* 1 minute for retrying _connection_ */
static const int RETRY_TIMEOUT_FUZZ = 60*1000*5;        /* add up to this much to keep clients from all connecting 
                                                         * at the same time.
                                                         */

/* We delay this long after a music change before sending it to the server */
static const int MUSIC_GRACE_PERIOD = 7000;             /* 7 seconds */

typedef struct MessageContext MessageContext;

/* This structure is generated internally if 
 * extended data is passed to hippo_connection_send_message_with_reply_full.
 * It is then passed to the callback function instead of the bare HippoConnection.
 */
struct MessageContext {
    guint refcount;
    HippoConnection *connection;
    gpointer data;
    GFreeFunc free_data_func;
};

static void 
message_context_ref(MessageContext *context)
{
    if (context == NULL)
        return;
    context->refcount++;  
}

static void
message_context_unref (gpointer ptr) 
{
    MessageContext *context = (MessageContext *) ptr;
    if (context == NULL)
        return;
    if (--context->refcount == 0) {
        if (context->free_data_func) {
            context->free_data_func(context->data);
        }
        g_free(context);
    }
}

/* === OutgoingMessage internal class === */

typedef struct OutgoingMessage OutgoingMessage;

struct OutgoingMessage {
    int               refcount;
    LmMessage        *message;
    LmHandleMessageFunction handler;
    int generation;
    MessageContext *context;
};

static OutgoingMessage*
outgoing_message_new(LmMessage               *message,
                     LmHandleMessageFunction  handler,
                     int                      generation,
                     MessageContext          *context)
{
    OutgoingMessage *outgoing = g_new0(OutgoingMessage, 1);
    outgoing->refcount = 1;
    outgoing->message = message;
    outgoing->handler = handler;
    outgoing->generation = generation;
    outgoing->context = context;
    message_context_ref(outgoing->context);
    if (message)
        lm_message_ref(message);
    return outgoing;
}

#if 0
/* not used right now */
static void
outgoing_message_ref(OutgoingMessage *outgoing)
{
    g_return_if_fail(outgoing != NULL);
    g_return_if_fail(outgoing->refcount > 0);
    outgoing->refcount += 1;
}
#endif

static void
outgoing_message_unref(OutgoingMessage *outgoing)
{
    g_return_if_fail(outgoing != NULL);
    g_return_if_fail(outgoing->refcount > 0);
    outgoing->refcount -= 1;
    if (outgoing->refcount == 0) {
        if (outgoing->message)
            lm_message_unref(outgoing->message);
        message_context_unref(outgoing->context);
        g_free(outgoing);
    }
}

/* === HippoConnection implementation === */

typedef enum {
    PROCESS_MESSAGE_IGNORE,
    PROCESS_MESSAGE_CONSUME
} ProcessMessageResult;


/* 
 * SendMode
 * 
 * We keep an "offline queue" of stuff to send when we become connected. This queue
 * makes sense for some messages (such as notifying that a post has clicked, or a
 * a song has played) and does not make sense for others (such as sending presence
 * to a chat room). hippo_connection_send() has an argument hinting what to do here.
 */

typedef enum {
    SEND_MODE_IGNORE_IF_DISCONNECTED,
    SEND_MODE_IMMEDIATELY,
    SEND_MODE_AFTER_AUTH
} SendMode;

static void hippo_connection_finalize(GObject *object);

static void     hippo_connection_start_signin_timeout (HippoConnection *connection);
static void     hippo_connection_stop_signin_timeout  (HippoConnection *connection);
static void     hippo_connection_start_retry_timeout  (HippoConnection *connection);
static void     hippo_connection_stop_retry_timeout   (HippoConnection *connection);
static void     hippo_connection_stop_music_timeout   (HippoConnection *connection);
static void     hippo_connection_queue_request_blocks (HippoConnection *connection);
static void     hippo_connection_unqueue_request_blocks (HippoConnection *connection);
static void     hippo_connection_connect              (HippoConnection *connection,
                                                       const char      *redirect_host);
static void     hippo_connection_disconnect           (HippoConnection *connection);
static void     hippo_connection_state_change         (HippoConnection *connection,
                                                       HippoState       state);
static gboolean hippo_connection_load_auth            (HippoConnection *connection);
static void     hippo_connection_authenticate         (HippoConnection *connection);
static void     hippo_connection_clear                (HippoConnection *connection);
static void     hippo_connection_flush_outgoing       (HippoConnection *connection);
static void     hippo_connection_send_message         (HippoConnection *connection,
                                                       LmMessage       *message,
                                                       SendMode         mode);
static void     hippo_connection_send_message_with_reply(HippoConnection *connection,
                                                         LmMessage         *message,
                                                         LmHandleMessageFunction handler,
                                                         SendMode           mode);
static void     hippo_connection_request_client_info  (HippoConnection *connection);

static void     hippo_connection_parse_prefs_node     (HippoConnection *connection,
                                                       LmMessageNode   *prefs_node);
static gboolean hippo_connection_parse_entity         (HippoConnection *connection,
                                                       LmMessageNode   *node);

/* enter/leave unconditionally send the presence message; send_state will 
 * send the presence only if there's a need given old_state -> new_state
 * transition, assuming no disconnect/connect between old and new state.
 */
static void     hippo_connection_send_chat_room_enter (HippoConnection *connection,
                                                       HippoChatRoom   *room,
                                                       HippoChatState   state);
static void     hippo_connection_send_chat_room_leave (HippoConnection *connection,
                                                       HippoChatRoom   *room);
static void     hippo_connection_send_chat_room_state (HippoConnection *connection,
                                                       HippoChatRoom   *room,
                                                       HippoChatState   old_state,
                                                       HippoChatState   new_state);

/* Loudmouth handlers */
static LmHandlerResult handle_message     (LmMessageHandler *handler,
                                           LmConnection     *connection,
                                           LmMessage        *message,
                                           gpointer          data);
static LmHandlerResult handle_stream_error (LmMessageHandler *handler,
                                            LmConnection     *connection,
                                            LmMessage        *message,
                                            gpointer          data);
static LmHandlerResult handle_presence    (LmMessageHandler *handler,
                                           LmConnection     *connection,
                                           LmMessage        *message,
                                           gpointer          data);
static void            handle_disconnect  (LmConnection       *connection,
                                           LmDisconnectReason  reason,
                                           gpointer            data);
static void            handle_open        (LmConnection *connection,
                                           gboolean      success,
                                           gpointer      data);
static void            handle_authenticate(LmConnection *connection,
                                           gboolean      success,
                                           gpointer      data);

static gboolean handle_data_notify (HippoConnection *connection,
                                    LmMessage       *message);

struct _HippoConnection {
    GObject parent;
    HippoPlatform *platform;
    HippoDataCache *cache;
    HippoState state;
    /* number of times we've reconnected */
    int generation;
    int signin_timeout_id;
    int signin_timeout_count;
    int retry_timeout_id;
    int music_timeout_id;
    LmConnection *lm_connection;
    /* queue of OutgoingMessage objects */
    GQueue *pending_outgoing_messages;
    HippoSong pending_song;
    HippoBrowserKind login_browser;
    int message_port;
    char *username;
    char *password;
    char *self_resource_id;
    gboolean contacts_loaded;
    char *download_url;
    char *tooltip;
    char *active_block_filter;
    int request_blocks_id;
    gint64 last_blocks_timestamp;
    gint64 server_time_offset;
    unsigned int too_old : 1;
    unsigned int upgrade_available : 1;
    unsigned int last_auth_failed : 1;
    
    guint external_iq_serial;
};

struct _HippoConnectionClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoConnection, hippo_connection, G_TYPE_OBJECT);

enum {
    /* Any kind of state change; new states may be added later.. */
    STATE_CHANGED,
    /* Emitted when we become ready to do arbitrary stuff, after all the initial authentication
     * and if we are a "new enough" client
     */
    CONNECTED_CHANGED,
    /* Emitted whenever we successfully load or forget the login cookie */
    HAS_AUTH_CHANGED,
    /* Emitted anytime we try and fail to auth */
    AUTH_FAILED,
    /* Emitted anytime we try and succeed at auth;
     * emitted after _auth_, not after connection (see CONNECTED_CHANGED) 
     */
    AUTH_SUCCEEDED,
    /* Emitted when we get the client info, even if we're "too old" and thus 
     * won't connect successfully. Comes just before CONNECTED_CHANGED.
     */
    CLIENT_INFO_AVAILABLE,
    POST_ACTIVITY,
    MYSPACE_CHANGED,
    GROUP_MEMBERSHIP_CHANGED,
    BLOCK_FILTER_CHANGED,
    SETTING_CHANGED,
    SETTINGS_LOADED,
    WHEREIM_CHANGED,
    /* Emitted to signal that we should temporarily rapidly upload application
     * activity instead of just once an hour */
    INITIAL_APPLICATION_BURST,
    EXTERNAL_IQ_RETURN,
   	CONTACTS_LOADED,
   	PREF_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_connection_init(HippoConnection *connection)
{
    connection->state = HIPPO_STATE_SIGNED_OUT;
    connection->pending_outgoing_messages = g_queue_new();

    /* default browsers if we don't discover otherwise 
     * (we'll use whatever the user has logged in with
     * if they've logged in with something)
     */
#ifdef G_OS_WIN32
    connection->login_browser = HIPPO_BROWSER_IE;
#else
    connection->login_browser = HIPPO_BROWSER_FIREFOX;
#endif
}

static void
hippo_connection_class_init(HippoConnectionClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
  
    signals[STATE_CHANGED] =
        g_signal_new ("state-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0); 

    signals[CONNECTED_CHANGED] =
        g_signal_new ("connected-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__BOOLEAN,
                      G_TYPE_NONE, 1, G_TYPE_BOOLEAN);

    signals[HAS_AUTH_CHANGED] =
        g_signal_new ("has-auth-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[AUTH_FAILED] =
        g_signal_new ("auth-failed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0); 

    signals[AUTH_SUCCEEDED] =
        g_signal_new ("auth-succeeded",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0); 

    signals[CLIENT_INFO_AVAILABLE] =
        g_signal_new ("client-info-available",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[POST_ACTIVITY] =
        g_signal_new ("post-activity",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[MYSPACE_CHANGED] =
        g_signal_new ("myspace-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0); 

    signals[GROUP_MEMBERSHIP_CHANGED] =
        g_signal_new ("group-membership-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      hippo_common_marshal_VOID__OBJECT_OBJECT_STRING,
                      G_TYPE_NONE, 3, G_TYPE_OBJECT, G_TYPE_OBJECT, G_TYPE_STRING); 
                      
    signals[BLOCK_FILTER_CHANGED] =
        g_signal_new ("block-filter-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__STRING,
                      G_TYPE_NONE, 1, G_TYPE_STRING);               

    signals[SETTING_CHANGED] =
        g_signal_new ("setting-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      hippo_common_marshal_VOID__STRING_STRING,
                      G_TYPE_NONE, 2, G_TYPE_STRING, G_TYPE_STRING);

    signals[SETTINGS_LOADED] =
        g_signal_new ("settings-loaded",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
    
    signals[WHEREIM_CHANGED] =
        g_signal_new ("whereim-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, G_TYPE_OBJECT);                      
    
    signals[INITIAL_APPLICATION_BURST] =
        g_signal_new ("initial-application-burst",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);        
                      
    signals[CONTACTS_LOADED] =
        g_signal_new ("contacts-loaded",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);  
                      
	signals[PREF_CHANGED] =
        g_signal_new ("pref-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      hippo_common_marshal_VOID__STRING_BOOLEAN,
                      G_TYPE_NONE, 2, G_TYPE_STRING, G_TYPE_BOOLEAN);   
                      
    signals[EXTERNAL_IQ_RETURN] =
        g_signal_new ("external-iq-return",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__UINT_POINTER,
                      G_TYPE_NONE, 2, G_TYPE_UINT, G_TYPE_POINTER);    
    
    object_class->finalize = hippo_connection_finalize;
}

static void
hippo_connection_finalize(GObject *object)
{
    HippoConnection *connection = HIPPO_CONNECTION(object);

    g_debug("Finalizing connection");

    hippo_connection_unqueue_request_blocks(connection);
    hippo_connection_stop_music_timeout(connection);
    hippo_connection_stop_signin_timeout(connection);
    hippo_connection_stop_retry_timeout(connection);
    
    hippo_connection_disconnect(connection);
    
    g_queue_foreach(connection->pending_outgoing_messages,
                    (GFunc) outgoing_message_unref, NULL);
    g_queue_free(connection->pending_outgoing_messages);

    g_free(connection->username);
    g_free(connection->password);
    g_free(connection->tooltip);
    g_free(connection->self_resource_id);

    g_object_unref(connection->platform);
    connection->platform = NULL;

    g_free(connection->download_url);

    G_OBJECT_CLASS(hippo_connection_parent_class)->finalize(object); 
}


/* === HippoConnection exported API === */


/* "platform" should be a construct property, but I'm lazy */
HippoConnection*
hippo_connection_new(HippoPlatform *platform)
{
    HippoConnection *connection;
    
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);

    connection = g_object_new(HIPPO_TYPE_CONNECTION, NULL);
    
    connection->platform = platform;
    g_object_ref(connection->platform);
    
    return connection;
}

HippoPlatform*
hippo_connection_get_platform(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), NULL);
    
    return connection->platform;
}

int
hippo_connection_get_generation(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), -1);
    
    return connection->generation;
}

gboolean
hippo_connection_get_too_old(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    
    return connection->too_old;
}

gboolean
hippo_connection_get_upgrade_available(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    
    return connection->upgrade_available;
}

const char*
hippo_connection_get_download_url(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), NULL);
    
    return connection->download_url;
}

void
hippo_connection_set_cache(HippoConnection  *connection,
                           HippoDataCache   *cache)
{
    /* We do NOT ref the cache, it refs us. Conceptually, we should really 
     * be emitting signals that the cache would see, rather than calling methods
     * on the cache; but in practice that's sort of painful and inefficient.
     */
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
     
    connection->cache = cache;
}

gboolean
hippo_connection_get_has_auth(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);

    return connection->username && connection->password;
}

HippoBrowserKind
hippo_connection_get_auth_browser(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), 0);

    return connection->login_browser;
}

static void
zero_str(char **s_p)
{
    g_free(*s_p);
    *s_p = NULL;
}

void
hippo_connection_forget_auth(HippoConnection *connection)
{
    gboolean old_has_auth;

    old_has_auth = hippo_connection_get_has_auth(connection);

    hippo_platform_delete_login_cookie(connection->platform);
    zero_str(&connection->username);
    zero_str(&connection->password);
    zero_str(&connection->self_resource_id);
    
    if (old_has_auth != hippo_connection_get_has_auth(connection)) {
        g_signal_emit(connection, signals[HAS_AUTH_CHANGED], 0);
    }
}

const char*
hippo_connection_get_self_guid(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), NULL);
    return connection->username;
}

const char*
hippo_connection_get_self_resource_id(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), NULL);

    if (connection->self_resource_id == NULL) {
        const char *server;
        const char *self_guid;
        
        self_guid = hippo_connection_get_self_guid(connection);
        
        server = hippo_platform_get_web_server(connection->platform);
        
        connection->self_resource_id = g_strdup_printf("http://%s/o/user/%s", server, self_guid);
    }

    return connection->self_resource_id;
}

HippoState
hippo_connection_get_state(HippoConnection *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), HIPPO_STATE_SIGNED_OUT);
    return connection->state;
}

gboolean
hippo_connection_get_connected(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    return connection->state == HIPPO_STATE_AUTHENTICATED;
}

/* Returns whether we have the login cookie */
gboolean
hippo_connection_signin(HippoConnection *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    
    hippo_connection_stop_signin_timeout(connection);
        
    if (hippo_connection_load_auth(connection)) {
        if (connection->state == HIPPO_STATE_AUTH_WAIT)
            hippo_connection_authenticate(connection);
        else
            hippo_connection_connect(connection, NULL);
        return FALSE;
    } else {
        if (connection->state != HIPPO_STATE_SIGN_IN_WAIT &&
            connection->state != HIPPO_STATE_AUTH_WAIT)
            hippo_connection_state_change(connection, HIPPO_STATE_SIGN_IN_WAIT);
        hippo_connection_start_signin_timeout(connection);
        return TRUE;
    }
}

void
hippo_connection_signout(HippoConnection *connection)
{
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));

    hippo_connection_state_change(connection, HIPPO_STATE_SIGNED_OUT);
    
    hippo_connection_disconnect(connection);
}

static void
add_track_props(LmMessageNode *node,
                char         **keys,
                char         **values)
{
    int i;
    for (i = 0; keys[i] != NULL; ++i) {
        LmMessageNode *prop_node = lm_message_node_add_child(node, "prop", NULL);
        lm_message_node_set_attribute(prop_node, "key", keys[i]);
        lm_message_node_set_value(prop_node, values[i]);  
    }
}

void             
hippo_connection_set_post_ignored (HippoConnection  *connection,
                                   const char       *post_id)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *method;
    HippoPost *post;
            
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    method = lm_message_node_add_child (node, "postControl", NULL);
    lm_message_node_set_attribute(method, "xmlns", "http://dumbhippo.com/protocol/postControl");
    lm_message_node_set_attribute(method, "type", "ignore");
    lm_message_node_set_attribute(method, "id", post_id);
 
    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
    
    /* Because we don't have change notification on this flag right now, we 
     * "write through" the cache and save the info locally also.
     * Also avoids a race condition where we might fail to ignore some 
     * incoming message.
     */
    post = hippo_data_cache_lookup_post(connection->cache, post_id);
    if (post == NULL) {
        g_warning("trying to ignore unknown post %s", post_id);
        return;
    } else {
        hippo_post_set_ignored(post, TRUE);
    }
}

void
hippo_connection_do_invite_to_group (HippoConnection  *connection,
                                     const char       *group_id,
                                     const char       *person_id)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *method;
            
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    method = lm_message_node_add_child (node, "groupSystem", NULL);
    lm_message_node_set_attribute(method, "xmlns", "http://dumbhippo.com/protocol/groupSystem");
    lm_message_node_set_attribute(method, "op", "addMember");
    lm_message_node_set_attribute(method, "groupId", group_id);
    lm_message_node_set_attribute(method, "userId", person_id);    
 
    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
}

static void
do_notify_music_changed(HippoConnection *connection,
                        gboolean         currently_playing,
                        const HippoSong *song)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *music;
            
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(!currently_playing || song != NULL);

    /* If the user has music sharing off, then we never send their info
     * to the server. (this is a last-ditch protection; we aren't supposed
     * to be monitoring the music app either in this case)
     */
    if (!hippo_data_cache_get_music_sharing_enabled(connection->cache))
        return;

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "musicChanged");

    if (currently_playing) {
        g_assert(song != NULL);
        add_track_props(music, song->keys, song->values);
    }

    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
    /* g_print("Sent music changed xmpp message"); */
}

static gboolean 
music_timeout(gpointer data)
{
    HippoConnection *connection = data;
    
    do_notify_music_changed(connection,
                            TRUE,
                            &connection->pending_song);
    
    hippo_connection_stop_music_timeout(connection);
    
    return FALSE;
}

void
hippo_connection_notify_music_changed(HippoConnection *connection,
                                      gboolean         currently_playing,
                                      const HippoSong *song)
{
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(!currently_playing || song != NULL);

    hippo_connection_stop_music_timeout(connection);

    /* When the user switches to a new song, we give them MUSIC_GRACE_PERIOD
     * milliseconds before we send it to the server, in case it is embarrassing.
     * Stopping the music is, however, sent immediately.
     */
    if (currently_playing) {
        connection->pending_song.keys = g_strdupv(song->keys);
        connection->pending_song.values = g_strdupv(song->values);

        connection->music_timeout_id = g_timeout_add(MUSIC_GRACE_PERIOD, 
                                                     music_timeout, connection);
    } else {
        do_notify_music_changed(connection,
                                FALSE,
                                NULL);
    }
}

void
hippo_connection_provide_priming_music(HippoConnection  *connection,
                                       const HippoSong  *songs,
                                       int               n_songs)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *music;
    int i;
    
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(songs != NULL);

    if (!hippo_data_cache_get_need_priming_music(connection->cache)) {
        /* didn't need to prime after all (maybe someone beat us to it) */
        return;
    }

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "primingTracks");

    for (i = 0; i < n_songs; ++i) {
        LmMessageNode *track = lm_message_node_add_child(music, "track", NULL);
        add_track_props(track, songs[i].keys, songs[i].values);
    }

    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    /* we should also get back a notification from the server when this changes,
     * but we want to avoid re-priming so this adds robustness
     */
    hippo_data_cache_set_music_sharing_primed(connection->cache, TRUE);
}


/* === HippoConnection private methods === */

/* also used for client info failure */
static void
hippo_connection_connect_failure(HippoConnection *connection,
                                 const char      *message)
{
    g_debug("Connection failure message: '%s'", message ? message : "NULL");

    /* message can be NULL */
    hippo_connection_clear(connection);
    hippo_connection_start_retry_timeout(connection);
    hippo_connection_state_change(connection, HIPPO_STATE_RETRYING);
}

static void
hippo_connection_auth_failure(HippoConnection *connection,
                              const char      *message)
{
    g_debug("Auth failure message: '%s'", message ? message : "NULL");

    /* message can be NULL */
    hippo_connection_forget_auth(connection);
    hippo_connection_start_signin_timeout(connection);
    hippo_connection_state_change(connection, HIPPO_STATE_AUTH_WAIT);
    connection->last_auth_failed = TRUE;
    g_signal_emit(connection, signals[AUTH_FAILED], 0);
}

static gboolean 
signin_timeout(gpointer data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    g_debug("Signin timeout");

    if (hippo_connection_load_auth(connection)) {
        hippo_connection_stop_signin_timeout(connection);

        if (connection->state == HIPPO_STATE_AUTH_WAIT)
            hippo_connection_authenticate(connection);
        else
            hippo_connection_connect(connection, NULL);

        return FALSE;
    }

    connection->signin_timeout_count += 1;
    if (connection->signin_timeout_count == SIGN_IN_INITIAL_COUNT) {
        /* Try more slowly */
        g_source_remove (connection->signin_timeout_id);
        connection->signin_timeout_id = g_timeout_add (SIGN_IN_SUBSEQUENT_TIMEOUT, signin_timeout, 
                                                       connection);
        return FALSE;
    }

    return TRUE;
}

static void
hippo_connection_start_signin_timeout(HippoConnection *connection)
{
    if (connection->signin_timeout_id == 0) {
        g_debug("Installing signin timeout for %g seconds", SIGN_IN_INITIAL_TIMEOUT / 1000.0);    
        connection->signin_timeout_id = g_timeout_add(SIGN_IN_INITIAL_TIMEOUT, 
                                                      signin_timeout, connection);
        connection->signin_timeout_count = 0;
    }    
}

static void
hippo_connection_stop_signin_timeout(HippoConnection *connection)
{
    if (connection->signin_timeout_id != 0) {
        g_debug("Removing signin timeout");
        g_source_remove (connection->signin_timeout_id);
        connection->signin_timeout_id = 0;
        connection->signin_timeout_count = 0;
    }
}

static void
hippo_connection_retry(HippoConnection *connection)
{
    g_debug("retrying connect to server");
    
    hippo_connection_stop_retry_timeout(connection);
    hippo_connection_connect(connection, NULL);
}

static gboolean 
retry_timeout(gpointer data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    hippo_connection_retry(connection);

    return FALSE;
}

static void
on_network_status_changed(HippoPlatform     *platform,
                          HippoNetworkStatus status,
                          gpointer           data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    g_debug("new network status from platform %d", status);
    
    if (status != HIPPO_NETWORK_STATUS_DOWN)
        hippo_connection_retry(connection);
}

static void
hippo_connection_start_retry_timeout(HippoConnection *connection)
{
    if (connection->retry_timeout_id == 0) {
        int timeout = RETRY_TIMEOUT + g_random_int_range(0, RETRY_TIMEOUT_FUZZ);
        g_debug("Installing retry timeout for %g seconds", timeout / 1000.0);
        connection->retry_timeout_id = g_timeout_add(timeout, 
                                                     retry_timeout, connection);

        g_signal_connect(G_OBJECT(connection->platform), "network-status-changed",
                         G_CALLBACK(on_network_status_changed), connection);
    }
}

static void
hippo_connection_stop_retry_timeout(HippoConnection *connection)
{
    if (connection->retry_timeout_id != 0) {
        g_debug("Removing retry timeout");
        g_source_remove (connection->retry_timeout_id);
        connection->retry_timeout_id = 0;
        
        g_signal_handlers_disconnect_by_func(G_OBJECT(connection->platform),
                                             G_CALLBACK(on_network_status_changed),
                                             connection);
    }
}

static void
hippo_connection_stop_music_timeout(HippoConnection *connection)
{
    if (connection->music_timeout_id != 0) {
        g_source_remove (connection->music_timeout_id);
        connection->music_timeout_id = 0;

        g_strfreev(connection->pending_song.keys);
        connection->pending_song.keys = NULL;
        
        g_strfreev(connection->pending_song.values);
        connection->pending_song.keys = NULL;
    }
}

static void
hippo_connection_connect(HippoConnection *connection, const char *redirect_host)
{
    char *message_host;
    int message_port;
    LmMessageHandler *handler;
    GError *error;

    if (connection->lm_connection != NULL) {
        g_warning("hippo_connection_connect() called when already connected");
        return;
    }
    
    hippo_platform_get_message_host_port(connection->platform, &message_host, &message_port);

    if (redirect_host) {
        g_free(message_host);
        message_host = g_strdup(redirect_host);
    }

    g_debug("Connecting to %s port %d", message_host, message_port);

    connection->lm_connection = lm_connection_new(message_host);

    g_free(message_host);
    
    hippo_override_loudmouth_log(); /* lm installed its log handler the first time we did connection_new */

    lm_connection_set_port(connection->lm_connection, message_port);
    lm_connection_set_keep_alive_rate(connection->lm_connection, KEEP_ALIVE_RATE);

    handler = lm_message_handler_new(handle_message, connection, NULL);
    lm_connection_register_message_handler(connection->lm_connection, handler,
                                           LM_MESSAGE_TYPE_MESSAGE,
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);
    handler = lm_message_handler_new(handle_presence, connection, NULL);

    lm_connection_register_message_handler(connection->lm_connection, handler,
                                           LM_MESSAGE_TYPE_PRESENCE,
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    handler = lm_message_handler_new(handle_stream_error, connection, NULL);
    lm_connection_register_message_handler(connection->lm_connection, handler, 
                                           LM_MESSAGE_TYPE_STREAM_ERROR,
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    lm_connection_set_disconnect_function(connection->lm_connection,
                                          handle_disconnect, connection, NULL);

    hippo_connection_state_change(connection, HIPPO_STATE_CONNECTING);

    error = NULL;

#ifdef HAVE_RES_INIT
    /* With GNU libc (and possibly on other systems), if the DNS configuration
     * changes, libc will *never* notice until you tell it explicitly to look
     * again by calling res_init(). Since we already throttle how often
     * we try to connect, we just call res_init() before every connection
     * attempt.
     */
    res_init();
#endif
            
    /* If lm_connection returns FALSE, then handle_open won't be called
     * at all. On a TRUE return it will be called exactly once, but that 
     * call might occur before or after lm_connection_open() returns, and
     * may occur for success or for failure.
     */
    if (!lm_connection_open(connection->lm_connection, 
                            handle_open, connection, NULL, 
                            &error)) {
        g_debug("lm_connection_open returned false");
        hippo_connection_connect_failure(connection, error ? error->message : "");
        if (error)
            g_error_free(error);
    } else {
        g_debug("lm_connection_open returned true, waiting for callback");
    }
}

static void
hippo_connection_disconnect(HippoConnection *connection)
{
    if (connection->lm_connection != NULL) {
        /* This normally calls our disconnect handler which clears 
           and unrefs lm_connection */
        lm_connection_close(connection->lm_connection, NULL);
        
        /* in case the above didn't happen (why?) */
        if (connection->lm_connection) {
            lm_connection_unref(connection->lm_connection);
            connection->lm_connection = NULL;
        }
    }
}

static void
hippo_connection_state_change(HippoConnection *connection,
                              HippoState       state)
{
    gboolean old_connected;
    gboolean connected;

    if (connection->state == state)
        return;
    
    old_connected = hippo_connection_get_connected(connection);

    connection->state = state;
    
    if (connection->tooltip) {
        g_free(connection->tooltip);
        connection->tooltip = NULL;
    }

    connected = hippo_connection_get_connected(connection);

    hippo_connection_flush_outgoing(connection);

    g_debug("Connection state = %s connected = %d", hippo_state_to_string(connection->state), connected);
    
    /* It's important to bump generation on _disconnect_,
     * so stuff queued while disconnected is in the right 
     * generation. On disconnect, we dump the pending queue.
     */
    if (!connected && old_connected)
        connection->generation += 1;
    
    g_signal_emit(connection, signals[STATE_CHANGED], 0);

    /* A "simplified" signal that only indicates the one "are we fully signed in" 
     * thing
     */
    if (old_connected != connected) {
        g_signal_emit(connection, signals[CONNECTED_CHANGED], 0, connected);
    }
}

static gboolean
hippo_connection_load_auth(HippoConnection *connection)
{
    gboolean result;
    gboolean old_has_auth;

    old_has_auth = hippo_connection_get_has_auth(connection);

    /* always clear current username/password */
    zero_str(&connection->username);
    zero_str(&connection->password);
    zero_str(&connection->self_resource_id);
    
    result = hippo_platform_read_login_cookie(connection->platform,
                                              &connection->login_browser,
                                              &connection->username, &connection->password);

    if (connection->username) {
        /* don't print the password in the log info */
        g_debug("Loaded username '%s' password %s", connection->username,
                connection->password ? "loaded" : "not found");
    }

    if (old_has_auth != hippo_connection_get_has_auth(connection)) {
        g_signal_emit(connection, signals[HAS_AUTH_CHANGED], 0);
    }

    return result;
}

static void
hippo_connection_authenticate(HippoConnection *connection)
{
    char *jabber_id;
    const char *resource;
    GError *error;
     
    if (connection->username == NULL || connection->password == NULL) {
        hippo_connection_auth_failure(connection, "Not signed in");
        return;
    }
    
    jabber_id = hippo_id_to_jabber_id(connection->username);
    resource = hippo_platform_get_jabber_resource(connection->platform);

    error = NULL;
    if (!lm_connection_authenticate(connection->lm_connection, 
                                    jabber_id,
                                    connection->password,
                                    resource,
                                    handle_authenticate, connection, NULL,
                                    &error))
        {
            hippo_connection_auth_failure(connection, error ? error->message : NULL);
            if (error)
                g_error_free(error);
        } else {
        hippo_connection_state_change(connection, HIPPO_STATE_AUTHENTICATING);
    }
    g_free(jabber_id);
}

static void
hippo_connection_clear(HippoConnection *connection)
{
    if (connection->lm_connection != NULL) {
        lm_connection_unref(connection->lm_connection);
        connection->lm_connection = NULL;
    }
}

static void
hippo_connection_send_message(HippoConnection *connection,
                              LmMessage       *message,
                              SendMode         mode)
{
    hippo_connection_send_message_with_reply(connection, message, NULL, mode);
}

static void
send_immediately(HippoConnection         *connection,
                 LmMessage               *message,
                 LmHandleMessageFunction  handler,
                 MessageContext          *context)
{
    GError *error;
    
    if (connection->state < HIPPO_STATE_AWAITING_CLIENT_INFO) {
        g_warning("SEND_MODE_IMMEDIATELY used when not authenticated");
        return;
    }

    error = NULL;
    if (handler != NULL) {
        LmMessageHandler *handler_obj = lm_message_handler_new(handler, 
                                                               context ? (void*) context : (void*) connection,  /* Defaults to connection */
                                                               context ? message_context_unref : (GDestroyNotify) NULL);
        lm_connection_send_with_reply(connection->lm_connection, message, handler_obj, &error);
        lm_message_handler_unref(handler_obj);
    } else {
        lm_connection_send(connection->lm_connection, message, &error);
    }
    if (error) {
        g_debug("Failed sending message: %s", error->message);
        g_error_free(error);
    }
}

static void
hippo_connection_send_message_with_reply_full(HippoConnection  *connection,
                                              LmMessage        *message,
                                              LmHandleMessageFunction handler,
                                              SendMode          mode,
                                              gpointer          data,
                                              GFreeFunc         free_func)
{
    MessageContext *context = NULL;
    
    if (mode == SEND_MODE_IGNORE_IF_DISCONNECTED) {
        if (!hippo_connection_get_connected(connection))
            return;    
        else
            mode = SEND_MODE_AFTER_AUTH;
    }
    
    if (data != NULL) {
        context = g_new0(MessageContext, 1);
        context->refcount = 1;
        context->connection = connection;
        context->data = data;
        context->free_data_func = free_func;   
    }
    
    if (mode == SEND_MODE_IMMEDIATELY) {
        send_immediately(connection, message, handler, context);
    } else {
        g_queue_push_tail(connection->pending_outgoing_messages,
                          outgoing_message_new(message, handler, connection->generation, context));

        hippo_connection_flush_outgoing(connection);    
    }
}

static void
hippo_connection_send_message_with_reply(HippoConnection  *connection,
                                         LmMessage        *message,
                                         LmHandleMessageFunction handler,
                                         SendMode          mode)
{
    hippo_connection_send_message_with_reply_full(connection, message, handler, mode, NULL, NULL);
}

static void
hippo_connection_flush_outgoing(HippoConnection *connection)
{
#if 0
    if (connection->pending_outgoing_messages->length > 1 &&
        connection->state == HIPPO_STATE_AUTHENTICATED)
        g_print("%d messages backlog to clear", connection->pending_outgoing_messages->length);
#endif

    /* We only flush the queue AFTER we have the client info */
    while (connection->state == HIPPO_STATE_AUTHENTICATED &&
           connection->pending_outgoing_messages->length > 0) {
        OutgoingMessage *om = g_queue_pop_head(connection->pending_outgoing_messages);
        /* we dump messages sent prior to a disconnect, the queue is for stuff
         * sent while disconnected, not stuff sent before disconnection.
         */
        if (om->generation == connection->generation)
            send_immediately(connection, om->message, om->handler, om->context);
        outgoing_message_unref(om);
    }

#if 0
    if (connection->pending_outgoing_messages->length > 0)
        g_print("%d messages could not be sent now, since we aren't connected; deferring",
                connection->pending_outgoing_messages->length);
#endif
}

typedef struct {
    HippoConnection *connection;
} RequestBlocksData;

static gboolean
request_blocks_idle(void *data)
{
    RequestBlocksData *rbd = data;

    rbd->connection->request_blocks_id = 0;

    g_debug("Firing request_blocks_idle");
    
    /* if the latest block's timestamp is still <= last_blocks_timestamp,
     * this will return an empty list of blocks
     */
    hippo_connection_request_blocks(rbd->connection,
                                    rbd->connection->last_blocks_timestamp,
                                    rbd->connection->active_block_filter);

    return FALSE;
}

static void
hippo_connection_queue_request_blocks (HippoConnection *connection)
{
    RequestBlocksData *rbd;
    
    hippo_connection_unqueue_request_blocks(connection);

    g_debug("adding request blocks idle");

    rbd = g_new0(RequestBlocksData, 1);
    rbd->connection = connection;
    
    connection->request_blocks_id = g_idle_add_full(G_PRIORITY_DEFAULT,
                                                    request_blocks_idle,
                                                    rbd, g_free);
}

static void
hippo_connection_unqueue_request_blocks (HippoConnection *connection)
{
    if (connection->request_blocks_id != 0) {
        g_debug("removing request blocks idle");
        g_source_remove(connection->request_blocks_id);
        connection->request_blocks_id = 0;
    }
}

static gboolean
parse_bool(const char *str)
{
    return strcmp(str, "true") == 0;
}

static gboolean
node_matches(LmMessageNode *node, const char *name, const char *expectedNamespace)
{
    const char *ns = lm_message_node_get_attribute(node, "xmlns");
    if (expectedNamespace && !ns)
        return FALSE;
    return strcmp(name, node->name) == 0 && (expectedNamespace == NULL || strcmp(expectedNamespace, ns) == 0);
}

static LmMessageNode*
find_child_node(LmMessageNode *node, 
                const char    *element_namespace, 
                const char    *element_name)
{
    LmMessageNode *child;
    for (child = node->children; child; child = child->next) {
        if (child->name == NULL)
            continue;
        if (element_namespace) {
            const char *ns = lm_message_node_get_attribute(child, "xmlns");
            if (!(ns && strcmp(ns, element_namespace) == 0))
                continue;
        }
        if (strcmp(child->name, element_name) != 0)
            continue;

        return child;
    }

    return NULL;
}

static gboolean
message_is_iq_with_namespace(LmMessage  *message,
                             const char *expected_namespace,
                             const char *document_element_name)
{
    LmMessageNode *child = message->node->children;

    if (lm_message_get_type(message) != LM_MESSAGE_TYPE_IQ ||
        lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_RESULT ||
        !child || child->next ||
        !node_matches(child, document_element_name, expected_namespace)) {
        return FALSE;
    } else {
        return TRUE;
    }
}

static LmHandlerResult
on_client_info_reply(LmMessageHandler *handler,
                     LmConnection     *lconnection,
                     LmMessage        *message,
                     gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;
    HippoClientInfo info;
    const char *minimum;
    const char *current;
    const char *download;
    
    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/clientinfo", "clientInfo")) {
        hippo_connection_connect_failure(connection, "Client info reply was wrong thing");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    child = message->node->children;

    if (!hippo_xml_split(connection->cache, child, NULL,
                         "minimum", HIPPO_SPLIT_STRING, &minimum,
                         "current", HIPPO_SPLIT_STRING, &current,
                         "download", HIPPO_SPLIT_STRING, &download,
                         NULL))
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;

    g_debug("Got clientInfo response: minimum=%s, current=%s, download=%s", minimum, current, download);
    
    /* cast off the const */
    info.minimum = (char*)minimum;
    info.current = (char*)current;
    info.download = (char*)download;
    hippo_data_cache_set_client_info(connection->cache, &info);
    
    /* FIXME right now this is only on Linux because it's too close to release to 
     * mess with HippoUpgrader, but logic should really be shared. Also I believe
     * the above data cache involvement should be nuked; the new accessors added
     * to HippoConnection and used on Linux should be enough.
     * 
     * But right now Windows uses the data cache stuff and potentially 
     * relies on staying connected while too old.
     */
#ifdef G_OS_UNIX
    if (hippo_compare_versions(VERSION, minimum) < 0) {
        connection->too_old = TRUE;
    }
    if (hippo_compare_versions(VERSION, current) < 0) {
        connection->upgrade_available = TRUE;
    }
    g_free(connection->download_url);
    connection->download_url = g_strdup(download);
#endif /* G_OS_UNIX */
    
    g_signal_emit(G_OBJECT(connection), signals[CLIENT_INFO_AVAILABLE], 0);
    
    if (connection->too_old) {
        /* we sign out rather than connect_failure because we don't want to retry,
         * the user has to exit this process and start a new one first
         */
        hippo_connection_signout(connection);
    } else {
        /* Now fully authenticated */
        hippo_connection_state_change(connection, HIPPO_STATE_AUTHENTICATED);
    }
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

static void
hippo_connection_request_client_info(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    HippoPlatformInfo info;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "clientInfo", NULL);

    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/clientinfo");

    hippo_platform_get_platform_info(connection->platform,
                                     &info);
    
    lm_message_node_set_attribute(child, "platform", info.name);
    if (info.distribution)
        lm_message_node_set_attribute(child, "distribution", info.distribution);
    if (info.version)
        lm_message_node_set_attribute(child, "version", info.version);
    if (info.architecture)
        lm_message_node_set_attribute(child, "architecture", info.architecture);
    
    hippo_connection_send_message_with_reply(connection, message, on_client_info_reply, SEND_MODE_IMMEDIATELY);

    lm_message_unref(message);
}

static LmHandlerResult
on_title_patterns_reply(LmMessageHandler *handler,
                        LmConnection     *lconnection,
                        LmMessage        *message,
                        gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;
    LmMessageNode *subchild;
    GSList *title_patterns = NULL;

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/applications", "titlePatterns")) {
        g_debug("Title patterns reply was wrong thing");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    child = message->node->children;

    for (subchild = child->children; subchild; subchild = subchild->next) {
        const char *app_id;
        const char *value;
        char **patterns, **p;
    
        if (strcmp (subchild->name, "application") != 0)
            continue;

        app_id = lm_message_node_get_attribute(subchild, "appId");
        if (!app_id) {
            g_warning("titlePatterns application node doesn't have an appId attribute");
            continue;
        }
        
        value = lm_message_node_get_value(subchild);
        if (!value)
            continue;

        patterns = g_strsplit(value, ";", -1);
        for (p = patterns; *p; p++) {
            g_strstrip(*p);
            title_patterns = g_slist_prepend(title_patterns, hippo_title_pattern_new(app_id, *p));
        }

        g_strfreev(patterns);
    }

    /* takes ownership */
    hippo_data_cache_set_title_patterns(connection->cache, title_patterns);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_title_patterns(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "titlePatterns", NULL);

    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/applications");

    hippo_connection_send_message_with_reply(connection, message, on_title_patterns_reply, SEND_MODE_IGNORE_IF_DISCONNECTED);

    lm_message_unref(message);
}

gboolean         
hippo_connection_get_contacts_loaded(HippoConnection  *connection)
{
	return connection->contacts_loaded;
}

static LmHandlerResult
on_contacts_reply(LmMessageHandler *handler,
                  LmConnection     *lconnection,
                  LmMessage        *message,
                  gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;
    LmMessageNode *subchild;

    child = message->node->children;
    
    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/contacts", "contacts")) {
        g_debug("Contacts reply was wrong thing");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    g_debug("got contacts reply");
    
    for (subchild = child->children; subchild; subchild = subchild->next) {
        if (!hippo_connection_parse_entity(connection, subchild)) {
            g_warning("failed to parse entity in on_contacts_reply");
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
    }
    
    connection->contacts_loaded = TRUE;
    g_signal_emit(G_OBJECT(connection), signals[CONTACTS_LOADED], 0);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_contacts(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));

    g_debug("requesting contacts");
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "contacts", NULL);

    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/contacts");

    hippo_connection_send_message_with_reply(connection, message, on_contacts_reply, SEND_MODE_IMMEDIATELY);

    lm_message_unref(message);
}

static LmHandlerResult
on_prefs_reply(LmMessageHandler *handler,
               LmConnection     *lconnection,
               LmMessage        *message,
               gpointer          data)
{ 
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *prefs_node = message->node->children;

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/prefs", "prefs")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (prefs_node == NULL || strcmp(prefs_node->name, "prefs") != 0)
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;

    hippo_connection_parse_prefs_node(connection, prefs_node);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_prefs(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "prefs", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/prefs");

    hippo_connection_send_message_with_reply(connection, message, on_prefs_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for prefs");
}

static void
hippo_connection_parse_prefs_node(HippoConnection *connection,
                                  LmMessageNode   *prefs_node)
{
    gboolean music_sharing_enabled = FALSE;
    gboolean saw_music_sharing_enabled = FALSE;
    gboolean music_sharing_primed = TRUE;
    gboolean saw_music_sharing_primed = FALSE;
    gboolean application_usage_enabled = FALSE;
    gboolean saw_application_usage_enabled = FALSE;
    LmMessageNode *child;
    
    g_debug("Parsing prefs message");

    for (child = prefs_node->children; child != NULL; child = child->next) {
        const char *key = lm_message_node_get_attribute(child, "key");
        const char *value = lm_message_node_get_value(child);
        gboolean emit;

        if (key == NULL) {
            g_debug("ignoring node '%s' with no 'key' attribute in prefs reply",
                    child->name);
            continue;
        }
        
        emit = TRUE;
        if (strcmp(key, "musicSharingEnabled") == 0) {
            music_sharing_enabled = value != NULL && parse_bool(value);
            saw_music_sharing_enabled = TRUE;
        } else if (strcmp(key, "musicSharingPrimed") == 0) {
            music_sharing_primed = value != NULL && parse_bool(value);
            saw_music_sharing_primed = TRUE;
        } else if (strcmp(key, "applicationUsageEnabled") == 0) {
            application_usage_enabled = value != NULL && parse_bool(value);
            saw_application_usage_enabled = TRUE;
        } else {
            g_debug("Unknown pref '%s'", key);
            emit = FALSE;
        }
        if (emit)
        	g_signal_emit(G_OBJECT(connection), signals[PREF_CHANGED], 0,
        	              key, parse_bool(value));
    }
    
    /* Important to set primed then enabled, so when the signal is emitted from the 
     * data cache for enabled, primed will already be set.
     */
    if (saw_music_sharing_primed)
        hippo_data_cache_set_music_sharing_primed(connection->cache, music_sharing_primed);
    
    if (saw_music_sharing_enabled)
        hippo_data_cache_set_music_sharing_enabled(connection->cache, music_sharing_enabled);

    if (saw_application_usage_enabled)
        hippo_data_cache_set_application_usage_enabled(connection->cache, application_usage_enabled);
}

static LmHandlerResult
on_get_myspace_name_reply(LmMessageHandler *handler,
                          LmConnection     *lconnection,
                          LmMessage        *message,
                          gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;
    const char *name;
    
    child = message->node->children;

    if (!message_is_iq_with_namespace(message,
                                      "http://dumbhippo.com/protocol/myspace", "mySpaceInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    name = lm_message_node_get_attribute(child, "mySpaceName");

    if (!name) {
        g_warning("getMySpaceName reply missing attributes");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    g_debug("getMySpaceName response: name=%s", name);
    hippo_data_cache_set_myspace_name(connection->cache, name);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_myspace_name(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
            
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "mySpaceInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(child, "type", "getName");

    hippo_connection_send_message_with_reply(connection, message, on_get_myspace_name_reply,
                                             SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for MySpace name");
}

static LmHandlerResult
on_get_myspace_blog_comments_reply(LmMessageHandler *handler,
                                   LmConnection     *lconnection,
                                   LmMessage        *message,
                                   gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;
    LmMessageNode *subchild;
    GSList *comments;
    
    child = message->node->children;

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/myspace", "mySpaceInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    comments = NULL;
    for (subchild = child->children; subchild; subchild = subchild->next) {
        int comment_id;
        int poster_id;
    
        if (strcmp (subchild->name, "comment") != 0)
            continue;

        if (!hippo_xml_split(connection->cache, subchild, NULL,
                             "commentId", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_ELEMENT, &comment_id,
                             "posterId", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_ELEMENT, &poster_id,
                             NULL))
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;

        g_debug("Got myspace comment id %d poster %d\n", comment_id, poster_id);
        
        comments = g_slist_prepend(comments,
                                   hippo_myspace_blog_comment_new(comment_id, poster_id));
    }

    /* takes ownership */
    hippo_data_cache_set_myspace_blog_comments(connection->cache, comments);
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_myspace_blog_comments(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "mySpaceInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(child, "type", "getBlogComments");

    hippo_connection_send_message_with_reply(connection, message, on_get_myspace_blog_comments_reply,
                                             SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for MySpace blog comments");
}

static LmHandlerResult
on_get_myspace_contacts_reply(LmMessageHandler *handler,
                              LmConnection     *lconnection,
                              LmMessage        *message,
                              gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;
    LmMessageNode *subchild;
    GSList *contacts;
    
    child = message->node->children;

    g_debug("got reply for getMySpaceContacts");

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/myspace", "mySpaceInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    contacts = NULL;
    for (subchild = child->children; subchild; subchild = subchild->next) {
        const char *name;
        const char *friend_id;
                
        if (strcmp (subchild->name, "contact") != 0)
            continue;

        if (!hippo_xml_split(connection->cache, subchild, NULL,
                             "name", HIPPO_SPLIT_STRING, &name,
                             "friendID", HIPPO_SPLIT_STRING, &friend_id,
                             NULL))
            continue;
        
        contacts = g_slist_prepend(contacts,
                                   hippo_myspace_contact_new(name, friend_id));
                
        g_debug("got myspace contact '%s'", name);
    }

    /* takes ownership */
    hippo_data_cache_set_myspace_contacts(connection->cache, contacts);
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_myspace_contacts(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "mySpaceInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(child, "type", "getContacts");

    hippo_connection_send_message_with_reply(connection, message,
                                             on_get_myspace_contacts_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for MySpace contacts");
}

void
hippo_connection_add_myspace_comment(HippoConnection *connection,
                                     int              comment_id,
                                     int              poster_id)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *subnode;
    LmMessageNode *prop;
    char *comment_id_str;
    char *poster_id_str;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    subnode = lm_message_node_add_child (node, "addBlogComment", NULL);
    lm_message_node_set_attribute(subnode, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(subnode, "type", "addBlogComment");
    
    prop = lm_message_node_add_child(subnode, "commentId", NULL);
    comment_id_str = g_strdup_printf("%d", comment_id);
    lm_message_node_set_value(prop, comment_id_str);
    g_free(comment_id_str);

    prop = lm_message_node_add_child(subnode, "posterId", NULL);
    poster_id_str = g_strdup_printf("%d", poster_id);
    lm_message_node_set_value(prop, poster_id_str);
    g_free(poster_id_str);

    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);
    lm_message_unref(message);
    g_debug("Sent MySpace comment xmpp message comment_id %d poster_id %d", comment_id, poster_id);
}

void
hippo_connection_notify_myspace_contact_post(HippoConnection *connection,
                                             const char      *myspace_name)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *subnode;

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    subnode = lm_message_node_add_child (node, "notifyContactComment", NULL);
    lm_message_node_set_attribute(subnode, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(subnode, "type", "notifyContactComment");
    lm_message_node_set_attribute(subnode, "name", myspace_name);

    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);
    lm_message_unref(message);
    g_debug("Sent MySpace contact post xmpp message");
}

void
hippo_connection_send_active_applications  (HippoConnection *connection,
                                            int              collection_period,
                                            GSList          *app_ids,
                                            GSList          *wm_classes)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *subnode;
    LmMessageNode *appnode;
    char *period_str;
    GSList *l;

    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    subnode = lm_message_node_add_child (node, "activeApplications", NULL);
    lm_message_node_set_attribute(subnode, "xmlns", "http://dumbhippo.com/protocol/applications");

    period_str = g_strdup_printf("%d", collection_period);
    lm_message_node_set_attribute(subnode, "period", period_str);
    g_free(period_str);

    for (l = app_ids; l; l = l->next) {
        appnode = lm_message_node_add_child (subnode, "application", NULL);
        lm_message_node_set_attribute(appnode, "appId", (char *)l->data);
    }
    
    for (l = wm_classes; l; l = l->next) {
        appnode = lm_message_node_add_child (subnode, "application", NULL);
        lm_message_node_set_attribute(appnode, "wmClass", (char *)l->data);
    }
    
    g_debug("sending active applications");
    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);
    lm_message_unref(message);
}

gint64
hippo_connection_get_server_time_offset(HippoConnection *connection)
{
    return connection->server_time_offset;
}

static void
hippo_connection_update_server_time_offset(HippoConnection *connection,
                                           gint64           server_time)
{
    connection->server_time_offset = server_time - hippo_current_time_ms();
}
 
static void
hippo_connection_update_filter(HippoConnection *connection,
                               const char      *filter)
{
    g_free(connection->active_block_filter);
    connection->active_block_filter = g_strdup(filter);
    g_signal_emit(connection, signals[BLOCK_FILTER_CHANGED], 0, connection->active_block_filter);
}
 
static gboolean
hippo_connection_parse_blocks(HippoConnection *connection,
                              LmMessageNode   *node)
{
    LmMessageNode *subchild;
    const char *filter = NULL;
    gint64 server_timestamp;

    /* g_debug("Parsing blocks list <%s>", node->name); */
    
    if (!hippo_xml_split(connection->cache, node, NULL,
                         "filter", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &filter,
                         "serverTime", HIPPO_SPLIT_TIME_MS, &server_timestamp,
                         NULL)) {
        g_debug("missing serverTime on blocks");
        return FALSE;
    }

    hippo_connection_update_server_time_offset(connection, server_timestamp);
    if (filter)
        hippo_connection_update_filter(connection, filter);
    
    for (subchild = node->children; subchild; subchild = subchild->next) {
        if (!hippo_data_cache_update_from_xml(connection->cache, subchild)) {
            g_debug("Did not successfully update <%s> from xml", subchild->name);
        } else {
            /* g_debug("Updated <%s>", subchild->name) */ ;
        }
    }

    /* g_debug("Done parsing blocks list <%s>", node->name); */
    
    return TRUE;
}

static LmHandlerResult
on_request_blocks_reply(LmMessageHandler *handler,
                        LmConnection     *lconnection,
                        LmMessage        *message,
                        gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;

    child = message->node->children;

    g_debug("got reply for blocks");

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/blocks", "blocks")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (!hippo_connection_parse_blocks(connection, child))
        g_warning("Failed to parse <blocks>");
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_blocks(HippoConnection *connection,
                                gint64           last_timestamp,
                                const char      *filter)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    char *s;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "blocks", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/blocks");
    s = g_strdup_printf("%" G_GINT64_FORMAT, last_timestamp);
    lm_message_node_set_attribute(child, "lastTimestamp", s);
    g_free(s);
    if (filter) {
        lm_message_node_set_attribute(child, "filter", filter);
    }
    if (filter != connection->active_block_filter) {
        g_free(connection->active_block_filter);
        connection->active_block_filter = g_strdup(filter);
    }
    
    hippo_connection_send_message_with_reply(connection, message,
                                             on_request_blocks_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for blocks lastTimestamp %" G_GINT64_FORMAT, last_timestamp);
}

static LmHandlerResult
on_block_hushed_reply(LmMessageHandler *handler,
                      LmConnection     *lconnection,
                      LmMessage        *message,
                      gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;

    child = message->node->children;

    g_debug("got reply for <blockHushed/>");

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/blocks", "blockHushed")) {
        g_warning("Got unexpected reply for <blockHushed/>");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (!hippo_connection_parse_blocks(connection, child))
        g_warning("Failed to parse <blockHushed/>");
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_set_block_hushed(HippoConnection *connection,
                                  const char      *block_id,
                                  gboolean         hushed)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "blockHushed", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/blocks");
    lm_message_node_set_attribute(child, "blockId", block_id);
    lm_message_node_set_attribute(child, "hushed", hushed ? "true" : "false");

    hippo_connection_send_message_with_reply(connection, message, on_block_hushed_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent blockHushed=%d", hushed);
}

void
hippo_connection_send_account_question_response(HippoConnection *connection,
                                                const char      *block_id,
                                                const char      *response)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "response", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/accountQuestion");
    lm_message_node_set_attribute(child, "blockId", block_id);
    lm_message_node_set_attribute(child, "response", response);

    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
}

void
hippo_connection_update_last_blocks_timestamp (HippoConnection *connection,
                                               gint64           timestamp)
{
    if (timestamp >= connection->last_blocks_timestamp) {
        g_debug("Have new latest block timestamp %" G_GINT64_FORMAT, timestamp);
        connection->last_blocks_timestamp = timestamp;
    }
}

static gboolean
get_entity_guid(LmMessageNode *node,
                const char   **guid_p)
{
    const char *attr = lm_message_node_get_attribute(node, "id");
    if (!attr)
        return FALSE;
    *guid_p = attr;
    return TRUE;
}

static gboolean
is_live_post(LmMessageNode *node)
{
    return node_matches(node, "livepost", NULL);
}

static gboolean
hippo_connection_parse_live_post(HippoConnection *connection,
                                 LmMessageNode   *child,
                                 HippoPost      **post_p)
{
    HippoPost *post;
    LmMessageNode *node;
    const char *post_id;
    gboolean seen_self;
    long chatting_user_count;
    long viewing_user_count;
    long total_viewers;
    GSList *viewers;
    
    post = NULL;
    
    post_id = lm_message_node_get_attribute (child, "id");
    if (!post_id)
        goto failed;

    post = hippo_data_cache_lookup_post(connection->cache, post_id);
    if (!post)
        goto failed;
    g_assert(post != NULL);
    g_object_ref(post);

    node = lm_message_node_get_child (child, "recentViewers");
    if (!node)
        goto failed;

    viewers = NULL;
    seen_self = FALSE;

    // At present, we aren't using this information.  For addressed
    // (i.e. shares which have a recipient other than the world)
    // we can always know who's viewing a share from the chat
    // room presence.  For world only shares, the current design
    // is that we only show total viewers.  It's unlikely that
    // someone would know anyone in the random subset of people
    // who just happened to look at a share.  In the future,
    // we might want to have recentViewers include only this
    // user's friends, to make it more interesting.  Probably
    // should rename it to recentFriendViewers.
#if 0
    for (subchild = node->children; subchild; subchild = subchild->next) {
        const char *entity_id;
        HippoEntity *entity;
        
        if (!get_entity_guid(subchild, &entity_id))
            goto failed;
    
        /* username could in theory be NULL if we were processing the queue
         * post-disconnect I believe ... anyway paranoia never hurt anyone
         */
        if (connection->username && strcmp(entity_id, connection->username) == 0)
            seen_self = TRUE;

        /* This entity is supposed to be in the cache; essentially the get posts messages
         * first send a list of all referenced entities with name+photo, then in this node 
         * they only have the guid. So if we don't have the entity here, we haven't 
         * processed the list of referenced entities, or didn't get it. Which is a bug probably.
         */        
        entity = hippo_data_cache_lookup_entity(connection->cache, entity_id);
        if (entity)
            viewers = g_slist_prepend(viewers, entity);
        else
            g_warning("entity '%s' in recentViewers on livePost is unknown", entity_id);
    }
#endif

    node = lm_message_node_get_child (child, "chattingUserCount");
    if (!(node && node->value))
        goto failed;
    chatting_user_count = strtol(node->value, NULL, 10);

    node = lm_message_node_get_child (child, "viewingUserCount");
    if (!(node && node->value))
        goto failed;
    viewing_user_count = strtol(node->value, NULL, 10);

    node = lm_message_node_get_child (child, "totalViewers");
    if (!(node && node->value))
        goto failed;
    total_viewers = strtol(node->value, NULL, 10);

    hippo_post_set_viewers(post, viewers);
    hippo_post_set_chatting_user_count(post, chatting_user_count);
    hippo_post_set_viewing_user_count(post, viewing_user_count);
    hippo_post_set_total_viewers(post, total_viewers);

    if (seen_self)
        hippo_post_set_have_viewed(post, TRUE);

    if (post_p)
        *post_p = post;
    else
        g_object_unref(post);

    return TRUE;
    
 failed:
    if (post)
        g_object_unref(post);
    g_slist_free(viewers);
    return FALSE;
}

static gboolean
is_entity(LmMessageNode *node)
{
    if (strcmp(node->name, "resource") == 0 || strcmp(node->name, "group") == 0
        || strcmp(node->name, "user") == 0 || strcmp(node->name, "feed") == 0)
        return TRUE;
    return FALSE;
}

/* Derive a fallback home location if the server didn't send one
 */
static void
set_fallback_home_url(HippoConnection *connection,
                      HippoEntity     *entity)
{
    HippoEntityType type = hippo_entity_get_entity_type(entity);
    const char *id = hippo_entity_get_guid(entity);
    char *relative;

    if (type == HIPPO_ENTITY_PERSON)
        relative = g_strdup_printf("/person?who=%s", id);
    else if (type == HIPPO_ENTITY_GROUP)
        relative = g_strdup_printf("/group?who=%s", id);
    else {
        return;
    }        

    hippo_entity_set_home_url(entity, relative);

    g_free(relative);
}

static gboolean
hippo_connection_parse_entity(HippoConnection *connection,
                              LmMessageNode   *node)
{
    HippoEntity *entity;
    gboolean created_entity;
    const char *guid;
    const char *name;
    const char *home_url;
    const char *photo_url;
    const char *is_contact;
    
    HippoEntityType type;
    if (strcmp(node->name, "resource") == 0)
        type = HIPPO_ENTITY_RESOURCE;
    else if (strcmp(node->name, "group") == 0)
        type = HIPPO_ENTITY_GROUP;
    else if (strcmp(node->name, "user") == 0)
        type = HIPPO_ENTITY_PERSON;
    else if (strcmp(node->name, "feed") == 0)
        type = HIPPO_ENTITY_FEED;
    else {
        g_warning("entity node lacks entity name");
        return FALSE;
    }

    guid = lm_message_node_get_attribute(node, "id");
    if (!guid) {
        g_warning("entity node lacks guid");
        return FALSE;
    }

    /* Resources generally shouldn't have a NULL name either, but
     * I'll leave the check for now, since the entire function is
     * obsolescent code; hippo_data_cache_update_from_xml() has
     * the more modern logic.
     */
    name = lm_message_node_get_attribute(node, "name");
    if (!name && type != HIPPO_ENTITY_RESOURCE) {
        g_warning("entity node lacks name");
        return FALSE;
    }

    home_url = lm_message_node_get_attribute(node, "homeUrl");

    if (type != HIPPO_ENTITY_RESOURCE) {
        photo_url = lm_message_node_get_attribute(node, "photoUrl");
        if (!photo_url)
            photo_url = lm_message_node_get_attribute(node, "smallPhotoUrl"); /* legacy attribute name */
        if (!photo_url) {
            g_warning("entity node guid='%s' name='%s' lacks photo url", guid, name);
            return FALSE;
        }
    } else {
        photo_url = NULL;
    }

    is_contact = lm_message_node_get_attribute(node, "isContact");
    
    entity = hippo_data_cache_lookup_entity(connection->cache, guid);
    if (entity == NULL) {
        created_entity = TRUE;
        entity = hippo_entity_new(type, guid);
    } else {
        created_entity = FALSE;
        g_object_ref(entity);
    }

    hippo_entity_set_name(entity, name);
    
    if (home_url) {
        hippo_entity_set_home_url(entity, home_url);
    } else {
        set_fallback_home_url(connection, entity);
    }
    hippo_entity_set_photo_url(entity, photo_url);

    /* old servers don't supply is_contact; even newer servers don't always supply it (only if they asked for the 'PersonViewExtra') */
    if (is_contact)
        hippo_entity_set_in_network(entity, strcmp(is_contact, "true") == 0);
        
    if (created_entity) {
        hippo_data_cache_add_entity(connection->cache, entity);
    }
    g_object_unref(entity);
   
    return TRUE;
}

static gboolean
is_post(LmMessageNode *node)
{
    return node_matches(node, "post", NULL);
}

static gboolean
hippo_connection_parse_post(HippoConnection *connection,
                            LmMessageNode   *post_node,
                            gboolean         is_new,
                            HippoPost      **post_return)
{
    LmMessageNode *recipients_node;
    HippoPost *post;
    const char *post_guid;
    const char *sender_guid;
    const char *url;
    const char *title;
    const char *text = NULL;
    const char *info =  NULL;
    gboolean is_public = FALSE;
    gboolean ignored = FALSE;
    gboolean viewed = FALSE;
    GTime post_date;
    int post_date_int;
    GSList *recipients = NULL;
    LmMessageNode *subchild;
    gboolean created_post;
    
    g_assert(connection->cache != NULL);

    if (!hippo_xml_split(connection->cache, post_node, NULL,
                         "id", HIPPO_SPLIT_GUID, &post_guid,
                         "poster", HIPPO_SPLIT_GUID | HIPPO_SPLIT_ELEMENT, &sender_guid,
                         "href", HIPPO_SPLIT_URI_ABSOLUTE | HIPPO_SPLIT_ELEMENT, &url,
                         "title", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &title,
                         "text", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &text,
                         "postInfo", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &info,
                         "isPublic", HIPPO_SPLIT_BOOLEAN | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &is_public,
                         "postDate", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_ELEMENT, &post_date_int,
                         "recipients", HIPPO_SPLIT_NODE | HIPPO_SPLIT_ELEMENT, &recipients_node,
                         "viewed", HIPPO_SPLIT_BOOLEAN | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &viewed,
                         "ignored", HIPPO_SPLIT_BOOLEAN | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &ignored,
                         NULL))
        return FALSE;

    if (text == NULL)
        text = "";
    
    /* FIXME: this deviates from our practice elsewhere, where we use gint64 ms */
    post_date = post_date_int;

    for (subchild = recipients_node->children; subchild; subchild = subchild->next) {
        const char *entity_id;
        HippoEntity *entity;
        if (!get_entity_guid(subchild, &entity_id))
            return FALSE;

        /* This entity is supposed to be in the cache; essentially the get posts messages
         * first send a list of all referenced entities with name+photo, then in this node 
         * they only have the guid. So if we don't have the entity here, we haven't 
         * processed the list of referenced entities, or didn't get it. Which is a bug probably.
         */        
        entity = hippo_data_cache_lookup_entity(connection->cache, entity_id);
        if (entity)
            recipients = g_slist_prepend(recipients, entity);
        else
            g_warning("Post receipient '%s' was not in the cache prior to post message", entity_id);
    }

    post = hippo_data_cache_lookup_post(connection->cache, post_guid);
    if (post == NULL) {
        post = hippo_post_new(post_guid);
        created_post = TRUE;
    } else {
        g_object_ref(post);
        created_post = FALSE;
    }
    g_assert(post != NULL);

    g_debug("Parsed post %s new = %d", post_guid, is_new);

    hippo_post_set_new(post, is_new);
    hippo_post_set_sender(post, sender_guid);
    hippo_post_set_url(post, url);
    hippo_post_set_title(post, title);
    hippo_post_set_description(post, text);
    hippo_post_set_info(post, info);
    hippo_post_set_date(post, post_date);
    hippo_post_set_recipients(post, recipients);
    hippo_post_set_ignored(post, ignored);
    hippo_post_set_public(post, is_public);
    hippo_post_set_have_viewed(post, viewed);

    g_slist_free(recipients);

    if (created_post) {
        /* As a side effect, this will start filling in the post's chatroom */
        hippo_data_cache_add_post(connection->cache, post);
    }
    
    g_object_unref(post);
    if (post_return)
        *post_return = post;

    return TRUE;
}

static gboolean
hippo_connection_parse_post_data_full(HippoConnection *connection,
                                      LmMessageNode   *node,
                                      gboolean         is_new,
                                      const char      *func_name,
                                      HippoPost      **post_return)
{
    gboolean seen_post;
    gboolean seen_live_post;
    LmMessageNode *subchild;
    
    seen_post = FALSE;
    seen_live_post = FALSE;

    for (subchild = node->children; subchild; subchild = subchild->next) {
        if (is_entity(subchild)) {
            if (!hippo_connection_parse_entity(connection, subchild)) {
                g_warning("failed to parse entity in %s", func_name);
                return FALSE;
            }
        } else if (is_post(subchild)) {
            if (seen_post) {
                g_warning("More than one <post/> child in %s", func_name);
                return FALSE;
            }

            if (!hippo_connection_parse_post(connection, subchild, is_new, post_return)) {
                g_warning("failed to parse post in %s", func_name);
                return FALSE;
            }
            seen_post = TRUE;
        } else if (is_live_post(subchild)) {
            if (!seen_post) {
                g_warning("<livepost/> before <post/> in %s", func_name);
                return FALSE;
            }
            if (seen_live_post) {
                g_warning("More than one <livepost/> child in %s", func_name);
                return FALSE;
            }

            if (!hippo_connection_parse_live_post(connection, subchild, NULL)) {
                g_warning("failed to parse live post in %s", func_name);
                return FALSE;
            }
            seen_live_post = TRUE;
        }
    }

    return TRUE;
}


static gboolean
hippo_connection_parse_post_data(HippoConnection *connection,
                                 LmMessageNode   *node,
                                 gboolean         is_new,
                                 const char      *func_name)
{
    return hippo_connection_parse_post_data_full(connection, node, is_new, func_name, NULL);
}

static void 
send_room_presence(HippoConnection *connection,
                   HippoChatRoom   *room,
                   LmMessageSubType subtype,
                   HippoChatState   state)
{
    const char *to;
    LmMessage *message;
    
    if (state == HIPPO_CHAT_STATE_NONMEMBER)
        return;

    to = hippo_chat_room_get_jabber_id(room);
    
    message = lm_message_new_with_sub_type(to, LM_MESSAGE_TYPE_PRESENCE, subtype);

    if (subtype == LM_MESSAGE_SUB_TYPE_AVAILABLE) {
        LmMessageNode *x_node;
        LmMessageNode *user_info_node;
        
        x_node = lm_message_node_add_child(message->node, "x", NULL);
        
        lm_message_node_set_attribute(x_node, "xmlns", "http://jabber.org/protocol/muc");

        user_info_node = lm_message_node_add_child(x_node, "userInfo", NULL);
        lm_message_node_set_attribute(user_info_node, "xmlns", "http://dumbhippo.com/protocol/rooms");
        lm_message_node_set_attribute(user_info_node, "role",
                                      state == HIPPO_CHAT_STATE_PARTICIPANT ? "participant" : "visitor");
    }

    hippo_connection_send_message(connection, message, SEND_MODE_IGNORE_IF_DISCONNECTED);

    lm_message_unref(message);
}

void
hippo_connection_send_chat_room_enter(HippoConnection *connection,
                                      HippoChatRoom   *room,
                                      HippoChatState   state)
{
    send_room_presence(connection, room, LM_MESSAGE_SUB_TYPE_AVAILABLE,
                       state);
}

void
hippo_connection_send_chat_room_leave(HippoConnection *connection,
                                      HippoChatRoom   *room)
{
    send_room_presence(connection, room, LM_MESSAGE_SUB_TYPE_UNAVAILABLE,
                       HIPPO_CHAT_STATE_PARTICIPANT);
}

void
hippo_connection_send_chat_room_state(HippoConnection *connection,
                                      HippoChatRoom   *room,
                                      HippoChatState   old_state,
                                      HippoChatState   new_state)
{    
    if (old_state == new_state)
        return;
    
    if (old_state == HIPPO_CHAT_STATE_NONMEMBER) {
        hippo_connection_send_chat_room_enter(connection, room, new_state);
    } else if (new_state == HIPPO_CHAT_STATE_NONMEMBER) {
        hippo_connection_send_chat_room_leave(connection, room);

        /* once we've left the chat room, we aren't updated on the contents, so
         * clear everything. We *could* leave the messages around since they 
         * shouldn't change, but there's not much point, and could result
         * in gaps, where we have messages from a week ago, but not from
         * yesterday, since the server only sends a limited history on
         * reconnect. Plus, discarding everything saves memory.
         */
        hippo_chat_room_clear(room);
    } else {
        /* Change from Visitor => Participant or vice-versa */
        hippo_connection_send_chat_room_enter(connection, room, new_state);
    }
}

static void
join_or_leave_chat_room(HippoConnection *connection,
                        HippoChatRoom   *room,
                        HippoChatState   state,
                        gboolean         join)
{
    HippoChatState old_state;
    HippoChatState new_state;

    old_state = hippo_chat_room_get_desired_state(room);
    
    if (join)
        hippo_chat_room_increment_state_count(room, state);
    else
        hippo_chat_room_decrement_state_count(room, state);
    
    new_state = hippo_chat_room_get_desired_state(room);

    hippo_connection_send_chat_room_state(connection, room, old_state, new_state);
}

void
hippo_connection_join_chat_room(HippoConnection *connection,
                                HippoChatRoom   *room,
                                HippoChatState   desired_state)
{
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    join_or_leave_chat_room(connection, room, desired_state, TRUE);
}

void
hippo_connection_leave_chat_room(HippoConnection *connection,
                                 HippoChatRoom   *room,
                                 HippoChatState   state_joined_with)
{
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    join_or_leave_chat_room(connection, room, state_joined_with, FALSE);
}

void
hippo_connection_rejoin_chat_room(HippoConnection *connection,
                                  HippoChatRoom   *room)
{
    HippoPerson *self;
    HippoChatState desired_state;

    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    self = hippo_data_cache_get_self(connection->cache);
    g_return_if_fail(self != NULL);

    /* when we get disconnected from the server, we don't clear the old state immediately, but
     * immediately, but wait until wereconnect 
     */
    hippo_chat_room_clear(room);

    /* but we preserved our "join count" so we know if we wanted
     * to be in the room
     */
    desired_state = hippo_chat_room_get_desired_state(room);

    hippo_connection_send_chat_room_state(connection, room, HIPPO_CHAT_STATE_NONMEMBER, desired_state);
}

static void
send_chat_room_message(HippoConnection *connection,
                       const char      *to,
                       const char      *text,
                       HippoSentiment   sentiment)
{
    LmMessage *message;
    LmMessageNode *body;
        
    message = lm_message_new(to, LM_MESSAGE_TYPE_MESSAGE);

    body = lm_message_node_add_child(message->node, "body", text);

    if (sentiment != HIPPO_SENTIMENT_INDIFFERENT) {
        LmMessageNode *child = lm_message_node_add_child(message->node, "messageInfo", NULL);
        lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/rooms");
        lm_message_node_set_attribute(child, "sentiment", hippo_sentiment_as_string(sentiment));
    }
    
    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
}

void
hippo_connection_send_chat_room_message(HippoConnection *connection,
                                        HippoChatRoom   *room,
                                        const char      *text,
                                        HippoSentiment   sentiment)
{
    const char *to;
        
    to = hippo_chat_room_get_jabber_id(room);
    send_chat_room_message(connection, to, text, sentiment);
}

void
hippo_connection_send_quip(HippoConnection *connection,
                           HippoChatKind    kind,
                           const char      *id,
                           const char      *text,
                           HippoSentiment   sentiment)
{
    char *node, *to;
    
    node = hippo_id_to_jabber_id(id);
    to = g_strconcat(node, "@" HIPPO_ROOMS_JID_DOMAIN, NULL);
    g_free(node);

    send_chat_room_message(connection, to, text, sentiment);

    g_free(to);
}

static gboolean
parse_room_jid(const char *jid,
               char      **chat_id_p,
               char      **user_id_p)
{
    const char *at;
    const char *slash;
    char *room_name;
 
    *chat_id_p = NULL;
    *user_id_p = NULL;
   
    at = strchr(jid, '@');
    if (!at)
        return FALSE;

    slash = strchr(at + 1, '/');
    if (!slash)
        slash = (at + 1) + strlen(at + 1);
        
    if (strncmp(at + 1, HIPPO_ROOMS_JID_DOMAIN, slash - (at + 1)) != 0)
        return FALSE;

    room_name = g_strndup(jid, at - jid);
    *chat_id_p = hippo_id_from_jabber_id(room_name);
    g_free(room_name);

    if (*chat_id_p == NULL)
        return FALSE;

    /* *slash == '\0' if there was no slash */
    if (*slash == '/') {
        *user_id_p = hippo_id_from_jabber_id(slash + 1);
        if (*user_id_p == NULL) {
            g_free(*chat_id_p);
            *chat_id_p = NULL;
            return FALSE;
        }
    }

    return TRUE;
}

/* Return FALSE if there's an _error_ - missing roomInfo is not necessarily */
static gboolean
parse_room_info(HippoConnection *connection, 
                LmMessage       *message,
                const char      *chat_id,
                HippoChatKind   *kind_p)
{
    HippoChatRoom *room;
    HippoChatKind existing_kind;
    HippoChatKind kind;
    const char *kind_str;
    LmMessageNode *info_node;
    LmMessageNode *child;

    if (kind_p)
        *kind_p = HIPPO_CHAT_KIND_UNKNOWN;

    info_node = find_child_node(message->node, "http://dumbhippo.com/protocol/rooms", "roomInfo");
    if (!info_node) {
        return TRUE; /* not an error */
    }

    kind_str = lm_message_node_get_attribute(info_node, "kind");
    if (kind_str == NULL) {
        /* assume it's an old server which lacked "kind" but was always about posts */
        kind = HIPPO_CHAT_KIND_POST;
    } else {
        kind = hippo_parse_chat_kind(kind_str);
        if (kind == HIPPO_CHAT_KIND_BROKEN || kind == HIPPO_CHAT_KIND_UNKNOWN) {
            g_warning("Invalid chat kind %s", kind_str);
            return FALSE;
        }
    }

    /* Look for the object associated with this room */
    for (child = info_node->children; child; child = child->next) {
        if (strcmp (child->name, "objects") == 0 && child->children) {
            if (kind == HIPPO_CHAT_KIND_GROUP)
                hippo_connection_parse_entity(connection, child->children);
            else if (kind == HIPPO_CHAT_KIND_POST)
                hippo_connection_parse_post_data(connection, child, FALSE, "parseRoomInfo");
        }
    }

    room = hippo_data_cache_lookup_chat_room(connection->cache, chat_id, &existing_kind);
    
    if (room) {
        if (existing_kind != HIPPO_CHAT_KIND_UNKNOWN && existing_kind != kind) {
            g_warning("confusion about kind of room %s, giving up", chat_id);
            return FALSE;
        } else if (existing_kind == HIPPO_CHAT_KIND_UNKNOWN) {
            hippo_chat_room_set_kind(room, kind);
        }
                    
        if (kind_p)
            *kind_p = hippo_chat_room_get_kind(room);
    } else {
        if (kind_p)
            *kind_p = kind;
    }
    
    return TRUE;
}            

static void
hippo_connection_parse_settings_node(HippoConnection *connection,
                                     LmMessageNode   *settings_node)
{
    LmMessageNode *child;
    
    for (child = settings_node->children; child != NULL; child = child->next) {
        const char *key = lm_message_node_get_attribute(child, "key");
        const char *unset = lm_message_node_get_attribute(child, "unset");
        const char *value = lm_message_node_get_value(child);
        
        if (key == NULL) {
            g_debug("ignoring node '%s' with no 'key' attribute in settings reply",
                    child->name);
            continue;
        }

        if (unset && strcmp(unset, "true") == 0) {
            value = NULL;
        } else if (value == NULL) {
            /* loudmouth tends to convert empty string to NULL for contentless nodes */
            value = "";
        }

        g_signal_emit(G_OBJECT(connection), signals[SETTING_CHANGED], 0, key, value);
    }
}

static LmHandlerResult
on_desktop_settings_reply(LmMessageHandler *handler,
                          LmConnection     *lconnection,
                          LmMessage        *message,
                          gpointer          data)
{ 
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *settings_node = message->node->children;
    
    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/settings", "settings")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (settings_node == NULL || strcmp(settings_node->name, "settings") != 0)
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;

    hippo_connection_parse_settings_node(connection, settings_node);

    /* FIXME this should really only be emitted if we asked for all settings, not every time
     * we ask for a single setting, but too annoying to do that for now
     */
    g_signal_emit(G_OBJECT(connection), signals[SETTINGS_LOADED], 0);
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_desktop_settings(HippoConnection *connection)
{
    hippo_connection_request_desktop_setting(connection, NULL);
}

void
hippo_connection_request_desktop_setting(HippoConnection *connection,
                                          const char      *key)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "settings", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/settings");

    /* key means just get the one value, no key means get all settings (expensive) */
    if (key != NULL)
        lm_message_node_set_attribute(child, "key", key);
    
    hippo_connection_send_message_with_reply(connection, message, on_desktop_settings_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for desktop setting (key is %s)", key ? key : "(null, getting all of them)");
}

static LmHandlerResult
on_desktop_setting_reply(LmMessageHandler *handler,
                         LmConnection     *lconnection,
                         LmMessage        *message,
                         gpointer          data)
{
    /* HippoConnection *connection = HIPPO_CONNECTION(data); */
    LmMessageNode *child;

    child = message->node->children;

    g_debug("got reply for <setting/> a new value");

#if 0
    /* the reply has nothing in it right now (no <setting> element) */
    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/settings", "setting")) {
        g_warning("Got wrong type of reply for <setting/> a new value");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
#endif

    /* Nothing we really need to do with the reply, it's just an ACK */
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_send_desktop_setting (HippoConnection *connection,
                                       const char      *key,
                                       const char      *value)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "setting", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/settings");
    lm_message_node_set_attribute(child, "key", key);

    if (value != NULL)
        lm_message_node_set_value(child, value);
    else
        lm_message_node_set_attribute(child, "unset", "true");

    hippo_connection_send_message_with_reply(connection, message, on_desktop_setting_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent setting %s=%s", key, value);
}


static void
hippo_connection_parse_whereim_node(HippoConnection *connection,
                                    LmMessageNode   *node)
{
    LmMessageNode *child;
    
    for (child = node->children; child != NULL; child = child->next) {
        HippoExternalAccount *acct;
        
        acct = hippo_external_account_new_from_xml(connection->cache, child);

        if (acct) {
            g_signal_emit(G_OBJECT(connection), signals[WHEREIM_CHANGED], 0, acct);
                
            g_object_unref(acct);
        }
    }
}

static LmHandlerResult
on_whereim_reply(LmMessageHandler *handler,
                 LmConnection     *lconnection,
                 LmMessage        *message,
                 gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *node = message->node->children;
    
    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/whereim", "whereim")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (node == NULL || strcmp(node->name, "whereim") != 0)
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;

    hippo_connection_parse_whereim_node(connection, node);
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_mugshot_whereim(HippoConnection *connection)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "whereim", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/whereim");
    
    hippo_connection_send_message_with_reply(connection, message, on_whereim_reply, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);

    g_debug("Sent request for whereim");        
}

static gboolean
parse_chat_user_info(HippoConnection *connection,
                     LmMessageNode   *parent,
                     HippoPerson     *person,
                     HippoChatState  *status_p,
                     gboolean        *newly_joined_p)
{
    LmMessageNode *info_node;
    
    info_node = find_child_node(parent, "http://dumbhippo.com/protocol/rooms", "userInfo");
    if (!info_node) {
        g_debug("Can't find userInfo node");
        return FALSE;
    }

    {
        HippoChatState status;
        const char *name;
        const char *photo_url = NULL;
        const char *role = NULL;
        const char *old_role = NULL;

        if (!hippo_xml_split(connection->cache, info_node, NULL,
                             "name", HIPPO_SPLIT_STRING, &name,
                             "smallPhotoUrl", HIPPO_SPLIT_URI_RELATIVE | HIPPO_SPLIT_OPTIONAL, &photo_url,
                             "role", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &role,
                             NULL))
            return FALSE;

        if (!role)
            status = HIPPO_CHAT_STATE_PARTICIPANT;
        else
            status = strcmp(role, "participant") == 0 ? HIPPO_CHAT_STATE_PARTICIPANT : HIPPO_CHAT_STATE_VISITOR;

        *status_p = status;
        *newly_joined_p = old_role && strcmp(old_role, "nonmember") == 0;
    
        hippo_entity_set_name(HIPPO_ENTITY(person), name);
        /* FIXME this is a temporary hack to deal with stale photos in chat */
        if (hippo_entity_get_photo_url(HIPPO_ENTITY(person)) == NULL)
            hippo_entity_set_photo_url(HIPPO_ENTITY(person), photo_url);
    }

    return TRUE;
}

static HippoChatMessage*
parse_chat_message_info(HippoConnection  *connection, 
                        LmMessageNode    *parent,
                        HippoPerson      *sender,
                        const char       *text,
                        const char      **name_p,
                        const char      **photo_url_p)
{
    LmMessageNode *info_node;
    
    info_node = find_child_node(parent, "http://dumbhippo.com/protocol/rooms", "messageInfo");
    if (!info_node) {
        g_debug("Can't find messageInfo node");
        return FALSE;
    }

    {
        const char *name_str;
        const char *photo_url;
        gint64 timestamp_milliseconds;
        GTime timestamp;
        int serial;
        const char *sentiment_str = NULL;
        HippoSentiment sentiment = HIPPO_SENTIMENT_INDIFFERENT;

        if (!hippo_xml_split(connection->cache, info_node, NULL,
                             "name", HIPPO_SPLIT_STRING, &name_str,
                             "sentiment", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &sentiment_str,
                             "smallPhotoUrl", HIPPO_SPLIT_URI_RELATIVE, &photo_url,
                             "timestamp", HIPPO_SPLIT_TIME_MS, &timestamp_milliseconds,
                             "serial", HIPPO_SPLIT_INT32, &serial,
                             NULL))
            return NULL;

        if (sentiment_str && !hippo_parse_sentiment(sentiment_str, &sentiment))
            return NULL;

        *name_p = name_str;
        *photo_url_p = photo_url;

        timestamp = (GTime) (timestamp_milliseconds / 1000);

        return hippo_chat_message_new(sender, text, sentiment, timestamp, serial);
    }
}

static void
process_room_chat_message(HippoConnection *connection,
                          LmMessage       *message,
                          HippoChatRoom   *room,
                          const char      *user_id)
{
    const char *text;
    LmMessageNode *body_node;
    const char *name;
    const char *photo_url;
    HippoChatMessage *chat_message;
    HippoEntity *sender;
    
    body_node = lm_message_node_find_child(message->node, "body");
    if (body_node)
        text = lm_message_node_get_value(body_node);
    else
        text = NULL;

    if (!text) {
        g_debug("Chat room message without body");
        return;
    }

    sender = hippo_data_cache_ensure_bare_entity(connection->cache, HIPPO_ENTITY_PERSON, user_id);

    chat_message = parse_chat_message_info(connection, message->node, HIPPO_PERSON(sender),
                                           text, &name, &photo_url);
    if (chat_message == NULL)
        return;

    /* update new info about the user */
    hippo_entity_set_name(sender, name);
    /* FIXME hack since chat has stale info for now */
    if (hippo_entity_get_photo_url(sender) == NULL)
        hippo_entity_set_photo_url(sender, photo_url);

    /* We can usually skip this in the case where the message was pending - but
     * it's tricky to get it exactly right. See comments in handleRoomPresence().
     * Unlike presence, it's harmless to add the message again since we catch
     * duplicate serials and ignore them.
     */
    /* Note, this passes ownership of the chat message and potentially 
     * frees it immediately if it's a dup
     */
    hippo_chat_room_add_message(room, chat_message);
}

static void
process_room_presence(HippoConnection *connection,
                      LmMessage       *message,
                      HippoChatRoom   *room,
                      const char      *user_id)
{
    LmMessageSubType subtype;
    HippoPost *post;
    gboolean is_self;
    
    subtype = lm_message_get_sub_type(message);

    /* remember that the chat may not be about a post in which case this
     * will be null (can also be null if we just have never heard of this post?)
     */
    post = hippo_data_cache_lookup_post(connection->cache, hippo_chat_room_get_id(room));

    is_self = (connection->username && strcmp(connection->username, user_id) == 0);

    /* FIXME: If we get pend a chat room presence while getting the details of the 
     * chatroom, then we'll have a more recent view of the chatroom's viewer set than that
     * the message, so we don't want to update the chatroom, we just want to 
     * notify the user. We should skip even notifying the user in the case where someone
     * started viewing and then left before we got around to notifying
     *
     * The tricky bit here is if we had the chatroom around (the user had joined
     * it by navigating to the post through /home, say), but not the post, then
     * we still need to update the chatroom despite wasPending. So, we really
     * need to keep more information around about the history of the message
     */

    if (subtype == LM_MESSAGE_SUB_TYPE_AVAILABLE) {
        LmMessageNode *x_node;
        HippoChatState status;
        gboolean newly_joined;
        HippoEntity *entity;
        HippoPerson *person;
        gboolean created_entity;
        
        x_node = find_child_node(message->node, "http://jabber.org/protocol/muc#user", "x");
        if (!x_node) {
            g_debug("Presence without x child");
            return;
        }

        entity = hippo_data_cache_lookup_entity(connection->cache, user_id);
        if (entity == NULL) {
            entity = hippo_entity_new(HIPPO_ENTITY_PERSON, user_id);
            created_entity = TRUE;
        } else {
            created_entity = FALSE;
            g_object_ref(entity);
        }
        if (!HIPPO_IS_PERSON(entity)) {
            g_warning("not a person entity corresponding to user_id");
            return;
        }
        person = HIPPO_PERSON(entity);

        if (!parse_chat_user_info(connection, x_node, person, &status, &newly_joined))
            return;
        if (created_entity) {
            hippo_data_cache_add_entity(connection->cache, entity);
        }
        g_object_unref(entity);

        /* add them to chat room or update their state in chat room */
        hippo_chat_room_set_user_state(room, person, status);

        if (is_self) {
            if (post)
                hippo_post_set_have_viewed(post, TRUE);
        }

        /* The obvious algorithm to tell whether we should notify the user about this member: 
         * was the user in room's list of users before doesn't work because of the case:
         *
         *   - Receive a message saying the user is newly joined, pend it
         *   - Get the information about all the users (including the newly joined one)
         *   - Process the pending message
         *
         * So instead, we go off the oldRole attribute in the <presence/> message which is 
         * there for exactly this purpose... to distinguish "interesting" presence transitions
         * like NONMEMBER => VISITOR from uninteresting ones like PARTICIPANT => VISITOR
         */
        /* FIXME this should get replaced by signal handling... which I tried to
           do, but maybe it isn't quite right since I'm not sure I fully grokked the above
           comment.

           if (post && !room->getFilling()) {
           if (room->getState() == HippoChatRoom::NONMEMBER && newlyJoined)
           ui_->onViewerJoin(post);
           else
           ui_->updatePost(post);
           }
        */

    } else if (subtype == LM_MESSAGE_SUB_TYPE_UNAVAILABLE) {
        HippoEntity *entity;
        
        entity = hippo_data_cache_lookup_entity(connection->cache, user_id);
        if (entity != NULL) {
            hippo_chat_room_set_user_state(room, HIPPO_PERSON(entity), HIPPO_CHAT_STATE_NONMEMBER);
        }
    }
}

static ProcessMessageResult
process_room_message(HippoConnection *connection,
                     LmMessage       *message)
{
    /* this could be a chat message or a presence notification */

    ProcessMessageResult result;
    const char *from;
    char *chat_id;
    char *user_id;
    HippoChatRoom *room;
    HippoChatKind kind;
    LmMessageNode *child;
    gboolean is_history_message;

    chat_id = NULL;
    user_id = NULL;

    /* IGNORE = run other handlers CONSUME = we handled it
     * PEND = save for after we fill chatroom
     */
    result = PROCESS_MESSAGE_IGNORE;

    from = lm_message_node_get_attribute(message->node, "from");

    if (!from || !parse_room_jid(from, &chat_id, &user_id)) {
        g_debug("Failed to parse room ID, probably not a chat-related message");
        goto out;
    }    
 
    g_debug("hippo-connection::process_room_message Chat id is %s", chat_id);
    /* We only use this for "spontaneous" chat messages right now, 
     * since otherwise we have the kind from get_chat_room_details reply
     */    
    if (!parse_room_info(connection, message, chat_id, &kind)) {
        /* the roomInfo was somehow broken */
        g_debug("Broken roomInfo in chat message");
        goto out;
    }
    
    room = hippo_data_cache_lookup_chat_room(connection->cache, chat_id, NULL);
    
    if (!room) {
        // Just ignore spontaneous messages; old versions of the server sent them
        // for notifications we now do via the block system
        result = PROCESS_MESSAGE_CONSUME;
        goto out;
    }

    is_history_message = FALSE;
    for (child = message->node->children; child != NULL; child = child->next) {
        if (node_matches(child, "x", "jabber:x:delay")) {
            is_history_message = TRUE;
            break;
        }
    }

    if (lm_message_get_type(message) == LM_MESSAGE_TYPE_MESSAGE) {
        g_debug("hippo-connection::process_room_message processing room message");
        process_room_chat_message(connection, message, room, user_id);
    } else if (lm_message_get_type(message) == LM_MESSAGE_TYPE_PRESENCE) {
        g_debug("hippo-connection::process_room_message processing room presence");
        process_room_presence(connection, message, room, user_id);
    } else {
        g_debug("hippo-connection::process_room_message unknown message type");
    }

    result = PROCESS_MESSAGE_CONSUME;
    
 out:
  
    g_free(chat_id);
    g_free(user_id);
    
    return result;
}

/* === HippoConnection Loudmouth handlers === */

static gboolean
handle_setting_changed(HippoConnection *connection,
                       LmMessage       *message)
{
    LmMessageNode *child;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/settings", "settings");
    if (child == NULL)
        return FALSE;
    g_debug("handling settings changed message");

    hippo_connection_parse_settings_node(connection, child);

    return TRUE;
}

static gboolean
handle_blocks_changed(HippoConnection *connection,
                      LmMessage       *message)
{
    LmMessageNode *child;
    gint64 last_timestamp;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/blocks", "blocksChanged");
    if (child == NULL)
        return FALSE;

    if (!hippo_xml_split(connection->cache, child, NULL,
                         "lastTimestamp", HIPPO_SPLIT_TIME_MS, &last_timestamp,
                         NULL))
        return TRUE;
    
    g_debug("handling blocksChanged message timestamp %" G_GINT64_FORMAT " our latest timestamp %" G_GINT64_FORMAT,
            last_timestamp, connection->last_blocks_timestamp);

    /* last_timestamp of -1 means the server has lost track of what the latest timestamp
     * is, but something has potentially changed
     */
    if (last_timestamp < 0 || last_timestamp > connection->last_blocks_timestamp)
        hippo_connection_queue_request_blocks(connection);
    
    return TRUE;
}

static gboolean
handle_live_post_changed(HippoConnection *connection,
                         LmMessage       *message)
{
    LmMessageNode *child;
    HippoPost *post;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE
        && lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_NOT_SET)
        return FALSE;

    child = find_child_node(message->node,
                            "http://dumbhippo.com/protocol/post", "livePostChanged");
    if (child == NULL)
        return FALSE;   

    g_debug("handling livePostChanged message");

    post = NULL;
    if (!hippo_connection_parse_post_data_full(connection, child, FALSE, "livePostChanged", &post)) {
        g_warning("failed to parse post stream from livePostChanged");
        return TRUE; /* still handled, just busted */
    }
    g_signal_emit(connection, signals[POST_ACTIVITY], 0, post);

    /* We don't display any information from the link message currently -- the bubbling
     * up when viewers are added comes from the separate "chat room" path, so just
     * suppress things here. There's some work later to rip out this path, assuming that
     * we actually don't ever need to update the bubble display from livePostChanged
     * ui_->onLinkMessage(post, FALSE);
     */

    return TRUE;
}

static gboolean
handle_active_posts_changed(HippoConnection *connection,
                            LmMessage       *message)
{
    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NOT_SET) {
        LmMessageNode *child;
        LmMessageNode *subchild;
        GSList *active_posts;    
        
        child = find_child_node(message->node, "http://dumbhippo.com/protocol/post", "activePostsChanged");
        if (!child)
            return FALSE;
            
        g_debug("handling activePostsChanged message");
        
        active_posts = NULL;
        
        for (subchild = child->children; subchild; subchild = subchild->next) {
            if (is_entity(subchild)) {
                if (!hippo_connection_parse_entity(connection, subchild)) {
                    g_warning("failed to parse entity in activePostsChanged");
                }
            } else if (is_post(subchild)) {
                if (!hippo_connection_parse_post(connection, subchild, FALSE, NULL)) {
                    g_warning("failed to parse post in activePostsChanged");
                }
                /* The ordering is important here - we expect the post node to come first,
                 * when the live post data is seen we add it
                 */
                continue;
            } else if (is_live_post(subchild)) {
                HippoPost *post;
                
                if (!hippo_connection_parse_live_post(connection, subchild, &post)) {
                    g_warning("failed to parse live post in activePostsChanged");
                } else {
                    g_assert(post != NULL);
                    g_assert(HIPPO_IS_POST(post));
                    active_posts = g_slist_prepend(active_posts, post);
                }
                continue;
            }
        }

        /* Keep the server's order */        
        active_posts = g_slist_reverse(active_posts);
        
        /* Set on the data cache, emitting active-posts-changed signal */
        hippo_data_cache_set_active_posts(connection->cache, active_posts);

        /* free it all */
        g_slist_foreach(active_posts, (GFunc) g_object_unref, NULL);
        g_slist_free(active_posts);
        
        return TRUE;
    } else {
        return FALSE;
    }
}

static gboolean
handle_prefs_changed(HippoConnection *connection,
                     LmMessage       *message)
{
    LmMessageNode *child;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/prefs", "prefs");
    if (child == NULL)
        return FALSE;
    g_debug("handling prefsChanged message");

    hippo_connection_parse_prefs_node(connection, child);

    return TRUE;
}

static gboolean
handle_entities_changed(HippoConnection *connection,
                        LmMessage       *message)
{
    LmMessageNode *child, *subchild;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/entity", "entitiesChanged");
    if (child == NULL)
        return FALSE;
    g_debug("handling entities changed message");

	for (subchild = child->children; subchild; subchild = subchild->next) {
        if (!hippo_data_cache_update_from_xml(connection->cache, subchild)) {
            g_debug("Did not successfully update <%s> from xml", subchild->name);
        }
    }

    return TRUE;
}

static gboolean
handle_myspace_name_changed(HippoConnection *connection,
                            LmMessage       *message)
{
    LmMessageNode *child;
    const char *name;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/myspace", "mySpaceNameChanged");
    if (child == NULL)
        return FALSE;
    name = lm_message_node_get_attribute(child, "name");
    if (!name) {
        g_warning("No name in mySpaceNameChanged message");
        return TRUE; /* still handled it */
    }
       
    hippo_data_cache_set_myspace_name(connection->cache, name);
    
    return TRUE;
}

static gboolean
handle_myspace_contact_comment(HippoConnection *connection,
                               LmMessage       *message)
{
    LmMessageNode *child;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/myspace", "mySpaceContactComment");
    if (child == NULL)
        return FALSE;
    
    /* signal that we need to re-poll myspace for new comments */
    g_signal_emit(connection, signals[MYSPACE_CHANGED], 0);
    
    return TRUE;
}

static gboolean
handle_group_membership_change(HippoConnection *connection,
                               LmMessage       *message)
{
    const char *group_id;
    HippoEntity *group;
    const char *user_id;
    HippoEntity *user;
    const char *membership_status;
    LmMessageNode *child;
    LmMessageNode *sub_child;

    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_NORMAL &&
        lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_NOT_SET)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/group", "membershipChange");
    if (child == NULL)
        return FALSE;
    
    /* Look for the objects associated with this message */
    for (sub_child = child->children; sub_child; sub_child = sub_child->next) {
        hippo_connection_parse_entity(connection, sub_child);
    }

    group_id = lm_message_node_get_attribute(child, "groupId");
    if (group_id == NULL)
        return FALSE;
    group = hippo_data_cache_lookup_entity(connection->cache, group_id);
    if (!group)
        return FALSE;
    user_id = lm_message_node_get_attribute(child, "userId");
    if (user_id == NULL)
        return FALSE;
    user = hippo_data_cache_lookup_entity(connection->cache, user_id);
    if (!user)
        return FALSE;

    membership_status = lm_message_node_get_attribute(child, "membershipStatus");

    g_signal_emit(connection, signals[GROUP_MEMBERSHIP_CHANGED], 0, 
                  group, user, membership_status);
    
    return TRUE;
}

static gboolean
handle_initial_application_burst(HippoConnection *connection,
                                 LmMessage       *message)
{
    LmMessageNode *child;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    child = find_child_node(message->node, "http://dumbhippo.com/protocol/applications", "initialApplicationBurst");
    if (child == NULL)
        return FALSE;
    g_debug("received a message to turn on initial application burst upload");

    g_signal_emit(connection, signals[INITIAL_APPLICATION_BURST], 0);

    return TRUE;
}

static LmHandlerResult 
handle_stream_error (LmMessageHandler *handler,
                     LmConnection     *lconnection,
                     LmMessage        *message,
                     gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child;

    g_debug("handling stream error message");

    child = find_child_node(message->node, NULL, "see-other-host");

    if (child) {
        char *redirect_host = g_strdup(child->value);
        g_debug("Got see-other-host message, redirected to '%s'", redirect_host);
        
        hippo_connection_signout(connection);
        hippo_connection_connect(connection, redirect_host);
        g_free (redirect_host);
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    g_debug("handle_stream-error: message not handled by any of our handlers");

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

static LmHandlerResult 
handle_message (LmMessageHandler *handler,
                LmConnection     *lconnection,
                LmMessage        *message,
                gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    
    switch (process_room_message(connection, message)) {
    case PROCESS_MESSAGE_IGNORE:
        break;
    case PROCESS_MESSAGE_CONSUME:
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_data_notify(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_blocks_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_active_posts_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_live_post_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_prefs_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_setting_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_entities_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }    
    
    if (handle_myspace_name_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_myspace_contact_comment(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_group_membership_change(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_initial_application_burst(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    /* Messages used to be HEADLINE, we accept both for compatibility */
    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NORMAL
        /* Shouldn't need this, default should be normal */
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NOT_SET
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        LmMessageNode *child = find_child_node(message->node, "http://dumbhippo.com/protocol/post", "newPost");
        if (child) {
            g_debug("newPost received");
            hippo_connection_parse_post_data(connection, child, TRUE, "newPost");
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
        child = find_child_node(message->node, "http://dumbhippo.com/protocol/whereim", "whereim");
        if (child) {
                g_debug("whereim received");
                hippo_connection_parse_whereim_node(connection, child);
                return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
    }
    
    g_debug("handle_message: message not handled by any of our handlers");

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

static LmHandlerResult 
handle_presence (LmMessageHandler *handler,
                 LmConnection     *lconnection,
                 LmMessage        *message,
                 gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    process_room_message(connection, message);

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

static const char*
disconnect_reason_debug_string(LmDisconnectReason reason)
{
    switch (reason) {
    case LM_DISCONNECT_REASON_ERROR:
        return "ERROR";
        break;
    case LM_DISCONNECT_REASON_HUP:
        return "HUP";
        break;
    case LM_DISCONNECT_REASON_OK:
        return "OK";
        break;
    case LM_DISCONNECT_REASON_PING_TIME_OUT:
        return "PING_TIME_OUT";
        break;
    case LM_DISCONNECT_REASON_UNKNOWN:
        return "UNKNOWN";
        break;
    case LM_DISCONNECT_REASON_RESOURCE_CONFLICT:
        return "RESOURCE_CONFLICT";
        break;
    case LM_DISCONNECT_REASON_INVALID_XML:
        return "INVALID_XML";
        break;
    }
    return "WHAT THE?";
}

static void 
handle_disconnect (LmConnection       *lconnection,
                   LmDisconnectReason  reason,
                   gpointer            data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    g_debug("handle_disconnect reason NO j/k reason=%s",
            disconnect_reason_debug_string(reason));

    if (connection->state == HIPPO_STATE_SIGNED_OUT) {
        hippo_connection_clear(connection);
    } else {
        hippo_connection_connect_failure(connection, "Lost connection");
    }
}

static void
handle_open (LmConnection *lconnection,
             gboolean      success,
             gpointer      data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    g_debug("handle_open success=%d", success);

    if (success) {
        hippo_connection_authenticate(connection);
    } else {
        hippo_connection_connect_failure(connection, NULL);
    }
}

static void 
handle_authenticate(LmConnection *lconnection,
                    gboolean      success,
                    gpointer      data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessage *message;

    g_debug("handle_authenticate success=%d", success);

    if (!success) {
        hippo_connection_auth_failure(connection, NULL);
        return;
    }

    message = lm_message_new_with_sub_type(NULL, 
                                           LM_MESSAGE_TYPE_PRESENCE, 
                                           LM_MESSAGE_SUB_TYPE_AVAILABLE);

    hippo_connection_state_change(connection, HIPPO_STATE_AWAITING_CLIENT_INFO);
    hippo_connection_send_message(connection, message, SEND_MODE_IMMEDIATELY);

    hippo_connection_request_client_info(connection);
    
    connection->last_auth_failed = FALSE;
    g_signal_emit(connection, signals[AUTH_SUCCEEDED], 0);
}


/* == Random cruft == */

const char*
hippo_connection_get_tooltip(HippoConnection *connection)
{
    HippoState state;
    HippoInstanceType instance;
    const char *name;
    const char *tip;
    
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), NULL);

    if (connection->tooltip)
        return connection->tooltip;
    
    state = hippo_connection_get_state(connection);
    instance = hippo_platform_get_instance_type(connection->platform);

    name = NULL;
    switch (instance) {
    case HIPPO_INSTANCE_NORMAL:
        name = _("Mugshot");
        break;
    case HIPPO_INSTANCE_DOGFOOD:
        name = _("Mugshot - I prefer dog food!");
        break;
    case HIPPO_INSTANCE_DEBUG:
        name = _("Mugshot - Eat pesky bugs!");
        break;
    }

    if (name == NULL)
        name = _("Mugshot");
    
    tip = NULL;
    switch (state) {
    case HIPPO_STATE_SIGNED_OUT:
    case HIPPO_STATE_RETRYING:
        if (connection->last_auth_failed) {
            /* This is because it's possible to transition from AUTH_WAIT or SIGN_IN_WAIT to 
             * SIGNED_OUT or RETRYING, which loses the information that we failed to log in,
             * we had someone asking about how to fix the "disconnected" on the list.
             */
            tip = _("%s (disconnected - please log in to mugshot.org)");
        } else {
            tip = _("%s (disconnected - will try reconnecting in a few)");
        }
        break;
    case HIPPO_STATE_SIGN_IN_WAIT:
        tip = _("%s (please log in to mugshot.org)");
        break;
    case HIPPO_STATE_CONNECTING:
    case HIPPO_STATE_AUTHENTICATING:
        tip = _("%s (connecting - please wait)");
        break;    
    case HIPPO_STATE_AWAITING_CLIENT_INFO:
        tip = _("%s (checking for updates)");
        break;    
    case HIPPO_STATE_AUTH_WAIT:
        tip = _("%s (login failed - please log in to mugshot.org)");
        break;
    case HIPPO_STATE_AUTHENTICATED:
        tip = _("%s");
        break;
    }

    if (tip == NULL)
        tip = _("%s");
    
    connection->tooltip = g_strdup_printf(tip, name);

    return connection->tooltip;
}

/* return TRUE if showing login dialog would be appropriate */
gboolean
hippo_connection_get_need_login(HippoConnection  *connection)
{
    switch (hippo_connection_get_state(connection)) {
    case HIPPO_STATE_SIGNED_OUT:
    case HIPPO_STATE_RETRYING:
    case HIPPO_STATE_SIGN_IN_WAIT:
    case HIPPO_STATE_AUTH_WAIT:
        if (connection->last_auth_failed || connection->username == NULL)
            return TRUE;
        else
            return FALSE;
        break;
    default:
        return FALSE;
    }
}

const char*
hippo_state_to_string(HippoState state)
{
    switch (state) {
    case HIPPO_STATE_SIGNED_OUT:
        return "SIGNED_OUT";
    case HIPPO_STATE_SIGN_IN_WAIT:
        return "SIGN_IN_WAIT";
    case HIPPO_STATE_CONNECTING:
        return "CONNECTING";
    case HIPPO_STATE_RETRYING:
        return "RETRYING";
    case HIPPO_STATE_AUTHENTICATING:
        return "AUTHENTICATING";
    case HIPPO_STATE_AUTH_WAIT:
        return "AUTH_WAIT";
    case HIPPO_STATE_AWAITING_CLIENT_INFO:
        return "AWAITING_CLIENT_INFO";
    case HIPPO_STATE_AUTHENTICATED:
        return "AUTHENTICATED";
    }
    /* not a default case so we get a warning if we omit one from the switch */
    return "WHAT THE?";
}

char*
hippo_connection_make_absolute_url(HippoConnection *connection,
                                   const char      *maybe_relative)
{
    if (*maybe_relative == '/') {
        char *server;
        char *url;
        
        server = hippo_platform_get_web_server(connection->platform);
        url = g_strdup_printf("http://%s%s", server, maybe_relative);
        g_free(server);

        return url;
    } else if (g_str_has_prefix(maybe_relative, "http:")) {
        return g_strdup(maybe_relative);
    } else {
        g_warning("weird url '%s', not sure what to do with it", maybe_relative);
        return g_strdup(maybe_relative);
    }
}

void
hippo_connection_open_maybe_relative_url(HippoConnection *connection,
                                         const char      *relative_url)
{
    char *url;
    url = hippo_connection_make_absolute_url(connection,
                                             relative_url);
    hippo_platform_open_url(connection->platform,
                            connection->login_browser,
                            url);
    g_free(url);
}

void
hippo_connection_visit_post(HippoConnection *connection,
                            HippoPost       *post)
{
    hippo_connection_visit_post_id(connection,
                                   hippo_post_get_guid(post));
}

void
hippo_connection_visit_post_id(HippoConnection *connection,
                               const char      *guid)
{
    char *relative;
    relative = g_strdup_printf("/visit?post=%s", guid);
    hippo_connection_open_maybe_relative_url(connection, relative);
    g_free(relative);
}

void
hippo_connection_visit_entity(HippoConnection *connection,
                              HippoEntity     *entity)
{

    const char *home_url;

    home_url = hippo_entity_get_home_url(entity);
    if (home_url) {
        hippo_connection_open_maybe_relative_url(connection, home_url);
    } else {
        g_warning("Don't know how to go to the home page for entity '%s'",
                  hippo_entity_get_guid(entity));
    }
}

#ifdef HIPPO_LOUDMOUTH_IS_10
typedef struct {
        gchar *key;
        gchar *value;
} KeyValuePair;
static char *
copied_lm_message_node_to_string(LmMessageNode *node)
{
    GString       *ret;
    GSList        *l;
    LmMessageNode *child;

    g_return_val_if_fail (node != NULL, NULL);
    
    if (node->name == NULL) {
        return g_strdup ("");
    }
    
    ret = g_string_new ("<");
    g_string_append (ret, node->name);
    
    for (l = node->attributes; l; l = l->next) {
        KeyValuePair *kvp = (KeyValuePair *) l->data;

        if (node->raw_mode == FALSE) {
            gchar *escaped;

            escaped = g_markup_escape_text (kvp->value, -1);
            g_string_append_printf (ret, " %s=\"%s\"", 
                        kvp->key, escaped);
            g_free (escaped);
        } else {
            g_string_append_printf (ret, " %s=\"%s\"", 
                        kvp->key, kvp->value);
        }
        
    }
    
    g_string_append_c (ret, '>');
    
    if (node->value) {
        gchar *tmp;

        if (node->raw_mode == FALSE) {
            tmp = g_markup_escape_text (node->value, -1);
            g_string_append (ret,  tmp);
            g_free (tmp);
        } else {
            g_string_append (ret, node->value);
        }
    } 

    for (child = node->children; child; child = child->next) {
        gchar *child_str = copied_lm_message_node_to_string (child);
        g_string_append_c (ret, ' ');
        g_string_append (ret, child_str);
        g_free (child_str);
    }

    g_string_append_printf (ret, "</%s>\n", node->name);
    
    return g_string_free (ret, FALSE);
}
#endif

static LmHandlerResult
on_external_iq_reply(LmMessageHandler *handler,
                     LmConnection     *lconnection,
                     LmMessage        *message,
                     gpointer          data)
{
    MessageContext *context = (MessageContext*) data;
    HippoConnection *connection = context->connection;
    LmMessageNode *node = message->node->children;
    guint external_id = GPOINTER_TO_UINT(context->data);
    char *content = NULL;
    
    g_debug("got external IQ reply (id=%u)", external_id);

    if (node) {
        lm_message_node_set_raw_mode(node, FALSE);        
#ifdef HIPPO_LOUDMOUTH_IS_10    
        content = copied_lm_message_node_to_string(node);
#else
        content = lm_message_node_to_string(node);
#endif
    } else {
        content = g_strdup("");
    }
    
    g_signal_emit(G_OBJECT(connection), signals[EXTERNAL_IQ_RETURN], 0, external_id, content);
                  
    g_free(content);
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}   

guint 
hippo_connection_send_external_iq(HippoConnection *connection,
                                  gboolean         is_set,
                                  const char      *element,
                                  int              attrs_count,
                                  char           **attrs,
                                  const char      *content)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    int i;
    
    g_return_val_if_fail(attrs_count % 2 == 0, 0);
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           is_set ? LM_MESSAGE_SUB_TYPE_SET : LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, element, NULL);
    for (i = 0; i < attrs_count; i += 2) {
		lm_message_node_set_attribute(child, attrs[i], attrs[i+1]);	
    }
    lm_message_node_set_raw_mode(child, TRUE);    
    lm_message_node_set_value(child, content);
    
    connection->external_iq_serial++;
    
    hippo_connection_send_message_with_reply_full(connection, message, on_external_iq_reply, SEND_MODE_AFTER_AUTH,
                                                  GUINT_TO_POINTER(connection->external_iq_serial), NULL);

    lm_message_unref(message);

    g_debug("Sent external IQ: %s (%d content characters)", element, (int)strlen(content));
    return connection->external_iq_serial;
}


/**********************************************************************
 * Handling of DataModel IQ's and messages.
 *
 * The complexity here is largely because loudmouth doesn't have proper
 * namespace support, so we have to roll our own.
 */

typedef struct {
    LmMessageNode *node;
    const char *prefix;
    const char *uri;
} DMNamespace;

typedef struct {
    HippoConnection *connection;
    HippoDataModel *model;
    const char *system_uri;
    GSList *nodes;
    GSList *resource_bases;
    GSList *default_namespaces;
    GSList *namespaces;
} DMContext;

/* Cut-and-paste to poke into loudmouth internal structures :-( */
typedef struct {
        gchar *key;
        gchar *value;
} CutPasteKeyValuePair;

static void
dm_context_init(DMContext       *context,
                HippoConnection *connection)
{
    context->connection = connection;
    context->model = hippo_data_cache_get_model(connection->cache);
    context->system_uri = g_intern_string("http://mugshot.org/p/system");
    context->nodes = NULL;
    context->resource_bases = NULL;
    context->default_namespaces = NULL;
    context->namespaces = NULL;
}

static gboolean
dm_context_decode(DMContext    *context,
                  const char   *prefixed_name,
                  const char  **uri,
                  const char  **name)
{
    const char *colon = strchr(prefixed_name, ':');
    if (colon == NULL) {
        *uri = context->default_namespaces->data;
        *name = prefixed_name;

        return TRUE;
    } else {
        int prefix_len = colon - prefixed_name;
        GSList *l2;
        
        for (l2 = context->namespaces; l2; l2 = l2->next) {
            DMNamespace *namespace = l2->data;

            if (strncmp(namespace->prefix, prefixed_name, prefix_len) == 0 &&
                namespace->prefix[prefix_len] == '\0')
            {
                *uri = namespace->uri;
                *name = colon + 1;

                return TRUE;
            }
        }
    }

    return FALSE;
}
                          

static const char *
dm_context_get_system_attribute(DMContext  *context,
                                const char *name)
{
    LmMessageNode *node = context->nodes->data;
    GSList *l;

    for (l = node->attributes; l; l = l->next) {
        CutPasteKeyValuePair *kvp = l->data;

        const char *attr_uri;
        const char *attr_name;

        if (dm_context_decode(context, kvp->key, &attr_uri, &attr_name)) {
            if (attr_uri == context->system_uri && strcmp(attr_name, name) == 0)
                return kvp->value;
        }
    }

    return NULL;
}

static void
dm_context_push_node(DMContext     *context,
                     LmMessageNode *node)
{
    GSList *l;

    const char *new_default_namespace;
    const char *new_resource_base;

    context->nodes = g_slist_prepend(context->nodes, node);

    if (context->default_namespaces)
        new_default_namespace = context->default_namespaces->data;
    else
        new_default_namespace = NULL;
    
    for (l = node->attributes; l; l = l->next) {
        CutPasteKeyValuePair *kvp = l->data;
        if (g_str_has_prefix(kvp->key, "xmlns")) {
            if (kvp->key[5] == '\0') {
                new_default_namespace = g_intern_string(kvp->value);
            } else if (kvp->key[5] == ':') {
                DMNamespace *namespace = g_new(DMNamespace, 1);
                namespace->node = node;
                namespace->prefix = g_intern_string(kvp->key + 6);
                namespace->uri = g_intern_string(kvp->value);

                context->namespaces = g_slist_prepend(context->namespaces, namespace);
            }
        }
    }

    context->default_namespaces = g_slist_prepend(context->default_namespaces, (char *)new_default_namespace);

    new_resource_base = dm_context_get_system_attribute(context, "resourceBase");
    if (new_resource_base == NULL && context->resource_bases)
        new_resource_base = context->resource_bases->data;

    context->resource_bases = g_slist_prepend(context->resource_bases, (char *)new_resource_base);
}

static void
dm_context_pop_node(DMContext *context)
{
    LmMessageNode *node = context->nodes->data;
    context->nodes = g_slist_delete_link(context->nodes, context->nodes);

    context->resource_bases = g_slist_delete_link(context->resource_bases, context->resource_bases);
    context->default_namespaces = g_slist_delete_link(context->default_namespaces, context->default_namespaces);
        
    while (context->namespaces) {
        DMNamespace *namespace = context->namespaces->data;
        if (namespace->node != node)
            break;

        context->namespaces = g_slist_delete_link(context->namespaces, context->namespaces);
        g_free(namespace);
    }
}

static gboolean
dm_context_node_info(DMContext   *context,
                     const char **uri,
                     const char **name)
{
    LmMessageNode *node = context->nodes->data;

    return dm_context_decode(context, node->name, uri, name);
}

static char *
dm_context_get_resource_id(DMContext *context)
{
    const char *value = dm_context_get_system_attribute(context, "resourceId");
    const char *resource_base = context->resource_bases->data;
    if (value == NULL)
        return NULL;

    /* FIXME: check to see if value is absolute */
    if (resource_base != NULL)
        return g_strconcat(resource_base, value, NULL);
    else
        return g_strdup(value);
}

static gboolean
dm_context_get_indirect(DMContext *context)
{
    const char *indirect_attr = dm_context_get_system_attribute(context, "indirect");
    if (indirect_attr != NULL)
        return g_ascii_strcasecmp(indirect_attr, "true") == 0;
    else
        return FALSE;
}

static HippoDataUpdate
dm_context_get_update(DMContext *context)
{
    const char *update_attr = dm_context_get_system_attribute(context, "update");
    if (update_attr != NULL) {
        if (strcmp(update_attr, "add") == 0)
            return HIPPO_DATA_UPDATE_ADD;
        else if (strcmp(update_attr, "replace") == 0)
            return HIPPO_DATA_UPDATE_REPLACE;
        else if (strcmp(update_attr,"delete") == 0)
            return HIPPO_DATA_UPDATE_DELETE;
        else if (strcmp(update_attr, "clear") == 0)
            return HIPPO_DATA_UPDATE_CLEAR;
        else {
            g_warning("Unknown value for m:update attribute. Assuming 'replace'");
        }
    }

    return HIPPO_DATA_UPDATE_REPLACE;
}

static gboolean
dm_context_get_type(DMContext            *context,
                    HippoDataType        *type,
                    HippoDataCardinality *cardinality,
                    gboolean             *default_include)
{
    const char *type_attr = dm_context_get_system_attribute(context, "type");
    const char *p;

    if (type_attr == NULL) {
        if (*type != HIPPO_DATA_NONE) {
            /* we already had a type */
            return TRUE;
        } else {
            g_warning("m:type attribute missing");
            return FALSE;
        }
    }

    p = type_attr;
    if (*p == '+') {
        *default_include = TRUE;
        p++;
    }

    switch (*p) {
    case 'b':
        *type = HIPPO_DATA_BOOLEAN;
        break;
    case 'i':
        *type = HIPPO_DATA_INTEGER;
        break;
    case 'l':
        *type = HIPPO_DATA_LONG;
        break;
    case 'f':
        *type = HIPPO_DATA_FLOAT;
        break;
    case 's':
        *type = HIPPO_DATA_STRING;
        break;
    case 'r':
        *type = HIPPO_DATA_RESOURCE;
        break;
    case 'u':
        *type = HIPPO_DATA_URL;
        break;
    default:
        g_warning("Can't understand m:type attribute '%s'", type_attr);
        return FALSE;
    }
        
    p++;

    switch (*p) {
    case '*':
        *cardinality = HIPPO_DATA_CARDINALITY_N;
        p++;
        break;
    case '?':
        *cardinality = HIPPO_DATA_CARDINALITY_01;
        p++;
        break;
    case '\0':
        *cardinality = HIPPO_DATA_CARDINALITY_1;
        break;
    default:
        g_warning("Can't understand m:type attribute '%s'", type_attr);
        return FALSE;
    }

    if (*p != '\0') {
        g_warning("Can't understand m:type attribute '%s'", type_attr);
        return FALSE;
    }

    return TRUE;
}

static gboolean
dm_context_get_value(DMContext      *context,
                     HippoDataType   type,
                     HippoDataValue *value)
{
    char *resource_id = dm_context_get_resource_id(context);
    
    if (resource_id != NULL) {
        HippoDataResource *resource;

        if (type != HIPPO_DATA_RESOURCE) {
            g_warning("m:resourceId found for non-resource property value");
            return FALSE;
        }

        resource = _hippo_data_model_get_resource(context->model, resource_id);
        
        if (resource == NULL) {
            // FIXME: We need to handle circular references
            g_warning("Reference to a resource %s that we don't know about", resource_id);
            g_free(resource_id);
            return FALSE;
        }
        g_free(resource_id);

        value->type = HIPPO_DATA_RESOURCE;
        value->u.resource = resource;

        return TRUE;
    } else {
        LmMessageNode *node = context->nodes->data;
        gboolean result = TRUE;
        char *str;
        
        if (type == HIPPO_DATA_RESOURCE) {
            g_warning("m:resourceId not found for a resource property value");
            return FALSE;
        }

        str = g_strdup(node->value != NULL ? node->value : "");
        g_strstrip(str);

        value->type = type;
        switch (type) {
        case HIPPO_DATA_BOOLEAN:
            value->u.boolean = g_ascii_strcasecmp(str, "true") == 0;
            break;
        case HIPPO_DATA_INTEGER:
            {
                char *end;
                long v = strtol(str, &end, 10);
                if (*str == '\0' || *end != '\0') {
                    g_warning("Invalid float property value '%s'", str);
                    result = FALSE;
                }
                value->u.integer = CLAMP(v, G_MININT, G_MAXINT);
            }
            break;
        case HIPPO_DATA_LONG:
            {
                char *end;
                value->u.long_ = g_ascii_strtoll(str, &end, 10);
                if (*str == '\0' || *end != '\0') {
                    g_warning("Invalid long property value '%s'", str);
                    result = FALSE;
                }
            }
            break;
        case HIPPO_DATA_FLOAT:
            {
                char *end;
                value->u.float_ = g_ascii_strtod(str, &end);
                if (*str == '\0' || *end != '\0') {
                    g_warning("Invalid float property value '%s'", str);
                    result = FALSE;
                }
            }
            break;
        case HIPPO_DATA_STRING:
            value->u.string = node->value;
            break;
        case HIPPO_DATA_URL:
            value->u.string = hippo_connection_make_absolute_url(context->connection, str);
            break;
        case HIPPO_DATA_NONE:
        case HIPPO_DATA_RESOURCE:
        case HIPPO_DATA_LIST:
            g_assert_not_reached();
            break;
        }

        g_free(str);
        
        return result;
    }

}

static void
update_property(DMContext            *context,
                HippoDataResource    *resource,
                HippoNotificationSet *notifications)
{
    const char *property_uri;
    const char *property_name;
    HippoQName *property_qname;
    HippoDataProperty *old_property;
    HippoDataType type = HIPPO_DATA_NONE;
    HippoDataUpdate update;
    HippoDataCardinality cardinality = HIPPO_DATA_CARDINALITY_1;
    gboolean default_include = FALSE;
    const char *default_children;
    
    if (!dm_context_node_info(context, &property_uri, &property_name) || property_uri == NULL) {
        g_warning("Couldn't resolve the namespace for child element of a resource %s",
                  hippo_data_resource_get_resource_id(resource));
        return;
    }
    
    property_qname = hippo_qname_get(property_uri, property_name);
    
    update = dm_context_get_update(context);

    /* Look for an old value to get the type as a default if the server doesn't send it. (In particular,
     * the type won't be sent for UPDATE_ADD
     */
    old_property = _hippo_data_resource_get_property_by_qname(resource, property_qname);
    if (old_property != NULL) {
        type = hippo_data_property_get_type(old_property);
        cardinality = hippo_data_property_get_cardinality(old_property);
        default_include = hippo_data_property_get_default_include(old_property);
    }

    if (!dm_context_get_type(context, &type, &cardinality, &default_include))
        return;

    if (default_include)
        default_children = dm_context_get_system_attribute(context, "defaultChildren");
    else
        default_children = NULL;
    
    if (update == HIPPO_DATA_UPDATE_CLEAR) {
        _hippo_data_resource_update_property(resource, property_qname, update, cardinality,
                                             default_include, default_children,
                                             NULL,
                                             notifications);
    } else {
        HippoDataValue value;
        
        if (dm_context_get_value(context, type, &value)) {
            _hippo_data_resource_update_property(resource, property_qname, update, cardinality,
                                                 default_include, default_children,
                                                 &value,
                                                 notifications);
        }
    }
}

static HippoDataResource *
update_resource(DMContext            *context,
                HippoNotificationSet *notifications)
{
    const char *uri;
    const char *name;
    char *resource_id = NULL;
    HippoDataResource *resource;
    LmMessageNode *node = context->nodes->data;
    LmMessageNode *property_node;
    gboolean indirect;
    
    if (!dm_context_node_info(context, &uri, &name))
        return NULL;
    
    if (uri == NULL)
        return NULL;
    
    if (strcmp(name, "resource") != 0)
        return NULL;
    
    resource_id = dm_context_get_resource_id(context);
    if (resource_id == NULL) {
        g_warning("Didn't find a resource ID for a resource node, ignoring");
        return NULL;
    }

    resource = _hippo_data_model_ensure_resource(context->model, resource_id, uri); 
    indirect = dm_context_get_indirect(context);
    
    for (property_node = node->children; property_node; property_node = property_node->next) {
        dm_context_push_node(context, property_node);
        update_property(context, resource, indirect ? NULL :  notifications);
        dm_context_pop_node(context);
    }
    
    g_free(resource_id);

    if (indirect)
        return NULL;
    else
        return resource;
}

static LmHandlerResult
on_query_reply(LmMessageHandler *handler,
               LmConnection     *lm_connection,
               LmMessage        *message,
               gpointer          data)
{
    MessageContext *message_context = data;
    HippoDataQuery *query = message_context->data;
    HippoQName *query_qname = hippo_data_query_get_qname(query);
    LmMessageNode *node = lm_message_get_node(message);
    DMContext context;
    LmMessageNode *child;
    LmMessageNode *resource_node;
    const char *child_uri;
    const char *child_name;
    GSList *results = NULL;
    
    dm_context_init(&context, message_context->connection);
    dm_context_push_node(&context, node);

    // FIXME: Check for <iq type="error">

    child = node->children;
    if (child == NULL || child->next != NULL) {
        g_warning("Reply to query didn't have a single child of the <iq/> node");
        goto pop_node;
    }

    dm_context_push_node(&context, child);
    if (!dm_context_node_info(&context, &child_uri, &child_name)) {
        g_warning("Couldn't resolve the namespace for the <iq/> child in a query reply");
        goto pop_child;
    }

    if (child_uri != query_qname->uri || strcmp(child_name, query_qname->name) != 0) {
        g_warning("<iq/> child name didn't match the query");
        goto pop_child;
    }
    
    for (resource_node = child->children; resource_node; resource_node = resource_node->next) {
        HippoDataResource *resource;
        
        dm_context_push_node(&context, resource_node);
        resource = update_resource(&context, NULL);
        if (resource != NULL)
            results = g_slist_prepend(results, resource); 
        
        dm_context_pop_node(&context);
    }

    _hippo_data_query_response(query, results);
    g_slist_free(results);

 pop_child:
    dm_context_pop_node(&context);

 pop_node:
    dm_context_pop_node(&context);
    g_assert(context.nodes == NULL);
    
    // FIXME: _hippo_data_query_error() if we had a validation error above
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

static void
add_param_foreach(gpointer key,
                  gpointer value,
                  gpointer data)
{
    const char *param_name = key;
    const char *param_value = value;
    LmMessageNode *node = data;
    LmMessageNode *param_node;
        
    param_node = lm_message_node_add_child (node, "m:param", NULL);
    lm_message_node_set_attribute(param_node, "name", param_name);
    lm_message_node_set_value(param_node, param_value);
}

void
hippo_connection_send_query(HippoConnection *connection,
                            HippoDataQuery  *query,
                            const char      *fetch,
                            GHashTable      *params)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    HippoQName *query_qname = hippo_data_query_get_qname(query);
    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);

    child = lm_message_node_add_child (node, query_qname->name, NULL);
    lm_message_node_set_attribute(child, "xmlns", query_qname->uri);
    lm_message_node_set_attribute(child, "xmlns:m", "http://mugshot.org/p/system");

    if (fetch != NULL) 
        lm_message_node_set_attribute(child, "m:fetch", fetch);

    g_hash_table_foreach(params, add_param_foreach, child);

    hippo_connection_send_message_with_reply_full(connection, message, on_query_reply, SEND_MODE_AFTER_AUTH, query, NULL);

    lm_message_unref(message);
}

static gboolean
handle_data_notify (HippoConnection *connection,
                    LmMessage       *message)
{
    DMContext context;
    LmMessageNode *node = lm_message_get_node(message);
    LmMessageNode *child;
    LmMessageNode *resource_node;
    gboolean found = FALSE;
    
    dm_context_init(&context, connection);
    dm_context_push_node(&context, node);

    for (child = node->children; !found && child; child = child->next) {
        const char *child_uri;
        const char *child_name;
        HippoNotificationSet *notifications;
    
        dm_context_push_node(&context, child);
        if (!dm_context_node_info(&context, &child_uri, &child_name)) {
            goto next_child;
        }
        
        if (child_uri != context.system_uri || strcmp(child_name, "notify") != 0) {
            goto next_child;
        }

        found = TRUE;

        notifications = _hippo_notification_set_new(context.model);
        
        for (resource_node = child->children; resource_node; resource_node = resource_node->next) {
            dm_context_push_node(&context, resource_node);
            update_resource(&context, notifications);
            dm_context_pop_node(&context);
        }

        _hippo_notification_set_send(notifications);

    next_child:
        dm_context_pop_node(&context);
    }

    dm_context_pop_node(&context);
    g_assert(context.nodes == NULL);
    
    return found;
}

