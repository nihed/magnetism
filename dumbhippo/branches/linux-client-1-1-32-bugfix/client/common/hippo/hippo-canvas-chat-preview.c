/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include <hippo/hippo-chat-room.h>
#include "hippo-actions.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-message-preview.h"
#include "hippo-canvas-block.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-entity-name.h>

/* max number of chat messages in the preview */
#define MAX_PREVIEWED 5

static void      hippo_canvas_chat_preview_init                (HippoCanvasChatPreview       *box);
static void      hippo_canvas_chat_preview_class_init          (HippoCanvasChatPreviewClass  *klass);
static void      hippo_canvas_chat_preview_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_chat_preview_dispose             (GObject                *object);
static void      hippo_canvas_chat_preview_finalize            (GObject                *object);

static void hippo_canvas_chat_preview_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_canvas_chat_preview_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);
static GObject* hippo_canvas_chat_preview_constructor (GType                  type,
                                                       guint                  n_construct_properties,
                                                       GObjectConstructParam *construct_properties);

/* Our own methods */
static void hippo_canvas_chat_preview_set_room           (HippoCanvasChatPreview *chat_preview,
                                                          HippoChatRoom          *room);
static void hippo_canvas_chat_preview_set_chat_id        (HippoCanvasChatPreview *chat_preview,
                                                          const char             *id);
static void hippo_canvas_chat_preview_set_actions        (HippoCanvasChatPreview *chat_preview,
                                                          HippoActions           *actions);
static void hippo_canvas_chat_preview_set_chatting_count (HippoCanvasChatPreview *chat_preview,
                                                          int                     count);
static void hippo_canvas_chat_preview_set_message_count  (HippoCanvasChatPreview *chat_preview,
                                                          int                     count);
static void hippo_canvas_chat_preview_add_recent_message (HippoCanvasChatPreview *chat_preview,
                                                          HippoChatMessage       *message);
static void update_chatting_count                        (HippoCanvasChatPreview *chat_preview);
static void update_message_count                         (HippoCanvasChatPreview *chat_preview);
static void update_recent_messages                       (HippoCanvasChatPreview *chat_preview);

static void hippo_canvas_chat_preview_update_visibility (HippoCanvasChatPreview *chat_preview);

typedef struct {
    HippoCanvasItem *item;
    HippoCanvasItem *message_text;
    HippoCanvasItem *entity_name;
} HippoMessagePreview;

struct _HippoCanvasChatPreview {
    HippoCanvasBox canvas_box;
    HippoActions *actions;
    HippoChatRoom  *room;
    char *chat_id;
    int chatting_count;
    int message_count;
    GSList *recent_messages;

    HippoCanvasItem *chat_link;
    HippoCanvasBox *count_parent;
    HippoCanvasItem *count_item;
    HippoCanvasItem *count_separator_item;
    HippoCanvasItem *message_previews[MAX_PREVIEWED];

    unsigned int hushed : 1;
};

struct _HippoCanvasChatPreviewClass {
    HippoCanvasBoxClass parent_class;
};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_CHAT_ROOM,
    PROP_CHAT_ID,
    PROP_ACTIONS,
    PROP_RECENT_MESSAGES,
    PROP_CHATTING_COUNT,
    PROP_MESSAGE_COUNT
};


G_DEFINE_TYPE_WITH_CODE(HippoCanvasChatPreview, hippo_canvas_chat_preview, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_chat_preview_iface_init));

static void
on_chat_activated(HippoCanvasItem        *chat_link,
                  HippoCanvasChatPreview *chat_preview)
{
    if (chat_preview->chat_id)
        hippo_actions_join_chat_id(chat_preview->actions,
                                   chat_preview->chat_id);
}

static void
hippo_canvas_chat_preview_init(HippoCanvasChatPreview *chat_preview)
{
    chat_preview->message_count = -1;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_chat_preview_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    
}

static void
hippo_canvas_chat_preview_class_init(HippoCanvasChatPreviewClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    /* HippoCanvasBoxClass *canvas_box_class = HIPPO_CANVAS_BOX_CLASS(klass); */

    object_class->set_property = hippo_canvas_chat_preview_set_property;
    object_class->get_property = hippo_canvas_chat_preview_get_property;
    object_class->constructor = hippo_canvas_chat_preview_constructor;

    object_class->dispose = hippo_canvas_chat_preview_dispose;
    object_class->finalize = hippo_canvas_chat_preview_finalize;

    g_object_class_install_property(object_class,
                                    PROP_CHAT_ROOM,
                                    g_param_spec_object("chat-room",
                                                        _("Chat Room"),
                                                        _("Chat room to preview"),
                                                        HIPPO_TYPE_CHAT_ROOM,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_CHAT_ID,
                                    g_param_spec_string("chat-id",
                                                        _("Chat Room"),
                                                        _("ID of chat room (for handling link clicking)"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));

    g_object_class_install_property(object_class,
                                    PROP_CHATTING_COUNT,
                                    g_param_spec_int("chatting-count",
                                                     _("Chatting count"),
                                                     _("Number of users currently chatting"),
                                                     0, G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_MESSAGE_COUNT,
                                    g_param_spec_int("message-count",
                                                     _("Message count"),
                                                     _("Total number of comments and quips"),
                                                     -1, G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_RECENT_MESSAGES,
                                    g_param_spec_pointer("recent-messages",
                                                         _("Recent Message"),
                                                         _("A list of recent messages"),
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
    
}

static void
free_recent_message(void *data, void *ignored)
{
    HippoChatMessage *message = data;
    hippo_chat_message_free(message);
}

static void
clear_recent_messages(HippoCanvasChatPreview *chat_preview)
{
    g_slist_foreach(chat_preview->recent_messages,
                    free_recent_message, NULL);
    g_slist_free(chat_preview->recent_messages);
    chat_preview->recent_messages = NULL;
}

static void
hippo_canvas_chat_preview_dispose(GObject *object)
{
    HippoCanvasChatPreview *chat_preview = HIPPO_CANVAS_CHAT_PREVIEW(object);

    hippo_canvas_chat_preview_set_room(chat_preview, NULL);
    hippo_canvas_chat_preview_set_chat_id(chat_preview, NULL);
    hippo_canvas_chat_preview_set_actions(chat_preview, NULL);
    clear_recent_messages(chat_preview);
    
    G_OBJECT_CLASS(hippo_canvas_chat_preview_parent_class)->dispose(object);
}

static void
hippo_canvas_chat_preview_finalize(GObject *object)
{
    /* HippoCanvasChatPreview *box = HIPPO_CANVAS_CHAT_PREVIEW(object); */

    G_OBJECT_CLASS(hippo_canvas_chat_preview_parent_class)->finalize(object);
}

static void
hippo_canvas_chat_preview_set_property(GObject         *object,
                                       guint            prop_id,
                                       const GValue    *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasChatPreview *chat_preview;

    chat_preview = HIPPO_CANVAS_CHAT_PREVIEW(object);

    switch (prop_id) {
    case PROP_CHAT_ROOM:
        {
            HippoChatRoom *new_room = (HippoChatRoom*) g_value_get_object(value);
            hippo_canvas_chat_preview_set_room(chat_preview, new_room);
        }
        break;
    case PROP_CHAT_ID:
        {
            const char *new_id = g_value_get_string(value);
            hippo_canvas_chat_preview_set_chat_id(chat_preview, new_id);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_chat_preview_set_actions(chat_preview, new_actions);
        }
        break;
    case PROP_RECENT_MESSAGES:
        {
            GSList *messages = (GSList *) g_value_get_pointer(value);
            GSList *l;

            clear_recent_messages(chat_preview);
            for (l = messages; l; l = l->next)
                hippo_canvas_chat_preview_add_recent_message(chat_preview, l->data);
            update_recent_messages(chat_preview);
        }
        break;
    case PROP_CHATTING_COUNT:
        hippo_canvas_chat_preview_set_chatting_count(chat_preview,
                                                     g_value_get_int(value));
        break;
    case PROP_MESSAGE_COUNT:
        hippo_canvas_chat_preview_set_message_count(chat_preview,
                                                    g_value_get_int(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_chat_preview_get_property(GObject         *object,
                                       guint            prop_id,
                                       GValue          *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasChatPreview *chat_preview;

    chat_preview = HIPPO_CANVAS_CHAT_PREVIEW (object);

    switch (prop_id) {
    case PROP_CHAT_ROOM:
        g_value_set_object(value, (GObject*) chat_preview->room);
        break;
    case PROP_CHAT_ID:
        g_value_set_string(value, chat_preview->chat_id);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) chat_preview->actions);
        break;
    case PROP_CHATTING_COUNT:
        g_value_set_int(value, chat_preview->chatting_count);
        break;
    case PROP_MESSAGE_COUNT:
        g_value_set_int(value, chat_preview->message_count);
        break;
    case PROP_RECENT_MESSAGES:
        g_value_set_pointer(value, chat_preview->recent_messages);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_chat_preview_constructor (GType                  type,
                                       guint                  n_construct_properties,
                                       GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_chat_preview_parent_class)->constructor(type,
                                                                                          n_construct_properties,
                                                                                          construct_properties);
    
    HippoCanvasChatPreview *chat_preview = HIPPO_CANVAS_CHAT_PREVIEW(object);
    HippoCanvasBox *box;
    HippoCanvasItem *item;
    int i;
    
    for (i = 0; i < MAX_PREVIEWED; ++i) {
        chat_preview->message_previews[i] = g_object_new(HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW,
                                                         "actions", chat_preview->actions,
                                                         NULL);
        
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(chat_preview),
                                chat_preview->message_previews[i],
                                0);
    }

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                       NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(chat_preview), HIPPO_CANVAS_ITEM(box), 0);
    
    chat_preview->count_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                            NULL);
    chat_preview->count_parent = box;
    hippo_canvas_box_append(box, chat_preview->count_item, 0);

    chat_preview->count_separator_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                      "text", " | ",
                                                      NULL);
    hippo_canvas_box_append(box, chat_preview->count_separator_item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "color-cascade", HIPPO_CASCADE_MODE_NONE,
                        "tooltip", "View all quips and comments",
                        NULL);
    chat_preview->chat_link = item;
    hippo_canvas_box_append(box, item, 0);

    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_chat_activated), chat_preview);

    update_chatting_count(chat_preview);
    update_message_count(chat_preview);
    update_recent_messages(chat_preview);
    hippo_canvas_chat_preview_update_visibility(chat_preview);

    return object;
}

static void
update_chatting_count(HippoCanvasChatPreview *chat_preview)
{
    /* We only know the chatting count if the chat room is joined for some reason */
    HippoChatRoom *room;

    room = chat_preview->room;
    
    if (room != NULL) {
        char *s;
        
        if (chat_preview->chatting_count == 0)
            s = g_strdup("Nobody chatting");
        else
            s = g_strdup_printf("%d people chatting", chat_preview->chatting_count);
        
        g_object_set(G_OBJECT(chat_preview->count_item),
                     "text", s,
                     NULL);

        g_free(s);
    }
}

static void
update_message_count(HippoCanvasChatPreview *chat_preview)
{
    char *s;

    if (chat_preview->message_count >= 0)
        s = g_strdup_printf("Quips and Comments (%d)\302\273", chat_preview->message_count);
    else
        s = g_strdup("Quips and Comments\302\273");

    g_object_set(G_OBJECT(chat_preview->chat_link),
                 "text", s,
                 NULL);
    
    g_free(s);
}

static void
update_recent_messages(HippoCanvasChatPreview *chat_preview)
{
    GSList *link;
    int i;
    
    link = chat_preview->recent_messages;
    for (i = 0; i < MAX_PREVIEWED; ++i) {
        HippoCanvasItem *message_preview = chat_preview->message_previews[i];
        HippoChatMessage *message = link ? link->data : NULL;
        if (link)
            link = link->next;

        g_object_set(G_OBJECT(message_preview),
                     "message", message,
                     NULL);
        
        hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(chat_preview),
                                           message_preview, message != NULL);
    }
}

static void
on_room_user_state_changed(HippoChatRoom          *room,
                           HippoPerson            *person,
                           HippoCanvasChatPreview *chat_preview)
{
    int chatting_count;

    chatting_count = hippo_chat_room_get_chatting_user_count(room);

    hippo_canvas_chat_preview_set_chatting_count(chat_preview, chatting_count);
}

static void
on_room_message_added(HippoChatRoom          *room,
                      HippoChatMessage       *message,
                      HippoCanvasChatPreview *chat_preview)
{
    hippo_canvas_chat_preview_add_recent_message(chat_preview, message);
    update_recent_messages(chat_preview);
}

static void
hippo_canvas_chat_preview_set_room(HippoCanvasChatPreview *chat_preview,
                                   HippoChatRoom          *room)
{
    if (room == chat_preview->room)
        return;

    if (chat_preview->room) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(chat_preview->room),
                                             G_CALLBACK(on_room_user_state_changed),
                                             chat_preview);
        g_signal_handlers_disconnect_by_func(G_OBJECT(chat_preview->room),
                                             G_CALLBACK(on_room_message_added),
                                             chat_preview);
        
        g_object_unref(chat_preview->room);
        chat_preview->room = NULL;
        
        g_free(chat_preview->chat_id);
        chat_preview->chat_id = NULL;
    }
    
    if (room) {
        GSList *messages;
        int count;

        chat_preview->chat_id = g_strdup(hippo_chat_room_get_id(room));
        
        g_object_ref(room);
        chat_preview->room = room;

        g_signal_connect(G_OBJECT(room), "user-state-changed",
                         G_CALLBACK(on_room_user_state_changed),
                         chat_preview);
        g_signal_connect(G_OBJECT(room), "message-added",
                         G_CALLBACK(on_room_message_added),
                         chat_preview);

        clear_recent_messages(chat_preview);
        
        hippo_canvas_chat_preview_set_chatting_count(chat_preview,
                                                     hippo_chat_room_get_chatting_user_count(room));
        count = 0;
        messages = hippo_chat_room_get_messages(room);
        while (messages != NULL && count < MAX_PREVIEWED) {

            hippo_canvas_chat_preview_add_recent_message(chat_preview, messages->data);
            
            ++count;
            messages = messages->next;
        }
        update_recent_messages(chat_preview);
    } else {
        clear_recent_messages(chat_preview);
        update_recent_messages(chat_preview);
        update_chatting_count(chat_preview);
    }
    
    g_object_notify(G_OBJECT(chat_preview), "chat-room");
    g_object_notify(G_OBJECT(chat_preview), "chat-id");
}

static void
hippo_canvas_chat_preview_set_chat_id(HippoCanvasChatPreview *chat_preview,
                                      const char             *id)
{
    if (id == chat_preview->chat_id ||
        (id && chat_preview->chat_id && strcmp(id, chat_preview->chat_id) == 0))
        return;

    hippo_canvas_chat_preview_set_room(chat_preview, NULL);
    if (chat_preview->chat_id) {
        g_free(chat_preview->chat_id);
        chat_preview->chat_id = NULL;
    }

    chat_preview->chat_id = g_strdup(id);

    hippo_canvas_chat_preview_update_visibility(chat_preview);

    g_object_notify(G_OBJECT(chat_preview), "chat-id");
}
                                                              
static void
hippo_canvas_chat_preview_set_actions(HippoCanvasChatPreview  *chat_preview,
                                      HippoActions            *actions)
{
    if (actions == chat_preview->actions)
        return;

    if (chat_preview->actions) {
        g_object_unref(chat_preview->actions);
        chat_preview->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        chat_preview->actions = actions;
    }

    hippo_canvas_chat_preview_update_visibility(chat_preview);

    g_object_notify(G_OBJECT(chat_preview), "actions");
}

static void
hippo_canvas_chat_preview_set_chatting_count (HippoCanvasChatPreview *chat_preview,
                                              int                     count)
{
    if (count == chat_preview->chatting_count)
        return;

    chat_preview->chatting_count = count;

    update_chatting_count(chat_preview);
    
    g_object_notify(G_OBJECT(chat_preview), "chatting-count");
}

static void
hippo_canvas_chat_preview_set_message_count (HippoCanvasChatPreview *chat_preview,
                                             int                     count)
{
    if (count == chat_preview->message_count)
        return;

    chat_preview->message_count = count;

    update_message_count(chat_preview);
    
    g_object_notify(G_OBJECT(chat_preview), "message-count");
}

/* Insert this message if it would be one of the MAX_PREVIEWED most
 * recent messages. Return TRUE if the recent message list was changed.
 */
static gboolean
insert_message_if_needed(HippoCanvasChatPreview *chat_preview,
                         HippoChatMessage       *message)
{
    GSList *link;
    int newer_count;

    /* we never want the description (serial -1) */
    if (hippo_chat_message_get_serial(message) < 0)
        return FALSE;
    
    newer_count = 0;
    /* The messages are sorted most-recent-serial-first */
    for (link = chat_preview->recent_messages;
         link != NULL;
         link = link->next) {
        HippoChatMessage *old = link->data;
        
        /* Insert before the first message we are newer than */
        if (hippo_chat_message_get_serial(old) < hippo_chat_message_get_serial(message)) {
            break;
        }

        if (hippo_chat_message_get_serial(old) == hippo_chat_message_get_serial(message)) {
            /* Never put in a duplicate */
            return FALSE;
        }
        
        ++newer_count;

        /* If the list already has MAX_PREVIEWED items, and we were not newer than
         * any of them, just bail out
         */
        if (newer_count == MAX_PREVIEWED)
            return FALSE;
    }

    /* link is NULL if the list was shorter than MAX_PREVIEWED */
    chat_preview->recent_messages =
        g_slist_insert_before(chat_preview->recent_messages,
                              link,
                              hippo_chat_message_copy(message));
    
    return TRUE;
}

static void
hippo_canvas_chat_preview_add_recent_message (HippoCanvasChatPreview *chat_preview,
                                              HippoChatMessage       *message)
{
    int count;
    
    if (!insert_message_if_needed(chat_preview, message))
        return;

    count = g_slist_length(chat_preview->recent_messages);
    if (count > MAX_PREVIEWED) {
        /* We need to kill the end of the list, to make this simple
         * just reverse the list, nuke items off the end, and
         * reverse it back
         */
        GSList *list = g_slist_reverse(chat_preview->recent_messages);
        while (count > MAX_PREVIEWED) {
            g_assert(list != NULL);
            hippo_chat_message_free(list->data);
            list = g_slist_remove(list, list->data);
            --count;
        }
        chat_preview->recent_messages = g_slist_reverse(list);
    }
}

void
hippo_canvas_chat_preview_set_hushed(HippoCanvasChatPreview *chat_preview,
                                     gboolean                value)
{
    value = value != FALSE;
    
    if (chat_preview->hushed == value)
        return;

    chat_preview->hushed = value;

    if (value) {
        g_object_set(G_OBJECT(chat_preview->chat_link),
                     "color-cascade", HIPPO_CASCADE_MODE_INHERIT,
                     NULL);
    } else {
        g_object_set(G_OBJECT(chat_preview->chat_link),
                     "color-cascade", HIPPO_CASCADE_MODE_NONE,
                     NULL);
    }
}

static void
hippo_canvas_chat_preview_update_visibility(HippoCanvasChatPreview *chat_preview)
{
    gboolean show_chat = chat_preview->room != NULL || chat_preview->chat_id != NULL;
    gboolean show_count = chat_preview->room != NULL;

    if (!chat_preview->count_parent) /* During initialization */
        return;
    
    hippo_canvas_box_set_child_visible(chat_preview->count_parent,
                                       chat_preview->chat_link,
                                       show_chat);
    hippo_canvas_box_set_child_visible(chat_preview->count_parent,
                                       chat_preview->count_separator_item,
                                       show_chat && show_count);
    hippo_canvas_box_set_child_visible(chat_preview->count_parent,
                                       chat_preview->count_item,
                                       show_count);
}
