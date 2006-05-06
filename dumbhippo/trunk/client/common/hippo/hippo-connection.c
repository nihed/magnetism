#include "hippo-connection.h"
#include "hippo-data-cache-internal.h"
#include <loudmouth/loudmouth.h>
#include <string.h>
#include <stdlib.h>

/* === CONSTANTS === */

static const int SIGN_IN_INITIAL_TIMEOUT = 5000;        /* 5 seconds */
static const int SIGN_IN_INITIAL_COUNT = 60;            /* 5 minutes */
static const int SIGN_IN_SUBSEQUENT_TIMEOUT = 30000;    /* 30 seconds */

static const int KEEP_ALIVE_RATE = 60;                  /* 1 minute; 0 disables */

static const int RETRY_TIMEOUT = 60000;                 /* 1 minute */


/* === OutgoingMessage internal class === */

typedef struct OutgoingMessage OutgoingMessage;

struct OutgoingMessage {
    int               refcount;
    LmMessage        *message;
    LmMessageHandler *handler;
};

static OutgoingMessage*
outgoing_message_new(LmMessage *message, LmMessageHandler *handler)
{
    OutgoingMessage *outgoing = g_new0(OutgoingMessage, 1);
    outgoing->refcount = 1;
    outgoing->message = message;
    outgoing->handler = handler;
    if (message)
        lm_message_ref(message);
    if (handler)
        lm_message_handler_ref(handler);
    return outgoing;
}

static void
outgoing_message_ref(OutgoingMessage *outgoing)
{
    g_return_if_fail(outgoing != NULL);
    g_return_if_fail(outgoing->refcount > 0);
    outgoing->refcount += 1;
}

static void
outgoing_message_unref(OutgoingMessage *outgoing)
{
    g_return_if_fail(outgoing != NULL);
    g_return_if_fail(outgoing->refcount > 0);
    outgoing->refcount -= 1;
    if (outgoing->refcount == 0) {
        if (outgoing->message)
            lm_message_unref(outgoing->message);
        if (outgoing->handler)
            lm_message_handler_unref(outgoing->handler);
        g_free(outgoing);
    }
}

/* === HippoConnection implementation === */

typedef enum {
    PROCESS_MESSAGE_IGNORE,
    PROCESS_MESSAGE_PEND,
    PROCESS_MESSAGE_CONSUME
} ProcessMessageResult;

static void hippo_connection_finalize(GObject *object);

static void     hippo_connection_start_signin_timeout (HippoConnection *connection);
static void     hippo_connection_stop_signin_timeout  (HippoConnection *connection);
static void     hippo_connection_start_retry_timeout  (HippoConnection *connection);
static void     hippo_connection_stop_retry_timeout   (HippoConnection *connection);
static void     hippo_connection_connect              (HippoConnection *connection);
static void     hippo_connection_disconnect           (HippoConnection *connection);
static void     hippo_connection_state_change         (HippoConnection *connection,
                                                       HippoState       state);
static void     hippo_connection_forget_auth          (HippoConnection *connection);
static gboolean hippo_connection_load_auth            (HippoConnection *connection);
static void     hippo_connection_authenticate         (HippoConnection *connection);
static void     hippo_connection_clear                (HippoConnection *connection);
static void     hippo_connection_flush_outgoing       (HippoConnection *connection);
static void     hippo_connection_send_message         (HippoConnection *connection,
                                                       LmMessage       *message);static void     hippo_connection_send_message_with_reply(HippoConnection *connection,
                                                       LmMessage *message,
                                                       LmMessageHandler *handler);
static void     hippo_connection_get_client_info      (HippoConnection *connection);
static void     hippo_connection_update_prefs         (HippoConnection *connection);
static void     hippo_connection_process_prefs_node   (HippoConnection *connection,
                                                       LmMessageNode   *prefs_node);
static void     hippo_connection_get_posts            (HippoConnection *connection);
static void     hippo_connection_get_post             (HippoConnection *connection,
                                                       const char      *post_id);
static void     hippo_connection_get_chat_room_details(HippoConnection *connection,
                                                       HippoChatRoom   *room);
static void     hippo_connection_process_pending_room_messages(HippoConnection *connection);

/* Loudmouth handlers */
static LmHandlerResult handle_message     (LmMessageHandler *handler,
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
    int signin_timeout_id;
    int signin_timeout_count;
    int retry_timeout_id;
    LmConnection *lm_connection;
    /* queue of OutgoingMessage objects */
    GQueue *pending_outgoing_messages;
    /* queue of LmMessage */
    GQueue *pending_room_messages;
    char *username;
    char *password;
};

struct _HippoConnectionClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoConnection, hippo_connection, G_TYPE_OBJECT);

enum {
    STATE_CHANGED,
    AUTH_FAILURE,
    AUTH_SUCCESS,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_connection_init(HippoConnection *connection)
{
    connection->state = HIPPO_STATE_SIGNED_OUT;
    connection->pending_outgoing_messages = g_queue_new();
    connection->pending_room_messages = g_queue_new();
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

    signals[AUTH_FAILURE] =
        g_signal_new ("auth-failure",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0); 

    signals[AUTH_SUCCESS] =
        g_signal_new ("auth-success",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0); 
          
    object_class->finalize = hippo_connection_finalize;
}

static void
hippo_connection_finalize(GObject *object)
{
    HippoConnection *connection = HIPPO_CONNECTION(object);

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

    g_object_unref(connection->platform);
    connection->platform = NULL;

    G_OBJECT_CLASS(hippo_connection_parent_class)->finalize(object); 
}


/* === HippoConnection exported API === */


/* "platform" should be a construct property, but I'm lazy */
HippoConnection*
hippo_connection_new(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);

    HippoConnection *connection = g_object_new(HIPPO_TYPE_CONNECTION, NULL);
    
    connection->platform = platform;
    g_object_ref(connection->platform);
    
    return connection;
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
            hippo_connection_connect(connection);
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

void 
hippo_connection_notify_post_clicked(HippoConnection *connection,
                                     const char      *post_id)
{
    LmMessage *message;
    
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *method = lm_message_node_add_child (node, "method", NULL);
    lm_message_node_set_attribute(method, "xmlns", "http://dumbhippo.com/protocol/servermethod");
    lm_message_node_set_attribute(method, "name", "postClicked");
    LmMessageNode *guid_arg = lm_message_node_add_child (method, "arg", NULL);
    lm_message_node_set_value (guid_arg, post_id);

    hippo_connection_send_message(connection, message);

    lm_message_unref(message);
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
hippo_connection_notify_music_changed(HippoConnection *connection,
                                      gboolean         currently_playing,
                                      const HippoSong *song)
{
    LmMessage *message;
    
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(!currently_playing || song != NULL);
    
    /* If the user has music sharing off, then we never send their info
     * to the server. (this is a last-ditch protection; we aren't supposed
     * to be monitoring the music app either in this case)
     */
    if (!hippo_data_cache_get_music_sharing_enabled(connection->cache))
        return;

    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "musicChanged");

    if (currently_playing) {
        g_assert(song != NULL);
        add_track_props(music, song->keys, song->values);
    }

    hippo_connection_send_message(connection, message);

    lm_message_unref(message);
    /* g_print("Sent music changed xmpp message"); */
}

void
hippo_connection_provide_priming_music(HippoConnection  *connection,
                                       const HippoSong **songs,
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

    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    node = lm_message_get_node(message);

    music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "primingTracks");

    for (i = 0; i < n_songs; ++i) {
        LmMessageNode *track = lm_message_node_add_child(music, "track", NULL);
        add_track_props(track, songs[i]->keys, songs[i]->values);
    }

    hippo_connection_send_message(connection, message);

    lm_message_unref(message);
    /* we should also get back a notification from the server when this changes,
     * but we want to avoid re-priming so this adds robustness
     */
    hippo_data_cache_set_music_sharing_primed(connection->cache, TRUE);
}


/* === HippoConnection private methods === */

static void
hippo_connection_connect_failure(HippoConnection *connection,
                                 const char      *message)
{
    /* message can be NULL */
    hippo_connection_clear(connection);
    hippo_connection_start_retry_timeout(connection);
    hippo_connection_state_change(connection, HIPPO_STATE_RETRYING);
}

static void
hippo_connection_auth_failure(HippoConnection *connection,
                              const char      *message)
{
    /* message can be NULL */
    hippo_connection_forget_auth(connection);
    hippo_connection_start_signin_timeout(connection);
    hippo_connection_state_change(connection, HIPPO_STATE_AUTH_WAIT);
    g_signal_emit(connection, signals[AUTH_FAILURE], 0);
}

static gboolean 
signin_timeout(gpointer data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    if (hippo_connection_load_auth(connection)) {
        hippo_connection_stop_signin_timeout(connection);

        if (connection->state == HIPPO_STATE_AUTH_WAIT)
            hippo_connection_authenticate(connection);
        else
            hippo_connection_connect(connection);

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
        connection->signin_timeout_id = g_timeout_add(SIGN_IN_INITIAL_TIMEOUT, 
                                                      signin_timeout, connection);
        connection->signin_timeout_count = 0;
    }    
}

static void
hippo_connection_stop_signin_timeout(HippoConnection *connection)
{
    if (connection->signin_timeout_id != 0) {
        g_source_remove (connection->signin_timeout_id);
        connection->signin_timeout_id = 0;
        connection->signin_timeout_count = 0;
    }
}


static gboolean 
retry_timeout(gpointer data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    hippo_connection_stop_retry_timeout(connection);
    hippo_connection_connect(connection);

    return FALSE;
}

static void
hippo_connection_start_retry_timeout(HippoConnection *connection)
{
    if (connection->retry_timeout_id == 0)
        connection->retry_timeout_id = g_timeout_add(RETRY_TIMEOUT, 
                                                    retry_timeout, connection);

}

static void
hippo_connection_stop_retry_timeout(HippoConnection *connection)
{
    if (connection->retry_timeout_id != 0) {
        g_source_remove (connection->retry_timeout_id);
        connection->retry_timeout_id = 0;
    }
}

static void
hippo_connection_connect(HippoConnection *connection)
{
    char *message_host;
    int message_port;

    hippo_platform_get_message_host_port(connection->platform, &message_host, &message_port);

    if (connection->lm_connection != NULL) {
        g_warning("hippo_connection_connect() called when already connected");
        return;
    }

    connection->lm_connection = lm_connection_new(message_host);
    lm_connection_set_port(connection->lm_connection, message_port);
    lm_connection_set_keep_alive_rate(connection->lm_connection, KEEP_ALIVE_RATE);

    LmMessageHandler *handler = lm_message_handler_new(handle_message, connection, NULL);
    lm_connection_register_message_handler(connection->lm_connection, handler, 
                                           LM_MESSAGE_TYPE_MESSAGE, 
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    handler = lm_message_handler_new(handle_presence, connection, NULL);
    lm_connection_register_message_handler(connection->lm_connection, handler, 
                                           LM_MESSAGE_TYPE_PRESENCE, 
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    lm_connection_set_disconnect_function(connection->lm_connection,
            handle_disconnect, connection, NULL);

    hippo_connection_state_change(connection, HIPPO_STATE_CONNECTING);

    GError *error = NULL;

    /* If lm_connection returns FALSE, then handle_open won't be called
     * at all. On a TRUE return it will be called exactly once, but that 
     * call might occur before or after lm_connection_open() returns, and
     * may occur for success or for failure.
     */
    if (!lm_connection_open(connection->lm_connection, 
                            handle_open, connection, NULL, 
                            &error)) 
    {
        hippo_connection_connect_failure(connection, error ? error->message : "");
        if (error)
            g_error_free(error);
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
    if (connection->state == state)
        return;
        
    connection->state = state;    
    hippo_connection_flush_outgoing(connection);

    g_debug("Connection state = %s", hippo_state_debug_string(connection->state));
    g_signal_emit(connection, signals[STATE_CHANGED], 0);
}

static void
zero_str(char **s_p)
{
    g_free(*s_p);
    *s_p = NULL;
}

static void
hippo_connection_forget_auth(HippoConnection *connection)
{
    hippo_platform_delete_login_cookie(connection->platform);
    zero_str(&connection->username);
    zero_str(&connection->password);
}

static gboolean
hippo_connection_load_auth(HippoConnection *connection)
{    
    /* always clear current username/password */
    zero_str(&connection->username);
    zero_str(&connection->password);
    
    return hippo_platform_read_login_cookie(connection->platform,
                &connection->username, &connection->password);
}

static void
hippo_connection_authenticate(HippoConnection *connection)
{
    char *jabber_id;
    char *resource;
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
    g_free(resource);
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
                              LmMessage       *message)
{
    hippo_connection_send_message_with_reply(connection, message, NULL);
}

static void
hippo_connection_send_message_with_reply(HippoConnection  *connection,
                                         LmMessage        *message,
                                         LmMessageHandler *handler)
{
    g_queue_push_tail(connection->pending_outgoing_messages, outgoing_message_new(message, handler));

    hippo_connection_flush_outgoing(connection);
}

static void
hippo_connection_flush_outgoing(HippoConnection *connection)
{
#if 0
    if (connection->pending_outgoing_messages->length > 1 &&
        connection->state == HIPPO_STATE_AUTHENTICATED)
        g_print("%d messages backlog to clear", connection->pending_outgoing_messages->length);
#endif

    while (connection->state == HIPPO_STATE_AUTHENTICATED &&
            connection->pending_outgoing_messages->length > 0) {
        OutgoingMessage *om = g_queue_pop_head(connection->pending_outgoing_messages);
        GError *error = NULL;
        if (om->handler != NULL)
            lm_connection_send_with_reply(connection->lm_connection, om->message, om->handler, &error);
        else
            lm_connection_send(connection->lm_connection, om->message, &error);
        if (error) {
            // g_printerr("Failed sending message: %s", error->message);
            g_error_free(error);
        }
        outgoing_message_unref(om);
    }

#if 0
    if (connection->pending_outgoing_messages->length > 0)
        g_print("%d messages could not be sent now, since we aren't connected; deferring",
                connection->pending_outgoing_messages->length);
#endif
}

static gboolean
node_matches(LmMessageNode *node, const char *name, const char *expectedNamespace)
{
    const char *ns = lm_message_node_get_attribute(node, "xmlns");
    if (expectedNamespace && !ns)
        return FALSE;
    return strcmp(name, node->name) == 0 && (expectedNamespace == NULL || strcmp(expectedNamespace, ns) == 0);
}

LmMessageNode*
find_child_node(LmMessageNode *node, 
                const char    *element_namespace, 
                const char    *element_name)
{
    LmMessageNode *child;
    for (child = node->children; child; child = child->next) {
        const char *ns = lm_message_node_get_attribute(child, "xmlns");
        if (!(ns && strcmp(ns, element_namespace) == 0 && child->name))
            continue;
        if (strcmp(child->name, element_name) != 0)
            continue;

        return child;
    }

    return NULL;
}

static gboolean
message_is_iq_with_namespace(LmMessage *message, const char *expectedNamespace, const char *documentElementName)
{
    LmMessageNode *child = message->node->children;

    if (lm_message_get_type(message) != LM_MESSAGE_TYPE_IQ ||
        lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_RESULT ||
        !child || child->next ||
        !node_matches(child, documentElementName, expectedNamespace))
    {
        return FALSE;
    }
    return TRUE;
}

static LmHandlerResult
on_client_info_reply(LmMessageHandler *handler,
                     LmConnection     *lconnection,
                     LmMessage        *message,
                     gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    LmMessageNode *child = message->node->children;

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/clientinfo", "clientInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    const char *minimum = lm_message_node_get_attribute(child, "minimum");
    const char *current = lm_message_node_get_attribute(child, "current");
    const char *download = lm_message_node_get_attribute(child, "download");

    if (!minimum || !current || !download) {
        g_warning("clientInfo reply missing attributes");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    g_debug("Got clientInfo response: minimum=%s, current=%s, download=%s", minimum, current, download);

    /* FIXME store this somewhere */

    /* Next get the MySpace info, current hotness, recent posts */
    /* FIXME 
    im->getMySpaceName();
    im->getHotness();
    */
    
    /* get some recent posts */
    hippo_connection_get_posts(connection);
    
    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

static void
hippo_connection_get_client_info(HippoConnection *connection)
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "clientInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/clientinfo");
#ifdef G_OS_WIN32
#define PLATFORM "windows"
#endif
#ifdef G_OS_UNIX
#define PLATFORM "x11"
#endif 
    /* FIXME windows hardcoded for now because the server won't reply about linux, need to 
     * fix... it's unclear this is useful for linux anyhow since the linux client uses RPM
     * and thus won't self-upgrade
     */
    lm_message_node_set_attribute(child, "platform", "windows");
    LmMessageHandler *handler = lm_message_handler_new(on_client_info_reply, connection, NULL);

    hippo_connection_send_message_with_reply(connection, message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
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

    hippo_connection_process_prefs_node(connection, prefs_node);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

static void
hippo_connection_update_prefs(HippoConnection *connection)
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "prefs", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/prefs");
    LmMessageHandler *handler = lm_message_handler_new(on_prefs_reply, connection, NULL);

    hippo_connection_send_message_with_reply(connection, message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    g_debug("Sent request for prefs");
}

static void
hippo_connection_process_prefs_node(HippoConnection *connection,
                                    LmMessageNode   *prefs_node)
{
    gboolean music_sharing_enabled = FALSE;
    gboolean saw_music_sharing_enabled = FALSE;
    gboolean music_sharing_primed = TRUE;
    gboolean saw_music_sharing_primed = FALSE;
    LmMessageNode *child;
    
    for (child = prefs_node->children; child != NULL; child = child->next) {
        const char *key = lm_message_node_get_attribute(child, "key");
        const char *value = lm_message_node_get_value(child);

        if (key == NULL) {
            g_debug("ignoring node '%s' with no 'key' attribute in prefs reply",
                    child->name);
            continue;
        }
        
        if (strcmp(key, "musicSharingEnabled") == 0) {
            music_sharing_enabled = value != NULL && strcmp(value, "TRUE") == 0;
            saw_music_sharing_enabled = TRUE;
        } else if (strcmp(key, "musicSharingPrimed") == 0) {
            music_sharing_primed = value != NULL && strcmp(value, "TRUE") == 0;
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
    LmMessageNode *subchild;
    
    viewers = NULL;
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
        
        /* FIXME the old C++ code did not create an entity here if it wasn't 
         * already in the cache ... not clear to me why, needs another look
         * (is it because we don't know the entity type?)
         */
        /* entity = hippo_data_cache_ensure_bare_entity(connection->cache, ?, entity_id); */
        entity = hippo_data_cache_lookup_entity(connection->cache, entity_id);
        if (entity)
            viewers = g_slist_prepend(viewers, entity);
    }

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
        || strcmp(node->name, "user") == 0)
        return TRUE;
    return FALSE;
}

static gboolean
hippo_connection_parse_entity(HippoConnection *connection,
                              LmMessageNode   *node)
{
    HippoEntity *entity;
    gboolean created_entity;
    const char *guid;
    const char *name;
    const char *small_photo_url;
 
    HippoEntityType type;
    if (strcmp(node->name, "resource") == 0)
        type = HIPPO_ENTITY_RESOURCE;
    else if (strcmp(node->name, "group") == 0)
        type = HIPPO_ENTITY_GROUP;
    else if (strcmp(node->name, "user") == 0)
        type = HIPPO_ENTITY_PERSON;
    else
        return FALSE;

    guid = lm_message_node_get_attribute(node, "id");
    if (!guid)
        return FALSE;

    if (type != HIPPO_ENTITY_RESOURCE) {
        name = lm_message_node_get_attribute(node, "name");
        if (!name)
            return FALSE;
    } else {
        name = NULL;
    }

    if (type != HIPPO_ENTITY_RESOURCE) {
        small_photo_url = lm_message_node_get_attribute(node, "smallPhotoUrl");
        if (!small_photo_url)
            return FALSE;
    } else {
        small_photo_url = NULL;
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
    hippo_entity_set_small_photo_url(entity, small_photo_url);
   
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
                            LmMessageNode   *post_node)
{
    LmMessageNode *node;
    HippoPost *post;
    const char *post_guid;
    const char *sender_guid;
    const char *url;
    const char *title;
    const char *text;
    const char *info;
    GTime post_date;
    GSList *recipients = NULL;
    LmMessageNode *subchild;
    gboolean created_post;
    
    g_assert(connection->cache != NULL);

    post_guid = lm_message_node_get_attribute (post_node, "id");
    if (!post_guid)
        return FALSE;

    node = lm_message_node_get_child (post_node, "poster");
    if (!(node && node->value))
        return FALSE;
    sender_guid = node->value;

    node = lm_message_node_get_child (post_node, "href");
    if (!(node && node->value))
        return FALSE;
    url = node->value;

    node = lm_message_node_get_child (post_node, "title");
    if (!(node && node->value))
        return FALSE;
    title = node->value;

    node = lm_message_node_get_child (post_node, "text");
    if (!(node && node->value))
        text = "";
    else
        text = node->value;

    node = lm_message_node_get_child (post_node, "postInfo");
    if (node && node->value)
        info = node->value;
    else
        info = NULL;

    node = lm_message_node_get_child (post_node, "postDate");
    if (!(node && node->value))
        return FALSE;
    post_date = strtol(node->value, NULL, 10);

    node = lm_message_node_get_child (post_node, "recipients");
    if (!node)
        return FALSE;
    for (subchild = node->children; subchild; subchild = subchild->next) {
        const char *entity_id;
        HippoEntity *entity;
        if (!get_entity_guid(subchild, &entity_id))
            return FALSE;
        
        /* FIXME the old C++ code did not create an entity here if it wasn't 
         * already in the cache ... not clear to me why, needs another look
         * (is it because we don't know the entity type?)
         */
        
        /* entity = hippo_data_cache_ensure_bare_entity(connection->cache, ?, entity_id); */
        entity = hippo_data_cache_lookup_entity(connection->cache, entity_id);
        if (entity)
            recipients = g_slist_prepend(recipients, entity);
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

    g_debug("Parsed post %s", post_guid);

    hippo_post_set_sender(post, sender_guid);
    hippo_post_set_url(post, url);
    hippo_post_set_title(post, title);
    hippo_post_set_description(post, text);
    hippo_post_set_info(post, info);
    hippo_post_set_date(post, post_date);
    hippo_post_set_recipients(post, recipients);

    g_slist_free(recipients);

    if (created_post) {
        HippoChatRoom *room;
        
        hippo_data_cache_add_post(connection->cache, post);
    
        /* Start filling in chatroom information for this post asynchronously */
        
        room = hippo_data_cache_ensure_chat_room(connection->cache, post_guid, HIPPO_CHAT_POST);

        hippo_connection_get_chat_room_details(connection, room);
    }
    
    g_object_unref(post);

    return TRUE;
}

static gboolean
hippo_connection_parse_post_stream(HippoConnection *connection,
                                   LmMessageNode   *node,
                                   const char      *func_name)
{
    LmMessageNode *subchild;
    for (subchild = node->children; subchild; subchild = subchild->next) {
        if (is_entity(subchild)) {
            if (!hippo_connection_parse_entity(connection, subchild)) {
                g_warning("failed to parse entity in %s", func_name);
                return FALSE;
            }
        } else if (is_post(subchild)) {
            if (!hippo_connection_parse_post(connection, subchild)) {
                g_warning("failed to parse post in %s", func_name);
                return FALSE;
            }
        } else if (is_live_post(subchild)) {
            if (!hippo_connection_parse_live_post(connection, subchild, NULL)) {
                g_warning("failed to parse live post in %s", func_name);
                return FALSE;
            }
        }
    }
    return TRUE;
}

static gboolean
hippo_connection_parse_post_data(HippoConnection *connection,
                                 LmMessageNode   *node,
                                 const char      *func_name)
{
    gboolean seen_post;
    gboolean seen_live_post;
    
    seen_post = FALSE;
    seen_live_post = FALSE;

    LmMessageNode *subchild;
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

            if (!hippo_connection_parse_post(connection, subchild)) {
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

static LmHandlerResult
on_get_posts_reply(LmMessageHandler *handler,
                   LmConnection     *lconnection,
                   LmMessage        *message,
                   gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);
    LmMessageNode *child = message->node->children;

    g_debug("Got reply for getRecentPosts");

    if (!message_is_iq_with_namespace(message, "http://dumbhippo.com/protocol/post", "recentPosts")) {
        g_warning("Mismatched getRecentPosts reply");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    hippo_connection_parse_post_stream(connection, child, "on_get_posts_reply");

    /* Some chat room messages where we waiting for results may now be ready to process */
    hippo_connection_process_pending_room_messages(connection);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

/* The recentPosts IQ is overloaded a bit, can also be used to get a specific post;
 * there are wrappers for this for clarity, don't call this directly
 */
static void
hippo_connection_get_recent_posts(HippoConnection *connection,
                                  const char      *post_id)
{
    LmMessage *message;
    LmMessageNode *node;
    LmMessageNode *child;
    LmMessageHandler *handler;
    
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    node = lm_message_get_node(message);
    
    child = lm_message_node_add_child (node, "recentPosts", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/post");

    if (post_id)
        lm_message_node_set_attribute(child, "id", post_id);

    handler = lm_message_handler_new(on_get_posts_reply, connection, NULL);

    hippo_connection_send_message_with_reply(connection, message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    g_debug("Sent request for recent posts");
}

static void
hippo_connection_get_posts(HippoConnection *connection)
{
    hippo_connection_get_recent_posts(connection, NULL);
}

static void
hippo_connection_get_post(HippoConnection *connection,
                          const char      *post_id)
{
    g_return_if_fail(post_id != NULL);
    hippo_connection_get_recent_posts(connection, post_id);
}

static void 
send_room_presence(HippoConnection *connection,
                   HippoChatRoom   *room,
                   LmMessageSubType subtype,
                   HippoChatState   state)
{
    const char *to;
    LmMessage *message;
    
    if (state == HIPPO_CHAT_NONMEMBER)
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
                                      state == HIPPO_CHAT_PARTICIPANT ? "participant" : "visitor");
    }

    hippo_connection_send_message(connection, message);

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
                       HIPPO_CHAT_PARTICIPANT);
}

void
hippo_connection_send_chat_room_state(HippoConnection *connection,
                                      HippoChatRoom   *room,
                                      HippoChatState   old_state,
                                      HippoChatState   new_state)
{
    if (connection->state != HIPPO_STATE_AUTHENTICATED)
        return;
    
    if (old_state == new_state)
        return;
    
    if (old_state == HIPPO_CHAT_NONMEMBER) {
        hippo_chat_room_clear(room);
        hippo_connection_send_chat_room_enter(connection, room, new_state);
    } else if (new_state == HIPPO_CHAT_NONMEMBER) {
        hippo_connection_send_chat_room_leave(connection, room);
    } else {
        /* Change from Visitor => Participant or vice-versa */
        hippo_connection_send_chat_room_enter(connection, room, new_state);
    }
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
        
    if (strncmp(at + 1, "rooms.dumbhippo.com", slash - (at + 1)) != 0)
        return FALSE;

    room_name = g_strndup(jid, at - jid);
    *chat_id_p = hippo_id_from_jabber_id(room_name);
    g_free(room_name);

    if (*chat_id_p == NULL)
        return FALSE;

    if (slash) {
        *user_id_p = hippo_id_from_jabber_id(slash + 1);
        if (*user_id_p == NULL) {
            g_free(*chat_id_p);
            *chat_id_p = NULL;
            return FALSE;
        }
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

    from = lm_message_node_get_attribute(message->node, "from");

    if (!from || !parse_room_jid(from, &chat_id, &user_id)) {
        g_warning("getChatRoomDetails reply doesn't come from a chat room!");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    g_debug("end of chat room details for %s", chat_id);

    room = hippo_data_cache_lookup_chat_room(connection->cache, chat_id, &kind);
    if (!room) {
        g_warning("getChatRoomDetails reply for an unknown chatroom");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    hippo_chat_room_set_filling(room, FALSE);

    /* FIXME the old code did the equivalent of emitting "changed" on 
     * HippoPost here so the UI could update. We want to do this by having
     * hippo_chat_room do that when set_filling(FALSE) perhaps, or just 
     * by allowing the UI to update as we go along. Or have the UI 
     * queue an idle when it gets the post changed signal, is another option.
     * Anyway, doing it here isn't convenient anymore with the new code.
     *
     * Remove this comment once we're sure we've replaced the functionality.
     */

    /* process any pending notifications of new users / messages */
    hippo_connection_process_pending_room_messages(connection);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

static void
hippo_connection_get_chat_room_details(HippoConnection *connection,
                                       HippoChatRoom   *room)
{
    const char *to;
    LmMessage *message;    
    LmMessageNode *child;
    LmMessageHandler *handler;
    
    if (hippo_chat_room_get_filling(room)) {
        /* not supposed to happen, if it does we could change 
         * "filling" flag to a count
         */
        g_warning("already filling chat room and tried to do so again");
        return;
    }

    hippo_chat_room_set_filling(room, TRUE);
    to = hippo_chat_room_get_jabber_id(room);

    message = lm_message_new_with_sub_type(to, LM_MESSAGE_TYPE_IQ, LM_MESSAGE_SUB_TYPE_GET);

    child = lm_message_node_add_child (message->node, "details", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/rooms");

    handler = lm_message_handler_new(on_get_chat_room_details_reply, connection, NULL);

    hippo_connection_send_message_with_reply(connection, message, handler);

    g_debug("Sent request for chat room details");
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
        int version_int;
        HippoChatState status;
        gboolean music_playing_bool;
        const char *version = lm_message_node_get_attribute(info_node, "version");
        const char *name = lm_message_node_get_attribute(info_node, "name");
        const char *role = lm_message_node_get_attribute(info_node, "role");
        const char *old_role = lm_message_node_get_attribute(info_node, "oldRole");
        const char *arrangement_name = lm_message_node_get_attribute(info_node, "arrangementName");
        const char *artist = lm_message_node_get_attribute(info_node, "artist");
        const char *music_playing = lm_message_node_get_attribute(info_node, "musicPlaying");

        if (!version || !name) {
            g_warning("userInfo node without name and version");
            return FALSE;
        }        

        version_int = atoi(version);
        music_playing_bool = music_playing ? strcmp(music_playing, "TRUE") == 0 : FALSE;
        
        if (!role)
            status = HIPPO_CHAT_PARTICIPANT;
        else
            status = strcmp(role, "participant") == 0 ? HIPPO_CHAT_PARTICIPANT : HIPPO_CHAT_VISITOR;

        *status_p = status;
        *newly_joined_p = old_role && strcmp(old_role, "nonmember") == 0;
    
        hippo_entity_set_version(HIPPO_ENTITY(person), version_int);
        hippo_entity_set_name(HIPPO_ENTITY(person), name);
        hippo_person_set_current_song(person, arrangement_name);
        hippo_person_set_current_artist(person, artist);
        hippo_person_set_music_playing(person, music_playing_bool);  
    }

    return TRUE;
}

static HippoChatMessage*
parse_chat_message_info(HippoConnection  *connection, 
                        LmMessageNode    *parent,
                        HippoPerson      *sender,
                        const char       *text,
                        int              *version_p,
                        const char      **name_p)
{
    LmMessageNode *info_node;
    
    info_node = find_child_node(parent, "http://dumbhippo.com/protocol/rooms", "messageInfo");
    if (!info_node) {
        g_debug("Can't find messageInfo node");
        return FALSE;
    }

    {
        const char *version_str = lm_message_node_get_attribute(info_node, "version");
        const char *name_str = lm_message_node_get_attribute(info_node, "name");
        const char *timestamp_str = lm_message_node_get_attribute(info_node, "timestamp");
        const char *serial_str = lm_message_node_get_attribute(info_node, "serial");
        GTime timestamp;
        int serial;

        if (!version_str || !name_str || !timestamp_str || !serial_str) {
            g_debug("messageInfo node without all fields");
            return NULL;
        }

        *version_p = atoi(version_str);
        *name_p = name_str;

        timestamp = strtol(timestamp_str, NULL, 10);
        serial = atoi(serial_str);
        
        return hippo_chat_message_new(sender, text, timestamp, serial);
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
    int version;
    const char *name;
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
                                           text, &version, &name);
    if (chat_message == NULL)
        return;

    /* update new info about the user */
    hippo_entity_set_version(sender, version);
    hippo_entity_set_name(sender, name);

    /* We can usually skip this in the case where the message was pending - but
     * it's tricky to get it exactly right. See comments in handleRoomPresence().
     * Unlike presence, it's harmless to add the message again since we catch
     * duplicate serials and ignore them.
     */
    /* Note, this passes ownership of the chat message and potentially 
     * frees it immediately if it's a dup
     */
    hippo_chat_room_add_message(room, chat_message);

    
    /* FIXME verify this is now handled by signals, then delete this
    
    HippoPost *post = ui_->getDataCache().getPost(room->getChatId());
    if (post && !room->getFilling()) {
        if (room->getState() == HippoChatRoom::NONMEMBER)
            ui_->onChatRoomMessage(post);
        else
            ui_->updatePost(post);
    }
    */
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

    /* FIXME - this was in the old code but makes no sense to me 
       because I don't see where we update the viewer list on the post
       again, and we don't fall back to using the chat room viewers list.
       
       See also comments in hippo-post.c about keeping redundant info
       on the post itself and in the chat room.
       
       I think this line can be deleted, and HippoPost changed so that if 
       it has a chat room, it always uses the viewer list from that and 
       not post->viewers
       
    if (post)
        post->resetCurrentViewers();
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

        /* FIXME this should now work via signals, verify then remove this comment
        if (artist && arrangementName) {
            room->updateMusicForUser(userId, arrangementName, artist, musicPlaying);
        }
        */

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
        /* FIXME this mess should get replaced by signal handling... verify then remove
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
            hippo_chat_room_set_user_state(room, HIPPO_PERSON(entity), HIPPO_CHAT_NONMEMBER);            /* FIXME be sure we handle this via signals 
            if (post && !room->getFilling())
                ui_->updatePost(post);
            */
        }
    }
}

ProcessMessageResult
process_room_message(HippoConnection *connection,
                     LmMessage       *message,
                     gboolean         was_pending)
{
    /* this could be a chat message or a presence notification */

    ProcessMessageResult result;
    const char *from;
    LmMessageNode *info_node;
    const char *kind_str;
    HippoChatKind kind;
    char *chat_id;
    char *user_id;
    HippoChatRoom *room;
    HippoChatKind existing_kind;

    chat_id = NULL;
    user_id = NULL;

    /* IGNORE = run other handlers CONSUME = we handled it
     * PEND = save for after we fill chatroom
     */
    result = PROCESS_MESSAGE_IGNORE;

    from = lm_message_node_get_attribute(message->node, "from");
    info_node = find_child_node(message->node, "http://dumbhippo.com/protocol/rooms", "roomInfo");
    if (!info_node) {
        g_debug("Can't find roomInfo node");
        goto out;
    }

    kind_str = lm_message_node_get_attribute(info_node, "kind");
    if (kind_str == NULL) {
        /* assume it's an old server which lacked "kind" but was always about posts */
        kind = HIPPO_CHAT_POST;
    } else if (strcmp(kind_str, "post") == 0) {
        kind = HIPPO_CHAT_POST;
    } else if (strcmp(kind_str, "group") == 0) {
        kind = HIPPO_CHAT_GROUP;
    } else {
        g_warning("Unknown chat kind %s", kind_str);
        goto out;
    }

    if (!from || !parse_room_jid(from, &chat_id, &user_id)) {
        g_warning("Failed to parse room ID");
        goto out;
    }

    room = hippo_data_cache_lookup_chat_room(connection->cache, chat_id, &existing_kind);
    
    if (room && existing_kind != kind) {
        g_warning("confusion about kind of room %s, giving up", chat_id);
        goto out;
    }
    
    if (!room) {
        /* This is a spontaneous message from a chatroom, which we'll bubble up
         * for the user, right now only if the chat is about a post.
         * In order to do that, we need both the rest of the information
         * about the chatroom (the present users, and so forth), and also the information
         * about the post like the title, description, and so on. We could get
         * these two things in parallel, but to simplify, we first get the 
         * post, to have a place to hang the chatroom off of, then we get the
         * chat room information.
         *
         * Here we rely on the fact that we always get the chat room for every post.
         */
        if (kind == HIPPO_CHAT_POST) {
            HippoPost *post;
            
            post = hippo_data_cache_lookup_post(connection->cache, chat_id);
            if (!post) {
                /* If multiple messages from a chatroom that we aren't part of come in,
                 * we might retrieve the chat room information multiple times; but 
                 * that's harmless, so we don't bother keeping track of what posts
                 * we are in the process of retrieving.
                 */
                 hippo_connection_get_post(connection, chat_id);
            }
            result = PROCESS_MESSAGE_PEND;
            goto out;
        } else {
            /* spontaneous messages that aren't about posts we just want to ignore */
            result = PROCESS_MESSAGE_CONSUME;
            goto out;
        }
    }

    /* If we got a message and request the room contents in response to that,
     * wait until we finish until we process that message
     */
    if (was_pending && hippo_chat_room_get_filling(room)) {
        result = PROCESS_MESSAGE_PEND;
        goto out;
    }

    if (lm_message_get_type(message) == LM_MESSAGE_TYPE_MESSAGE)
        process_room_chat_message(connection, message, room, user_id, was_pending);
    else if (lm_message_get_type(message) == LM_MESSAGE_TYPE_PRESENCE)
        process_room_presence(connection, message, room, user_id, was_pending);

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

gboolean
handle_live_post_changed(HippoConnection *connection,
                         LmMessage       *message)
{
    LmMessageNode *child;
    
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE
        && lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_NOT_SET)
        return FALSE;

    child = find_child_node(message->node,
                    "http://dumbhippo.com/protocol/post", "livePostChanged");
    if (child == NULL)
        return FALSE;   

    g_debug("handling livePostChanged message");

    if (!hippo_connection_parse_post_data(connection, child, "livePostChanged")) {
        g_warning("failed to parse post stream from livePostChanged");
        return FALSE;
    }

    /* We don't display any information from the link message currently -- the bubbling
     * up when viewers are added comes from the separate "chat room" path, so just
     * suppress things here. There's some work later to rip out this path, assuming that
     * we actually don't ever need to update the bubble display from livePostChanged
     * ui_->onLinkMessage(post, FALSE);
     */

    return TRUE;
}

gboolean
handle_active_posts_message(HippoConnection *connection,
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
                if (!hippo_connection_parse_post(connection, subchild)) {
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

static HippoHotness
hotness_from_string(const char *str)
{
#define M(s,v) do { if (strcmp(str,(s)) == 0) { return (v); } } while(0)
    M("COLD", HIPPO_HOTNESS_COLD);
    M("COOL", HIPPO_HOTNESS_COOL);
    M("WARM", HIPPO_HOTNESS_WARM);
    M("GETTING_HOT", HIPPO_HOTNESS_GETTING_HOT);
    M("HOT", HIPPO_HOTNESS_HOT);
    
    return HIPPO_HOTNESS_UNKNOWN;
}

gboolean
handle_hotness_changed(HippoConnection *connection,
                       LmMessage       *message)
{
   if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        HippoHotness hotness;
        LmMessageNode *child = find_child_node(message->node, "http://dumbhippo.com/protocol/hotness", "hotness");
        if (!child)
            return FALSE;
        const char *hotness_str = lm_message_node_get_attribute(child, "value");
        if (!hotness_str)
            return FALSE;
        hotness = hotness_from_string(hotness_str);
 
        hippo_data_cache_set_hotness(connection->cache, hotness);
 
        return TRUE;
   }
   return FALSE;
}

gboolean
handle_prefs_changed(HippoConnection *connection,
                     LmMessage       *message)
{
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return FALSE;

    LmMessageNode *child = find_child_node(message->node, "http://dumbhippo.com/protocol/prefs", "prefs");
    if (child == NULL)
        return FALSE;
    g_debug("handling prefsChanged message");

    hippo_connection_process_prefs_node(connection, child);

    return TRUE;
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

    /* FIXME
    char *mySpaceName = NULL;
    if (im->checkMySpaceNameChangedMessage(message, &mySpaceName)) {
        im->handleMySpaceNameChangedMessage(mySpaceName);
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->checkMySpaceContactCommentMessage(message)) {
        im->handleMySpaceContactCommentMessage();
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    */

    if (handle_active_posts_message(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_live_post_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (handle_hotness_changed(connection, message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    
    if (handle_prefs_changed(connection, message)) {
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
            hippo_connection_parse_post_data(connection, child, "newPost");
        }
    } else {
        g_debug("handle_message: message not handled");
    }

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

static void 
handle_disconnect (LmConnection       *lconnection,
                   LmDisconnectReason  reason,
                   gpointer            data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);

    g_debug("handle_disconnect reason=%d", reason);

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

    if (success) {        hippo_connection_authenticate(connection);
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

    hippo_connection_send_message(connection, message);
    hippo_connection_state_change(connection, HIPPO_STATE_AUTHENTICATED);

    hippo_connection_get_client_info(connection);
    hippo_connection_update_prefs(connection);
    
    g_signal_emit(connection, signals[AUTH_SUCCESS], 0);
}


/* == Random cruft == */


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
    case HIPPO_STATE_AUTHENTICATED:
        return "AUTHENTICATED";
    }
    /* not a default case so we get a warning if we omit one from the switch */
    return "WHAT THE?";
}
