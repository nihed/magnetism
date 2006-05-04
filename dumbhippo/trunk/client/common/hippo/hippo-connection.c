#include "hippo-connection.h"
#include <loudmouth/loudmouth.h>
#include <string.h>

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
    HippoState state;
    int signin_timeout_id;
    int signin_timeout_count;
    int retry_timeout_id;
    LmConnection *lm_connection;
    /* queue of OutgoingMessage objects */
    GQueue *pending_outgoing_messages;
    /* queue of LmMessage */
    GQueue *pending_room_messages;
    unsigned int music_sharing_enabled : 1;
    unsigned int music_sharing_primed : 1;
    char *username;
    char *password;
    HippoHotness hotness;
};

struct _HippoConnectionClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoConnection, hippo_connection, G_TYPE_OBJECT);

enum {
    STATE_CHANGED,
    AUTH_FAILURE,
    AUTH_SUCCESS,
    NEW_POST,
    MUSIC_SHARING_TOGGLED,
    HOTNESS_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_connection_init(HippoConnection *connection)
{
    connection->state = HIPPO_STATE_SIGNED_OUT;
    connection->pending_outgoing_messages = g_queue_new();
    connection->pending_room_messages = g_queue_new();
    
    /* these defaults are important to be sure we
     * do nothing until we hear otherwise
     * (and to be sure a signal is emitted if we need to
     * do something, since stuff will have changed)
     */
    connection->music_sharing_enabled = FALSE;
    connection->music_sharing_primed = TRUE;
    
    connection->hotness = HIPPO_HOTNESS_UNKNOWN;
}

static void
hippo_connection_class_init(HippoConnectionClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
  
    signals[STATE_CHANGED] =
        g_signal_new ("state_changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0); 

    signals[AUTH_FAILURE] =
        g_signal_new ("auth_failure",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0); 

    signals[AUTH_SUCCESS] =
        g_signal_new ("auth_success",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0); 

    signals[NEW_POST] =
        g_signal_new ("new_post",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__POINTER,
            		  G_TYPE_NONE, 1, G_TYPE_POINTER);

    signals[MUSIC_SHARING_TOGGLED] =
        g_signal_new ("music_sharing_toggled",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__BOOLEAN,
            		  G_TYPE_NONE, 1, G_TYPE_BOOLEAN);

    signals[HOTNESS_CHANGED] =
        g_signal_new ("hotness_changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__INT,
            		  G_TYPE_NONE, 1, G_TYPE_INT);
          
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

HippoState
hippo_connection_get_state(HippoConnection *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), HIPPO_STATE_SIGNED_OUT);
    return connection->state;
}

HippoHotness
hippo_connection_get_hotness(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), HIPPO_HOTNESS_UNKNOWN);
    return connection->hotness;
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
     * to the server.
     */
    if (!connection->music_sharing_enabled)
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

gboolean
hippo_connection_get_music_sharing_enabled (HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    
    return connection->music_sharing_enabled;
}

gboolean
hippo_connection_get_need_priming_music(HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    
    return connection->music_sharing_enabled && !connection->music_sharing_primed;
}

void
hippo_connection_provide_priming_music(HippoConnection  *connection,
                                       const HippoSong **songs,
                                        int               n_songs)
{
    g_return_if_fail(HIPPO_IS_CONNECTION(connection));
    g_return_if_fail(songs != NULL);

    if (!connection->music_sharing_enabled || connection->music_sharing_primed) {
        /* didn't need to prime after all (maybe someone beat us to it) */
        return;
    }

    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "primingTracks");

    int i;
    for (i = 0; i < n_songs; ++i) {
        LmMessageNode *track = lm_message_node_add_child(music, "track", NULL);
        add_track_props(track, songs[i]->keys, songs[i]->values);
    }

    hippo_connection_send_message(connection, message);

    lm_message_unref(message);
    /* we should also get back a notification from the server when this changes,
       but we want to avoid re-priming so this adds robustness
     */
    connection->music_sharing_primed = TRUE;
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

    /* If lm_connection returns false, then handle_open won't be called
     * at all. On a true return it will be called exactly once, but that 
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
    im->getPosts(NULL); // Get some recent posts
    */
    
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
    lm_message_node_set_attribute(child, "platform", PLATFORM);
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
    gboolean old_music_sharing_enabled = connection->music_sharing_enabled;
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
            connection->music_sharing_enabled = value != NULL && strcmp(value, "true") == 0;
            g_debug("musicSharingEnabled set to %d", (int) connection->music_sharing_enabled);
        } else if (strcmp(key, "musicSharingPrimed") == 0) {
            connection->music_sharing_primed = value != NULL && strcmp(value, "true") == 0;
            g_debug("musicSharingPrimed set to %d", (int) connection->music_sharing_primed);
        } else {
            g_debug("Unknown pref '%s'", key);
        }
    }
    /* notify the music monitor engines that they may want to kick in or out */
    if (old_music_sharing_enabled != connection->music_sharing_enabled)
        g_signal_emit(connection, signals[MUSIC_SHARING_TOGGLED], 0,
            (gboolean) connection->music_sharing_enabled);
}

/* === HippoConnection Loudmouth handlers === */

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
 
        if (hotness != connection->hotness) {
            g_debug("new hotness %s", hippo_hotness_debug_string(hotness));
            connection->hotness = hotness;
            g_signal_emit(connection, signals[HOTNESS_CHANGED], 0, hotness);
        }
 
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
    
    g_debug("handle_message");
    /* FIXME
    switch (im->processRoomMessage(message, false)) {
        case PROCESS_MESSAGE_IGNORE:
            break;
        case PROCESS_MESSAGE_CONSUME:
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        case PROCESS_MESSAGE_PEND:
            lm_message_ref(message);
            g_queue_push_tail(im->pendingRoomMessages_, message);
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
     }

    char *mySpaceName = NULL;
    if (im->checkMySpaceNameChangedMessage(message, &mySpaceName)) {
        im->handleMySpaceNameChangedMessage(mySpaceName);
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->handleActivePostsMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->handleLivePostChangedMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->checkMySpaceContactCommentMessage(message)) {
        im->handleMySpaceContactCommentMessage();
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    */

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
            /* FIXME parse the post and pass it to this signal as argument */
            g_signal_emit(connection, signals[NEW_POST], 0, NULL);
        }
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

    /* FIXME */

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

    /* FIXME

    // Enter any chatrooms that we are (logically) connected to
    for (unsigned long i = 0; i < im->chatRooms_.length(); i++) {
        // We left the previous contents there while we were disconnected,
        // clear it since we'll now get the current contents sent
        im->chatRooms_[i]->clear();
        if (im->chatRooms_[i]->getState() != HippoChatRoom::NONMEMBER) 
            im->sendChatRoomEnter(im->chatRooms_[i], im->chatRooms_[i]->getState() == HippoChatRoom::PARTICIPANT);
    }
    */

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
