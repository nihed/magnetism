/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include <hippo/hippo-chat-room.h>
#include "hippo-actions.h"
#include "hippo-canvas-message-preview.h"
#include "hippo-canvas-block.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-entity-name.h>

static void      hippo_canvas_message_preview_init                (HippoCanvasMessagePreview       *box);
static void      hippo_canvas_message_preview_class_init          (HippoCanvasMessagePreviewClass  *klass);
static void      hippo_canvas_message_preview_dispose             (GObject                *object);
static void      hippo_canvas_message_preview_finalize            (GObject                *object);

static void hippo_canvas_message_preview_set_property (GObject      *object,
                                                       guint         prop_id,
                                                       const GValue *value,
                                                       GParamSpec   *pspec);
static void hippo_canvas_message_preview_get_property (GObject      *object,
                                                       guint         prop_id,
                                                       GValue       *value,
                                                       GParamSpec   *pspec);
static GObject* hippo_canvas_message_preview_constructor (GType                  type,
                                                          guint                  n_construct_properties,
                                                          GObjectConstructParam *construct_properties);

static void hippo_canvas_message_preview_set_message (HippoCanvasMessagePreview *message_preview,
                                                      HippoChatMessage          *message);
static void hippo_canvas_message_preview_update      (HippoCanvasMessagePreview *message_preview);
static void hippo_canvas_message_preview_set_actions (HippoCanvasMessagePreview *message_preview,
                                                      HippoActions              *actions);
static void hippo_canvas_message_preview_set_hushed  (HippoCanvasMessagePreview *message_preview,
                                                      gboolean                   value);

struct _HippoCanvasMessagePreview {
    HippoCanvasBox parent;
    
    HippoChatMessage *message;
    HippoActions *actions;
    unsigned int hushed : 1;

    HippoCanvasItem *message_text;
    HippoCanvasItem *entity_name;
};

struct _HippoCanvasMessagePreviewClass {
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
    PROP_MESSAGE,
    PROP_HUSHED
};


G_DEFINE_TYPE(HippoCanvasMessagePreview, hippo_canvas_message_preview, HIPPO_TYPE_CANVAS_BOX);

static void
hippo_canvas_message_preview_init(HippoCanvasMessagePreview *message_preview)
{
}

static void
hippo_canvas_message_preview_class_init(HippoCanvasMessagePreviewClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_message_preview_set_property;
    object_class->get_property = hippo_canvas_message_preview_get_property;
    object_class->constructor = hippo_canvas_message_preview_constructor;

    object_class->dispose = hippo_canvas_message_preview_dispose;
    object_class->finalize = hippo_canvas_message_preview_finalize;

    g_object_class_install_property(object_class,
                                    PROP_MESSAGE,
                                    g_param_spec_pointer("message",
                                                         _("Chat Message"),
                                                         _("Chat message to preview"),
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));

    g_object_class_install_property(object_class,
                                    PROP_HUSHED,
                                    g_param_spec_boolean("hushed",
                                                         _("Hushed"),
                                                         _("Whether the chat this message is part of is currently hushed"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_message_preview_dispose(GObject *object)
{
    HippoCanvasMessagePreview *message_preview = HIPPO_CANVAS_MESSAGE_PREVIEW(object);

    hippo_canvas_message_preview_set_message(message_preview, NULL);
    hippo_canvas_message_preview_set_actions(message_preview, NULL);
    
    G_OBJECT_CLASS(hippo_canvas_message_preview_parent_class)->dispose(object);
}

static void
hippo_canvas_message_preview_finalize(GObject *object)
{
    /* HippoCanvasMessagePreview *box = HIPPO_CANVAS_MESSAGE_PREVIEW(object); */

    G_OBJECT_CLASS(hippo_canvas_message_preview_parent_class)->finalize(object);
}

static void
hippo_canvas_message_preview_set_property(GObject         *object,
                                          guint            prop_id,
                                          const GValue    *value,
                                          GParamSpec      *pspec)
{
    HippoCanvasMessagePreview *message_preview;

    message_preview = HIPPO_CANVAS_MESSAGE_PREVIEW(object);

    switch (prop_id) {
    case PROP_MESSAGE:
        {
            HippoChatMessage *new_room = (HippoChatMessage*) g_value_get_pointer(value);
            hippo_canvas_message_preview_set_message(message_preview, new_room);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_message_preview_set_actions(message_preview, new_actions);
        }
        break;
    case PROP_HUSHED:
        hippo_canvas_message_preview_set_hushed(message_preview,
                                                g_value_get_boolean(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_message_preview_get_property(GObject         *object,
                                       guint            prop_id,
                                       GValue          *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasMessagePreview *message_preview;

    message_preview = HIPPO_CANVAS_MESSAGE_PREVIEW (object);

    switch (prop_id) {
    case PROP_MESSAGE:
        g_value_set_pointer(value, message_preview->message);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) message_preview->actions);
        break;
    case PROP_HUSHED:
        g_value_set_boolean(value, message_preview->hushed);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_message_preview_constructor (GType                  type,
                                          guint                  n_construct_properties,
                                          GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_message_preview_parent_class)->constructor(type,
                                                                                          n_construct_properties,
                                                                                          construct_properties);
    HippoCanvasMessagePreview *message_preview = HIPPO_CANVAS_MESSAGE_PREVIEW(object);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(object);
    HippoCanvasItem *item;
        
    g_object_set(object, 
                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                 NULL);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "chat",
                        "xalign", HIPPO_ALIGNMENT_START,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        "border-right", 4,
                        NULL);
    
    hippo_canvas_box_append(box, item, 0);
    
    message_preview->message_text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                 "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                                 "xalign", HIPPO_ALIGNMENT_START,
                                                 NULL);
    hippo_canvas_box_append(box, message_preview->message_text, 0);
        
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", " - ", /* FIXME figure out mdash */
                        NULL);
    hippo_canvas_box_append(box, item, 0);
        
    message_preview->entity_name = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_NAME,
                                                "actions", message_preview->actions,
                                                NULL);
    hippo_canvas_box_append(box, message_preview->entity_name, 0);

    hippo_canvas_message_preview_update(message_preview);
    
    return object;
}

static void
hippo_canvas_message_preview_update(HippoCanvasMessagePreview *message_preview)
{
    HippoChatMessage *message = message_preview->message;
    
    g_object_set(G_OBJECT(message_preview->message_text),
                 "text", message ? hippo_chat_message_get_text(message) : NULL,
                 NULL);
    g_object_set(G_OBJECT(message_preview->entity_name),
                 "entity", message ? hippo_chat_message_get_person(message) : NULL,
                 NULL);
}

static void
hippo_canvas_message_preview_set_message(HippoCanvasMessagePreview *message_preview,
                                         HippoChatMessage          *message)
{
    if (message == message_preview->message)
        return;

    if (message_preview->message) {
        hippo_chat_message_free(message_preview->message);
        message_preview->message = NULL;
    }
    
    if (message) {
        message_preview->message = hippo_chat_message_copy(message);
    }

    hippo_canvas_message_preview_update(message_preview);
    
    g_object_notify(G_OBJECT(message_preview), "message");
}

static void
hippo_canvas_message_preview_set_actions(HippoCanvasMessagePreview  *message_preview,
                                         HippoActions            *actions)
{
    if (actions == message_preview->actions)
        return;

    if (message_preview->actions) {
        g_object_unref(message_preview->actions);
        message_preview->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        message_preview->actions = actions;
    }

    g_object_notify(G_OBJECT(message_preview), "actions");
}

static void
hippo_canvas_message_preview_set_hushed(HippoCanvasMessagePreview *message_preview,
                                        gboolean                   value)
{
    value = value != FALSE;
    
    if (message_preview->hushed == value)
        return;

    message_preview->hushed = value;

    g_object_notify(G_OBJECT(message_preview), "hushed");
}
