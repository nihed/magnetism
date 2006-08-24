#ifndef __HIPPO_PLATFORM_IMPL_H__
#define __HIPPO_PLATFORM_IMPL_H__

#include <hippo/hippo-platform.h>
#include "HippoPreferences.h"

typedef struct _HippoPlatformImpl      HippoPlatformImpl;
typedef struct _HippoPlatformImplClass HippoPlatformImplClass;

#define HIPPO_TYPE_PLATFORM_IMPL              (hippo_platform_impl_get_type ())
#define HIPPO_PLATFORM_IMPL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PLATFORM_IMPL, HippoPlatformImpl))
#define HIPPO_PLATFORM_IMPL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PLATFORM_IMPL, HippoPlatformImplClass))
#define HIPPO_IS_PLATFORM_IMPL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PLATFORM_IMPL))
#define HIPPO_IS_PLATFORM_IMPL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PLATFORM_IMPL))
#define HIPPO_PLATFORM_IMPL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_PLATFORM_IMPL, HippoPlatformImplClass))

GType             hippo_platform_impl_get_type               (void) G_GNUC_CONST;

HippoPlatform*    hippo_platform_impl_new                    (HippoInstanceType  instance);
HippoPreferences* hippo_platform_impl_get_preferences        (HippoPlatformImpl *impl);

// Here so we can share cookie reading code with the standard cookie handling
void hippo_platform_impl_windows_migrate_cookie(const char *from_web_host,
                                                const char *to_web_host);

#endif /* __HIPPO_PLATFORM_IMPL_H__ */
