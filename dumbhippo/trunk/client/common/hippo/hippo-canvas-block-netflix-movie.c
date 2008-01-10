/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-netflix-movie.h"
#include "hippo-canvas-block-netflix-movie.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-last-message-preview.h"
#include "hippo-canvas-quipper.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"
#include "hippo-canvas-url-image.h"

#define HIPPO_CANVAS_BLOCK_NETFLIX_COVER_ART_WIDTH 65
#define HIPPO_CANVAS_BLOCK_NETFLIX_COVER_ART_HEIGHT 90

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

static void hippo_canvas_block_netflix_movie_stack_reason_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_netflix_movie_expand               (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_netflix_movie_unexpand             (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockNetflixMovie *block_netflix,
                        HippoPerson                 *person);


struct _HippoCanvasBlockNetflixMovie {
    HippoCanvasBlock canvas_block;
    HippoCanvasItem *thumbnail;
    HippoCanvasItem *favicon;
    HippoCanvasItem *title_link;
    HippoCanvasItem *description_item;
    HippoCanvasItem *quipper;
    HippoCanvasItem *last_message_preview;
    HippoCanvasItem *chat_preview;
    HippoCanvasBox *queue_box;
    HippoCanvasBox *queue_list_box;    
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
    block->skip_heading = TRUE;    
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
    canvas_block_class->stack_reason_changed = hippo_canvas_block_netflix_movie_stack_reason_changed;
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
update_visibility(HippoCanvasBlockNetflixMovie *block_netflix)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_netflix);
    HippoStackReason stack_reason;
    gboolean have_chat_id;
    
    hippo_canvas_item_set_visible(HIPPO_CANVAS_ITEM(block_netflix->queue_box), canvas_block->expanded);

    if (canvas_block->block)
        stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    else
        stack_reason = HIPPO_STACK_BLOCK_UPDATE;

    have_chat_id = FALSE;
    if (canvas_block->block)
        have_chat_id = hippo_block_get_chat_id(canvas_block->block) != NULL;

    hippo_canvas_item_set_visible(block_netflix->quipper,
                                  canvas_block->expanded && have_chat_id);
    hippo_canvas_item_set_visible(block_netflix->last_message_preview,
                                  !canvas_block->expanded && stack_reason == HIPPO_STACK_CHAT_MESSAGE);
    hippo_canvas_item_set_visible(block_netflix->chat_preview,
                                  canvas_block->expanded && have_chat_id);
}

static void
hippo_canvas_block_netflix_movie_append_content_items (HippoCanvasBlock *block,
                                                       HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(block);    
    HippoCanvasBox *box;
    HippoCanvasBox *top_box;
    HippoCanvasBox *beside_box;
    HippoCanvasItem *item;
    
    top_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                           "spacing", 4,
                           "border-bottom", 3,                          
                           NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(top_box), 0);

    block_netflix->thumbnail = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE,
                                            "image-name", "netflix_no_image",
                                            "tooltip", "More information about this movie",
                                            "actions", hippo_canvas_block_get_actions(block),
                                            "xalign", HIPPO_ALIGNMENT_START,
                                            "yalign", HIPPO_ALIGNMENT_START,
                                            "box-width", HIPPO_CANVAS_BLOCK_NETFLIX_COVER_ART_WIDTH,
                                            "box-height", HIPPO_CANVAS_BLOCK_NETFLIX_COVER_ART_HEIGHT,
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
    
    block_netflix->favicon = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                                          "xalign", HIPPO_ALIGNMENT_CENTER,
                                          "yalign", HIPPO_ALIGNMENT_CENTER,
                                          "scale-width", 16, /* favicon size */
                                          "scale-height", 16,
                                          "border-right", 6,
                                          NULL);
    hippo_canvas_box_append(box, block_netflix->favicon, 0);

    block_netflix->title_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                                             "actions", hippo_canvas_block_get_actions(block),
                                             "xalign", HIPPO_ALIGNMENT_START,
                                             "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                             "font", "Bold 12px",
                                             "tooltip", "More information about this movie",
                                             NULL);
    hippo_canvas_box_append(box, block_netflix->title_link, 0);
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0);  
    
    block_netflix->description_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                   "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                                                   "xalign", HIPPO_ALIGNMENT_START,
                                                   "yalign", HIPPO_ALIGNMENT_START,
                                                   "text", NULL,
                                                   "border-top", 4,
                                                   "border-bottom", 4,
                                                   NULL);
    hippo_canvas_box_append(beside_box, block_netflix->description_item, 0);
    
    block_netflix->last_message_preview = g_object_new(HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW,
                                                       "actions", hippo_canvas_block_get_actions(block),
                                                       NULL);
    hippo_canvas_box_append(parent_box, block_netflix->last_message_preview, 0);
    hippo_canvas_item_set_visible(block_netflix->last_message_preview,
                                  FALSE); /* no messages yet */

    block_netflix->quipper = g_object_new(HIPPO_TYPE_CANVAS_QUIPPER,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          NULL);
    hippo_canvas_box_append(parent_box, block_netflix->quipper, 0);
    hippo_canvas_item_set_visible(block_netflix->quipper,
                                  FALSE); /* not expanded */

    block_netflix->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                               "actions", hippo_canvas_block_get_actions(block),
                                               "padding-bottom", 8,
                                               NULL);
    hippo_canvas_box_append(parent_box,
                            block_netflix->chat_preview, 0);
    hippo_canvas_item_set_visible(block_netflix->chat_preview,
                                  FALSE); /* not expanded at first */
    
    block_netflix->queue_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                            "orientation", HIPPO_ORIENTATION_VERTICAL,
                                            NULL);
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", _("Movies in the Queue:"),
                        "xalign", HIPPO_ALIGNMENT_START,                        
                        NULL);                  
    hippo_canvas_box_append(block_netflix->queue_box, item, 0);
    block_netflix->queue_list_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                                 "orientation", HIPPO_ORIENTATION_VERTICAL,
                                                 NULL);
    hippo_canvas_box_append(block_netflix->queue_box, HIPPO_CANVAS_ITEM(block_netflix->queue_list_box), 0);
    
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(block_netflix->queue_box), 0);
    
    update_visibility(block_netflix);
}

static void
set_person(HippoCanvasBlockNetflixMovie *block_netflix,
           HippoPerson                  *person)
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
                                  person ? HIPPO_ENTITY(person) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockNetflixMovie *block_netflix)
{
    HippoPerson *person;
    person = NULL;

    if (block)
        g_object_get(G_OBJECT(block), "user", &person, NULL);
    
    set_person(block_netflix, person);
    if (person)
        g_object_unref(person);
}

static void
on_block_description_changed(HippoBlock *block,
                             GParamSpec *arg, /* null when first calling this */
                             void       *data)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    char *description = NULL;

    if (block)
        g_object_get(G_OBJECT(block), "description", &description, NULL);

    g_object_set(G_OBJECT(block_netflix->description_item),
                 "text", description,
                 NULL);

    g_free(description);
}

static void
on_block_chat_id_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         void       *data)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    
    update_visibility(block_netflix);
}

static void
on_block_image_url_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when first calling this */
                           void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    const char *thumbnail_url = NULL;

    if (block)
        thumbnail_url = hippo_block_netflix_movie_get_image_url(HIPPO_BLOCK_NETFLIX_MOVIE(block));
    
    g_object_set(block_netflix->thumbnail,
                 "image-name", "netflix_no_image",
                 NULL);
    
    if (thumbnail_url)
        hippo_actions_load_thumbnail_async(hippo_canvas_block_get_actions(canvas_block),
                                           thumbnail_url,
                                           block_netflix->thumbnail);     
}

static void
on_queued_movies_changed(DDMDataResource *block_resource,
                         GSList          *changed_properties,
                         void            *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    GSList *queued_movies = NULL;
    GSList *l;

    if (block_resource)
        ddm_data_resource_get(block_resource,
                              "queuedMovies", DDM_DATA_RESOURCE | DDM_DATA_LIST, &queued_movies,
                              NULL);
        
    hippo_canvas_box_remove_all(block_netflix->queue_list_box);
    
    for (l = queued_movies; l; l = l->next) {
        DDMDataResource *movie_resource = l->data;
        HippoCanvasBox *box;
        HippoCanvasItem *number, *title;
        char *priority_str;
        const char *movie_title;
        const char *movie_link;
        int movie_priority;

        ddm_data_resource_get(movie_resource,
                              "link", DDM_DATA_URL, &movie_link,
                              "title", DDM_DATA_STRING, &movie_title,
                              "priority", DDM_DATA_INTEGER, &movie_priority,
                              NULL);
        
        box = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL, NULL);
        
        priority_str = g_strdup_printf("%d", movie_priority);
        number = g_object_new(HIPPO_TYPE_CANVAS_TEXT, "text", priority_str, NULL);
        g_free(priority_str);
        hippo_canvas_box_append(box, number, 0);
        
        title = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                             "actions", hippo_canvas_block_get_actions(canvas_block),
                             "xalign", HIPPO_ALIGNMENT_START,
                             "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                             "text", movie_title,
                             "url", movie_link,
                             "underline", FALSE,
                             "padding-left", 4,
                             NULL);
        
        hippo_canvas_box_append(box, title, 0);
        
        hippo_canvas_box_append(block_netflix->queue_list_box, HIPPO_CANVAS_ITEM(box), 0);            
    }
}

static void
on_block_title_changed(HippoBlock *block,
                       GParamSpec *arg, /* null when first calling this */
                       void       *data)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    const char *title = NULL;

    if (block)
        title = hippo_block_get_title(block);
            
    g_object_set(block_netflix->title_link,
                 "text", title,
                 NULL);                        
    g_object_set(G_OBJECT(block_netflix->quipper),
                 "title", title,
                 NULL);
}

static void
on_block_title_link_changed(HippoBlock *block,
                            GParamSpec *arg, /* null when first calling this */
                            void       *data)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    const char *title_link = NULL;

    if (block)
        title_link = hippo_block_get_title_link(block);
            
    g_object_set(block_netflix->title_link,
                 "url", title_link,
                 NULL);                        
    g_object_set(block_netflix->thumbnail,
                 "url", title_link,
                 NULL);
}

static void
on_block_icon_url_changed(HippoBlock *block,
                          GParamSpec *arg, /* null when first calling this */
                          void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(data);
    const char *icon_url = NULL;

    if (block)
        icon_url = hippo_block_get_icon_url(block);
    
    if (icon_url)
        hippo_actions_load_favicon_async(canvas_block->actions,
                                         hippo_block_get_icon_url(canvas_block->block),
                                         block_netflix->favicon);
    else
        g_object_set(block_netflix->favicon,
                     "image", NULL,
                     NULL);
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
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_description_changed),
                                             canvas_block);                                             
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_chat_id_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_image_url_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_title_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_title_link_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_icon_url_changed),
                                             canvas_block);
        
        ddm_data_resource_disconnect(hippo_block_get_resource(canvas_block->block),
                                     on_queued_movies_changed, canvas_block);
        
        set_person(HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->set_block(canvas_block, block);

    g_object_set(block_netflix->quipper,
                 "block", canvas_block->block,
                 NULL);
    g_object_set(block_netflix->last_message_preview,
                 "block", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);
    g_object_set(block_netflix->chat_preview,
                 "block", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);
    
    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::description",
                         G_CALLBACK(on_block_description_changed),
                         canvas_block);                         
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::chat-id",
                         G_CALLBACK(on_block_chat_id_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::image-url",
                         G_CALLBACK(on_block_image_url_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title",
                         G_CALLBACK(on_block_title_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title-link",
                         G_CALLBACK(on_block_title_link_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::icon-url",
                         G_CALLBACK(on_block_icon_url_changed),
                         canvas_block);
        
        ddm_data_resource_connect(hippo_block_get_resource(canvas_block->block),
                                  "queuedMovies",
                                  on_queued_movies_changed, canvas_block);
    }

    on_user_changed(canvas_block->block, NULL, block_netflix);
    on_block_description_changed(canvas_block->block, NULL, block_netflix);
    on_block_chat_id_changed(canvas_block->block, NULL, block_netflix);
    on_block_image_url_changed(canvas_block->block, NULL, block_netflix);
    on_block_title_changed(canvas_block->block, NULL, block_netflix);
    on_block_title_link_changed(canvas_block->block, NULL, block_netflix);
    on_block_icon_url_changed(canvas_block->block, NULL, block_netflix);
    on_queued_movies_changed(canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL, NULL, block_netflix);
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
hippo_canvas_block_netflix_movie_stack_reason_changed(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block);

    if (HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->stack_reason_changed)
        HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->stack_reason_changed(canvas_block);
    
    update_visibility(block_netflix);
}

static void
hippo_canvas_block_netflix_movie_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->expand(canvas_block);
    
    update_visibility(block_netflix);
}

static void
hippo_canvas_block_netflix_movie_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockNetflixMovie *block_netflix = HIPPO_CANVAS_BLOCK_NETFLIX_MOVIE(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_netflix_movie_parent_class)->unexpand(canvas_block);
    
    update_visibility(block_netflix);    
}
