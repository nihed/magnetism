#ifndef __HIPPO_PLATFORM_H__
#define __HIPPO_PLATFORM_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef struct _HippoPlatform      HippoPlatform;
typedef struct _HippoPlatformClass HippoPlatformClass;

#define HIPPO_TYPE_PLATFORM              (hippo_platform_get_type ())
#define HIPPO_PLATFORM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PLATFORM, HippoPlatform))
#define HIPPO_PLATFORM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PLATFORM, HippoPlatformClass))
#define HIPPO_IS_PLATFORM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PLATFORM))
#define HIPPO_IS_PLATFORM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PLATFORM))
#define HIPPO_PLATFORM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_PLATFORM, HippoPlatformClass))

struct _HippoPlatformClass {
	GTypeInterface base_iface;

	char*  (* read_login_cookie) (HippoPlatform *platform);
};

GType        	 hippo_platform_get_type               (void) G_GNUC_CONST;

char*            hippo_platform_read_login_cookie      (HippoPlatform *platform);

G_END_DECLS

#endif /* __HIPPO_PLATFORM_H__ */
