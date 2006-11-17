/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-blog-person.h"
#include "hippo-canvas-block-blog-person.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_block_blog_person_init                (HippoCanvasBlockBlogPerson       *block);
static void      hippo_canvas_block_blog_person_class_init          (HippoCanvasBlockBlogPersonClass  *klass);
static void      hippo_canvas_block_blog_person_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_blog_person_dispose             (GObject                *object);
static void      hippo_canvas_block_blog_person_finalize            (GObject                *object);

static void hippo_canvas_block_blog_person_set_property (GObject      *object,
                                                         guint         prop_id,
                                                         const GValue *value,
                                                         GParamSpec   *pspec);
static void hippo_canvas_block_blog_person_get_property (GObject      *object,
                                                         guint         prop_id,
                                                         GValue       *value,
                                                         GParamSpec   *pspec);
static GObject* hippo_canvas_block_blog_person_constructor (GType                  type,
                                                            guint                  n_construct_properties,
                                                            GObjectConstructParam *construct_params);

/* Canvas block methods */
static void hippo_canvas_block_blog_person_set_block       (HippoCanvasBlock *canvas_block,
                                                            HippoBlock       *block);

static void hippo_canvas_block_blog_person_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_blog_person_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_blog_person_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockBlogPerson *block_blog_person,
                        HippoPerson                 *person);


struct _HippoCanvasBlockBlogPerson {
    HippoCanvasBlock canvas_block;
    HippoPerson *person;
};

struct _HippoCanvasBlockBlogPersonClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockBlogPerson, hippo_canvas_block_blog_person, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_blog_person_iface_init));

static void
hippo_canvas_block_blog_person_init(HippoCanvasBlockBlogPerson *block_blog_person)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_blog_person);

    block->required_type = HIPPO_BLOCK_TYPE_BLOG_PERSON;
    block->expandable = FALSE; /* currently we have nothing to show on expand */
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_blog_person_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_blog_person_class_init(HippoCanvasBlockBlogPersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_blog_person_set_property;
    object_class->get_property = hippo_canvas_block_blog_person_get_property;
    object_class->constructor = hippo_canvas_block_blog_person_constructor;

    object_class->dispose = hippo_canvas_block_blog_person_dispose;
    object_class->finalize = hippo_canvas_block_blog_person_finalize;

    canvas_block_class->set_block = hippo_canvas_block_blog_person_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_blog_person_title_activated;
    canvas_block_class->expand = hippo_canvas_block_blog_person_expand;
    canvas_block_class->unexpand = hippo_canvas_block_blog_person_unexpand;
}

static void
hippo_canvas_block_blog_person_dispose(GObject *object)
{
    HippoCanvasBlockBlogPerson *block_blog_person;

    block_blog_person = HIPPO_CANVAS_BLOCK_BLOG_PERSON(object);

    set_person(block_blog_person, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_blog_person_parent_class)->dispose(object);
}

static void
hippo_canvas_block_blog_person_finalize(GObject *object)
{
    /* HippoCanvasBlockBlogPerson *block = HIPPO_CANVAS_BLOCK_BLOG_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_blog_person_parent_class)->finalize(object);
}

static void
hippo_canvas_block_blog_person_set_property(GObject         *object,
                                            guint            prop_id,
                                            const GValue    *value,
                                            GParamSpec      *pspec)
{
    HippoCanvasBlockBlogPerson *block_blog_person;

    block_blog_person = HIPPO_CANVAS_BLOCK_BLOG_PERSON(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_blog_person_get_property(GObject         *object,
                                            guint            prop_id,
                                            GValue          *value,
                                            GParamSpec      *pspec)
{
    HippoCanvasBlockBlogPerson *block_blog_person;

    block_blog_person = HIPPO_CANVAS_BLOCK_BLOG_PERSON (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_blog_person_constructor (GType                  type,
                                            guint                  n_construct_properties,
                                            GObjectConstructParam *construct_properties)
{
    GObject *object;
    HippoCanvasBlock *block;
    HippoCanvasBlockBlogPerson *block_blog;
    HippoCanvasBox *box;

    object = G_OBJECT_CLASS(hippo_canvas_block_blog_person_parent_class)->constructor(type,
                                                                                      n_construct_properties,
                                                                                      construct_properties);
    block = HIPPO_CANVAS_BLOCK(object);
    block_blog = HIPPO_CANVAS_BLOCK_BLOG_PERSON(object);

    hippo_canvas_block_set_heading(block, _("Blog Post"));

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 4,
                       NULL);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));

    return object;
}

static void
set_person(HippoCanvasBlockBlogPerson *block_blog_person,
           HippoPerson                 *person)
{
    if (person == block_blog_person->person)
        return;

    if (block_blog_person->person) {
        g_object_unref(block_blog_person->person);
        block_blog_person->person = NULL;
    }

    if (person) {
        block_blog_person->person = person;
        g_object_ref(G_OBJECT(person));
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_blog_person),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockBlogPerson *block_blog_person)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_blog_person, person);
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_blog_person_set_block(HippoCanvasBlock *canvas_block,
                                         HippoBlock       *block)
{
    /* g_debug("canvas-block-blog-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_BLOG_PERSON(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_blog_person_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        HippoFeedEntry *entry;
        
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_BLOG_PERSON(canvas_block));

        entry = hippo_block_blog_person_get_entry(HIPPO_BLOCK_BLOG_PERSON(canvas_block->block));
        if (entry != NULL) {
            hippo_canvas_block_set_title(canvas_block,
                                         hippo_feed_entry_get_title(entry),
                                         hippo_feed_entry_get_url(entry),
                                         FALSE);
        }
    }
}

static void
hippo_canvas_block_blog_person_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoFeedEntry *entry;
    
    if (canvas_block->block == NULL)
        return;

    entry = hippo_block_blog_person_get_entry(HIPPO_BLOCK_BLOG_PERSON(canvas_block->block));
    if (entry == NULL)
        return;
    
    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_feed_entry_get_url(entry));
}

static void
hippo_canvas_block_blog_person_expand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockBlogPerson *block_blog_person = HIPPO_CANVAS_BLOCK_BLOG_PERSON(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_blog_person_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_blog_person_unexpand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockBlogPerson *block_blog_person = HIPPO_CANVAS_BLOCK_BLOG_PERSON(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_blog_person_parent_class)->unexpand(canvas_block);
}
