/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_PLATFORM_IMPL_H__
#define __HIPPO_PLATFORM_IMPL_H__

#include <hippo/hippo-common.h>

G_BEGIN_DECLS

typedef struct _HippoPlatformImpl      HippoPlatformImpl;
typedef struct _HippoPlatformImplClass HippoPlatformImplClass;

#define HIPPO_TYPE_PLATFORM_IMPL              (hippo_platform_impl_get_type ())
#define HIPPO_PLATFORM_IMPL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PLATFORM_IMPL, HippoPlatformImpl))
#define HIPPO_PLATFORM_IMPL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PLATFORM_IMPL, HippoPlatformImplClass))
#define HIPPO_IS_PLATFORM_IMPL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PLATFORM_IMPL))
#define HIPPO_IS_PLATFORM_IMPL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PLATFORM_IMPL))
#define HIPPO_PLATFORM_IMPL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_PLATFORM_IMPL, HippoPlatformImplClass))

GType        	 hippo_platform_impl_get_type               (void) G_GNUC_CONST;

HippoPlatform*   hippo_platform_impl_new                    (HippoInstanceType instance);

G_END_DECLS

#endif /* __HIPPO_PLATFORM_IMPL_H__ */
