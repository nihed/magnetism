/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-stacker-internal.h"
#include "hippo-actions.h"
#include "hippo-canvas-last-message-preview.h"
#include "hippo-canvas-message-preview.h"

static void      hippo_canvas_last_message_preview_init                (HippoCanvasLastMessagePreview       *box);
static void      hippo_canvas_last_message_preview_class_init          (HippoCanvasLastMessagePreviewClass  *klass);
static void      hippo_canvas_last_message_preview_dispose             (GObject                *object);
static void      hippo_canvas_last_message_preview_finalize            (GObject                *object);

static void hippo_canvas_last_message_preview_set_property (GObject      *object,
                                                       guint         prop_id,
                                                       const GValue *value,
                                                       GParamSpec   *pspec);
static void hippo_canvas_last_message_preview_get_property (GObject      *object,
                                                       guint         prop_id,
                                                       GValue       *value,
                                                       GParamSpec   *pspec);

static void hippo_canvas_last_message_preview_set_block(HippoCanvasLastMessagePreview *message_preview,
                                                        DDMDataResource               *block);

struct _HippoCanvasLastMessagePreview {
    HippoCanvasMessagePreview parent;

    DDMDataResource *block;
    DDMFeed *feed;
};

struct _HippoCanvasLastMessagePreviewClass {
    HippoCanvasMessagePreviewClass parent_class;
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
    PROP_BLOCK,
};


G_DEFINE_TYPE(HippoCanvasLastMessagePreview, hippo_canvas_last_message_preview, HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW);

static void
hippo_canvas_last_message_preview_init(HippoCanvasLastMessagePreview *message_preview)
{
}

static void
hippo_canvas_last_message_preview_class_init(HippoCanvasLastMessagePreviewClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_last_message_preview_set_property;
    object_class->get_property = hippo_canvas_last_message_preview_get_property;

    object_class->dispose = hippo_canvas_last_message_preview_dispose;
    object_class->finalize = hippo_canvas_last_message_preview_finalize;

    g_object_class_install_property(object_class,
                                    PROP_BLOCK,
                                    g_param_spec_pointer("block",
                                                         _("Block"),
                                                         _("Block with the chat to display"),
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_last_message_preview_dispose(GObject *object)
{
    HippoCanvasLastMessagePreview *message_preview = HIPPO_CANVAS_LAST_MESSAGE_PREVIEW(object);

    hippo_canvas_last_message_preview_set_block(message_preview, NULL);

    G_OBJECT_CLASS(hippo_canvas_last_message_preview_parent_class)->dispose(object);
}

static void
hippo_canvas_last_message_preview_finalize(GObject *object)
{
    /* HippoCanvasLastMessagePreview *box = HIPPO_CANVAS_LAST_MESSAGE_PREVIEW(object); */

    G_OBJECT_CLASS(hippo_canvas_last_message_preview_parent_class)->finalize(object);
}

static void
hippo_canvas_last_message_preview_set_property(GObject         *object,
                                          guint            prop_id,
                                          const GValue    *value,
                                          GParamSpec      *pspec)
{
    HippoCanvasLastMessagePreview *message_preview;

    message_preview = HIPPO_CANVAS_LAST_MESSAGE_PREVIEW(object);

    switch (prop_id) {
    case PROP_BLOCK:
        {
            DDMDataResource *block = (DDMDataResource*) g_value_get_pointer(value);
            hippo_canvas_last_message_preview_set_block(message_preview, block);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_last_message_preview_get_property(GObject         *object,
                                               guint            prop_id,
                                               GValue          *value,
                                               GParamSpec      *pspec)
{
    HippoCanvasLastMessagePreview *message_preview;

    message_preview = HIPPO_CANVAS_LAST_MESSAGE_PREVIEW (object);

    switch (prop_id) {
    case PROP_BLOCK:
        g_value_set_pointer(value, (GObject*) message_preview->block);
        break;
    }
}

static void
update_chat_messages(HippoCanvasLastMessagePreview *message_preview)
{
    DDMDataResource *last_message = NULL;

    if (message_preview->feed) {
        DDMFeedIter iter;
        ddm_feed_iter_init(&iter, message_preview->feed);
        ddm_feed_iter_next(&iter, &last_message, NULL);
    }

    g_object_set(G_OBJECT(message_preview),
                 "message", last_message,
                 NULL);
}

static void
on_feed_item_added(DDMFeed                       *feed,
                   DDMDataResource               *resource,
                   gint64                         timestamp,
                   HippoCanvasLastMessagePreview *message_preview)
{
    update_chat_messages(message_preview);
}

static void
on_feed_item_changed(DDMFeed                       *feed,
                     DDMDataResource               *resource,
                     gint64                         timestamp,
                     HippoCanvasLastMessagePreview *message_preview)
{
    update_chat_messages(message_preview);
}

static void
on_feed_item_removed(DDMFeed                       *feed,
                     DDMDataResource               *resource,
                     HippoCanvasLastMessagePreview *message_preview)
{
    update_chat_messages(message_preview);
}

static void
set_chat_messages_feed(HippoCanvasLastMessagePreview *message_preview,
                       DDMFeed                       *feed)
{
    if (feed == message_preview->feed)
        return;

    if (message_preview->feed != NULL) {
        g_signal_handlers_disconnect_by_func(message_preview->feed,
                                             (gpointer)on_feed_item_added,
                                             message_preview);
        g_signal_handlers_disconnect_by_func(message_preview->feed,
                                             (gpointer)on_feed_item_removed,
                                             message_preview);
        g_signal_handlers_disconnect_by_func(message_preview->feed,
                                             (gpointer)on_feed_item_changed,
                                             message_preview);
        
        g_object_unref(message_preview->feed);
    }

    message_preview->feed = feed;

    if (message_preview->feed != NULL) {
        g_object_ref(message_preview->feed);

        g_signal_connect(message_preview->feed, "item-added",
                         G_CALLBACK(on_feed_item_added), message_preview);
        g_signal_connect(message_preview->feed, "item-changed",
                         G_CALLBACK(on_feed_item_changed), message_preview);
        g_signal_connect(message_preview->feed, "item-removed",
                         G_CALLBACK(on_feed_item_removed), message_preview);
    }
    
    update_chat_messages(message_preview);
}

static void
on_chat_messages_changed(DDMDataResource *block,
                         GSList          *changed_properties,
                         gpointer         data)
{
    HippoCanvasLastMessagePreview *message_preview = data;
    DDMFeed *feed = NULL;
    
    if (message_preview->block) {
        ddm_data_resource_get(message_preview->block,
                              "chatMessages", DDM_DATA_FEED, &feed,
                              NULL);
    }

    set_chat_messages_feed(message_preview, feed);
}

static void
hippo_canvas_last_message_preview_set_block(HippoCanvasLastMessagePreview *message_preview,
                                            DDMDataResource               *block)
{
    if (block == message_preview->block)
        return;

    if (message_preview->block) {
        ddm_data_resource_disconnect(message_preview->block, on_chat_messages_changed, message_preview);
        
        ddm_data_resource_unref(message_preview->block);
    }

    message_preview->block = block;

    if (message_preview->block) {
        ddm_data_resource_ref(message_preview->block);

        ddm_data_resource_connect(message_preview->block, "chatMessages",
                                  on_chat_messages_changed, message_preview);
    }

    on_chat_messages_changed(block, NULL, message_preview);
    
    g_object_notify(G_OBJECT(message_preview), "block");
}
