#ifndef __HIPPO_EMBEDDED_IMAGE_H__
#define __HIPPO_EMBEDDED_IMAGE_H__

#include <gdk-pixbuf/gdk-pixbuf.h>

G_BEGIN_DECLS

/* does not return a refcount; the image is guaranteed to 
 * exist forever, i.e. we hold a static refcount.
 */
GdkPixbuf* hippo_embedded_image_get(const char *name);

G_END_DECLS

#endif /* __HIPPO_EMBEDDED_IMAGE_H__ */
