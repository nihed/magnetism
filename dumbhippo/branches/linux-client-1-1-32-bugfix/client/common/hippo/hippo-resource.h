/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_RESOURCE_H__
#define __HIPPO_RESOURCE_H__

#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

typedef struct _HippoResource      HippoResource;
typedef struct _HippoResourceClass HippoResourceClass;

#define HIPPO_TYPE_RESOURCE              (hippo_resource_get_type ())
#define HIPPO_RESOURCE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_RESOURCE, HippoResource))
#define HIPPO_RESOURCE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_RESOURCE, HippoResourceClass))
#define HIPPO_IS_RESOURCE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_RESOURCE))
#define HIPPO_IS_RESOURCE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_RESOURCE))
#define HIPPO_RESOURCE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_RESOURCE, HippoResourceClass))

GType            hippo_resource_get_type                  (void) G_GNUC_CONST;
HippoResource*   hippo_resource_new                       (const char  *guid);

G_END_DECLS

#endif /* __HIPPO_RESOURCE_H__ */
