/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_STACKER_PLATFORM_IMPL_H__
#define __HIPPO_STACKER_PLATFORM_IMPL_H__

#include <hippo/hippo-common.h>
#include <hippo/hippo-stacker-platform.h>

G_BEGIN_DECLS

typedef struct _HippoStackerPlatformImpl      HippoStackerPlatformImpl;
typedef struct _HippoStackerPlatformImplClass HippoStackerPlatformImplClass;

#define HIPPO_TYPE_STACKER_PLATFORM_IMPL              (hippo_stacker_platform_impl_get_type ())
#define HIPPO_STACKER_PLATFORM_IMPL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_STACKER_PLATFORM_IMPL, HippoStackerPlatformImpl))
#define HIPPO_STACKER_PLATFORM_IMPL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_STACKER_PLATFORM_IMPL, HippoStackerPlatformImplClass))
#define HIPPO_IS_STACKER_PLATFORM_IMPL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_STACKER_PLATFORM_IMPL))
#define HIPPO_IS_STACKER_PLATFORM_IMPL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_STACKER_PLATFORM_IMPL))
#define HIPPO_STACKER_PLATFORM_IMPL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_STACKER_PLATFORM_IMPL, HippoStackerPlatformImplClass))

GType        	 hippo_stacker_platform_impl_get_type               (void) G_GNUC_CONST;

HippoStackerPlatform*   hippo_stacker_platform_impl_new             (void);

G_END_DECLS

#endif /* __HIPPO_STACKER_PLATFORM_IMPL_H__ */
