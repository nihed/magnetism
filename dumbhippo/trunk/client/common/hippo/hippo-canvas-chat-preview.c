/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-chat-room.h>
#include "hippo-actions.h"
#include "hippo-canvas-chat-preview.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-entity-name.h>

/* max number of chat messages in the preview */
#define MAX_PREVIEWED 3

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

/* Our own methods */
static void hippo_canvas_chat_preview_set_room           (HippoCanvasChatPreview *chat_preview,
                                                          HippoChatRoom          *room);
static void hippo_canvas_chat_preview_set_actions        (HippoCanvasChatPreview *chat_preview,
                                                          HippoActions           *actions);
static void hippo_canvas_chat_preview_set_chatting_count (HippoCanvasChatPreview *chat_preview,
                                                          int                     count);
static void hippo_canvas_chat_preview_add_recent_message (HippoCanvasChatPreview *chat_preview,
                                                          HippoChatMessage       *message);
static void update_chatting_count                        (HippoCanvasChatPreview *chat_preview);
static void update_recent_messages                       (HippoCanvasChatPreview *chat_preview);


typedef struct {
    HippoCanvasItem *item;
    HippoCanvasItem *message_text;
    HippoCanvasItem *entity_name;
} HippoMessagePreview;

struct _HippoCanvasChatPreview {
    HippoCanvasBox canvas_box;
    HippoActions *actions;
    HippoChatRoom  *room;
    int chatting_count;
    GSList *recent_messages;

    HippoCanvasBox *count_parent;
    HippoCanvasItem *count_item;
    HippoCanvasItem *count_separator_item;
    HippoMessagePreview message_previews[MAX_PREVIEWED];
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
    PROP_ACTIONS,
    PROP_RECENT_MESSAGE,
    PROP_CHATTING_COUNT
};


G_DEFINE_TYPE_WITH_CODE(HippoCanvasChatPreview, hippo_canvas_chat_preview, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_chat_preview_iface_init));

static void
on_chat_activated(HippoCanvasItem        *chat_link,
                  HippoCanvasChatPreview *chat_preview)
{
    if (chat_preview->actions) {
        if (chat_preview->room)
            hippo_actions_join_chat_room(chat_preview->actions,
                                         chat_preview->room);
        else
            /* FIXME - we'll have to provide a chat id or something */;
    }
}

static void
hippo_canvas_chat_preview_init(HippoCanvasChatPreview *chat_preview)
{
    HippoCanvasBox *box;
    HippoCanvasItem *item;
    int i;
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(chat_preview), HIPPO_CANVAS_ITEM(box), 0);
    
    chat_preview->count_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                            "color", 0x666666ff,
                                            NULL);
    chat_preview->count_parent = box;
    hippo_canvas_box_append(box, chat_preview->count_item, 0);

    chat_preview->count_separator_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                      "text", " | ",
                                                      "color", 0x666666ff,
                                                      NULL);
    hippo_canvas_box_append(box, chat_preview->count_separator_item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text", "Chat",
                        NULL);
    hippo_canvas_box_append(box, item, 0);

    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_chat_activated), chat_preview);

    for (i = 0; i < MAX_PREVIEWED; ++i) {
        HippoMessagePreview *mp = &chat_preview->message_previews[i];
        mp->item= g_object_new(HIPPO_TYPE_CANVAS_BOX,
                               "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                               NULL);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(chat_preview), mp->item, 0);

        box = HIPPO_CANVAS_BOX(mp->item);
        
        item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                            "image-name", "chat",
                            "xalign", HIPPO_ALIGNMENT_START,
                            "yalign", HIPPO_ALIGNMENT_CENTER,
                            "border-right", 4,
                            NULL);
        hippo_canvas_box_append(box, item, 0);

        mp->message_text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                        "xalign", HIPPO_ALIGNMENT_START,
                                        NULL);
        hippo_canvas_box_append(box, mp->message_text, 0);
        
        item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                            "text", " - ", /* FIXME figure out mdash */
                            NULL);
        hippo_canvas_box_append(box, item, 0);
        
        mp->entity_name = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_NAME,
                                       NULL);
        hippo_canvas_box_append(box, mp->entity_name, 0);
    }

    update_chatting_count(chat_preview);
    update_recent_messages(chat_preview);
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
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_CHATTING_COUNT,
                                    g_param_spec_int("chatting-count",
                                                     _("Chatting count"),
                                                     _("Number of users currently chatting"),
                                                     0, G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_RECENT_MESSAGE,
                                    g_param_spec_pointer("recent-message",
                                                         _("Recent Message"),
                                                         _("A recent message to consider displaying"),
                                                         G_PARAM_WRITABLE));
    
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

HippoCanvasItem*
hippo_canvas_chat_preview_new(void)
{
    HippoCanvasChatPreview *chat_preview;

    chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW, NULL);

    return HIPPO_CANVAS_ITEM(chat_preview);
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
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_chat_preview_set_actions(chat_preview, new_actions);
        }
        break;
    case PROP_RECENT_MESSAGE:
        {
            HippoChatMessage *message = (HippoChatMessage*) g_value_get_pointer(value);
            hippo_canvas_chat_preview_add_recent_message(chat_preview, message);
        }
        break;
    case PROP_CHATTING_COUNT:
        hippo_canvas_chat_preview_set_chatting_count(chat_preview,
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
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) chat_preview->actions);
        break;
    case PROP_CHATTING_COUNT:
        g_value_set_int(value, chat_preview->chatting_count);
        break;
    case PROP_RECENT_MESSAGE: /* it's not readable */
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
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

    hippo_canvas_box_set_child_visible(chat_preview->count_parent,
                                       chat_preview->count_item,
                                       room != NULL);
    hippo_canvas_box_set_child_visible(chat_preview->count_parent,
                                       chat_preview->count_separator_item,
                                       room != NULL);
}

static void
update_recent_messages(HippoCanvasChatPreview *chat_preview)
{
    GSList *link;
    int i;
    
    link = chat_preview->recent_messages;
    for (i = 0; i < MAX_PREVIEWED; ++i) {
        HippoMessagePreview *mp = &chat_preview->message_previews[i];
        HippoChatMessage *message = link ? link->data : NULL;
        if (link)
            link = link->next;

        /* In some sense there's no point setting this stuff if we hide the
         * message item, but we should really drop references to objects
         * we aren't using so we always set to NULL if appropriate.
         */
        g_object_set(G_OBJECT(mp->message_text),
                     "text", message ? hippo_chat_message_get_text(message) : NULL,
                     NULL);
        g_object_set(G_OBJECT(mp->entity_name),
                     "entity", message ? hippo_chat_message_get_person(message) : NULL,
                     NULL);

        hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(chat_preview),
                                           mp->item, message != NULL);
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
    }
    
    if (room) {
        GSList *messages;
        int count;
        
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
    } else {
        clear_recent_messages(chat_preview);
        update_recent_messages(chat_preview);
        update_chatting_count(chat_preview);
    }
    
    g_object_notify(G_OBJECT(chat_preview), "chat-room");
}

static void
hippo_canvas_chat_preview_set_actions(HippoCanvasChatPreview  *chat_preview,
                                      HippoActions            *actions)
{
    int i;
    
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

    for (i = 0; i < MAX_PREVIEWED; ++i) {
        HippoMessagePreview *mp = &chat_preview->message_previews[i];
        g_object_set(G_OBJECT(mp->entity_name), "actions", chat_preview->actions, NULL);
    }
        
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

    update_recent_messages(chat_preview);
}
