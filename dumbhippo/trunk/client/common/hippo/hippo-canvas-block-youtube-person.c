/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-youtube-person.h"
#include "hippo-canvas-block-youtube-person.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>
#include "hippo-canvas-url-link.h"
#include "hippo-canvas-thumbnails.h"

static void      hippo_canvas_block_youtube_person_init                (HippoCanvasBlockYouTubePerson       *block);
static void      hippo_canvas_block_youtube_person_class_init          (HippoCanvasBlockYouTubePersonClass  *klass);
static void      hippo_canvas_block_youtube_person_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_youtube_person_dispose             (GObject                *object);
static void      hippo_canvas_block_youtube_person_finalize            (GObject                *object);

static void hippo_canvas_block_youtube_person_set_property (GObject      *object,
                                                           guint         prop_id,
                                                           const GValue *value,
                                                           GParamSpec   *pspec);
static void hippo_canvas_block_youtube_person_get_property (GObject      *object,
                                                           guint         prop_id,
                                                           GValue       *value,
                                                           GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_youtube_person_append_content_items (HippoCanvasBlock *block,
                                                                    HippoCanvasBox   *parent_box);
static void hippo_canvas_block_youtube_person_set_block       (HippoCanvasBlock *canvas_block,
                                                              HippoBlock       *block);

static void hippo_canvas_block_youtube_person_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_youtube_person_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_youtube_person_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockYouTubePerson *block_youtube_person,
                        HippoPerson                 *person);


struct _HippoCanvasBlockYouTubePerson {
    HippoCanvasBlock canvas_block;
    HippoCanvasBox *thumbnails_parent;
    HippoCanvasItem *tip;
    HippoCanvasItem *thumbnails;
    HippoPerson *person;
};

struct _HippoCanvasBlockYouTubePersonClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockYouTubePerson, hippo_canvas_block_youtube_person, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_youtube_person_iface_init));

static void
hippo_canvas_block_youtube_person_init(HippoCanvasBlockYouTubePerson *block_youtube_person)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_youtube_person);

    block->required_type = HIPPO_BLOCK_TYPE_YOUTUBE_PERSON;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_youtube_person_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_youtube_person_class_init(HippoCanvasBlockYouTubePersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_youtube_person_set_property;
    object_class->get_property = hippo_canvas_block_youtube_person_get_property;

    object_class->dispose = hippo_canvas_block_youtube_person_dispose;
    object_class->finalize = hippo_canvas_block_youtube_person_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_youtube_person_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_youtube_person_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_youtube_person_title_activated;
    canvas_block_class->expand = hippo_canvas_block_youtube_person_expand;
    canvas_block_class->unexpand = hippo_canvas_block_youtube_person_unexpand;
}

static void
hippo_canvas_block_youtube_person_dispose(GObject *object)
{
    HippoCanvasBlockYouTubePerson *block_youtube_person;

    block_youtube_person = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(object);

    set_person(block_youtube_person, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_youtube_person_parent_class)->dispose(object);
}

static void
hippo_canvas_block_youtube_person_finalize(GObject *object)
{
    /* HippoCanvasBlockYouTubePerson *block = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_youtube_person_parent_class)->finalize(object);
}

static void
hippo_canvas_block_youtube_person_set_property(GObject         *object,
                                              guint            prop_id,
                                              const GValue    *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockYouTubePerson *block_youtube_person;

    block_youtube_person = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_youtube_person_get_property(GObject         *object,
                                              guint            prop_id,
                                              GValue          *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockYouTubePerson *block_youtube_person;

    block_youtube_person = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_youtube_person_append_content_items (HippoCanvasBlock *block,
                                                        HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockYouTubePerson *block_youtube_person = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(block);

    hippo_canvas_block_set_heading(block, _("New YouTube videos"));

    block_youtube_person->thumbnails_parent = parent_box;

    block_youtube_person->thumbnails =
        g_object_new(HIPPO_TYPE_CANVAS_THUMBNAILS,
                     "actions", block->actions,
                     NULL);

    block_youtube_person->tip = 
        g_object_new(HIPPO_TYPE_CANVAS_LINK,
                     "text", _("View thumbnails"),
                     "xalign", HIPPO_ALIGNMENT_START,
                     NULL);
    HIPPO_CANVAS_BOX(block_youtube_person->tip)->clickable = FALSE;

    hippo_canvas_box_append(parent_box,
                            block_youtube_person->tip, HIPPO_PACK_EXPAND);
    hippo_canvas_box_append(parent_box,
                            block_youtube_person->thumbnails, HIPPO_PACK_EXPAND);

    hippo_canvas_box_set_child_visible(parent_box,
                                       block_youtube_person->thumbnails,
                                       FALSE);
}

static void
set_person(HippoCanvasBlockYouTubePerson *block_youtube_person,
           HippoPerson                 *person)
{
    if (person == block_youtube_person->person)
        return;

    if (block_youtube_person->person) {
        g_object_unref(block_youtube_person->person);
        block_youtube_person->person = NULL;
    }

    if (person) {
        block_youtube_person->person = person;
        g_object_ref(G_OBJECT(person));
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_youtube_person),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockYouTubePerson *block_youtube_person)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_youtube_person, person);
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_youtube_person_set_block(HippoCanvasBlock *canvas_block,
                                           HippoBlock       *block)
{
    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_youtube_person_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        HippoThumbnails *thumbnails;

        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(canvas_block));

        hippo_canvas_block_set_title(canvas_block,
                                     hippo_block_get_title(canvas_block->block),
                                     hippo_block_get_title_link(canvas_block->block),
                                     FALSE);

        thumbnails = hippo_block_youtube_person_get_thumbnails(HIPPO_BLOCK_YOUTUBE_PERSON(canvas_block->block));
        if (thumbnails != NULL) {
            g_object_set(HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(canvas_block)->thumbnails,
                         "thumbnails", thumbnails,
                         NULL);
        }
    }
}

static void
hippo_canvas_block_youtube_person_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoThumbnails *thumbnails;

    if (canvas_block->block == NULL)
        return;

    thumbnails = hippo_block_youtube_person_get_thumbnails(HIPPO_BLOCK_YOUTUBE_PERSON(canvas_block->block));
    if (thumbnails == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_thumbnails_get_more_link(thumbnails));
}

static void
hippo_canvas_block_youtube_person_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockYouTubePerson *block_youtube_person = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(canvas_block);

    hippo_canvas_box_set_child_visible(block_youtube_person->thumbnails_parent,
                                       block_youtube_person->thumbnails,
                                       TRUE);
    hippo_canvas_box_set_child_visible(block_youtube_person->thumbnails_parent,
                                       block_youtube_person->tip,
                                       FALSE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_youtube_person_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_youtube_person_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockYouTubePerson *block_youtube_person = HIPPO_CANVAS_BLOCK_YOUTUBE_PERSON(canvas_block);

    hippo_canvas_box_set_child_visible(block_youtube_person->thumbnails_parent,
                                       block_youtube_person->thumbnails,
                                       FALSE);
    hippo_canvas_box_set_child_visible(block_youtube_person->thumbnails_parent,
                                       block_youtube_person->tip,
                                       TRUE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_youtube_person_parent_class)->unexpand(canvas_block);
}
