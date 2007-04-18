/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-netflix-movie.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-netflix-movie.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_netflix_movie_init                (HippoBlockNetflixMovie       *block);
static void      hippo_block_netflix_movie_class_init          (HippoBlockNetflixMovieClass  *klass);

static void      hippo_block_netflix_movie_dispose             (GObject              *object);
static void      hippo_block_netflix_movie_finalize            (GObject              *object);

static gboolean  hippo_block_netflix_movie_update_from_xml     (HippoBlock           *block,
                                                                HippoDataCache       *cache,
                                                                LmMessageNode        *node);

static void hippo_block_netflix_movie_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_block_netflix_movie_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);

static void set_queue(HippoBlockNetflixMovie *block_netflix,
                      GSList                 *queue);

struct _HippoBlockNetflixMovie {
    HippoBlockAbstractPerson      parent;
    char *image_url;
    char *description;
    GSList *queue;
};

struct _HippoBlockNetflixMovieClass {
    HippoBlockAbstractPersonClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_DESCRIPTION,
    PROP_IMAGE_URL,
    PROP_QUEUE
};

G_DEFINE_TYPE(HippoBlockNetflixMovie, hippo_block_netflix_movie, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_netflix_movie_init(HippoBlockNetflixMovie *block_netflix)
{
}

static void
hippo_block_netflix_movie_class_init(HippoBlockNetflixMovieClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_netflix_movie_set_property;
    object_class->get_property = hippo_block_netflix_movie_get_property;

    object_class->dispose = hippo_block_netflix_movie_dispose;
    object_class->finalize = hippo_block_netflix_movie_finalize;

    block_class->update_from_xml = hippo_block_netflix_movie_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Description of the movie, may be NULL"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_IMAGE_URL,
                                    g_param_spec_string("image-url",
                                                        _("Image URL"),
                                                        _("URL to an image of the movie"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_QUEUE,
                                    g_param_spec_pointer("queue",
                                                         _("Queue"),
                                                         _("Upcoming movie in the user's queue"),
                                                         G_PARAM_READABLE));
}

static void
hippo_block_netflix_movie_dispose(GObject *object)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object);

    set_queue(block_netflix, NULL);
    
    G_OBJECT_CLASS(hippo_block_netflix_movie_parent_class)->dispose(object);
}

static void
hippo_block_netflix_movie_finalize(GObject *object)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object);

    g_free(block_netflix->image_url);
    g_free(block_netflix->description);

    G_OBJECT_CLASS(hippo_block_netflix_movie_parent_class)->finalize(object);
}

static void
hippo_block_netflix_movie_set_property(GObject         *object,
                                         guint            prop_id,
                                         const GValue    *value,
                                         GParamSpec      *pspec)
{
    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_netflix_movie_get_property(GObject         *object,
                                         guint            prop_id,
                                         GValue          *value,
                                         GParamSpec      *pspec)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object);

    switch (prop_id) {
    case PROP_DESCRIPTION:
        g_value_set_string(value, block_netflix->description);
        break;
    case PROP_IMAGE_URL:
        g_value_set_string(value, block_netflix->image_url);
        break;
    case PROP_QUEUE:
        g_value_set_pointer(value, block_netflix->queue);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void 
set_description(HippoBlockNetflixMovie *block_netflix,
                const char             *description)
{
    if (block_netflix->description == description ||
        (block_netflix->description && description && strcmp(block_netflix->description, description) == 0))
        return;

    g_free(block_netflix->description);

    block_netflix->description = g_strdup(description);

    g_object_notify(G_OBJECT(block_netflix), "description");
}

static void 
set_image_url(HippoBlockNetflixMovie *block_netflix,
                const char             *image_url)
{
    if (block_netflix->image_url == image_url ||
        (block_netflix->image_url && image_url && strcmp(block_netflix->image_url, image_url) == 0))
        return;
    
    g_free(block_netflix->image_url);

    block_netflix->image_url = g_strdup(image_url);

    g_object_notify(G_OBJECT(block_netflix), "image-url");
}

static void 
set_queue(HippoBlockNetflixMovie *block_netflix,
          GSList                 *queue)
{
    if (block_netflix->queue) {
        g_slist_foreach(block_netflix->queue, (GFunc)g_object_unref, NULL);
        g_slist_free(block_netflix->queue);
    }

    block_netflix->queue = g_slist_copy(queue);
    g_slist_foreach(block_netflix->queue, (GFunc)g_object_ref, NULL);

    g_object_notify(G_OBJECT(block_netflix), "queue");
}

static gboolean
hippo_block_netflix_movie_update_from_xml (HippoBlock           *block,
                                           HippoDataCache       *cache,
                                           LmMessageNode        *node)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(block);
    LmMessageNode *netflix_node, *queue_node;
    LmMessageNode *child_node;
    HippoPerson *user;
    const char *description = NULL;
    const char *image_url = NULL;
    GSList *queue = NULL;

    if (!HIPPO_BLOCK_CLASS(hippo_block_netflix_movie_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "netflixMovie", HIPPO_SPLIT_NODE, &netflix_node,
                         "description", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &description,
                         NULL))
        return FALSE;

    /* The imageUrl should be on the netflixMovie node, but we have to keep it on the
     * queue node in what the server sends for a while for compatibility purposes,
     * so we support it in either location.
     */
    if (!hippo_xml_split(cache, netflix_node, NULL,
                         "queue", HIPPO_SPLIT_NODE, &queue_node,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "imageUrl", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &image_url,
                         NULL))
        return FALSE;
    
    if (image_url == NULL) {
        if (!hippo_xml_split(cache, queue_node, NULL, 
                             "imageUrl", HIPPO_SPLIT_STRING, &image_url,
                             NULL))
            return FALSE;
    }
        
    for (child_node = queue_node->children; child_node; child_node = child_node->next) {
       HippoNetflixMovie *movie;        
       if (strcmp(child_node->name, "movie") != 0)
           continue;
       movie = hippo_netflix_movie_new_from_xml(cache, child_node);
       if (movie != NULL)
           queue = g_slist_prepend(queue, movie);
    }

    queue = g_slist_reverse(queue);

    set_description(block_netflix, description);
    set_image_url(block_netflix, image_url);
    set_queue(block_netflix, queue);

    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block_netflix), user);
    
    return TRUE;
}

const char *
hippo_block_netflix_movie_get_image_url (HippoBlockNetflixMovie *netflix)
{
    return netflix->image_url;
}

GSList *
hippo_block_netflix_movie_get_queue (HippoBlockNetflixMovie *netflix)
{
    return netflix->queue;
}
