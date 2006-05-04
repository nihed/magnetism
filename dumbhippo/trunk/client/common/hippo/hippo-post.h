#ifndef __HIPPO_POST_H__
#define __HIPPO_POST_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct _HippoPost      HippoPost;
typedef struct _HippoPostClass HippoPostClass;

#define HIPPO_TYPE_POST              (hippo_post_get_type ())
#define HIPPO_POST(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_POST, HippoPost))
#define HIPPO_POST_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_POST, HippoPostClass))
#define HIPPO_IS_POST(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_POST))
#define HIPPO_IS_POST_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_POST))
#define HIPPO_POST_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_POST, HippoPostClass))

GType        	 hippo_post_get_type                  (void) G_GNUC_CONST;
HippoPost       *hippo_post_new                       (void);

G_END_DECLS

#endif /* __HIPPO_POST_H__ */
