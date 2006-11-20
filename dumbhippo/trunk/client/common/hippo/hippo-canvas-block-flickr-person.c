/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-flickr-person.h"
#include "hippo-canvas-block-flickr-person.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"
#include "hippo-canvas-thumbnails.h"

static void      hippo_canvas_block_flickr_person_init                (HippoCanvasBlockFlickrPerson       *block);
static void      hippo_canvas_block_flickr_person_class_init          (HippoCanvasBlockFlickrPersonClass  *klass);
static void      hippo_canvas_block_flickr_person_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_flickr_person_dispose             (GObject                *object);
static void      hippo_canvas_block_flickr_person_finalize            (GObject                *object);

static void hippo_canvas_block_flickr_person_set_property (GObject      *object,
                                                           guint         prop_id,
                                                           const GValue *value,
                                                           GParamSpec   *pspec);
static void hippo_canvas_block_flickr_person_get_property (GObject      *object,
                                                           guint         prop_id,
                                                           GValue       *value,
                                                           GParamSpec   *pspec);
static GObject* hippo_canvas_block_flickr_person_constructor (GType                  type,
                                                              guint                  n_construct_properties,
                                                              GObjectConstructParam *construct_params);

/* Canvas block methods */
static void hippo_canvas_block_flickr_person_set_block       (HippoCanvasBlock *canvas_block,
                                                              HippoBlock       *block);

static void hippo_canvas_block_flickr_person_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_flickr_person_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_flickr_person_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockFlickrPerson *block_flickr_person,
                        HippoPerson                 *person);


struct _HippoCanvasBlockFlickrPerson {
    HippoCanvasBlock canvas_block;
    HippoCanvasBox *thumbnails_parent;
    HippoCanvasItem *thumbnails;
    HippoPerson *person;
};

struct _HippoCanvasBlockFlickrPersonClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockFlickrPerson, hippo_canvas_block_flickr_person, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_flickr_person_iface_init));

static void
hippo_canvas_block_flickr_person_init(HippoCanvasBlockFlickrPerson *block_flickr_person)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_flickr_person);

    block->required_type = HIPPO_BLOCK_TYPE_FLICKR_PERSON;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_flickr_person_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_flickr_person_class_init(HippoCanvasBlockFlickrPersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_flickr_person_set_property;
    object_class->get_property = hippo_canvas_block_flickr_person_get_property;
    object_class->constructor = hippo_canvas_block_flickr_person_constructor;

    object_class->dispose = hippo_canvas_block_flickr_person_dispose;
    object_class->finalize = hippo_canvas_block_flickr_person_finalize;

    canvas_block_class->set_block = hippo_canvas_block_flickr_person_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_flickr_person_title_activated;
    canvas_block_class->expand = hippo_canvas_block_flickr_person_expand;
    canvas_block_class->unexpand = hippo_canvas_block_flickr_person_unexpand;
}

static void
hippo_canvas_block_flickr_person_dispose(GObject *object)
{
    HippoCanvasBlockFlickrPerson *block_flickr_person;

    block_flickr_person = HIPPO_CANVAS_BLOCK_FLICKR_PERSON(object);

    set_person(block_flickr_person, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_flickr_person_parent_class)->dispose(object);
}

static void
hippo_canvas_block_flickr_person_finalize(GObject *object)
{
    /* HippoCanvasBlockFlickrPerson *block = HIPPO_CANVAS_BLOCK_FLICKR_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_flickr_person_parent_class)->finalize(object);
}

static void
hippo_canvas_block_flickr_person_set_property(GObject         *object,
                                              guint            prop_id,
                                              const GValue    *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockFlickrPerson *block_flickr_person;

    block_flickr_person = HIPPO_CANVAS_BLOCK_FLICKR_PERSON(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_flickr_person_get_property(GObject         *object,
                                              guint            prop_id,
                                              GValue          *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockFlickrPerson *block_flickr_person;

    block_flickr_person = HIPPO_CANVAS_BLOCK_FLICKR_PERSON (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_flickr_person_constructor (GType                  type,
                                              guint                  n_construct_properties,
                                              GObjectConstructParam *construct_properties)
{
    GObject *object;
    HippoCanvasBlock *block;
    HippoCanvasBlockFlickrPerson *block_flickr_person;

    object = G_OBJECT_CLASS(hippo_canvas_block_flickr_person_parent_class)->constructor(type,
                                                                                        n_construct_properties,
                                                                                        construct_properties);
    block = HIPPO_CANVAS_BLOCK(object);
    block_flickr_person = HIPPO_CANVAS_BLOCK_FLICKR_PERSON(object);

    hippo_canvas_block_set_heading(block, _("New Flickr photos"));

    block_flickr_person->thumbnails_parent =
        g_object_new(HIPPO_TYPE_CANVAS_BOX,
                     NULL);

    block_flickr_person->thumbnails =
        g_object_new(HIPPO_TYPE_CANVAS_THUMBNAILS,
                     "actions", block->actions,
                     NULL);

    hippo_canvas_box_append(block_flickr_person->thumbnails_parent,
                            block_flickr_person->thumbnails, 0);

    hippo_canvas_box_set_child_visible(block_flickr_person->thumbnails_parent,
                                       block_flickr_person->thumbnails,
                                       FALSE);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(block_flickr_person->thumbnails_parent));

    return object;
}

static void
set_person(HippoCanvasBlockFlickrPerson *block_flickr_person,
           HippoPerson                 *person)
{
    if (person == block_flickr_person->person)
        return;

    if (block_flickr_person->person) {
        g_object_unref(block_flickr_person->person);
        block_flickr_person->person = NULL;
    }

    if (person) {
        block_flickr_person->person = person;
        g_object_ref(G_OBJECT(person));
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_flickr_person),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockFlickrPerson *block_flickr_person)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_flickr_person, person);
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_flickr_person_set_block(HippoCanvasBlock *canvas_block,
                                           HippoBlock       *block)
{
    /* g_debug("canvas-block-flickr-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_FLICKR_PERSON(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_flickr_person_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        HippoThumbnails *thumbnails;

        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_FLICKR_PERSON(canvas_block));

        thumbnails = hippo_block_flickr_person_get_thumbnails(HIPPO_BLOCK_FLICKR_PERSON(canvas_block->block));
        if (thumbnails != NULL) {
            hippo_canvas_block_set_title(canvas_block,
                                         hippo_thumbnails_get_more_title(thumbnails),
                                         hippo_thumbnails_get_more_link(thumbnails),
                                         FALSE);
        }
    }
}

static void
hippo_canvas_block_flickr_person_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoThumbnails *thumbnails;

    if (canvas_block->block == NULL)
        return;

    thumbnails = hippo_block_flickr_person_get_thumbnails(HIPPO_BLOCK_FLICKR_PERSON(canvas_block->block));
    if (thumbnails == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_thumbnails_get_more_link(thumbnails));
}

static void
hippo_canvas_block_flickr_person_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockFlickrPerson *block_flickr_person = HIPPO_CANVAS_BLOCK_FLICKR_PERSON(canvas_block);

    hippo_canvas_box_set_child_visible(block_flickr_person->thumbnails_parent,
                                       block_flickr_person->thumbnails,
                                       TRUE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_flickr_person_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_flickr_person_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockFlickrPerson *block_flickr_person = HIPPO_CANVAS_BLOCK_FLICKR_PERSON(canvas_block);

    hippo_canvas_box_set_child_visible(block_flickr_person->thumbnails_parent,
                                       block_flickr_person->thumbnails,
                                       FALSE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_flickr_person_parent_class)->unexpand(canvas_block);
}
