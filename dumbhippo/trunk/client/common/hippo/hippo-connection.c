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
static gboolean hippo_connection_load_auth            (HippoConnection *connection);
static void     hippo_connection_authenticate         (HippoConnection *connection);
static void     hippo_connection_clear                (HippoConnection *connection);
static void     hippo_connection_flush_outgoing       (HippoConnection *connection);
static void     hippo_connection_send_message         (HippoConnection *connection,
                                                       LmMessage       *message);static void     hippo_connection_send_message_with_reply(HippoConnection *connection,
                                                       LmMessage *message,
                                                       LmMessageHandler *handler);
static void     hippo_connection_get_client_info      (HippoConnection *connection);
static LmHandlerResult hippo_connection_handle_message (LmMessageHandler *handler,
                                                        LmConnection     *connection,
                                                        LmMessage        *message,
                                                        gpointer          data);

struct _HippoConnection {
    GObject parent;
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
};

struct _HippoConnectionClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoConnection, hippo_connection, G_TYPE_OBJECT);

enum {
    STATE_CHANGED,
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
    g_signal_new ("state_changed",
        		  G_TYPE_FROM_CLASS (object_class),
        		  G_SIGNAL_RUN_FIRST,
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

    G_OBJECT_CLASS(hippo_connection_parent_class)->finalize(object); 
}


/* === HippoConnection exported API === */


HippoConnection*
hippo_connection_new(void)
{
    HippoConnection *connection = g_object_new(HIPPO_TYPE_CONNECTION, NULL);
    
    return connection;
}

HippoState
hippo_connection_get_state(HippoConnection *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), HIPPO_STATE_SIGNED_OUT);
    return connection->state;
}

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
hippo_connection_get_need_priming_music (HippoConnection  *connection)
{
    g_return_val_if_fail(HIPPO_IS_CONNECTION(connection), FALSE);
    
    return connection->music_sharing_enabled && !connection->music_sharing_primed;
}

void
hippo_connection_provide_priming_music (HippoConnection  *connection,
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
        // Try more slowly
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
    
    // g_print("IM connection state changed to %d", (int) state);
    
    hippo_connection_flush_outgoing(connection);

    g_signal_emit(connection, signals[STATE_CHANGED], 0);
}

static gboolean
hippo_connection_load_auth(HippoConnection *connection)
{

    return FALSE;
}

static void
hippo_connection_authenticate(HippoConnection *connection)
{

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
        g_printerr("clientInfo reply missing attributes");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    /* g_print("Got clientInfo response: minimum=%s, current=%s, download=%s", minimum, current, download); */

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
hippo_connection_handle_message (LmMessageHandler *handler,
                                 LmConnection     *lconnection,
                                 LmMessage        *message,
                                 gpointer          data)
{
    HippoConnection *connection = HIPPO_CONNECTION(data);/* FIXME
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

    if (im->handleHotnessMessage(message)) {
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

    if (im->handlePrefsChangedMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    // Messages used to be HEADLINE, we accept both for compatibility
    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NORMAL
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NOT_SET // Shouldn't need this, default should be normal
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/post", "newPost");
        if (child) {
            HippoPtr<HippoPost> post;
            if (im->parsePostData(child, "newPost", &post)) {
                im->ui_->onLinkMessage(post);
            } else {
                im->ui_->logErrorU("failed to parse post stream in newPost");
            }
        } 
    }
*/

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}
