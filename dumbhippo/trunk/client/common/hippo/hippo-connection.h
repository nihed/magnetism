#ifndef __HIPPO_CONNECTION_H__
#define __HIPPO_CONNECTION_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef struct _HippoConnection      HippoConnection;
typedef struct _HippoConnectionClass HippoConnectionClass;

#define HIPPO_TYPE_CONNECTION              (hippo_connection_get_type ())
#define HIPPO_CONNECTION(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CONNECTION, HippoConnection))
#define HIPPO_CONNECTION_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CONNECTION, HippoConnectionClass))
#define HIPPO_IS_CONNECTION(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CONNECTION))
#define HIPPO_IS_CONNECTION_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CONNECTION))
#define HIPPO_CONNECTION_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CONNECTION, HippoConnectionClass))

GType        	 hippo_connection_get_type        (void) G_GNUC_CONST;
HippoConnection *hippo_connection_new             (void);

G_END_DECLS

#endif /* __HIPPO_CONNECTION_H__ */
