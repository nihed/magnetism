/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_GENERIC_H__
#define __HIPPO_BLOCK_GENERIC_H__

#include <hippo/hippo-connection.h>
#include <hippo/hippo-entity.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockGeneric      HippoBlockGeneric;
typedef struct _HippoBlockGenericClass HippoBlockGenericClass;


#define HIPPO_TYPE_BLOCK_GENERIC              (hippo_block_generic_get_type ())
#define HIPPO_BLOCK_GENERIC(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_GENERIC, HippoBlockGeneric))
#define HIPPO_BLOCK_GENERIC_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_GENERIC, HippoBlockGenericClass))
#define HIPPO_IS_BLOCK_GENERIC(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_GENERIC))
#define HIPPO_IS_BLOCK_GENERIC_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_GENERIC))
#define HIPPO_BLOCK_GENERIC_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_GENERIC, HippoBlockGenericClass))

GType            hippo_block_generic_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_GENERIC_H__ */
