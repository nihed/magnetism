/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-connection.h"
#include "hippo-data-cache-internal.h"
#include "hippo-common-marshal.h"
#include "hippo-xml-utils.h"
#include <loudmouth/loudmouth.h>
#include <string.h>
#include <stdlib.h>

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

/* === OutgoingMessage internal class === */

typedef struct OutgoingMessage OutgoingMessage;

struct OutgoingMessage {
    int               refcount;
    LmMessage        *message;
    LmHandleMessageFunction handler;
    int generation;
};

static OutgoingMessage*
outgoing_message_new(LmMessage               *message,
                     LmHandleMessageFunction  handler,
                     int                      generation)
{
    OutgoingMessage *outgoing = g_new0(OutgoingMessage, 1);
    outgoing->refcount = 1;
    outgoing->message = message;
    outgoing->handler = handler;
    outgoing->generation = generation;
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
        g_free(outgoing);
    }
}

/* === HippoConnection implementation === */

typedef enum {
    PROCESS_MESSAGE_IGNORE,
    PROCESS_MESSAGE_PEND,
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
static void     hippo_connection_process_pending_room_messages(HippoConnection *connection);

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
    LmConnection *lm_connection;
    /* queue of OutgoingMessage objects */
    GQueue *pending_outgoing_messages;
    /* queue of LmMessage */
    GQueue *pending_room_messages;
    HippoBrowserKind login_browser;
    int message_port;
    char *username;
    char *password;
    char *download_url;
    char *tooltip;
    int request_blocks_id;
    gint64 last_blocks_timestamp;
    gint64 server_time_offset;
    unsigned int too_old : 1;
    unsigned int upgrade_available : 1;
    unsigned int last_auth_failed : 1;
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
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_connection_init(HippoConnection *connection)
{
    connection->state = HIPPO_STATE_SIGNED_OUT;
    connection->pending_outgoing_messages = g_queue_new();
    connection->pending_room_messages = g_queue_new();

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

    object_class->finalize = hippo_connection_finalize;
}

static void
hippo_connection_finalize(GObject *object)
{
    HippoConnection *connection = HIPPO_CONNECTION(object);

    g_debug("Finalizing connection");

    hippo_connection_unqueue_request_blocks(connection);
    hippo_connection_stop_signin_timeout(connection);
    hippo_connection_stop_retry_timeout(connection);
    
    hippo_connection_disconnect(connection);
    
    g_queue_foreach(connection->pending_outgoing_messages,
                    (GFunc) outgoing_message_unref, NULL);
    g_queue_free(connection->pending_outgoing_messages);

    g_queue_foreach(connection->pending_room_messages,
                    (GFunc)lm_message_unref, NULL);
    g_queue_free(connection->pending_room_messages);

    g_free(connection->username);
    g_free(connection->password);
    g_free(connection->tooltip);

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

void
hippo_connection_notify_music_changed(HippoConnection *connection,
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


static gboolean 
retry_timeout(gpointer data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    g_debug("Retry timeout");

    hippo_connection_stop_retry_timeout(connection);
    hippo_connection_connect(connection, NULL);

    return FALSE;
}

static void
hippo_connection_start_retry_timeout(HippoConnection *connection)
{
    if (connection->retry_timeout_id == 0) {
        int timeout = RETRY_TIMEOUT + g_random_int_range(0, RETRY_TIMEOUT_FUZZ);
        g_debug("Installing retry timeout for %g seconds", timeout / 1000.0);
        connection->retry_timeout_id = g_timeout_add(timeout, 
                                                     retry_timeout, connection);
    }
}

static void
hippo_connection_stop_retry_timeout(HippoConnection *connection)
{
    if (connection->retry_timeout_id != 0) {
        g_debug("Removing retry timeout");
        g_source_remove (connection->retry_timeout_id);
        connection->retry_timeout_id = 0;
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

    g_debug("Connection state = %s connected = %d", hippo_state_debug_string(connection->state), connected);
    
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
                 LmHandleMessageFunction  handler)
{
    GError *error;
    
    if (connection->lm_connection == NULL) {
        g_debug("not sending message, not connected");
        return;
    }
    
    error = NULL;
    if (handler != NULL) {
        LmMessageHandler *handler_obj = lm_message_handler_new(handler, connection, NULL);
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
hippo_connection_send_message_with_reply(HippoConnection  *connection,
                                         LmMessage        *message,
                                         LmHandleMessageFunction handler,
                                         SendMode          mode)
{
    if (mode == SEND_MODE_IGNORE_IF_DISCONNECTED) {
        if (!hippo_connection_get_connected(connection))
            return;    
        else
            mode = SEND_MODE_AFTER_AUTH;
    }
    
    if (mode == SEND_MODE_IMMEDIATELY) {
        send_immediately(connection, message, handler);
    } else {
        g_queue_push_tail(connection->pending_outgoing_messages,
                          outgoing_message_new(message, handler, connection->generation));

        hippo_connection_flush_outgoing(connection);    
    }
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
            send_immediately(connection, om->message, om->handler);
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
                                    rbd->connection->last_blocks_timestamp);

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
        !node_matches(child, document_element_name, expected_namespace))
        {
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
    
    hippo_connection_send_message_with_reply(connection, message, on_client_info_reply, SEND_MODE_IMMEDIATELY);

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
    LmMessageNode *child;
    
    g_debug("Parsing prefs message");

    for (child = prefs_node->children; child != NULL; child = child->next) {
        const char *key = lm_message_node_get_attribute(child, "key");
        const char *value = lm_message_node_get_value(child);

        if (key == NULL) {
            g_debug("ignoring node '%s' with no 'key' attribute in prefs reply",
                    child->name);
            continue;
        }
        
        if (strcmp(key, "musicSharingEnabled") == 0) {
            music_sharing_enabled = value != NULL && parse_bool(value);
            saw_music_sharing_enabled = TRUE;
        } else if (strcmp(key, "musicSharingPrimed") == 0) {
            music_sharing_primed = value != NULL && parse_bool(value);
            saw_music_sharing_primed = TRUE;
        } else {
            g_debug("Unknown pref '%s'", key);
        }
    }
    
    /* Important to set primed then enabled, so when the signal is emitted from the 
     * data cache for enabled, primed will already be set.
     */
    if (saw_music_sharing_primed)
        hippo_data_cache_set_music_sharing_primed(connection->cache, music_sharing_primed);
    
    if (saw_music_sharing_enabled)
        hippo_data_cache_set_music_sharing_enabled(connection->cache, music_sharing_enabled);
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

    /* This takes ownership of the comments in the list but not the list itself */
    hippo_data_cache_set_myspace_blog_comments(connection->cache, comments);
    
    g_slist_free(comments);
    
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

    /* takes ownership of contacts but not the list */
    hippo_data_cache_set_myspace_contacts(connection->cache, contacts);
    
    g_slist_free(contacts);

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
 
static gboolean
hippo_connection_parse_blocks(HippoConnection *connection,
                              LmMessageNode   *node)
{
    LmMessageNode *subchild;
    gint64 server_timestamp;

    /* g_debug("Parsing blocks list <%s>", node->name); */
    
    if (!hippo_xml_split(connection->cache, node, NULL,
                         "serverTime", HIPPO_SPLIT_TIME_MS, &server_timestamp,
                         NULL)) {
        g_debug("missing serverTime on blocks");
        return FALSE;
    }

    hippo_connection_update_server_time_offset(connection, server_timestamp);
    
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
                                gint64           last_timestamp)
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
        photo_url = lm_message_node_get_attribute(node, "smallPhotoUrl");
        if (!photo_url) {
            g_warning("entity node lacks photo url");
            return FALSE;
        }
    } else {
        photo_url = NULL;
    }

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

    /* clear all data we have cached about the chat room */
    hippo_chat_room_reconnected(connection, room);

    /* but we preserved our "join count" so we know if we wanted
     * to be in the room
     */
    desired_state = hippo_chat_room_get_desired_state(room);

    hippo_connection_send_chat_room_state(connection, room, HIPPO_CHAT_STATE_NONMEMBER, desired_state);
}

void
hippo_connection_send_chat_room_message(HippoConnection *connection,
                                        HippoChatRoom   *room,
                                        const char      *text)
{
    const char *to;
    LmMessage *message;
    LmMessageNode *body;
        
    to = hippo_chat_room_get_jabber_id(room);
    message = lm_message_new(to, LM_MESSAGE_TYPE_MESSAGE);

    body = lm_message_node_add_child(message->node, "body", text);

    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
}

void
hippo_connection_send_quip(HippoConnection *connection,
                           HippoChatKind    kind,
                           const char      *id,
                           const char      *text,
                           HippoSentiment   sentiment)
{
    char *node, *to;
    LmMessage *message;
    LmMessageNode *body;
    LmMessageNode *child;
    
    node = hippo_id_to_jabber_id(id);
    to = g_strconcat(node, "@" HIPPO_ROOMS_JID_DOMAIN, NULL);
    g_free(node);

    message = lm_message_new(to, LM_MESSAGE_TYPE_MESSAGE);
    body = lm_message_node_add_child(message->node, "body", text);

    if (sentiment != HIPPO_SENTIMENT_INDIFFERENT) {
        child = lm_message_node_add_child(message->node, "messageInfo", NULL);
        lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/rooms");
        lm_message_node_set_attribute(child, "sentiment", hippo_sentiment_as_string(sentiment));
    }
    
    hippo_connection_send_message(connection, message, SEND_MODE_AFTER_AUTH);

    lm_message_unref(message);
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
    const char *title;
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

    title = lm_message_node_get_attribute(info_node, "title");
    /* title can be NULL, roomInfo only optionally has it */

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
        if (title)
            hippo_chat_room_set_title(room, title);
    
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

/* This reply has no content, it just signals the end of the 
 * chat room data we asked the server to send.
 */
static LmHandlerResult
on_get_chat_room_details_reply(LmMessageHandler *handler,
                               LmConnection     *lconnection,
                               LmMessage        *message,
                               gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    const char *from;
    char *chat_id;
    char *user_id;
    HippoChatRoom *room;
    HippoChatKind kind;
    LmMessageSubType subtype;
    
    subtype = lm_message_get_sub_type(message);

    if (!(subtype == LM_MESSAGE_SUB_TYPE_ERROR || subtype == LM_MESSAGE_SUB_TYPE_RESULT)) {
        g_warning("Chat room details IQ reply has unexpected subtype %d", subtype);
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    from = lm_message_node_get_attribute(message->node, "from");

    if (!from || !parse_room_jid(from, &chat_id, &user_id)) {
        g_warning("getChatRoomDetails reply doesn't come from a chat room!");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    g_debug("got chat room details reply for %s", chat_id);

    room = hippo_data_cache_lookup_chat_room(connection->cache, chat_id, &kind);
    if (!room) {
        g_warning("getChatRoomDetails reply for an unknown chatroom");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (!hippo_chat_room_get_loading(room)) {
        /* If this prints it's probably because of calling chat_room_clear without
         * actually disconnecting... so loading was set to false but the reply
         * still arrived.
         */
        g_warning("chat room should have loading=TRUE but does not on details reply");
    }
    
    g_object_ref(room); /* since we'll be emitting some signals */

    if (subtype == LM_MESSAGE_SUB_TYPE_ERROR) {
        hippo_chat_room_set_kind(room, HIPPO_CHAT_KIND_BROKEN);
    } else {
        /* Sets the kind */
        parse_room_info(connection, message, chat_id, NULL);
    }
 
    /* if we still don't know the kind, this thing is hosed, e.g. bad roomInfo */
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_UNKNOWN)   
        hippo_chat_room_set_kind(room, HIPPO_CHAT_KIND_BROKEN);
    kind = hippo_chat_room_get_kind(room);

    /* We require that the associated entity be loaded when we signal
     * a chat room as loaded, otherwise we crash.
     */
    if (kind == HIPPO_CHAT_KIND_POST && 
        hippo_data_cache_lookup_post(connection->cache, chat_id) == NULL) {
        g_warning("Couldn't find post associated with chat");
        goto out;
    } else if (kind == HIPPO_CHAT_KIND_GROUP &&
               hippo_data_cache_lookup_entity(connection->cache, chat_id) == NULL) {
        g_warning("Couldn't find entity associated with chat");
        goto out;
    }

    g_debug("Chat room loaded, kind=%d", hippo_chat_room_get_kind(room));
    /* notify listeners we're loaded */
    hippo_chat_room_set_loading(room, connection->generation, FALSE);

    /* FIXME the old code did the equivalent of emitting "changed" on 
     * HippoPost here so the UI could update. We want to do this by having
     * hippo_chat_room do that when set_loading(FALSE) perhaps, or just 
     * by allowing the UI to update as we go along. Or have the UI 
     * queue an idle when it gets the post changed signal, is another option.
     * Anyway, doing it here isn't convenient anymore with the new code.
     *
     * Remove this comment once we're sure we've replaced the functionality.
     */

    /* process any pending notifications of new users / messages */
    hippo_connection_process_pending_room_messages(connection);

 out:
    g_object_unref(room);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

void
hippo_connection_request_chat_room_details(HippoConnection *connection,
                                           HippoChatRoom   *room)
{
    const char *to;
    LmMessage *message;    
    LmMessageNode *child;
    
    if (hippo_chat_room_get_loading(room)) {
        g_debug("already loading chat room, not doing it again");
        return;
    }

    hippo_chat_room_set_loading(room, connection->generation, TRUE);
    to = hippo_chat_room_get_jabber_id(room);

    message = lm_message_new_with_sub_type(to, LM_MESSAGE_TYPE_IQ, LM_MESSAGE_SUB_TYPE_GET);

    child = lm_message_node_add_child (message->node, "details", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/rooms");

    hippo_connection_send_message_with_reply(connection, message, on_get_chat_room_details_reply,
                                             SEND_MODE_AFTER_AUTH);

    g_debug("Sent request for chat room details on '%s' '%s'",
            hippo_chat_room_get_id(room), hippo_chat_room_get_jabber_id(room));
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
        HippoTrack *track;
        HippoChatState status;
        const char *name;
        const char *photo_url = NULL;
        const char *role = NULL;
        const char *old_role = NULL;
        const char *arrangement_name = NULL;
        const char *artist = NULL;
        gboolean music_playing = FALSE;

        if (!hippo_xml_split(connection->cache, info_node, NULL,
                             "name", HIPPO_SPLIT_STRING, &name,
                             "smallPhotoUrl", HIPPO_SPLIT_URI_RELATIVE | HIPPO_SPLIT_OPTIONAL, &photo_url,
                             "role", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &role,
                             "old_role", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &old_role,
                             "arrangementName", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &arrangement_name,
                             "artist", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &artist,
                             "musicPlaying", HIPPO_SPLIT_BOOLEAN | HIPPO_SPLIT_OPTIONAL, &music_playing,
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

        track = hippo_track_new_deprecated(artist, arrangement_name, music_playing);
        g_object_set(G_OBJECT(person), "current-track", track, NULL);
        g_object_unref(track);
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
                          const char      *user_id,
                          gboolean         was_pending)
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
                      const char      *user_id,
                      gboolean         was_pending)
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
                     LmMessage       *message,
                     gboolean         was_pending)
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

    /* If we got a message and started loading the room in response to that,
     * we need to wait until we finish loading before we can start processing 
     * any non-history messages.  History messages are immediately appended to the 
     * chat room.
     */
    if (!is_history_message && hippo_chat_room_get_loading(room)) {
        result = PROCESS_MESSAGE_PEND;
        goto out;
    }

    if (lm_message_get_type(message) == LM_MESSAGE_TYPE_MESSAGE) {
        g_debug("hippo-connection::process_room_message processing room message");
        process_room_chat_message(connection, message, room, user_id, was_pending);
    } else if (lm_message_get_type(message) == LM_MESSAGE_TYPE_PRESENCE) {
        g_debug("hippo-connection::process_room_message processing room presence");
        process_room_presence(connection, message, room, user_id, was_pending);
    } else {
        g_debug("hippo-connection::process_room_message unknown message type");
    }

    result = PROCESS_MESSAGE_CONSUME;
    
 out:
  
    g_free(chat_id);
    g_free(user_id);
    
    return result;
}

static void 
hippo_connection_process_pending_room_messages(HippoConnection *connection)
{
    GList *l = connection->pending_room_messages->head;
    while (l) {
        GList *next = l->next;

        LmMessage *message = l->data;
        if (process_room_message(connection, message, TRUE) != PROCESS_MESSAGE_PEND) {
            g_queue_delete_link(connection->pending_room_messages, l);
            lm_message_unref(message);
        }
        
        l = next;
    }
}

/* === HippoConnection Loudmouth handlers === */

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
    
    switch (process_room_message(connection, message, FALSE)) {
    case PROCESS_MESSAGE_IGNORE:
        break;
    case PROCESS_MESSAGE_CONSUME:
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    case PROCESS_MESSAGE_PEND:
        lm_message_ref(message);
        g_queue_push_tail(connection->pending_room_messages, message);
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

    if (handle_myspace_name_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_myspace_contact_comment(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_group_membership_change(connection, message)) {
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
    g_debug("handle_presence");

    if (process_room_message(connection, message, FALSE) == PROCESS_MESSAGE_PEND) {
        lm_message_ref(message);
        g_queue_push_tail(connection->pending_room_messages, message);
    }

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

    hippo_connection_send_message(connection, message, SEND_MODE_IMMEDIATELY);
    hippo_connection_state_change(connection, HIPPO_STATE_AWAITING_CLIENT_INFO);

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

const char*
hippo_state_debug_string(HippoState state)
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
