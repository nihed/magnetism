/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-facebook-event.h"
#include "hippo-canvas-block-facebook-event.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"
#include "hippo-canvas-thumbnails.h"

static void      hippo_canvas_block_facebook_event_init                (HippoCanvasBlockFacebookEvent       *block);
static void      hippo_canvas_block_facebook_event_class_init          (HippoCanvasBlockFacebookEventClass  *klass);
static void      hippo_canvas_block_facebook_event_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_facebook_event_dispose             (GObject                *object);
static void      hippo_canvas_block_facebook_event_finalize            (GObject                *object);

static void hippo_canvas_block_facebook_event_set_property (GObject      *object,
                                                             guint         prop_id,
                                                             const GValue *value,
                                                             GParamSpec   *pspec);
static void hippo_canvas_block_facebook_event_get_property (GObject      *object,
                                                             guint         prop_id,
                                                             GValue       *value,
                                                             GParamSpec   *pspec);
static GObject* hippo_canvas_block_facebook_event_constructor (GType                  type,
                                                                guint                  n_construct_properties,
                                                                GObjectConstructParam *construct_params);

/* Canvas block methods */
static void hippo_canvas_block_facebook_event_set_block       (HippoCanvasBlock *canvas_block,
                                                                HippoBlock       *block);

static void hippo_canvas_block_facebook_event_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_facebook_event_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_facebook_event_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockFacebookEvent *block_facebook_event,
                        HippoPerson                 *person);


struct _HippoCanvasBlockFacebookEvent {
    HippoCanvasBlock canvas_block;
    HippoCanvasBox *thumbnails_parent;
    HippoCanvasItem *thumbnails;
    HippoPerson *person;
};

struct _HippoCanvasBlockFacebookEventClass {
    HippoCanvasBlockClass parent_class;

};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockFacebookEvent, hippo_canvas_block_facebook_event, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_facebook_event_iface_init));

static void
hippo_canvas_block_facebook_event_init(HippoCanvasBlockFacebookEvent *block_facebook_event)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_facebook_event);

    block->required_type = HIPPO_BLOCK_TYPE_FACEBOOK_EVENT;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_facebook_event_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_facebook_event_class_init(HippoCanvasBlockFacebookEventClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_facebook_event_set_property;
    object_class->get_property = hippo_canvas_block_facebook_event_get_property;
    object_class->constructor = hippo_canvas_block_facebook_event_constructor;

    object_class->dispose = hippo_canvas_block_facebook_event_dispose;
    object_class->finalize = hippo_canvas_block_facebook_event_finalize;

    canvas_block_class->set_block = hippo_canvas_block_facebook_event_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_facebook_event_title_activated;
    canvas_block_class->expand = hippo_canvas_block_facebook_event_expand;
    canvas_block_class->unexpand = hippo_canvas_block_facebook_event_unexpand;
}

static void
hippo_canvas_block_facebook_event_dispose(GObject *object)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event;

    block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(object);

    set_person(block_facebook_event, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_facebook_event_parent_class)->dispose(object);
}

static void
hippo_canvas_block_facebook_event_finalize(GObject *object)
{
    /* HippoCanvasBlockFacebookEvent *block = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(object); */

    G_OBJECT_CLASS(hippo_canvas_block_facebook_event_parent_class)->finalize(object);
}

static void
hippo_canvas_block_facebook_event_set_property(GObject         *object,
                                                guint            prop_id,
                                                const GValue    *value,
                                                GParamSpec      *pspec)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event;

    block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_facebook_event_get_property(GObject         *object,
                                                guint            prop_id,
                                                GValue          *value,
                                                GParamSpec      *pspec)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event;

    block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_facebook_event_constructor (GType                  type,
                                                guint                  n_construct_properties,
                                                GObjectConstructParam *construct_properties)
{
    GObject *object;
    HippoCanvasBlock *block;
    HippoCanvasBlockFacebookEvent *block_facebook_event;

    object = G_OBJECT_CLASS(hippo_canvas_block_facebook_event_parent_class)->constructor(type,
                                                                                          n_construct_properties,
                                                                                          construct_properties);
    block = HIPPO_CANVAS_BLOCK(object);
    block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(object);

    hippo_canvas_block_set_heading(block, _("Facebook event"));

    block_facebook_event->thumbnails_parent =
        g_object_new(HIPPO_TYPE_CANVAS_BOX,
                     NULL);

    block_facebook_event->thumbnails =
        g_object_new(HIPPO_TYPE_CANVAS_THUMBNAILS,
                     "actions", block->actions,
                     NULL);

    hippo_canvas_box_append(block_facebook_event->thumbnails_parent,
                            block_facebook_event->thumbnails, HIPPO_PACK_EXPAND);

    hippo_canvas_box_set_child_visible(block_facebook_event->thumbnails_parent,
                                       block_facebook_event->thumbnails,
                                       FALSE);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(block_facebook_event->thumbnails_parent));

    return object;
}

static void
set_person(HippoCanvasBlockFacebookEvent *block_facebook_event,
           HippoPerson                 *person)
{
    if (person == block_facebook_event->person)
        return;

    if (block_facebook_event->person) {
        g_object_unref(block_facebook_event->person);
        block_facebook_event->person = NULL;
    }

    if (person) {
        block_facebook_event->person = person;
        g_object_ref(G_OBJECT(person));
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_facebook_event),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockFacebookEvent *block_facebook_event)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_facebook_event, person);
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_facebook_event_set_block(HippoCanvasBlock *canvas_block,
                                             HippoBlock       *block)
{
    /* g_debug("canvas-block-facebook-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_facebook_event_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        HippoThumbnails *thumbnails;

        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block));

        thumbnails = hippo_block_facebook_event_get_thumbnails(HIPPO_BLOCK_FACEBOOK_EVENT(canvas_block->block));
        if (thumbnails != NULL) {
            hippo_canvas_block_set_title(canvas_block,
                                         hippo_block_facebook_event_get_title(HIPPO_BLOCK_FACEBOOK_EVENT(canvas_block->block)),
                                         hippo_thumbnails_get_more_link(thumbnails),
                                         FALSE);
            g_object_set(HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block)->thumbnails,
                         "thumbnails", thumbnails,
                         NULL);            
        }

        if (hippo_thumbnails_get_count(thumbnails) <= 0)
            canvas_block->expandable = FALSE;
    }
}

static void
hippo_canvas_block_facebook_event_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoThumbnails *thumbnails;

    if (canvas_block->block == NULL)
        return;

    thumbnails = hippo_block_facebook_event_get_thumbnails(HIPPO_BLOCK_FACEBOOK_EVENT(canvas_block->block));
    if (thumbnails == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_thumbnails_get_more_link(thumbnails));
}

static void
hippo_canvas_block_facebook_event_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block);

    hippo_canvas_box_set_child_visible(block_facebook_event->thumbnails_parent,
                                       block_facebook_event->thumbnails,
                                       TRUE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_facebook_event_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_facebook_event_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block);

    hippo_canvas_box_set_child_visible(block_facebook_event->thumbnails_parent,
                                       block_facebook_event->thumbnails,
                                       FALSE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_facebook_event_parent_class)->unexpand(canvas_block);
}
