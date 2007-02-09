/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_NETFLIX_MOVIE_H__
#define __HIPPO_BLOCK_NETFLIX_MOVIE_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockNetflixMovie      HippoBlockNetflixMovie;
typedef struct _HippoBlockNetflixMovieClass HippoBlockNetflixMovieClass;


#define HIPPO_TYPE_BLOCK_NETFLIX_MOVIE              (hippo_block_netflix_movie_get_type ())
#define HIPPO_BLOCK_NETFLIX_MOVIE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_NETFLIX_MOVIE, HippoBlockNetflixMovie))
#define HIPPO_BLOCK_NETFLIX_MOVIE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_NETFLIX_MOVIE, HippoBlockNetflixMovieClass))
#define HIPPO_IS_BLOCK_NETFLIX_MOVIE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_NETFLIX_MOVIE))
#define HIPPO_IS_BLOCK_NETFLIX_MOVIE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_NETFLIX_MOVIE))
#define HIPPO_BLOCK_NETFLIX_MOVIE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_NETFLIX_MOVIE, HippoBlockNetflixMovieClass))

GType            hippo_block_netflix_movie_get_type               (void) G_GNUC_CONST;

const char *     hippo_block_netflix_movie_get_image_url          (HippoBlockNetflixMovie *netflix);
GList *          hippo_block_netflix_movie_get_queue              (HippoBlockNetflixMovie *netflix);

G_END_DECLS

#endif /* __HIPPO_BLOCK_NETFLIX_MOVIE_H__ */
