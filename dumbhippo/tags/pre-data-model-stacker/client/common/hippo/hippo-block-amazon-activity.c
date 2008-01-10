/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-amazon-activity.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_amazon_activity_init                (HippoBlockAmazonActivity       *block);
static void      hippo_block_amazon_activity_class_init          (HippoBlockAmazonActivityClass  *klass);

static void      hippo_block_amazon_activity_dispose             (GObject              *object);
static void      hippo_block_amazon_activity_finalize            (GObject              *object);

static gboolean  hippo_block_amazon_activity_update_from_xml     (HippoBlock           *block,
                                                                HippoDataCache       *cache,
                                                                LmMessageNode        *node);

static void hippo_block_amazon_activity_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_block_amazon_activity_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);

struct _HippoBlockAmazonActivity {
    HippoBlockAbstractPerson      parent;
    char *description;
    char *image_url;
    guint image_width;
    guint image_height;
    char *review_title;
    int review_rating;
    char *list_name;
    char *list_link;
    char *list_item_comment;
};

struct _HippoBlockAmazonActivityClass {
    HippoBlockAbstractPersonClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_DESCRIPTION,
    PROP_IMAGE_URL,
    PROP_IMAGE_WIDTH,
    PROP_IMAGE_HEIGHT,
    PROP_REVIEW_TITLE,
    PROP_REVIEW_RATING,
    PROP_LIST_NAME,
    PROP_LIST_LINK,
    PROP_LIST_ITEM_COMMENT
};

G_DEFINE_TYPE(HippoBlockAmazonActivity, hippo_block_amazon_activity, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_amazon_activity_init(HippoBlockAmazonActivity *block_amazon_activity)
{
}

static void
hippo_block_amazon_activity_class_init(HippoBlockAmazonActivityClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_amazon_activity_set_property;
    object_class->get_property = hippo_block_amazon_activity_get_property;

    object_class->dispose = hippo_block_amazon_activity_dispose;
    object_class->finalize = hippo_block_amazon_activity_finalize;

    block_class->update_from_xml = hippo_block_amazon_activity_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Amazon product user or editorial review"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_IMAGE_URL,
                                    g_param_spec_string("image-url",
                                                        _("Image URL"),
                                                        _("URL to an image of the product"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_IMAGE_WIDTH,
                                    g_param_spec_uint("image-width",
                                                      _("Image Width"),
                                                      _("Width of the product image"),
                                                      0, 300, 0,
                                                      G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_IMAGE_HEIGHT,
                                    g_param_spec_uint("image-height",
                                                      _("Image Height"),
                                                      _("Height of the product image"),
                                                      0, 300, 0,
                                                      G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_REVIEW_TITLE,
                                    g_param_spec_string("review-title",
                                                        _("Review Title"),
                                                        _("Title of the user review"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_REVIEW_RATING,
                                    g_param_spec_int("review-rating",
                                                     _("Review Rating"),
                                                     _("Rating given by the user who wrote the review"),
                                                     -1, 5, -1,
                                                     G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_LIST_NAME,
                                    g_param_spec_string("list-name",
                                                        _("List Name"),
                                                        _("A name of the wish list to which the item was added"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_LIST_LINK,
                                    g_param_spec_string("list-link",
                                                        _("List Link"),
                                                        _("A link to the wish list to which the item was added"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_LIST_ITEM_COMMENT,
                                    g_param_spec_string("list-item-comment",
                                                        _("List Item Comment"),
                                                        _("A comment added by the user to the item they wish-listed"),
                                                        NULL,
                                                        G_PARAM_READABLE));

}

static void
hippo_block_amazon_activity_dispose(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_amazon_activity_parent_class)->dispose(object);
}

static void
hippo_block_amazon_activity_finalize(GObject *object)
{
    HippoBlockAmazonActivity *block_amazon_activity = HIPPO_BLOCK_AMAZON_ACTIVITY(object);

    g_free(block_amazon_activity->image_url);
    g_free(block_amazon_activity->description);
    g_free(block_amazon_activity->review_title);
    g_free(block_amazon_activity->list_name);
    g_free(block_amazon_activity->list_link);
    g_free(block_amazon_activity->list_item_comment);
 
   G_OBJECT_CLASS(hippo_block_amazon_activity_parent_class)->finalize(object);
}

static void
hippo_block_amazon_activity_set_property(GObject         *object,
                                         guint            prop_id,
                                         const GValue    *value,
                                         GParamSpec      *pspec)
{
    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_amazon_activity_get_property(GObject         *object,
                                         guint            prop_id,
                                         GValue          *value,
                                         GParamSpec      *pspec)
{
    HippoBlockAmazonActivity *block_amazon_activity = HIPPO_BLOCK_AMAZON_ACTIVITY(object);

    switch (prop_id) {
    case PROP_DESCRIPTION:
        g_value_set_string(value, block_amazon_activity->description);
        break;
    case PROP_IMAGE_URL:
        g_value_set_string(value, block_amazon_activity->image_url);
        break;
    case PROP_IMAGE_WIDTH:
        g_value_set_uint(value, block_amazon_activity->image_width);
        break;
    case PROP_IMAGE_HEIGHT:
        g_value_set_uint(value, block_amazon_activity->image_height);
        break;
    case PROP_REVIEW_TITLE:
        g_value_set_string(value, block_amazon_activity->review_title);
        break;
    case PROP_REVIEW_RATING:
        g_value_set_int(value, block_amazon_activity->review_rating);
        break;
    case PROP_LIST_NAME:
        g_value_set_string(value, block_amazon_activity->list_name);
        break;
    case PROP_LIST_LINK:
        g_value_set_string(value, block_amazon_activity->list_link);
        break;
    case PROP_LIST_ITEM_COMMENT:
        g_value_set_string(value, block_amazon_activity->list_item_comment);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void 
set_description(HippoBlockAmazonActivity *block_amazon_activity,
                const char             *description)
{
    if (block_amazon_activity->description == description ||
        (block_amazon_activity->description && description && strcmp(block_amazon_activity->description, description) == 0))
        return;

    g_free(block_amazon_activity->description);

    block_amazon_activity->description = g_strdup(description);

    g_object_notify(G_OBJECT(block_amazon_activity), "description");
}

static void 
set_image_url(HippoBlockAmazonActivity *block_amazon_activity,
                const char             *image_url)
{
    if (block_amazon_activity->image_url == image_url ||
        (block_amazon_activity->image_url && image_url && strcmp(block_amazon_activity->image_url, image_url) == 0))
        return;
    
    g_free(block_amazon_activity->image_url);

    block_amazon_activity->image_url = g_strdup(image_url);

    g_object_notify(G_OBJECT(block_amazon_activity), "image-url");
}

static void
set_image_width(HippoBlockAmazonActivity *block_amazon_activity,
                guint                    image_width)
{
    if (image_width != block_amazon_activity->image_width) {
        block_amazon_activity->image_width = image_width;
        g_object_notify(G_OBJECT(block_amazon_activity), "image-width");
    }
}

static void
set_image_height(HippoBlockAmazonActivity *block_amazon_activity,
                 guint                    image_height)
{
    if (image_height != block_amazon_activity->image_height) {
        block_amazon_activity->image_height = image_height;
        g_object_notify(G_OBJECT(block_amazon_activity), "image-height");
    }
}

static void 
set_review_title(HippoBlockAmazonActivity *block_amazon_activity,
                 const char               *review_title)
{
    if (block_amazon_activity->review_title == review_title ||
        (block_amazon_activity->review_title && review_title && strcmp(block_amazon_activity->review_title, review_title) == 0))
        return;
    
    g_free(block_amazon_activity->review_title);

    block_amazon_activity->review_title = g_strdup(review_title);

    g_object_notify(G_OBJECT(block_amazon_activity), "review-title");
}

static void
set_review_rating(HippoBlockAmazonActivity *block_amazon_activity,
                  int                      review_rating)
{
    if (review_rating != block_amazon_activity->review_rating) {
        block_amazon_activity->review_rating = review_rating;
        g_object_notify(G_OBJECT(block_amazon_activity), "review-rating");
    }
}

static void 
set_list_name(HippoBlockAmazonActivity *block_amazon_activity,
              const char             *list_name)
{
    if (block_amazon_activity->list_name == list_name ||
        (block_amazon_activity->list_name && list_name && strcmp(block_amazon_activity->list_name, list_name) == 0))
        return;
    
    g_free(block_amazon_activity->list_name);

    block_amazon_activity->list_name = g_strdup(list_name);

    g_object_notify(G_OBJECT(block_amazon_activity), "list-name");
}

static void 
set_list_link(HippoBlockAmazonActivity *block_amazon_activity,
                const char             *list_link)
{
    if (block_amazon_activity->list_link == list_link ||
        (block_amazon_activity->list_link && list_link && strcmp(block_amazon_activity->list_link, list_link) == 0))
        return;
    
    g_free(block_amazon_activity->list_link);

    block_amazon_activity->list_link = g_strdup(list_link);

    g_object_notify(G_OBJECT(block_amazon_activity), "list-link");
}

static void 
set_list_item_comment(HippoBlockAmazonActivity *block_amazon_activity,
                const char             *list_item_comment)
{
    if (block_amazon_activity->list_item_comment == list_item_comment ||
        (block_amazon_activity->list_item_comment && list_item_comment && strcmp(block_amazon_activity->list_item_comment, list_item_comment) == 0))
        return;
    
    g_free(block_amazon_activity->list_item_comment);

    block_amazon_activity->list_item_comment = g_strdup(list_item_comment);

    g_object_notify(G_OBJECT(block_amazon_activity), "list-item-comment");
}

static gboolean
hippo_block_amazon_activity_update_from_xml (HippoBlock           *block,
                                             HippoDataCache       *cache,
                                             LmMessageNode        *node)
{
    HippoBlockAmazonActivity *block_amazon_activity = HIPPO_BLOCK_AMAZON_ACTIVITY(block);
    LmMessageNode *amazon_activity_node;
    LmMessageNode *review_node = NULL;
    LmMessageNode *list_item_node = NULL;
    HippoPerson *user;
    const char *description = NULL;
    const char *image_url = NULL;
    guint image_width;
    guint image_height;
    const char *review_title = NULL;
    int review_rating = -1;
    const char *list_name = NULL;
    const char *list_link = NULL;
    const char *list_item_comment = NULL; 

    if (!HIPPO_BLOCK_CLASS(hippo_block_amazon_activity_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "amazonActivity", HIPPO_SPLIT_NODE, &amazon_activity_node,
                         "description", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT | HIPPO_SPLIT_OPTIONAL, &description,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, amazon_activity_node, NULL,
                         "review", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &review_node,
                         "listItem", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &list_item_node,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "imageUrl", HIPPO_SPLIT_STRING, &image_url,
                         "imageWidth", HIPPO_SPLIT_INT32, &image_width,
                         "imageHeight", HIPPO_SPLIT_INT32, &image_height, 
                         NULL))
        return FALSE;
    
    if (review_node != NULL) {
        if (!hippo_xml_split(cache, review_node, NULL, 
                             "title", HIPPO_SPLIT_STRING, &review_title,
                             "rating", HIPPO_SPLIT_INT32, &review_rating,   
                             NULL))
            return FALSE;
    }

    if (list_item_node != NULL) {
        if (!hippo_xml_split(cache, list_item_node, NULL, 
                             "listName", HIPPO_SPLIT_STRING, &list_name,
                             "listLink", HIPPO_SPLIT_STRING, &list_link, 
                             "comment", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &list_item_comment,
                             NULL))
            return FALSE;
     }

    set_description(block_amazon_activity, description);
    set_image_width(block_amazon_activity, image_width);
    set_image_height(block_amazon_activity, image_height);
    set_image_url(block_amazon_activity, image_url); 
    set_review_title(block_amazon_activity, review_title);
    set_review_rating(block_amazon_activity, review_rating);
    set_list_name(block_amazon_activity, list_name);
    set_list_link(block_amazon_activity, list_link);
    set_list_item_comment(block_amazon_activity, list_item_comment);

    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block_amazon_activity), user);
    
    return TRUE;
}

const char *
hippo_block_amazon_activity_get_image_url(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->image_url;
}

guint
hippo_block_amazon_activity_get_image_width(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->image_width;
}

guint
hippo_block_amazon_activity_get_image_height(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->image_height;
}

const char *
hippo_block_amazon_activity_get_review_title(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->review_title;
}

int
hippo_block_amazon_activity_get_review_rating(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->review_rating;
}

const char *
hippo_block_amazon_activity_get_list_name(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->list_name;
}

const char *
hippo_block_amazon_activity_get_list_link(HippoBlockAmazonActivity *amazon_activity)
{
    return amazon_activity->list_link;
}

const char *
hippo_block_amazon_activity_get_list_item_comment(HippoBlockAmazonActivity *amazon_activity)
{
  return amazon_activity->list_item_comment;
}




