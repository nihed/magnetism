/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include <hippo/hippo-chat-room.h>
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
                                                        HippoBlock                    *block);

struct _HippoCanvasLastMessagePreview {
    HippoCanvasMessagePreview parent;

    HippoBlock *block;
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
                                    g_param_spec_object("block",
                                                        _("Block"),
                                                        _("Block with the chat to display"),
                                                        HIPPO_TYPE_BLOCK,
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
            HippoBlock *block = (HippoBlock*) g_value_get_object(value);
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
        g_value_set_object(value, (GObject*) message_preview->block);
        break;
    }
}

static void
update_recent_messages(HippoCanvasLastMessagePreview *message_preview)
{
    HippoChatMessage *last_message = NULL;

    if (message_preview->block) {
        GSList *messages = hippo_block_get_recent_messages(message_preview->block);
        if (messages)
            last_message = messages->data;
    }

    g_object_set(G_OBJECT(message_preview),
                 "message", last_message,
                 NULL);
}

static void
on_recent_messages_changed(HippoBlock                    *block,
                           GParamSpec                    *arg,
                           HippoCanvasLastMessagePreview *message_preview)
{
    update_recent_messages(message_preview);
}

static void
hippo_canvas_last_message_preview_set_block(HippoCanvasLastMessagePreview *message_preview,
                                            HippoBlock                    *block)
{
    if (block == message_preview->block)
        return;

    if (message_preview->block) {
        g_object_unref(message_preview->block);

        g_signal_handlers_disconnect_by_func(message_preview->block,
                                             (void *)on_recent_messages_changed, message_preview);
    }

    message_preview->block = block;

    if (message_preview->block) {
        g_object_ref(message_preview->block);
        
        g_signal_connect(message_preview->block, "notify::recent-messages",
                         G_CALLBACK(on_recent_messages_changed), message_preview);
    }

    update_recent_messages(message_preview);
    
    g_object_notify(G_OBJECT(message_preview), "block");
}
