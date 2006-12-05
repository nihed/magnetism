/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-myspace-person.h"
#include "hippo-canvas-block-myspace-person.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_block_myspace_person_init                (HippoCanvasBlockMySpacePerson       *block);
static void      hippo_canvas_block_myspace_person_class_init          (HippoCanvasBlockMySpacePersonClass  *klass);
static void      hippo_canvas_block_myspace_person_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_myspace_person_dispose             (GObject                *object);
static void      hippo_canvas_block_myspace_person_finalize            (GObject                *object);

static void hippo_canvas_block_myspace_person_set_property (GObject      *object,
                                                         guint         prop_id,
                                                         const GValue *value,
                                                         GParamSpec   *pspec);
static void hippo_canvas_block_myspace_person_get_property (GObject      *object,
                                                         guint         prop_id,
                                                         GValue       *value,
                                                         GParamSpec   *pspec);
static GObject* hippo_canvas_block_myspace_person_constructor (GType                  type,
                                                            guint                  n_construct_properties,
                                                            GObjectConstructParam *construct_params);

/* Canvas block methods */
static void hippo_canvas_block_myspace_person_set_block       (HippoCanvasBlock *canvas_block,
                                                            HippoBlock       *block);

static void hippo_canvas_block_myspace_person_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_myspace_person_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_myspace_person_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockMySpacePerson *block_myspace_person,
                        HippoPerson                 *person);


struct _HippoCanvasBlockMySpacePerson {
    HippoCanvasBlock canvas_block;
    HippoCanvasItem *description_item;
    HippoPerson *person;
};

struct _HippoCanvasBlockMySpacePersonClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockMySpacePerson, hippo_canvas_block_myspace_person, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_myspace_person_iface_init));

static void
hippo_canvas_block_myspace_person_init(HippoCanvasBlockMySpacePerson *block_myspace_person)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_myspace_person);

    block->required_type = HIPPO_BLOCK_TYPE_MYSPACE_PERSON;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_myspace_person_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_myspace_person_class_init(HippoCanvasBlockMySpacePersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_myspace_person_set_property;
    object_class->get_property = hippo_canvas_block_myspace_person_get_property;
    object_class->constructor = hippo_canvas_block_myspace_person_constructor;

    object_class->dispose = hippo_canvas_block_myspace_person_dispose;
    object_class->finalize = hippo_canvas_block_myspace_person_finalize;

    canvas_block_class->set_block = hippo_canvas_block_myspace_person_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_myspace_person_title_activated;
    canvas_block_class->expand = hippo_canvas_block_myspace_person_expand;
    canvas_block_class->unexpand = hippo_canvas_block_myspace_person_unexpand;
}

static void
hippo_canvas_block_myspace_person_dispose(GObject *object)
{
    HippoCanvasBlockMySpacePerson *block_myspace_person;

    block_myspace_person = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(object);

    set_person(block_myspace_person, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_myspace_person_parent_class)->dispose(object);
}

static void
hippo_canvas_block_myspace_person_finalize(GObject *object)
{
    /* HippoCanvasBlockMySpacePerson *block = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_myspace_person_parent_class)->finalize(object);
}

static void
hippo_canvas_block_myspace_person_set_property(GObject         *object,
                                            guint            prop_id,
                                            const GValue    *value,
                                            GParamSpec      *pspec)
{
    HippoCanvasBlockMySpacePerson *block_myspace_person;

    block_myspace_person = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_myspace_person_get_property(GObject         *object,
                                            guint            prop_id,
                                            GValue          *value,
                                            GParamSpec      *pspec)
{
    HippoCanvasBlockMySpacePerson *block_myspace_person;

    block_myspace_person = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_myspace_person_constructor (GType                  type,
                                            guint                  n_construct_properties,
                                            GObjectConstructParam *construct_properties)
{
    GObject *object;
    HippoCanvasBlock *block;
    HippoCanvasBlockMySpacePerson *block_myspace;
    HippoCanvasBox *box;

    object = G_OBJECT_CLASS(hippo_canvas_block_myspace_person_parent_class)->constructor(type,
                                                                                      n_construct_properties,
                                                                                      construct_properties);
    block = HIPPO_CANVAS_BLOCK(object);
    block_myspace = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(object);

    hippo_canvas_block_set_heading(block, _("Blog Post"));

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       NULL);

    block_myspace->description_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                                "xalign", HIPPO_ALIGNMENT_START,
                                                "yalign", HIPPO_ALIGNMENT_START,
                                                "text", NULL,
                                                "border-top", 4,
                                                "border-bottom", 4,
                                                NULL);
    hippo_canvas_box_append(box, block_myspace->description_item, 0);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));

    return object;
}

static void
set_person(HippoCanvasBlockMySpacePerson *block_myspace_person,
           HippoPerson                 *person)
{
    if (person == block_myspace_person->person)
        return;

    if (block_myspace_person->person) {
        g_object_unref(block_myspace_person->person);
        block_myspace_person->person = NULL;
    }

    if (person) {
        block_myspace_person->person = person;
        g_object_ref(G_OBJECT(person));
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_myspace_person),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockMySpacePerson *block_myspace_person)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_myspace_person, person);
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_myspace_person_set_block(HippoCanvasBlock *canvas_block,
                                         HippoBlock       *block)
{
    HippoCanvasBlockMySpacePerson *block_myspace_person = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(canvas_block);

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(block_myspace_person, NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_myspace_person_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        HippoFeedEntry *entry;
        
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(canvas_block));

        entry = hippo_block_myspace_person_get_entry(HIPPO_BLOCK_MYSPACE_PERSON(canvas_block->block));
        if (entry != NULL) {
            hippo_canvas_block_set_title(canvas_block,
                                         hippo_feed_entry_get_title(entry),
                                         hippo_feed_entry_get_url(entry),
                                         FALSE);
            g_object_set(block_myspace_person->description_item,
                         "text", hippo_feed_entry_get_description(entry),
                         NULL);
        } else {
            /* Won't normally happen */
            hippo_canvas_block_set_title(canvas_block,
                                         "MySpace Blog",
                                         NULL,
                                         FALSE);
            g_object_set(block_myspace_person->description_item,
                         "text", NULL,
                         NULL);
        }
    }
}

static void
hippo_canvas_block_myspace_person_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoFeedEntry *entry;
    
    if (canvas_block->block == NULL)
        return;

    entry = hippo_block_myspace_person_get_entry(HIPPO_BLOCK_MYSPACE_PERSON(canvas_block->block));
    if (entry == NULL)
        return;
    
    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_feed_entry_get_url(entry));
}

static void
hippo_canvas_block_myspace_person_update_visibility(HippoCanvasBlockMySpacePerson *block_myspace_person)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_myspace_person);

    g_object_set(G_OBJECT(block_myspace_person->description_item),
                 "size-mode", canvas_block->expanded ? HIPPO_CANVAS_SIZE_WRAP_WORD : HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                 NULL);
}

static void
hippo_canvas_block_myspace_person_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockMySpacePerson *block_myspace_person = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(canvas_block);

    hippo_canvas_block_myspace_person_update_visibility(block_myspace_person);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_myspace_person_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_myspace_person_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockMySpacePerson *block_myspace_person = HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(canvas_block);

    hippo_canvas_block_myspace_person_update_visibility(block_myspace_person);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_myspace_person_parent_class)->unexpand(canvas_block);
}
