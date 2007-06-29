/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include <hippo/hippo-chat-room.h>
#include "hippo-actions.h"
#include "hippo-canvas-quipper.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image-button.h>
#include <hippo/hippo-canvas-link.h>

static void      hippo_canvas_quipper_init                (HippoCanvasQuipper       *box);
static void      hippo_canvas_quipper_class_init          (HippoCanvasQuipperClass  *klass);
static void      hippo_canvas_quipper_dispose             (GObject                *object);
static void      hippo_canvas_quipper_finalize            (GObject                *object);

static void hippo_canvas_quipper_set_property (GObject      *object,
                                                       guint         prop_id,
                                                       const GValue *value,
                                                       GParamSpec   *pspec);
static void hippo_canvas_quipper_get_property (GObject      *object,
                                                       guint         prop_id,
                                                       GValue       *value,
                                                       GParamSpec   *pspec);
static GObject* hippo_canvas_quipper_constructor (GType                  type,
                                                  guint                  n_construct_properties,
                                                  GObjectConstructParam *construct_properties);

static void hippo_canvas_quipper_set_actions (HippoCanvasQuipper *quipper,
                                              HippoActions       *actions);
static void hippo_canvas_quipper_set_chat_id (HippoCanvasQuipper *quipper,
                                              const char         *id);
static void hippo_canvas_quipper_set_title   (HippoCanvasQuipper *quipper,
                                              const char         *title);

static void on_quip_activated(HippoCanvasItem       *item,
                              HippoCanvasQuipper *quipper);
static void on_love_activated(HippoCanvasItem       *item,
                              HippoCanvasQuipper *quipper);
static void on_hate_activated(HippoCanvasItem       *item,
                              HippoCanvasQuipper *quipper);


struct _HippoCanvasQuipper {
    HippoCanvasBox parent;
    
    HippoActions *actions;
    char *chat_id;
    char *title;
};

struct _HippoCanvasQuipperClass {
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
    PROP_CHAT_ID,
    PROP_TITLE
};


G_DEFINE_TYPE(HippoCanvasQuipper, hippo_canvas_quipper, HIPPO_TYPE_CANVAS_BOX);

static void
hippo_canvas_quipper_init(HippoCanvasQuipper *quipper)
{
}

static void
hippo_canvas_quipper_class_init(HippoCanvasQuipperClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_quipper_set_property;
    object_class->get_property = hippo_canvas_quipper_get_property;
    object_class->constructor = hippo_canvas_quipper_constructor;

    object_class->dispose = hippo_canvas_quipper_dispose;
    object_class->finalize = hippo_canvas_quipper_finalize;

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));
    g_object_class_install_property(object_class,
                                    PROP_CHAT_ID,
                                    g_param_spec_string("chat-id",
                                                        _("Chat ID"),
                                                        _("ID of chat room"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("Title"),
                                                        _("Title to display in quip window"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
}

static void
hippo_canvas_quipper_dispose(GObject *object)
{
    HippoCanvasQuipper *quipper = HIPPO_CANVAS_QUIPPER(object);

    hippo_canvas_quipper_set_actions(quipper, NULL);
    hippo_canvas_quipper_set_chat_id(quipper, NULL);
    hippo_canvas_quipper_set_title(quipper, NULL);
    
    G_OBJECT_CLASS(hippo_canvas_quipper_parent_class)->dispose(object);
}

static void
hippo_canvas_quipper_finalize(GObject *object)
{
    /* HippoCanvasQuipper *box = HIPPO_CANVAS_QUIPPER(object); */

    G_OBJECT_CLASS(hippo_canvas_quipper_parent_class)->finalize(object);
}

static void
hippo_canvas_quipper_set_property(GObject         *object,
                                          guint            prop_id,
                                          const GValue    *value,
                                          GParamSpec      *pspec)
{
    HippoCanvasQuipper *quipper;

    quipper = HIPPO_CANVAS_QUIPPER(object);

    switch (prop_id) {
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_quipper_set_actions(quipper, new_actions);
        }
        break;
    case PROP_CHAT_ID:
        {
            const char *new_id = g_value_get_string(value);
            hippo_canvas_quipper_set_chat_id(quipper, new_id);
        }
        break;
    case PROP_TITLE:
        {
            const char *new_title = g_value_get_string(value);
            hippo_canvas_quipper_set_title(quipper, new_title);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_quipper_get_property(GObject         *object,
                                       guint            prop_id,
                                       GValue          *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasQuipper *quipper;

    quipper = HIPPO_CANVAS_QUIPPER (object);

    switch (prop_id) {
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) quipper->actions);
        break;
    case PROP_CHAT_ID:
        g_value_set_string(value, quipper->chat_id);
        break;
    case PROP_TITLE:
        g_value_set_string(value, quipper->title);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_quipper_constructor (GType                  type,
                                  guint                  n_construct_properties,
                                  GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_quipper_parent_class)->constructor(type,
                                                                                          n_construct_properties,
                                                                                          construct_properties);
    HippoCanvasQuipper *quipper = HIPPO_CANVAS_QUIPPER(object);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(object);
    HippoCanvasItem *item;
        
    g_object_set(object, 
                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                 NULL);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "normal-image-name", "chat",
                        "tooltip", "Add a quip",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_quip_activated), quipper);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "Quip",
                        "tooltip", "Add a quip",
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_quip_activated), quipper);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "padding-left", 8,
                        "normal-image-name", "quiplove_icon",
                        "tooltip", "Add a quip",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_love_activated), quipper);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I love it!",
                        "tooltip", "Add a quip",
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_love_activated), quipper);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "padding-left", 8,
                        "tooltip", "Add a quip",
                        "normal-image-name", "quiphate_icon",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hate_activated), quipper);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I hate it!",
                        "tooltip", "Add a quip",
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hate_activated), quipper);
    
    return object;
}

static void
hippo_canvas_quipper_set_actions(HippoCanvasQuipper  *quipper,
                                         HippoActions            *actions)
{
    if (actions == quipper->actions)
        return;

    if (quipper->actions) {
        g_object_unref(quipper->actions);
        quipper->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        quipper->actions = actions;
    }

    g_object_notify(G_OBJECT(quipper), "actions");
}

static void
hippo_canvas_quipper_set_chat_id(HippoCanvasQuipper *quipper,
                                 const char         *id)
{
    if (id == quipper->chat_id ||
        (id && quipper->chat_id && strcmp(id, quipper->chat_id) == 0))
        return;

    if (quipper->chat_id)
        g_free(quipper->chat_id);

    quipper->chat_id = g_strdup(id);

    g_object_notify(G_OBJECT(quipper), "chat-id");
}
                                                              
static void
hippo_canvas_quipper_set_title(HippoCanvasQuipper *quipper,
                               const char         *title)
{
    if (title == quipper->title ||
        (title && quipper->title && strcmp(title, quipper->title) == 0))
        return;

    if (quipper->title)
        g_free(quipper->title);

    quipper->title = g_strdup(title);

    g_object_notify(G_OBJECT(quipper), "chat-id");
}

static void
quip_on(HippoCanvasQuipper *quipper,
        HippoSentiment      sentiment)
{
    if (!quipper->chat_id)
        return;
    
    hippo_actions_quip(quipper->actions,
                       HIPPO_CHAT_KIND_UNKNOWN, quipper->chat_id,
                       sentiment, quipper->title);
}

static void
on_quip_activated(HippoCanvasItem       *item,
                  HippoCanvasQuipper *quipper)
{
    quip_on(quipper, HIPPO_SENTIMENT_INDIFFERENT);
}

static void
on_love_activated(HippoCanvasItem       *item,
                  HippoCanvasQuipper *quipper)
{
    quip_on(quipper, HIPPO_SENTIMENT_LOVE);
}

static void
on_hate_activated(HippoCanvasItem       *item,
                  HippoCanvasQuipper *quipper)
{
    quip_on(quipper, HIPPO_SENTIMENT_HATE);
}
