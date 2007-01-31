/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-netflix-movie.h"
#include "hippo-canvas-block-netflix-movie.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"
#include "hippo-canvas-url-image.h"

static void      hippo_canvas_block_netflix_movie_init                (HippoCanvasBlockNetflixMovie       *block);
static void      hippo_canvas_block_netflix_movie_class_init          (HippoCanvasBlockNetflixMovieClass  *klass);
static void      hippo_canvas_block_netflix_movie_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_netflix_movie_dispose             (GObject                *object);
static void      hippo_canvas_block_netflix_movie_finalize            (GObject                *object);

static void hippo_canvas_block_netflix_movie_set_property (GObject      *object,
                                                             guint         prop_id,
                                                             const GValue *value,
                                                             GParamSpec   *pspec);
static void hippo_canvas_block_netflix_movie_get_property (GObject      *object,
                                                             guint         prop_id,
                                                             GValue       *value,
                                                             GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_netflix_movie_append_content_items (HippoCanvasBlock *block,
                                                                     HippoCanvasBox   *parent_box);
static void hippo_canvas_block_netflix_movie_set_block       (HippoCanvasBlock *canvas_block,
                                                                HippoBlock       *block);

static void hippo_canvas_block_netflix_movie_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_netflix_movie_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_netflix_movie_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockNetflixMovie *block_netflix,
                        HippoPerson                 *person);


struct _HippoCanvasBlockNetflixMovie {
    HippoCanvasBlock canvas_block;
    HippoCanvasItem *thumbnail;
    HippoCanvasBox *title_link_parent;
    HippoCanvasItem *title_link;
    HippoPerson *person;
};

struct _HippoCanvasBlockNetflixMovieClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockNetflixMovie, hippo_canvas_block_netflix_movie, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_netflix_movie_iface_init));

static void
hippo_canvas_block_netflix_movie_init(HippoCanvasBlockNetflixMovie *block_netflix)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_netflix);

    block->required_type = HIPPO_BLOCK_TYPE_NETFLIX_MOVIE;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_netflix_movie_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_netflix_movie_class_init(HippoCanvasBlockNetflixMovieClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_netflix_movie_set_property;
    object_class->get_property = hippo_canvas_block_netflix_movie_get_property;

    object_class->dispose = hippo_canvas_block_netflix_movie_dispose;
    object_class->finalize = hippo_canvas_block_netflix_movie_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_netflix_movie_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_netflix_movie_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_netflix_movie_title_activated;
    canvas_block_class->expand = hippo_canvas_block_netflix_movie_expand;
    canvas_block_class->unexpand = hippo_canvas_block_netflix_movie_unexpand;
}

static void
hippo_canvas_block_netflix_movie_dispose(GObject *object)
{
    HippoCanvasBlockNetflixMovie *block;

    block = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(object);

    set_person(block, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_netflix_movie_parent_class)->dispose(object);
}

static void
hippo_canvas_block_netflix_movie_finalize(GObject *object)
{
    /* HippoCanvasBlockNetflixMovie *block = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(object); */

    G_OBJECT_CLASS(hippo_canvas_block_netflix_movie_parent_class)->finalize(object);
}

static void
hippo_canvas_block_netflix_movie_set_property(GObject         *object,
                                              guint            prop_id,
                                              const GValue    *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockNetflixMovie *block;

    block = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_netflix_movie_get_property(GObject         *object,
                                              guint            prop_id,
                                              GValue          *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockNetflixMovie *block;

    block = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_netflix_movie_append_content_items (HippoCanvasBlock *block,
                                                       HippoCanvasBox   *parent_box)
{
    HippoCanvasBox *box;
    HippoCanvasBox *top_box;
    HippoCanvasBox *beside_box; 
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(block);

    hippo_canvas_block_set_heading(block, _("Netflix movie"));
    
    top_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                           "spacing", 4,
                           "border-bottom", 3,
                           NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(top_box), 0);

    block_netflix->thumbnail = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          NULL);
    hippo_canvas_box_append(top_box, block_netflix->thumbnail, 0);

    beside_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_VERTICAL,
                              NULL);
    hippo_canvas_box_append(top_box, HIPPO_CANVAS_ITEM(beside_box), 0);

    /* An extra box to keep the title link from expanding beyond its text */
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0);

    block_netflix->title_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                                          "actions", hippo_canvas_block_get_actions(block),
                                          "xalign", HIPPO_ALIGNMENT_START,
                                          "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                          NULL);
    block_netflix->title_link_parent = box;
    hippo_canvas_box_append(box, block_netflix->title_link, 0);
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0);  
}

static void
set_person(HippoCanvasBlockNetflixMovie *block_netflix,
           HippoPerson                 *person)
{
    if (person == block_netflix->person)
        return;

    if (block_netflix->person) {
        g_object_unref(block_netflix->person);
        block_netflix->person = NULL;
    }

    if (person) {
        block_netflix->person = person;
        g_object_ref(G_OBJECT(person));
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_netflix),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockNetflixMovie *block_netflix)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_netflix, person);
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_netflix_movie_set_block(HippoCanvasBlock *canvas_block,
                                           HippoBlock       *block)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block);

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
                const char *thumbnail_url;
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block));
                        
        g_object_set(block_netflix->title_link,
                     "text", hippo_block_get_title(canvas_block->block),
                     "font", "12px",
                     "tooltip", "More information about this movie",
                     "url", hippo_block_get_title_link(canvas_block->block),
                     NULL);                        
     
        thumbnail_url = hippo_block_netflix_movie_get_image_url(HIPPO_BLOCK_NETFLIX_MOVIE(canvas_block->block));
        g_object_set(block_netflix->thumbnail,
                     "image-name", "noart",
                     "tooltip", "More information about this movie",
                     "url", hippo_block_get_title_link(canvas_block->block),
                     "box-width", 65,
                     "box-height", 90,
                     NULL);
        hippo_actions_load_thumbnail_async(hippo_canvas_block_get_actions(canvas_block),
                                           thumbnail_url,
                                           block_netflix->thumbnail);                
    }
}

static void
hippo_canvas_block_netflix_movie_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;

    if (canvas_block->block == NULL)
        return;
        
    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_block_get_title_link(canvas_block->block));
}

static void
hippo_canvas_block_netflix_movie_expand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block); */

    /*
    hippo_canvas_box_set_child_visible(block_netflix->thumbnails_parent,
                                       block_netflix->thumbnails,
                                       TRUE);
    */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_netflix_movie_unexpand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block); */

    /*
    hippo_canvas_box_set_child_visible(block_netflix->thumbnails_parent,
                                       block_netflix->thumbnails,
                                       FALSE);
    */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->unexpand(canvas_block);
}
