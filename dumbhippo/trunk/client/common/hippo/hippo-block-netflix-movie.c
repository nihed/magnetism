/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-netflix-movie.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include <string.h>

static void      hippo_block_netflix_movie_init                (HippoBlockNetflixMovie       *block);
static void      hippo_block_netflix_movie_class_init          (HippoBlockNetflixMovieClass  *klass);

static void      hippo_block_netflix_movie_dispose             (GObject              *object);
static void      hippo_block_netflix_movie_finalize            (GObject              *object);

static void      hippo_block_netflix_movie_update              (HippoBlock           *block);

static void hippo_block_netflix_movie_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_block_netflix_movie_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);

struct _HippoBlockNetflixMovie {
    HippoBlockAbstractPerson      parent;
    char *image_url;
    char *description;
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
    PROP_IMAGE_URL
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

    block_class->update = hippo_block_netflix_movie_update;
    
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
}

static void
hippo_block_netflix_movie_dispose(GObject *object)
{
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
hippo_block_netflix_movie_update (HippoBlock           *block)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(block);
    const char *description = NULL;
    const char *image_url = NULL;

    HIPPO_BLOCK_CLASS(hippo_block_netflix_movie_parent_class)->update(block);

    ddm_data_resource_get(block->resource,
                          "description", DDM_DATA_STRING, &description,
                          "imageUrl", DDM_DATA_URL, &image_url,
                          NULL);

    set_description(block_netflix, description);
    set_image_url(block_netflix, image_url);
}

const char *
hippo_block_netflix_movie_get_image_url (HippoBlockNetflixMovie *netflix)
{
    return netflix->image_url;
}
