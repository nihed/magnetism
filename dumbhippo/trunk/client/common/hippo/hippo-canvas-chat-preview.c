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
static void hippo_canvas_chat_preview_set_actions        (HippoCanvasChatPreview *chat_preview,
                                                          HippoActions           *actions);
static void hippo_canvas_chat_preview_set_block          (HippoCanvasChatPreview *chat_preview,
                                                          DDMDataResource        *block);

static void hippo_canvas_chat_preview_update_visibility (HippoCanvasChatPreview *chat_preview);

static void update_chat_messages       (HippoCanvasChatPreview *chat_preview);
static void update_chat_messages_count (HippoCanvasChatPreview *chat_preview);

typedef struct {
    HippoCanvasItem *item;
    HippoCanvasItem *message_text;
    HippoCanvasItem *entity_name;
} HippoMessagePreview;

struct _HippoCanvasChatPreview {
    HippoCanvasBox canvas_box;
    HippoActions *actions;
    DDMDataResource *block;
    DDMFeed *feed;

    HippoCanvasItem *chat_link;
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
    PROP_ACTIONS,
    PROP_BLOCK
};


G_DEFINE_TYPE_WITH_CODE(HippoCanvasChatPreview, hippo_canvas_chat_preview, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_chat_preview_iface_init));

static const char *
hippo_canvas_chat_preview_get_chat_id(HippoCanvasChatPreview *chat_preview)
{
    const char *chat_id = NULL;
    
    if (chat_preview->block)
        ddm_data_resource_get(chat_preview->block,
                              "chatId", DDM_DATA_STRING, &chat_id,
                              NULL);

    return chat_id;
}

static void
on_chat_activated(HippoCanvasItem        *chat_link,
                  HippoCanvasChatPreview *chat_preview)
{
    const char *chat_id = hippo_canvas_chat_preview_get_chat_id(chat_preview);
    if (chat_id)
        hippo_actions_join_chat_id(chat_preview->actions, chat_id);
}

static void
hippo_canvas_chat_preview_init(HippoCanvasChatPreview *chat_preview)
{
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
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));
    
    g_object_class_install_property(object_class,
                                    PROP_BLOCK,
                                    g_param_spec_pointer("block",
                                                        _("Block"),
                                                        _("Block with the chat to display"),
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_chat_preview_dispose(GObject *object)
{
    HippoCanvasChatPreview *chat_preview = HIPPO_CANVAS_CHAT_PREVIEW(object);

    hippo_canvas_chat_preview_set_actions(chat_preview, NULL);
    hippo_canvas_chat_preview_set_block(chat_preview, NULL);
    
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
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_chat_preview_set_actions(chat_preview, new_actions);
        }
        break;
    case PROP_BLOCK:
        {
            DDMDataResource *block = (DDMDataResource*) g_value_get_pointer(value);
            hippo_canvas_chat_preview_set_block(chat_preview, block);
        }
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
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) chat_preview->actions);
        break;
    case PROP_BLOCK:
        g_value_set_pointer(value, (GObject*) chat_preview->block);
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
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "color-cascade", HIPPO_CASCADE_MODE_NONE,
                        "tooltip", "View all quips and comments",
                        NULL);
    chat_preview->chat_link = item;
    hippo_canvas_box_append(box, item, 0);

    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_chat_activated), chat_preview);

    update_chat_messages_count(chat_preview);
    update_chat_messages(chat_preview);
    hippo_canvas_chat_preview_update_visibility(chat_preview);

    return object;
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
update_chat_messages(HippoCanvasChatPreview *chat_preview)
{
    int i = 0;

    if (chat_preview->feed) {
        DDMFeedIter iter;
        DDMDataResource *message;
        
        ddm_feed_iter_init(&iter, chat_preview->feed);
        
        while (i < MAX_PREVIEWED && ddm_feed_iter_next(&iter, &message, NULL)) {
            HippoCanvasItem *message_preview = chat_preview->message_previews[i];
            g_object_set(G_OBJECT(message_preview),
                         "message", message,
                         NULL);
            
            hippo_canvas_item_set_visible(message_preview, TRUE);
            i++;
        }
    }

    while (i < MAX_PREVIEWED) {
        HippoCanvasItem *message_preview = chat_preview->message_previews[i];
        g_object_set(G_OBJECT(message_preview),
                     "message", NULL,
                     NULL);
        
        hippo_canvas_item_set_visible(message_preview, FALSE);
        i++;
    }
}

static void
on_feed_item_added(DDMFeed                *feed,
                   DDMDataResource        *resource,
                   gint64                  timestamp,
                   HippoCanvasChatPreview *chat_preview)
{
    update_chat_messages(chat_preview);
}

static void
on_feed_item_changed(DDMFeed                *feed,
                     DDMDataResource        *resource,
                     gint64                  timestamp,
                     HippoCanvasChatPreview *chat_preview)
{
    update_chat_messages(chat_preview);
}

static void
on_feed_item_removed(DDMFeed                *feed,
                     DDMDataResource        *resource,
                     HippoCanvasChatPreview *chat_preview)
{
    update_chat_messages(chat_preview);
}

static void
set_chat_messages_feed(HippoCanvasChatPreview *chat_preview,
                       DDMFeed                *feed)
{
    if (feed == chat_preview->feed)
        return;

    if (chat_preview->feed != NULL) {
        g_signal_handlers_disconnect_by_func(chat_preview->feed,
                                             (gpointer)on_feed_item_added,
                                             chat_preview);
        g_signal_handlers_disconnect_by_func(chat_preview->feed,
                                             (gpointer)on_feed_item_removed,
                                             chat_preview);
        g_signal_handlers_disconnect_by_func(chat_preview->feed,
                                             (gpointer)on_feed_item_changed,
                                             chat_preview);
        
        g_object_unref(chat_preview->feed);
    }

    chat_preview->feed = feed;

    if (chat_preview->feed != NULL) {
        g_object_ref(chat_preview->feed);

        g_signal_connect(chat_preview->feed, "item-added",
                         G_CALLBACK(on_feed_item_added), chat_preview);
        g_signal_connect(chat_preview->feed, "item-changed",
                         G_CALLBACK(on_feed_item_changed), chat_preview);
        g_signal_connect(chat_preview->feed, "item-removed",
                         G_CALLBACK(on_feed_item_removed), chat_preview);
    }
    
    update_chat_messages(chat_preview);
}

static void
on_chat_messages_changed(DDMDataResource *block,
                         GSList          *changed_properties,
                         gpointer         data)
{
    HippoCanvasChatPreview *chat_preview = data;
    DDMFeed *feed = NULL;
    
    if (chat_preview->block)
        ddm_data_resource_get(chat_preview->block,
                              "chatMessages", DDM_DATA_FEED, &feed,
                              NULL);

    set_chat_messages_feed(chat_preview, feed);
}

static void
update_chat_messages_count(HippoCanvasChatPreview *chat_preview)
{
    int count = 0;
    char *s;
    
    if (chat_preview->block)
        ddm_data_resource_get(chat_preview->block,
                              "chatMessagesCount", DDM_DATA_INTEGER, &count,
                              NULL);

    s = g_strdup_printf("Quips and Comments (%d)\302\273", count);
    g_object_set(G_OBJECT(chat_preview->chat_link),
                 "text", s,
                 NULL);
    g_free(s);
}

static void
on_chat_messages_count_changed(DDMDataResource *block,
                               GSList          *changed_properties,
                               gpointer         data)
{
    HippoCanvasChatPreview *chat_preview = data;
    
    update_chat_messages_count(chat_preview);
}

static void
on_chat_id_changed(DDMDataResource *block,
                   GSList          *changed_properties,
                   gpointer         data)
{
    HippoCanvasChatPreview *chat_preview = data;
    
    hippo_canvas_chat_preview_update_visibility(chat_preview);
}

static void
hippo_canvas_chat_preview_set_block(HippoCanvasChatPreview *chat_preview,
                                    DDMDataResource               *block)
{
    if (block == chat_preview->block)
        return;

    if (chat_preview->block) {
        ddm_data_resource_disconnect(chat_preview->block, on_chat_messages_changed, chat_preview);
        ddm_data_resource_disconnect(chat_preview->block, on_chat_messages_count_changed, chat_preview);
        ddm_data_resource_disconnect(chat_preview->block, on_chat_id_changed, chat_preview);
        
        ddm_data_resource_unref(chat_preview->block);
    }

    chat_preview->block = block;

    if (chat_preview->block) {
        ddm_data_resource_ref(chat_preview->block);

        ddm_data_resource_connect(chat_preview->block, "chatMessages",
                                  on_chat_messages_changed, chat_preview);
        ddm_data_resource_connect(chat_preview->block, "chatMessagesCount",
                                  on_chat_messages_count_changed, chat_preview);
        ddm_data_resource_connect(chat_preview->block, "chatId",
                                  on_chat_id_changed, chat_preview);
    }

    on_chat_messages_changed(block, NULL, chat_preview);
    on_chat_messages_count_changed(block, NULL, chat_preview);
    on_chat_id_changed(block, NULL, chat_preview);
    
    g_object_notify(G_OBJECT(chat_preview), "block");
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
    gboolean show_chat = hippo_canvas_chat_preview_get_chat_id(chat_preview) != NULL;

    if (chat_preview->chat_link == NULL) /* during initialization */
        return;
    
    hippo_canvas_item_set_visible(chat_preview->chat_link,
                                  show_chat);
}
