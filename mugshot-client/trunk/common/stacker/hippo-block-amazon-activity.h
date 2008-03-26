/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_AMAZON_ACTIVITY_H__
#define __HIPPO_BLOCK_AMAZON_ACTIVITY_H__

#include "hippo-block.h"

G_BEGIN_DECLS

typedef struct _HippoBlockAmazonActivity      HippoBlockAmazonActivity;
typedef struct _HippoBlockAmazonActivityClass HippoBlockAmazonActivityClass;


#define HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY              (hippo_block_amazon_activity_get_type ())
#define HIPPO_BLOCK_AMAZON_ACTIVITY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY, HippoBlockAmazonActivity))
#define HIPPO_BLOCK_AMAZON_ACTIVITY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY, HippoBlockAmazonActivityClass))
#define HIPPO_IS_BLOCK_AMAZON_ACTIVITY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY))
#define HIPPO_IS_BLOCK_AMAZON_ACTIVITY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY))
#define HIPPO_BLOCK_AMAZON_ACTIVITY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY, HippoBlockAmazonActivityClass))

GType            hippo_block_amazon_activity_get_type               (void) G_GNUC_CONST;

const char *     hippo_block_amazon_activity_get_image_url          (HippoBlockAmazonActivity *activity);
guint            hippo_block_amazon_activity_get_image_width        (HippoBlockAmazonActivity *activity);
guint            hippo_block_amazon_activity_get_image_height       (HippoBlockAmazonActivity *activity);
const char *     hippo_block_amazon_activity_get_review_title       (HippoBlockAmazonActivity *activity);
int              hippo_block_amazon_activity_get_review_rating      (HippoBlockAmazonActivity *activity);
const char *     hippo_block_amazon_activity_get_list_name          (HippoBlockAmazonActivity *activity);
const char *     hippo_block_amazon_activity_get_list_link          (HippoBlockAmazonActivity *activity);
const char *     hippo_block_amazon_activity_get_list_item_comment  (HippoBlockAmazonActivity *activity);


G_END_DECLS

#endif /* __HIPPO_BLOCK_AMAZON_ACTIVITY_H__ */
