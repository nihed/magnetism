/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-block-facebook-event.h"
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
/* Canvas block methods */
static void hippo_canvas_block_facebook_event_append_content_items (HippoCanvasBlock *block,
                                                                    HippoCanvasBox   *parent_box);
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
    HippoCanvasItem *thumbnails;
    HippoPerson *person;

    gboolean have_thumbnails;
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

    block->required_type = HIPPO_BLOCK_TYPE_GENERIC;
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

    object_class->dispose = hippo_canvas_block_facebook_event_dispose;
    object_class->finalize = hippo_canvas_block_facebook_event_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_facebook_event_append_content_items;
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

static void
on_has_thumbnails_changed(HippoCanvasItem               *thumbnails_item,
                          GParamSpec                    *pspec,
                          HippoCanvasBlockFacebookEvent *block_facebook_event)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_facebook_event);
    gboolean has_thumbnails;

    g_object_get(thumbnails_item, "has-thumbnails", &has_thumbnails, NULL);

    if (has_thumbnails != block_facebook_event->have_thumbnails) {
        block_facebook_event->have_thumbnails = has_thumbnails;
        canvas_block->expandable = has_thumbnails;

        if (!has_thumbnails)
            hippo_canvas_block_set_expanded(canvas_block, FALSE);
    }
}

static void
hippo_canvas_block_facebook_event_append_content_items (HippoCanvasBlock *block,
                                                        HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(block);

    hippo_canvas_block_set_heading(block, _("Facebook event"));

    block_facebook_event->thumbnails =
        g_object_new(HIPPO_TYPE_CANVAS_THUMBNAILS,
                     "actions", block->actions,
                     NULL);

    g_signal_connect(block_facebook_event->thumbnails, "notify::has-thumbnails",
                     G_CALLBACK(on_has_thumbnails_changed), block_facebook_event);
    
    hippo_canvas_box_append(parent_box,
                            block_facebook_event->thumbnails, 0);

    hippo_canvas_item_set_visible(block_facebook_event->thumbnails,
                                  FALSE);
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
                                  person ? HIPPO_ENTITY(person) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockFacebookEvent *block_facebook_event)
{
    HippoPerson *person = NULL;

    if (block)
        g_object_get(G_OBJECT(block), "user", &person, NULL);
    
    set_person(block_facebook_event, person);
    
    if (person)
        g_object_unref(person);
}

static void 
on_title_changed(HippoBlock *block,
                 GParamSpec *arg, /* null when first calling this */
                 HippoCanvasBlockFacebookEvent *block_facebook_event)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_facebook_event);

    const char *title = NULL;
    const char *title_link = NULL;

    if (block != NULL) {
        title = hippo_block_get_title(block);
        title_link = hippo_block_get_title_link(block);
    }
    
    hippo_canvas_block_set_title(canvas_block, title, title_link, FALSE);
}

static void
hippo_canvas_block_facebook_event_set_block(HippoCanvasBlock *canvas_block,
                                            HippoBlock       *block)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block);
    
    /* g_debug("canvas-block-facebook-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(canvas_block->block,
                                             (gpointer)on_user_changed,
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block), NULL);

        g_signal_handlers_disconnect_by_func(canvas_block->block,
                                             (gpointer)on_title_changed,
                                             canvas_block);

        
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_facebook_event_parent_class)->set_block(canvas_block, block);

    g_object_set(block_facebook_event->thumbnails,
                 "resource", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);
    
    if (canvas_block->block != NULL) {
        g_signal_connect(canvas_block->block,
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);
        g_signal_connect(canvas_block->block,
                         "notify::title",
                         G_CALLBACK(on_title_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title-link",
                         G_CALLBACK(on_title_changed),
                         canvas_block);
    }
    
    on_user_changed(canvas_block->block, NULL, block_facebook_event);
    on_title_changed(canvas_block->block, NULL, block_facebook_event);
}

static void
hippo_canvas_block_facebook_event_title_activated(HippoCanvasBlock *canvas_block)
{
    const char *link;

    if (canvas_block->block == NULL)
        return;
    
    link = hippo_block_get_title_link(canvas_block->block);
    if (link != NULL) {
        HippoActions *actions = hippo_canvas_block_get_actions(canvas_block);

        hippo_actions_open_url(actions, link);
    }
}

static void
hippo_canvas_block_facebook_event_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block);

    hippo_canvas_item_set_visible(block_facebook_event->thumbnails,
                                  TRUE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_facebook_event_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_facebook_event_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockFacebookEvent *block_facebook_event = HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(canvas_block);

    hippo_canvas_item_set_visible(block_facebook_event->thumbnails,
                                  FALSE);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_facebook_event_parent_class)->unexpand(canvas_block);
}
