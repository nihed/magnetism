/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_NETFLIX_MOVIE_H__
#define __HIPPO_NETFLIX_MOVIE_H__

#include <loudmouth/loudmouth.h>

G_BEGIN_DECLS

typedef struct _HippoNetflixMovie      HippoNetflixMovie;
typedef struct _HippoNetflixMovieClass HippoNetflixMovieClass;

#define HIPPO_TYPE_NETFLIX_MOVIE              (hippo_netflix_movie_get_type ())
#define HIPPO_NETFLIX_MOVIE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_NETFLIX_MOVIE, HippoNetflixMovie))
#define HIPPO_NETFLIX_MOVIE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_NETFLIX_MOVIE, HippoNetflixMovieClass))
#define HIPPO_IS_NETFLIX_MOVIE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_NETFLIX_MOVIE))
#define HIPPO_IS_NETFLIX_MOVIE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_NETFLIX_MOVIE))
#define HIPPO_NETFLIX_MOVIE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_NETFLIX_MOVIE, HippoNetflixMovieClass))

GType hippo_netflix_movie_get_type(void) G_GNUC_CONST;

HippoNetflixMovie* hippo_netflix_movie_new_from_xml   (HippoDataCache *cache,
                                                       LmMessageNode  *node);

const char* hippo_netflix_movie_get_title            (HippoNetflixMovie *movie);
const char* hippo_netflix_movie_get_description      (HippoNetflixMovie *movie);
guint       hippo_netflix_movie_get_priority         (HippoNetflixMovie *movie);
const char* hippo_netflix_movie_get_url              (HippoNetflixMovie *movie);

G_END_DECLS

#endif /* __HIPPO_NETFLIX_MOVIE_H__ */
