/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-common-internal.h"
#include "hippo-connection.h"
#include "hippo-data-cache-internal.h"
#include "hippo-common-marshal.h"
#include "hippo-external-account.h"
#include "hippo-title-pattern.h"
#include "hippo-xml-utils.h"
#include "hippo-disk-cache.h"
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

/* Context structure we use to do namespace handling */

typedef struct {
    LmMessageNode *node;
    const char *prefix;
    const char *uri;
} DMNamespace;

typedef struct {
    HippoConnection *connection;
    DDMDataModel *model;
    const char *system_uri;
    GSList *nodes;
    GSList *resource_bases;
    GSList *default_namespaces;
    GSList *namespaces;
    char *base_url;
} DMContext;

static void dm_context_init      (DMContext       *context,
                                  HippoConnection *connection);
static void dm_context_finish    (DMContext       *context);
static void dm_context_push_node (DMContext       *context,
                                  LmMessageNode   *node);
static void dm_context_pop_node  (DMContext       *context);

static DDMDataResource *update_resource (DMContext          *context,
                                         DDMNotificationSet *broadcast_notifications,
                                         DDMNotificationSet *save_notifications,
                                         gboolean            mark_received);

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
static void     hippo_connection_run_signin_timeout   (HippoConnection *connection);
static void     hippo_connection_stop_music_timeout   (HippoConnection *connection);
static void     hippo_connection_connect              (HippoConnection *connection,
                                                       const char      *redirect_host);
static void     hippo_connection_disconnect           (HippoConnection *connection);
static void     hippo_connection_retry                (HippoConnection *connection);
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

static void on_cookies_maybe_changed(HippoPlatform     *platform,
                                     gpointer           data);

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
    HippoServerType auth_server_type;
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
    BLOCK_FILTER_CHANGED,
    SETTING_CHANGED,
    SETTINGS_LOADED,
    /* Emitted to signal that we should temporarily rapidly upload application
     * activity instead of just once an hour */
    INITIAL_APPLICATION_BURST,
    EXTERNAL_IQ_RETURN,
    PREF_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_connection_init(HippoConnection *connection)
{
    connection->state = HIPPO_STATE_SIGNED_OUT;
    connection->pending_outgoing_messages = g_queue_new();

    /* desktop is the "more conservative" one (we don't
     * show the stacker UI), though it's probably
     * wrong to care about this value unless we
     * are in fact logged in
     */
    connection->auth_server_type = HIPPO_SERVER_DESKTOP;
    
    /* default browsers if we don't discover otherwise 
     * (we'll use whatever the user has logged in with
     * if they've logged in with something)
     */
#ifdef G_OS_WIN32
    connection->login_browser = HIPPO_BROWSER_IE;
#else

#ifdef WITH_MAEMO
    connection->login_browser = HIPPO_BROWSER_MAEMO;
#else
    connection->login_browser = HIPPO_BROWSER_FIREFOX;
#endif /* WITH_MAEMO */

#endif /* G_OS_WIN32 */
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
    
    signals[INITIAL_APPLICATION_BURST] =
        g_signal_new ("initial-application-burst",
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

    g_signal_connect(G_OBJECT(connection->platform), "cookies-maybe-changed",
                     G_CALLBACK(on_cookies_maybe_changed), connection);
    
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

HippoServerType
hippo_connection_get_auth_server_type (HippoConnection  *connection)
{
    return connection->auth_server_type;
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
    zero_str(&connection->self_resource_id);
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

    if (connection->self_resource_id == NULL && connection->username != NULL) {

        /* FIXME this seems hosed; we should ask the server for the self resource ID
         * by making yourself a property on the global object or whatever perhaps.
         * 
         * Right now both the "desktop" and "stacker" servers return resource
         * ids with the "stacker" hostname, so we hardcode HIPPO_SERVER_STACKER
         * below.
         */
        
        const char *self_guid = hippo_connection_get_self_guid(connection);

        /* The resource ID does not change according to which server we connect to,
         * it is always mugshot.org
         */
        connection->self_resource_id = g_strdup_printf("http://mugshot.org/o/user/%s", self_guid);
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

static void
on_cookies_maybe_changed(HippoPlatform     *platform,
                         gpointer           data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    
    g_debug("cookies maybe changed");

    /* the semantics here should be that if we had a timeout waiting to
     * try reconnect or reauth, we should run it now, but otherwise
     * we don't care that cookies changed.
     *
     * A future enhancement might disconnect if we no longer have the auth
     * cookie, but we don't do that for now.
     */
    if (connection->signin_timeout_id != 0) {
        hippo_connection_run_signin_timeout(connection);
    } else if (connection->retry_timeout_id != 0) {
        hippo_connection_retry(connection);
    }
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
    /* message can be NULL */
    g_debug("Connection failure message: '%s'", message ? message : "NULL");

    hippo_connection_clear(connection);
    
    if (connection->state == HIPPO_STATE_REDIRECTING)
        return;

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
        if (connection->signin_timeout_id != 0) /* 0 if we were called directly */
            g_source_remove (connection->signin_timeout_id);
        connection->signin_timeout_id = g_timeout_add (SIGN_IN_SUBSEQUENT_TIMEOUT, signin_timeout, 
                                                       connection);
        return FALSE;
    }

    return TRUE;
}

/* run the signin timeout immediately */
static void
hippo_connection_run_signin_timeout(HippoConnection *connection)
{
    hippo_connection_stop_signin_timeout(connection);
    signin_timeout(connection);
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
    
    hippo_platform_get_message_host_port(connection->platform, connection->auth_server_type,
                                         &message_host, &message_port);

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

    /*
     * Right now, we always connect to the "stacker" (mugshot.org)
     * server if we have a cookie for it, and to the "desktop" (online.gnome.org)
     * server otherwise. Eventually, we may want to adjust this to
     * be a setting or something else.
     * 
     */
    
    old_has_auth = hippo_connection_get_has_auth(connection);

    /* always clear current username/password */
    zero_str(&connection->username);
    zero_str(&connection->password);
    zero_str(&connection->self_resource_id);
    
    result = hippo_platform_read_login_cookie(connection->platform,
                                              HIPPO_SERVER_STACKER,
                                              &connection->login_browser,
                                              &connection->username, &connection->password);    
    
    if (connection->username) {
        /* don't print the password in the log info */
        g_debug("Loaded username '%s' password %s for STACKER mode", connection->username,
                connection->password ? "loaded" : "not found");
    }

    /* If we aren't authed to the stacker (i.e. mugshot.org) try auth to the
     * desktop server (i.e. online.gnome.org).
     */
    if (hippo_connection_get_has_auth(connection)) {
        connection->auth_server_type = HIPPO_SERVER_STACKER;
    } else {
        zero_str(&connection->username);
        zero_str(&connection->password);
        zero_str(&connection->self_resource_id);
        
        result = hippo_platform_read_login_cookie(connection->platform,
                                                  HIPPO_SERVER_DESKTOP,
                                                  &connection->login_browser,
                                                  &connection->username, &connection->password);    
        
        if (connection->username) {
            /* don't print the password in the log info */
            g_debug("Loaded username '%s' password %s for DESKTOP mode", connection->username,
                    connection->password ? "loaded" : "not found");
        }

        connection->auth_server_type = HIPPO_SERVER_DESKTOP;
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
    const char *ddm_protocol_version = NULL;
    const char *minimum;
    const char *current;
    const char *download;
    
    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/clientinfo", "clientInfo")) {
        hippo_connection_connect_failure(connection, "Client info reply was wrong thing");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    child = message->node->children;

    if (!hippo_xml_split(connection->cache, child, NULL,
    		             "ddmProtocolVersion", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &ddm_protocol_version,
                         "minimum", HIPPO_SPLIT_STRING, &minimum,
                         "current", HIPPO_SPLIT_STRING, &current,
                         "download", HIPPO_SPLIT_STRING, &download,
                         NULL))
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;

    g_debug("Got clientInfo response: protocol=%s, minimum=%s, current=%s, download=%s", 
    		ddm_protocol_version ? ddm_protocol_version : "(null)", minimum, current, download);
    
    /* cast off the const */
    info.minimum = (char*)minimum;
    info.current = (char*)current;
    info.download = (char*)download;
    info.ddm_protocol_version = (char*)ddm_protocol_version;
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
        gboolean value_parsed;
        
        if (key == NULL) {
            g_debug("ignoring node '%s' with no 'key' attribute in prefs reply",
                    child->name);
            continue;
        }

        value_parsed = value != NULL && parse_bool(value);
        
        emit = TRUE;
        if (strcmp(key, "musicSharingEnabled") == 0) {
            music_sharing_enabled = value_parsed;
            saw_music_sharing_enabled = TRUE;

            /* For now, always assume it's disabled if we aren't logged in
             * to the mugshot.org server, since there's no UI for this
             * otherwise.
             */
            if (connection->auth_server_type == HIPPO_SERVER_DESKTOP)
                music_sharing_enabled = FALSE;
            
        } else if (strcmp(key, "musicSharingPrimed") == 0) {
            music_sharing_primed = value_parsed;
            saw_music_sharing_primed = TRUE;

            /* For now, always assume it's already primed if we aren't
             * logged in to the mugshot.org server, since there's no
             * UI for this otherwise.
             */
            if (connection->auth_server_type == HIPPO_SERVER_DESKTOP)
                music_sharing_primed = TRUE;
            
        } else if (strcmp(key, "applicationUsageEnabled") == 0) {
            application_usage_enabled = value_parsed;
            saw_application_usage_enabled = TRUE;

            /* We collect app usage even for online.gnome.org since BigBoard
             * need it, so it's exposed in the UI there
             */
            
        } else {
            g_debug("Unknown pref '%s'", key);
            emit = FALSE;
        }
        if (emit)
        	g_signal_emit(G_OBJECT(connection), signals[PREF_CHANGED], 0,
        	              key, value_parsed);
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
    
#if 0    
    LmMessageNode *subchild;
    
    for (subchild = node->children; subchild; subchild = subchild->next) {
        if (!hippo_data_cache_update_from_xml(connection->cache, subchild)) {
            g_debug("Did not successfully update <%s> from xml", subchild->name);
        } else {
            /* g_debug("Updated <%s>", subchild->name) */ ;
        }
    }

    /* g_debug("Done parsing blocks list <%s>", node->name); */
#endif    
    
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
        /* Protocol 1 flags that we want to use the data model for user information */
        lm_message_node_set_attribute(user_info_node, "protocol", "1");
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
parse_chat_resources(DMContext *context)
{
    LmMessageNode *resources_node = find_child_node(context->nodes->data, NULL, "resources");
    LmMessageNode *resource_node;
    if (!resources_node)
        return;

    dm_context_push_node(context, resources_node);
    
    for (resource_node = resources_node->children; resource_node; resource_node = resource_node->next) {
        dm_context_push_node(context, resource_node);
        update_resource(context, NULL, NULL, TRUE);
        dm_context_pop_node(context);
    }
    
    dm_context_pop_node(context);
}

static gboolean
parse_chat_user_info(DMContext       *context,
                     HippoPerson    **person_p,
                     HippoChatState  *status_p)
{
    LmMessageNode *info_node;
    
    info_node = find_child_node(context->nodes->data, "http://dumbhippo.com/protocol/rooms", "userInfo");
    if (!info_node) {
        g_debug("Can't find userInfo node");
        return FALSE;
    }

    dm_context_push_node(context, info_node);
    parse_chat_resources(context);
    dm_context_pop_node(context);
    
    {
        const char *user_str;
        DDMDataResource *user_resource;
        const char *role = NULL;
        HippoChatState status;
            
        if (!hippo_xml_split(context->connection->cache, info_node, NULL,
                             "user", HIPPO_SPLIT_STRING, &user_str,
                             "role", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &role,
                             NULL))
            return FALSE;

        user_resource = ddm_data_model_lookup_resource(context->model, user_str);
        if (user_resource == NULL) {
            g_warning("Can't find referenced user resource in chat message");
            return FALSE;
        }

        *person_p = hippo_person_get_for_resource(user_resource);

        if (!role)
            status = HIPPO_CHAT_STATE_PARTICIPANT;
        else
            status = strcmp(role, "participant") == 0 ? HIPPO_CHAT_STATE_PARTICIPANT : HIPPO_CHAT_STATE_VISITOR;
            
        *status_p = status;
    }

    return TRUE;
}

static HippoChatMessage*
parse_chat_message_info(DMContext        *context,
                        const char       *text)
{
    LmMessageNode *info_node;
    
    info_node = find_child_node(context->nodes->data, "http://dumbhippo.com/protocol/rooms", "messageInfo");
    if (!info_node) {
        g_debug("Can't find messageInfo node");
        return NULL;
    }

    dm_context_push_node(context, info_node);
    parse_chat_resources(context);
    dm_context_pop_node(context);
    
    {
        const char *user_str;
        DDMDataResource *user_resource;
        HippoPerson *user;
        gint64 timestamp_milliseconds;
        GTime timestamp;
        int serial;
        const char *sentiment_str = NULL;
        HippoSentiment sentiment = HIPPO_SENTIMENT_INDIFFERENT;
        HippoChatMessage *result;

        if (!hippo_xml_split(context->connection->cache, info_node, NULL,
                             "user", HIPPO_SPLIT_STRING, &user_str,
                             "sentiment", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &sentiment_str,
                             "timestamp", HIPPO_SPLIT_TIME_MS, &timestamp_milliseconds,
                             "serial", HIPPO_SPLIT_INT32, &serial,
                             NULL))
            return NULL;

        user_resource = ddm_data_model_lookup_resource(context->model, user_str);
        if (user_resource == NULL) {
            g_warning("Can't find referenced user resource in chat message");
            return NULL;
        }

        user = hippo_person_get_for_resource(user_resource);

        if (sentiment_str && !hippo_parse_sentiment(sentiment_str, &sentiment))
            return NULL;

        timestamp = (GTime) (timestamp_milliseconds / 1000);

        result = hippo_chat_message_new(user, text, sentiment, timestamp, serial);
        
        g_object_unref(user);

        return result;
    }
}

static void
process_room_chat_message(DMContext     *context,
                          HippoChatRoom *room)
{
    const char *text;
    LmMessageNode *body_node;
    HippoChatMessage *chat_message;

    body_node = lm_message_node_find_child(context->nodes->data, "body");
    if (body_node)
        text = lm_message_node_get_value(body_node);
    else
        text = NULL;

    if (!text) {
        g_debug("Chat room message without body");
        return;
    }

    chat_message = parse_chat_message_info(context, text);
    if (chat_message == NULL)
        return;

    hippo_chat_room_add_message(room, chat_message);
}

static void
process_room_presence(DMContext       *context,
                      LmMessage       *message,
                      HippoChatRoom   *room,
                      const char      *user_id)
{
    LmMessageSubType subtype;
    LmMessageNode *x_node;
    HippoChatState status;
    HippoPerson *person;
    gboolean result;
    
    x_node = find_child_node(message->node, "http://jabber.org/protocol/muc#user", "x");
    if (!x_node) {
        g_debug("Presence without x child");
        return;
    }
    
    dm_context_push_node(context, x_node);
    result = parse_chat_user_info(context, &person, &status);
    dm_context_pop_node(context);
    
    if (!result)
        return;
    
    subtype = lm_message_get_sub_type(message);

    if (subtype == LM_MESSAGE_SUB_TYPE_AVAILABLE) {
        /* add them to chat room or update their state in chat room */
        hippo_chat_room_set_user_state(room, person, status);
    } else if (subtype == LM_MESSAGE_SUB_TYPE_UNAVAILABLE) {
        hippo_chat_room_set_user_state(room, person, HIPPO_CHAT_STATE_NONMEMBER);
    }

    g_object_unref(person);
}

static ProcessMessageResult
process_room_message(HippoConnection *connection,
                     LmMessage       *message)
{
    /* this could be a chat message or a presence notification */

    DMContext context;
    ProcessMessageResult result;
    const char *from;
    char *chat_id = NULL;
    char *user_id = NULL;
    HippoChatRoom *room;
    HippoChatKind kind;
    LmMessageNode *child;
    gboolean is_history_message;

    chat_id = NULL;

    /* IGNORE = run other handlers CONSUME = we handled it
     * PEND = save for after we fill chatroom
     */
    result = PROCESS_MESSAGE_IGNORE;

    dm_context_init(&context, connection);
    dm_context_push_node(&context, message->node);

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
        process_room_chat_message(&context, room);
    } else if (lm_message_get_type(message) == LM_MESSAGE_TYPE_PRESENCE) {
        g_debug("hippo-connection::process_room_message processing room presence");
        process_room_presence(&context, message, room, user_id);
    } else {
        g_debug("hippo-connection::process_room_message unknown message type");
    }

    result = PROCESS_MESSAGE_CONSUME;
    
 out:
    dm_context_pop_node(&context);
    dm_context_finish(&context);
  
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
#if 0    
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
#endif
    
    return TRUE;
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

        hippo_connection_state_change(connection, HIPPO_STATE_REDIRECTING);
        hippo_connection_disconnect(connection);
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
    
    if (handle_prefs_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_setting_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_initial_application_burst(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
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
    case HIPPO_STATE_REDIRECTING:
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
    case HIPPO_STATE_REDIRECTING:
        return "REDIRECTING";
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


static char *
hippo_connection_make_absolute_url_for_server(HippoConnection *connection,
                                              HippoServerType  server_type,
                                              const char      *maybe_relative)
{
    if (*maybe_relative == '/') {
        char *server;
        char *url;
        
        server = hippo_platform_get_web_server(connection->platform,
                                               server_type);
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

char*
hippo_connection_make_absolute_url(HippoConnection *connection,
                                   const char      *maybe_relative)
{
    return hippo_connection_make_absolute_url_for_server(connection, connection->auth_server_type, maybe_relative);
}

void
hippo_connection_open_maybe_relative_url(HippoConnection *connection,
                                         const char      *relative_url)
{
    char *url;
    /* For opening a web page in this process, we always use the Mugshot Stacker server. */
    url = hippo_connection_make_absolute_url_for_server(connection, HIPPO_SERVER_STACKER,
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

/* Cut-and-paste to poke into loudmouth internal structures :-( */
typedef struct {
        gchar *key;
        gchar *value;
} CutPasteKeyValuePair;

static void
dm_context_init(DMContext       *context,
                HippoConnection *connection)
{
    char *server;
    
    context->connection = connection;
    context->model = hippo_data_cache_get_model(connection->cache);
    context->system_uri = g_intern_string("http://mugshot.org/p/system");
    context->nodes = NULL;
    context->resource_bases = NULL;
    context->default_namespaces = NULL;
    context->namespaces = NULL;

    server = hippo_platform_get_web_server(connection->platform,
                                           connection->auth_server_type);
    context->base_url = g_strconcat("http://", server, NULL);
    g_free(server);
}

static void
dm_context_finish(DMContext *context)
{
    g_assert(context->nodes == NULL);
    g_free(context->base_url);
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

static DDMDataFetch *
dm_context_get_fetch(DMContext *context)
{
    const char *fetch_attr = dm_context_get_system_attribute(context, "fetch");
    if (fetch_attr != NULL)
        return ddm_data_fetch_from_string(fetch_attr);
    else
        return NULL;
}

static DDMDataUpdate
dm_context_get_update(DMContext *context)
{
    const char *update_attr = dm_context_get_system_attribute(context, "update");
    if (update_attr != NULL) {
        if (strcmp(update_attr, "add") == 0)
            return DDM_DATA_UPDATE_ADD;
        else if (strcmp(update_attr, "replace") == 0)
            return DDM_DATA_UPDATE_REPLACE;
        else if (strcmp(update_attr,"delete") == 0)
            return DDM_DATA_UPDATE_DELETE;
        else if (strcmp(update_attr, "clear") == 0)
            return DDM_DATA_UPDATE_CLEAR;
        else {
            g_warning("Unknown value for m:update attribute. Assuming 'replace'");
        }
    }

    return DDM_DATA_UPDATE_REPLACE;
}

static gboolean
dm_context_get_type(DMContext            *context,
                    DDMQName             *property_qname,
                    DDMDataType          *type,
                    DDMDataCardinality   *cardinality,
                    gboolean             *default_include)
{
    const char *type_attr = dm_context_get_system_attribute(context, "type");

    if (type_attr == NULL) {
        if (*type != DDM_DATA_NONE) {
            /* we already had a type */
            return TRUE;
        } else {
            g_warning("m:type attribute missing for %s#%s", property_qname->uri, property_qname->name);
            return FALSE;
        }
    }

    return ddm_data_parse_type(type_attr, type, cardinality, default_include);
}

static gint64
dm_context_get_ts(DMContext *context)
{
    const char *ts_attr = dm_context_get_system_attribute(context, "ts");
    if (ts_attr == NULL) {
        return -1;
    } else {
        char *str_stripped;
        char *end;
        gint64 result;
        
        str_stripped = g_strdup(ts_attr);
        g_strstrip(str_stripped);
        
        result = g_ascii_strtoll(str_stripped, &end, 10);
        if (*str_stripped == '\0' || *end != '\0') {
            g_warning("Invalid m:ts attribute '%s'", ts_attr);
            result = -1;
        }

        g_free(str_stripped);
        return result;
    }
}

static gboolean
dm_context_get_value(DMContext       *context,
                     DDMDataType      type,
                     DDMDataValue    *value)
{
    char *resource_id = dm_context_get_resource_id(context);
    
    if (resource_id != NULL) {
        DDMDataResource *resource;

        if (type != DDM_DATA_RESOURCE) {
            g_warning("m:resourceId found for non-resource property value");
            return FALSE;
        }

        resource = ddm_data_model_ensure_resource(context->model, resource_id, NULL);

        g_assert (resource != NULL);

        g_free(resource_id);

        value->type = DDM_DATA_RESOURCE;
        value->u.resource = resource;

        return TRUE;
    } else {
        LmMessageNode *node = context->nodes->data;
        GError *error = NULL;
        
        if (type == DDM_DATA_RESOURCE) {
            g_warning("m:resourceId not found for a resource property value");
            return FALSE;
        }

        if (!ddm_data_value_from_string(value, type,
                                        node->value != NULL ? node->value : "",
                                        context->base_url,
                                        &error)) {
            g_warning("%s", error->message);
            g_error_free(error);
            return FALSE;
        }

        return TRUE;
    }
}

static void
update_property(DMContext            *context,
                DDMDataResource    *resource,
                DDMNotificationSet *broadcast_notifications,
                DDMNotificationSet *save_notifications)
{
    const char *property_uri;
    const char *property_name;
    DDMQName *property_qname;
    DDMDataProperty *old_property;
    DDMDataType type = DDM_DATA_NONE;
    DDMDataUpdate update;
    DDMDataCardinality cardinality = DDM_DATA_CARDINALITY_1;
    gboolean default_include = FALSE;
    const char *default_children;
    gboolean changed = FALSE;
    
    if (!dm_context_node_info(context, &property_uri, &property_name) || property_uri == NULL) {
        g_warning("Couldn't resolve the namespace for child element of a resource %s",
                  ddm_data_resource_get_resource_id(resource));
        return;
    }
    
    property_qname = ddm_qname_get(property_uri, property_name);
    
    update = dm_context_get_update(context);

    /* Look for an old value to get the type as a default if the server doesn't send it. (In particular,
     * the type won't be sent for UPDATE_ADD
     */
    old_property = ddm_data_resource_get_property_by_qname(resource, property_qname);
    if (old_property != NULL) {
        type = ddm_data_property_get_type(old_property);
        cardinality = ddm_data_property_get_cardinality(old_property);
        default_include = ddm_data_property_get_default_include(old_property);
    }

    if (!dm_context_get_type(context, property_qname, &type, &cardinality, &default_include))
        return;

    if (default_include)
        default_children = dm_context_get_system_attribute(context, "defaultChildren");
    else
        default_children = NULL;

    if (type == DDM_DATA_FEED) {
        if (update == DDM_DATA_UPDATE_CLEAR) {
            changed = ddm_data_resource_update_feed_property(resource, property_qname, update,
                                                             default_include, default_children,
                                                             NULL, -1);
        } else {
            DDMDataValue value;
            gint64 ts = dm_context_get_ts(context);
            
            if (dm_context_get_value(context, DDM_DATA_RESOURCE, &value)) {
                changed = ddm_data_resource_update_feed_property(resource, property_qname, update,
                                                                 default_include, default_children,
                                                                 value.u.resource, ts);
                ddm_data_value_clear(&value);
            }
        }
    } else {
        if (update == DDM_DATA_UPDATE_CLEAR) {
            changed = ddm_data_resource_update_property(resource, property_qname, update, cardinality,
                                                        default_include, default_children,
                                                        NULL);
        } else {
            DDMDataValue value;
            
            if (dm_context_get_value(context, type, &value)) {
                changed = ddm_data_resource_update_property(resource, property_qname, update, cardinality,
                                                            default_include, default_children,
                                                            &value);
                ddm_data_value_clear(&value);
            }
        }
    }

    if (changed) {
        if (broadcast_notifications)
            ddm_notification_set_add(broadcast_notifications, resource, property_qname);
        if (save_notifications && save_notifications != broadcast_notifications)
            ddm_notification_set_add(save_notifications, resource, property_qname);
    }
}

static DDMDataResource *
update_resource(DMContext          *context,
                DDMNotificationSet *broadcast_notifications,
                DDMNotificationSet *save_notifications,
                gboolean            mark_received)
{
    const char *uri;
    const char *name;
    char *resource_id = NULL;
    DDMDataResource *resource;
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

    resource = ddm_data_model_ensure_resource(context->model, resource_id, uri); 
    indirect = dm_context_get_indirect(context);

    for (property_node = node->children; property_node; property_node = property_node->next) {
        dm_context_push_node(context, property_node);
        update_property(context, resource, indirect ? NULL :  broadcast_notifications, save_notifications);
        dm_context_pop_node(context);
    }
    
    g_free(resource_id);

    if (mark_received) {
        DDMDataFetch *fetch = dm_context_get_fetch(context);

        if (fetch != NULL) {
            ddm_data_resource_fetch_received(resource, fetch);
            ddm_data_fetch_unref(fetch);
        }
    }

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
    DDMDataQuery *query = message_context->data;
    HippoDiskCache *disk_cache;
    DDMQName *query_qname = ddm_data_query_get_qname(query);
    LmMessageNode *node = lm_message_get_node(message);
    DMContext context;
    LmMessageNode *child;
    LmMessageNode *resource_node;
    const char *child_uri;
    const char *child_name;
    GSList *results = NULL;
    const char *error_message = NULL;
    DDMDataError error_code = DDM_DATA_ERROR_INTERNAL;
    
    dm_context_init(&context, message_context->connection);

    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_ERROR) {
        LmMessageNode *error_node;
        LmMessageNode *text_node;
        
        error_code = DDM_DATA_ERROR_BAD_REQUEST;
        error_message = "Server error";

        error_node = lm_message_node_find_child(message->node, "error");
        if (error_node != NULL) {
            text_node = lm_message_node_find_child(message->node, "text");
            error_message = lm_message_node_get_value(text_node);

            /* FIXME: refine error code based on the <error/> element */
        }
        
        goto out;
    }
    
    dm_context_push_node(&context, node);

    /* We take an empty IQ reply (no child element) as being an empty result list; this would
     * be an odd way for a server to implement a query, but makes sense for updates, that
     * normally have no results.
     */
    child = node->children;
    if (child != NULL) {
        DDMNotificationSet *notifications;
        
        if (child->next != NULL) {
            error_message = "Reply to query didn't have a single child of the <iq/> node";
            error_code = DDM_DATA_ERROR_BAD_REPLY;
            goto pop_node;
        }

        dm_context_push_node(&context, child);
        if (!dm_context_node_info(&context, &child_uri, &child_name)) {
            error_message = "Couldn't resolve the namespace for the <iq/> child in a query reply";
            error_code = DDM_DATA_ERROR_BAD_REPLY;
            goto pop_child;
        }
        
        if (child_uri != query_qname->uri || strcmp(child_name, query_qname->name) != 0) {
            error_message = "<iq/> child name didn't match the query";
            error_code = DDM_DATA_ERROR_BAD_REPLY;
            goto pop_child;
        }
        
        notifications = ddm_notification_set_new(context.model);
        
        for (resource_node = child->children; resource_node; resource_node = resource_node->next) {
            DDMDataResource *resource;
            
            dm_context_push_node(&context, resource_node);

            /* When we query for resources from the server, we mark what we received not based
             * on what the server tells us we fetched, but based on what we *actually* fetched,
             * so we pass mark_received=FALSE here and mark fetches in data_query_response_internal()
             * instead. Doing it this way prevents us against getting into loops if we ask
             * for something from the server, and the server gives us a response with a fetch that
             * doesn't include what we asked for. If we trusted the server, we'd then ask again
             * for that same thing and presumably again be lied to by the server.
             *
             * But when the server tells us something spontaneously, as in a notification, then
             * we pass TRUE and trust the server. (No infinite loop, because if we ask for more,
             * the response to that additional request will end up here, and the loop terminates.)
             */
            resource = update_resource(&context, NULL, notifications, FALSE);
            if (resource != NULL)
                results = g_slist_prepend(results, resource); 
            
            dm_context_pop_node(&context);
        }

        if (!ddm_data_query_is_update(query)) {
            disk_cache = _hippo_data_model_get_disk_cache(context.model);
            if (disk_cache)
                _hippo_disk_cache_save_query_to_disk(disk_cache, query, results, notifications);
        }
        
        ddm_notification_set_free(notifications);

    pop_child:
        dm_context_pop_node(&context);
    }

    if (error_message == NULL)
        ddm_data_query_response(query, results);
    
    g_slist_free(results);

 pop_node:
    dm_context_pop_node(&context);
    g_assert(context.nodes == NULL);

 out:
    dm_context_finish(&context);
    
    if (error_message != NULL) {
        ddm_data_query_error(query, error_code, error_message);
    }
    
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
                            DDMDataQuery    *query)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    LmMessageSubType message_subtype;
    
    DDMQName *query_qname = ddm_data_query_get_qname(query);
    GHashTable *params = ddm_data_query_get_params(query);
    const char *fetch = ddm_data_query_get_fetch_string(query);

    if (ddm_data_query_is_update(query))
        message_subtype = LM_MESSAGE_SUB_TYPE_SET;
    else
        message_subtype = LM_MESSAGE_SUB_TYPE_GET;

    
    message = lm_message_new_with_sub_type(HIPPO_ADMIN_JID, LM_MESSAGE_TYPE_IQ,
                                           message_subtype);
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
    HippoDiskCache *disk_cache;
    LmMessageNode *node = lm_message_get_node(message);
    LmMessageNode *child;
    LmMessageNode *resource_node;
    gboolean found = FALSE;
    
    dm_context_init(&context, connection);
    dm_context_push_node(&context, node);

    for (child = node->children; !found && child; child = child->next) {
        const char *child_uri;
        const char *child_name;
        DDMNotificationSet *broadcast_notifications;
        DDMNotificationSet *save_notifications;
    
        dm_context_push_node(&context, child);
        if (!dm_context_node_info(&context, &child_uri, &child_name)) {
            goto next_child;
        }
        
        if (child_uri != context.system_uri || strcmp(child_name, "notify") != 0) {
            goto next_child;
        }

        found = TRUE;

        broadcast_notifications = ddm_notification_set_new(context.model);
        save_notifications = ddm_notification_set_new(context.model);
        
        for (resource_node = child->children; resource_node; resource_node = resource_node->next) {
            dm_context_push_node(&context, resource_node);
            update_resource(&context, broadcast_notifications, save_notifications, TRUE);
            dm_context_pop_node(&context);
        }

        ddm_notification_set_send(broadcast_notifications);
        ddm_notification_set_free(broadcast_notifications);
        
        disk_cache = _hippo_data_model_get_disk_cache(context.model);
        if (disk_cache)
            _hippo_disk_cache_save_update_to_disk(disk_cache, save_notifications);
        
        ddm_notification_set_free(save_notifications);

    next_child:
        dm_context_pop_node(&context);
    }

    dm_context_pop_node(&context);
    dm_context_finish(&context);
    
    return found;
}

