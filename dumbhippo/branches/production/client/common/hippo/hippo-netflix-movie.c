/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include "hippo-netflix-movie.h"
#include "hippo-xml-utils.h"

static void hippo_netflix_movie_finalize (GObject *object);

static void hippo_netflix_movie_set_property (GObject      *object,
                                      guint         prop_id,
                                      const GValue *value,
                                      GParamSpec   *pspec);
static void hippo_netflix_movie_get_property (GObject      *object,
                                      guint         prop_id,
                                      GValue       *value,
                                      GParamSpec   *pspec);

struct _HippoNetflixMovie {
    GObject parent;
    
    char *title;
    char *description;
    guint priority;
    char *url;
};

struct _HippoNetflixMovieClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoNetflixMovie, hippo_netflix_movie, G_TYPE_OBJECT);

/*
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
*/

enum {
    PROP_0,
    PROP_TITLE,
    PROP_DESCRIPTION,
    PROP_PRIORITY,
    PROP_URL
};

static void
hippo_netflix_movie_init(HippoNetflixMovie *movie)
{
}

static void
hippo_netflix_movie_class_init(HippoNetflixMovieClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
          
    object_class->finalize = hippo_netflix_movie_finalize;

    object_class->set_property = hippo_netflix_movie_set_property;
    object_class->get_property = hippo_netflix_movie_get_property;

    g_object_class_install_property(object_class,
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("Title"),
                                                        _("Title of movie"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Description of movie"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_PRIORITY,
                                    g_param_spec_uint("priority",
                                                      _("Priority"),
                                                      _("Priority in queue"),
                                                      0, G_MAXUINT, 1,
                                                      G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_URL,
                                    g_param_spec_string("url",
                                                        _("URL"),
                                                        _("An URL for more information about the movie"),
                                                        NULL,
                                                        G_PARAM_READABLE));
}

static void
hippo_netflix_movie_finalize(GObject *object)
{
    HippoNetflixMovie *movie = HIPPO_NETFLIX_MOVIE(object);

    g_free(movie->title);
    g_free(movie->description);
    g_free(movie->url);

    G_OBJECT_CLASS(hippo_netflix_movie_parent_class)->finalize(object); 
}

static void
hippo_netflix_movie_set_property(GObject         *object,
                         guint            prop_id,
                         const GValue    *value,
                         GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_netflix_movie_get_property(GObject         *object,
                         guint            prop_id,
                         GValue          *value,
                         GParamSpec      *pspec)
{
    HippoNetflixMovie *movie;

    movie = HIPPO_NETFLIX_MOVIE(object);

    switch (prop_id) {
    case PROP_TITLE:
        g_value_set_string(value, movie->title);
        break;
    case PROP_DESCRIPTION:
        g_value_set_string(value, movie->description);
        break;
    case PROP_PRIORITY:
        g_value_set_uint(value, movie->priority);
        break;
    case PROP_URL:
        g_value_set_string(value, movie->url);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

HippoNetflixMovie *
hippo_netflix_movie_new_from_xml(HippoDataCache *cache,
                                 LmMessageNode  *node)
{
    HippoNetflixMovie *movie;    
    const char *title;
    const char *description;
    guint priority;
    const char *url = NULL;
    
    if (!hippo_xml_split(cache, node, NULL,
                         "title", HIPPO_SPLIT_STRING, &title,
                         "description", HIPPO_SPLIT_STRING, &description,
                         "priority", HIPPO_SPLIT_INT32, &priority,
                         "url", HIPPO_SPLIT_STRING, &url,
                         NULL))
        return NULL;

    movie = g_object_new(HIPPO_TYPE_NETFLIX_MOVIE, NULL);

    movie->title = g_strdup(title);
    movie->description = g_strdup(description);
    movie->priority = priority;
    movie->url = g_strdup(url);

    return movie;
}

const char*
hippo_netflix_movie_get_title (HippoNetflixMovie *movie)
{
    return movie->title;
}

const char*
hippo_netflix_movie_get_description (HippoNetflixMovie *movie)
{
    return movie->description;
}

guint
hippo_netflix_movie_get_priority (HippoNetflixMovie *movie)
{
    return movie->priority;
}

const char*
hippo_netflix_movie_get_url (HippoNetflixMovie *movie)
{
    return movie->url;
}
