/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-block-amazon-activity.h"
#include "hippo-canvas-block-amazon-activity.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-last-message-preview.h"
#include "hippo-canvas-quipper.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"
#include "hippo-canvas-url-image.h"
#include <string.h>

/* number of rating stars for reviews */
#define RATING_STARS_COUNT 5
#define HIPPO_CANVAS_BLOCK_AMAZON_ITEM_IMAGE_WIDTH 80
#define HIPPO_CANVAS_BLOCK_AMAZON_ITEM_DEFAULT_IMAGE_WIDTH 80
#define HIPPO_CANVAS_BLOCK_AMAZON_ITEM_DEFAULT_IMAGE_HEIGHT 100

static void      hippo_canvas_block_amazon_activity_init                (HippoCanvasBlockAmazonActivity       *block);
static void      hippo_canvas_block_amazon_activity_class_init          (HippoCanvasBlockAmazonActivityClass  *klass);
static void      hippo_canvas_block_amazon_activity_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_amazon_activity_dispose             (GObject                *object);
static void      hippo_canvas_block_amazon_activity_finalize            (GObject                *object);

static void hippo_canvas_block_amazon_activity_set_property (GObject      *object,
                                                             guint         prop_id,
                                                             const GValue *value,
                                                             GParamSpec   *pspec);
static void hippo_canvas_block_amazon_activity_get_property (GObject      *object,
                                                             guint         prop_id,
                                                             GValue       *value,
                                                             GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_amazon_activity_append_content_items (HippoCanvasBlock *block,
                                                                     HippoCanvasBox   *parent_box);
static void hippo_canvas_block_amazon_activity_set_block       (HippoCanvasBlock *canvas_block,
                                                                HippoBlock       *block);

static void hippo_canvas_block_amazon_activity_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_amazon_activity_stack_reason_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_amazon_activity_expand               (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_amazon_activity_unexpand             (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockAmazonActivity *block_amazon_activity,
                        HippoPerson                 *person);


struct _HippoCanvasBlockAmazonActivity {
    HippoCanvasBlock canvas_block;
    HippoCanvasItem *thumbnail;
    HippoCanvasItem *favicon;
    HippoCanvasItem *title_link;
    HippoCanvasItem *rating_stars[RATING_STARS_COUNT];
    HippoCanvasItem *review_title;
    HippoCanvasItem *user_review;
    HippoCanvasItem *list_link;
    HippoCanvasItem *comment;
    HippoCanvasItem *editorial_review;
    HippoCanvasItem *quipper;
    HippoCanvasItem *last_message_preview;
    HippoCanvasItem *chat_preview;
    HippoPerson *person;
};

struct _HippoCanvasBlockAmazonActivityClass {
    HippoCanvasBlockClass parent_class;

};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockAmazonActivity, hippo_canvas_block_amazon_activity, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_amazon_activity_iface_init));

static void
hippo_canvas_block_amazon_activity_init(HippoCanvasBlockAmazonActivity *block_amazon_activity)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_amazon_activity);

    block->required_type = HIPPO_BLOCK_TYPE_AMAZON_ACTIVITY;
    block->skip_heading = TRUE;    
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_amazon_activity_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_amazon_activity_class_init(HippoCanvasBlockAmazonActivityClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_amazon_activity_set_property;
    object_class->get_property = hippo_canvas_block_amazon_activity_get_property;

    object_class->dispose = hippo_canvas_block_amazon_activity_dispose;
    object_class->finalize = hippo_canvas_block_amazon_activity_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_amazon_activity_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_amazon_activity_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_amazon_activity_title_activated;
    canvas_block_class->stack_reason_changed = hippo_canvas_block_amazon_activity_stack_reason_changed;
    canvas_block_class->expand = hippo_canvas_block_amazon_activity_expand;
    canvas_block_class->unexpand = hippo_canvas_block_amazon_activity_unexpand;
}

static void
hippo_canvas_block_amazon_activity_dispose(GObject *object)
{
    HippoCanvasBlockAmazonActivity *block;

    block = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(object);

    set_person(block, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_amazon_activity_parent_class)->dispose(object);
}

static void
hippo_canvas_block_amazon_activity_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_canvas_block_amazon_activity_parent_class)->finalize(object);
}

static void
hippo_canvas_block_amazon_activity_set_property(GObject         *object,
                                              guint            prop_id,
                                              const GValue    *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockAmazonActivity *block;

    block = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_amazon_activity_get_property(GObject         *object,
                                              guint            prop_id,
                                              GValue          *value,
                                              GParamSpec      *pspec)
{
    HippoCanvasBlockAmazonActivity *block;

    block = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
update_visibility(HippoCanvasBlockAmazonActivity *block_amazon_activity)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_amazon_activity);
    HippoStackReason stack_reason;
    gboolean have_chat_id, have_rating, have_list_info, have_comment;
    const char *list_name = NULL;
    const char *comment = NULL;
    int i;

    if (canvas_block->block)
        stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    else
        stack_reason = HIPPO_STACK_BLOCK_UPDATE;

    have_chat_id = FALSE;
    if (canvas_block->block)
        have_chat_id = hippo_block_get_chat_id(canvas_block->block) != NULL;

    hippo_canvas_item_set_visible(block_amazon_activity->quipper,
                                  canvas_block->expanded && have_chat_id);
    hippo_canvas_item_set_visible(block_amazon_activity->last_message_preview,
                                  !canvas_block->expanded && stack_reason == HIPPO_STACK_CHAT_MESSAGE);
    hippo_canvas_item_set_visible(block_amazon_activity->chat_preview,
                                  canvas_block->expanded && have_chat_id);

    have_rating = FALSE;
    if (canvas_block->block)
        have_rating = (hippo_block_amazon_activity_get_review_rating(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block)) > 0);
        
    for (i = 0; i < RATING_STARS_COUNT; ++i) {
        hippo_canvas_item_set_visible(block_amazon_activity->rating_stars[i], have_rating);  
    }
    // if we have the review rating, it means we must have the review details, and will
    // be displaying the review title and content as well
    hippo_canvas_item_set_visible(block_amazon_activity->review_title, have_rating);
    hippo_canvas_item_set_visible(block_amazon_activity->user_review, have_rating);    

    have_list_info = FALSE;
    if (canvas_block->block) {
        list_name = hippo_block_amazon_activity_get_list_name(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block));
        have_list_info = (list_name != NULL && (strcmp(list_name, "") != 0));
    }
    hippo_canvas_item_set_visible(block_amazon_activity->list_link, have_list_info);   
    hippo_canvas_item_set_visible(block_amazon_activity->editorial_review, have_list_info);

    // a comment is optional for list items, if there is no comment, we are not displaying the comment section 
    have_comment = FALSE;
    if (canvas_block->block) {
        comment = hippo_block_amazon_activity_get_list_item_comment(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block));
        have_comment = (comment != NULL && (strcmp(comment, "") != 0));
    }
    hippo_canvas_item_set_visible(block_amazon_activity->comment, have_comment); 
}

static void
hippo_canvas_block_amazon_activity_append_content_items (HippoCanvasBlock *block,
                                                       HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(block);    
    HippoCanvasBox *box;
    HippoCanvasBox *top_box;
    HippoCanvasBox *beside_box;
    HippoCanvasBox *review_details_box;   
    HippoCanvasBox *list_item_details_box; 
    int i;

    top_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                           "spacing", 4,
                           "border-bottom", 3,                          
                           NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(top_box), 0);

    block_amazon_activity->thumbnail = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE,
                                            "image-name", "amazon_no_image",
                                            "tooltip", "More information about this item",
                                            "actions", hippo_canvas_block_get_actions(block),
                                            "xalign", HIPPO_ALIGNMENT_START,
                                            "yalign", HIPPO_ALIGNMENT_START,
					    "scale-width", HIPPO_CANVAS_BLOCK_AMAZON_ITEM_IMAGE_WIDTH,
					    "scale-height",  HIPPO_CANVAS_BLOCK_AMAZON_ITEM_DEFAULT_IMAGE_HEIGHT * HIPPO_CANVAS_BLOCK_AMAZON_ITEM_IMAGE_WIDTH /  HIPPO_CANVAS_BLOCK_AMAZON_ITEM_DEFAULT_IMAGE_WIDTH,
                                            NULL);
    hippo_canvas_box_append(top_box, block_amazon_activity->thumbnail, 0);

    beside_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_VERTICAL,
                              NULL);
    hippo_canvas_box_append(top_box, HIPPO_CANVAS_ITEM(beside_box), 0);

    /* An extra box to keep the title link from expanding beyond its text */
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0);
    
    block_amazon_activity->favicon = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                                          "xalign", HIPPO_ALIGNMENT_CENTER,
                                          "yalign", HIPPO_ALIGNMENT_CENTER,
                                          "scale-width", 16, /* favicon size */
                                          "scale-height", 16,
                                          "border-right", 6,
                                          NULL);
    hippo_canvas_box_append(box, block_amazon_activity->favicon, 0);

    block_amazon_activity->title_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                                             "actions", hippo_canvas_block_get_actions(block),
                                             "xalign", HIPPO_ALIGNMENT_START,
                                             "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                             "font", "Bold 12px",
                                             "tooltip", "More information about this item",
                                             NULL);
    hippo_canvas_box_append(box, block_amazon_activity->title_link, 0);
    
    review_details_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                      "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                      NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(review_details_box), 0);

    for (i=0; i < RATING_STARS_COUNT; ++i) {
        block_amazon_activity->rating_stars[i] = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                                                              "padding-top", 2,
                                                              "image-name", "rating_star",
                                                              "xalign", HIPPO_ALIGNMENT_CENTER,
                                                              "yalign", HIPPO_ALIGNMENT_START,
                                                              "scale-width", 13,
                                                              "scale-height", 12,
                                                              NULL);
        hippo_canvas_box_append(review_details_box, block_amazon_activity->rating_stars[i], 0);
        hippo_canvas_item_set_visible(block_amazon_activity->rating_stars[i],
                                      FALSE); /* we don't yet know if this is a review */    
    }

    block_amazon_activity->review_title = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                       "padding-top", 2,
                                                       "padding-left", 2,  
                                                       "xalign", HIPPO_ALIGNMENT_START,
                                                       "yalign", HIPPO_ALIGNMENT_END,
                                                       "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                                       "font", "Bold 11px",
                                                       NULL);
    hippo_canvas_box_append(review_details_box, block_amazon_activity->review_title, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->review_title,
                                  FALSE); /* we don't yet know if this is a review */          

    list_item_details_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                         "orientation", HIPPO_ORIENTATION_VERTICAL,
                                         NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(list_item_details_box), 0);

    block_amazon_activity->list_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                                                    "actions", hippo_canvas_block_get_actions(block),
                                                    "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD, 
                                                    "xalign", HIPPO_ALIGNMENT_START,
                                                    "yalign", HIPPO_ALIGNMENT_CENTER,
                                                    "font", "Bold 12px",
                                                    "tooltip", "Check out this list",
                                                    "underline", FALSE,  
                                                    NULL);
    hippo_canvas_box_append(list_item_details_box, block_amazon_activity->list_link, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->list_link,
                                  FALSE); /* we don't yet know if this is a list item */          
    block_amazon_activity->comment = g_object_new(HIPPO_TYPE_CANVAS_TEXT,  
                                                  "xalign", HIPPO_ALIGNMENT_START,
                                                  "yalign", HIPPO_ALIGNMENT_CENTER,
                                                  "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                                                  NULL);
    hippo_canvas_box_append(list_item_details_box, block_amazon_activity->comment, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->comment,
                                  FALSE); /* we don't yet know if this is a list item and if there is a comment */

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0); 

    block_amazon_activity->user_review = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                   "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                                                   "xalign", HIPPO_ALIGNMENT_START,
                                                   "yalign", HIPPO_ALIGNMENT_START,
                                                   "text", NULL,
                                                   "border-top", 3,
                                                   "border-bottom", 4,
                                                   NULL);
    hippo_canvas_box_append(beside_box, block_amazon_activity->user_review, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->user_review,
                                  FALSE); /* we don't yet know if this is a review*/

    block_amazon_activity->editorial_review = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                   "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                                                   "xalign", HIPPO_ALIGNMENT_START,
                                                   "yalign", HIPPO_ALIGNMENT_START,
                                                   "text", NULL,
                                                   "border-top", 2,
                                                   "border-bottom", 4,
                                                   NULL);
    hippo_canvas_box_append(beside_box, block_amazon_activity->editorial_review, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->editorial_review,
                                  FALSE); /* we don't yet know if this is a list item*/

    
    block_amazon_activity->last_message_preview = g_object_new(HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW,
                                                       "actions", hippo_canvas_block_get_actions(block),
                                                       NULL);
    hippo_canvas_box_append(parent_box, block_amazon_activity->last_message_preview, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->last_message_preview,
                                  FALSE); /* no messages yet */

    block_amazon_activity->quipper = g_object_new(HIPPO_TYPE_CANVAS_QUIPPER,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          NULL);
    hippo_canvas_box_append(parent_box, block_amazon_activity->quipper, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->quipper,
                                  FALSE); /* not expanded */

    block_amazon_activity->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                               "actions", hippo_canvas_block_get_actions(block),
                                               "padding-bottom", 8,
                                               NULL);
    hippo_canvas_box_append(parent_box,
                            block_amazon_activity->chat_preview, 0);
    hippo_canvas_item_set_visible(block_amazon_activity->chat_preview,
                                  FALSE); /* not expanded at first */           
 
    update_visibility(block_amazon_activity);
}

static void 
set_list_link_text(HippoCanvasBlockAmazonActivity *block_amazon_activity)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_amazon_activity);
    GString *list_link_text;

    if (canvas_block->block && hippo_block_amazon_activity_get_list_name(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block))) {
        list_link_text = g_string_new("has been added to ");

        if (block_amazon_activity->person) {
            g_string_append(list_link_text, (const char*) hippo_entity_get_name(HIPPO_ENTITY(block_amazon_activity->person)));
            g_string_append(list_link_text, "'s ");
        }

        g_string_append(list_link_text, (const char*) hippo_block_amazon_activity_get_list_name(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block)));

        // because we have to keep the whole wish list info line as a link, it looks nicer without
        // a period in the end
        // g_string_append(list_link_text, ".");

        g_object_set(block_amazon_activity->list_link,
                     "text", list_link_text->str,
                     NULL);                        
        
        g_string_free(list_link_text, TRUE);
    }
}

static void 
set_list_item_comment_markup(HippoCanvasBlockAmazonActivity *block_amazon_activity)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_amazon_activity);
    char *list_item_comment_markup;
    const char* name = NULL;

    if (canvas_block->block && hippo_block_amazon_activity_get_list_item_comment(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block))) {

        if (block_amazon_activity->person) 
            name = (const char*) hippo_entity_get_name(HIPPO_ENTITY(block_amazon_activity->person));

        list_item_comment_markup = g_markup_printf_escaped ("<i>%s says:</i> %s", name ? name : "Person",
                                                            hippo_block_amazon_activity_get_list_item_comment(HIPPO_BLOCK_AMAZON_ACTIVITY(canvas_block->block)));

        g_object_set(block_amazon_activity->comment,
                     "markup", list_item_comment_markup,
                     NULL);                        
    
        g_free(list_item_comment_markup);
    }
}

static void
set_person(HippoCanvasBlockAmazonActivity *block_amazon_activity,
           HippoPerson                  *person)
{
    if (person == block_amazon_activity->person)
        return;

    if (block_amazon_activity->person) {
        g_object_unref(block_amazon_activity->person);
        block_amazon_activity->person = NULL;
    }

    if (person) {
        block_amazon_activity->person = person;
        g_object_ref(G_OBJECT(person));
    }

    set_list_link_text(block_amazon_activity);
    set_list_item_comment_markup(block_amazon_activity);

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_amazon_activity),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockAmazonActivity *block_amazon_activity)
{
    HippoPerson *person;
    person = NULL;

    if (block)
        g_object_get(G_OBJECT(block), "user", &person, NULL);
    
    set_person(block_amazon_activity, person);
    if (person)
        g_object_unref(person);
}

static void
on_block_description_changed(HippoBlock *block,
                             GParamSpec *arg, /* null when first calling this */
                             void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    char *description = NULL;
    char *description_markup;

    if (block)
        g_object_get(G_OBJECT(block), "description", &description, NULL);

    g_object_set(G_OBJECT(block_amazon_activity->user_review),
                 "text", description,
                 NULL);

    if (description != NULL && (strcmp(description, "") != 0)) {
        description_markup = g_markup_printf_escaped ("<i>Editorial review:</i> %s", description);

        g_object_set(G_OBJECT(block_amazon_activity->editorial_review),
                     "markup", description_markup,
                     NULL);
        g_free(description_markup);
    }

    g_free(description);
}

static void
on_block_chat_id_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    
    update_visibility(block_amazon_activity);
}

static void
on_block_image_url_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when first calling this */
                           void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    const char *thumbnail_url = NULL;

    if (block)
        thumbnail_url = hippo_block_amazon_activity_get_image_url(HIPPO_BLOCK_AMAZON_ACTIVITY(block));
    
    g_object_set(block_amazon_activity->thumbnail,
                 "image-name", "amazon_no_image",
                 NULL);
    
    if (thumbnail_url && (strstr(thumbnail_url, "amazon_no_image") == NULL))
        hippo_actions_load_thumbnail_async(hippo_canvas_block_get_actions(canvas_block),
                                           thumbnail_url,
                                           block_amazon_activity->thumbnail);     
}

static void
on_block_image_dimension_changed(HippoBlock *block,
                                 GParamSpec *arg, /* null when first calling this */
                                 void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    guint image_width = 0;
    guint image_height = 0;

    if (block) {
        image_width = hippo_block_amazon_activity_get_image_width(HIPPO_BLOCK_AMAZON_ACTIVITY(block));
        image_height = hippo_block_amazon_activity_get_image_height(HIPPO_BLOCK_AMAZON_ACTIVITY(block));
    }

    if (image_width > 0 && image_height > 0)
        g_object_set(block_amazon_activity->thumbnail,
                     "scale-height", image_height * HIPPO_CANVAS_BLOCK_AMAZON_ITEM_IMAGE_WIDTH / image_width,
                     NULL); 
}

static void
on_block_title_changed(HippoBlock *block,
                       GParamSpec *arg, /* null when first calling this */
                       void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    const char *title = NULL;

    if (block)
        title = hippo_block_get_title(block);
            
    g_object_set(block_amazon_activity->title_link,
                 "text", title,
                 NULL);                        
    g_object_set(G_OBJECT(block_amazon_activity->quipper),
                 "title", title,
                 NULL);
}

static void
on_block_title_link_changed(HippoBlock *block,
                            GParamSpec *arg, /* null when first calling this */
                            void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    const char *title_link = NULL;

    if (block)
        title_link = hippo_block_get_title_link(block);
            
    g_object_set(block_amazon_activity->title_link,
                 "url", title_link,
                 NULL);                        
    g_object_set(block_amazon_activity->thumbnail,
                 "url", title_link,
                 NULL);
}

static void
on_block_icon_url_changed(HippoBlock *block,
                          GParamSpec *arg, /* null when first calling this */
                          void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    const char *icon_url = NULL;

    if (block)
        icon_url = hippo_block_get_icon_url(block);
    
    if (icon_url)
        hippo_actions_load_favicon_async(canvas_block->actions,
                                         hippo_block_get_icon_url(canvas_block->block),
                                         block_amazon_activity->favicon);
    else
        g_object_set(block_amazon_activity->favicon,
                     "image", NULL,
                     NULL);
}

static void
on_block_review_rating_changed(HippoBlock *block,
                        GParamSpec *arg, /* null when first calling this */
                        void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    int review_rating = -1;
    int i; 

    if (block)
        review_rating = hippo_block_amazon_activity_get_review_rating(HIPPO_BLOCK_AMAZON_ACTIVITY(block));

    // 0 stars is not a valid number of stars for an Amazon review
    if (review_rating > 0) {
        for (i = RATING_STARS_COUNT; i > review_rating ; --i) {
            g_object_set(block_amazon_activity->rating_stars[i-1],
                         "image-name", "rating_star_blank",
                         NULL);
        }
    }

    update_visibility(block_amazon_activity);
}

static void
on_block_review_title_changed(HippoBlock *block,
                        GParamSpec *arg, /* null when first calling this */
                        void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    const char *review_title = NULL;
   
    if (block)
        review_title = hippo_block_amazon_activity_get_review_title(HIPPO_BLOCK_AMAZON_ACTIVITY(block));

    g_object_set(block_amazon_activity->review_title,
                 "text", review_title,
                 NULL);                        
    
    // we can update review details visibility only once when review rating is set, since
    // the rating is always expected to be there
}

static void
on_block_list_name_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when first calling this */
                           void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);

    set_list_link_text(block_amazon_activity);

    update_visibility(block_amazon_activity);
}

static void
on_block_list_link_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when first calling this */
                           void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    const char *list_link = NULL;

    if (block)
        list_link = hippo_block_amazon_activity_get_list_link(HIPPO_BLOCK_AMAZON_ACTIVITY(block));
            
    g_object_set(block_amazon_activity->list_link,
                 "url", list_link,
                 NULL);                        
}

static void
on_block_list_item_comment_changed(HippoBlock *block,
                                   GParamSpec *arg, /* null when first calling this */
                                   void       *data)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(data);
    
    set_list_item_comment_markup(block_amazon_activity);

    update_visibility(block_amazon_activity);
}

static void
hippo_canvas_block_amazon_activity_set_block(HippoCanvasBlock *canvas_block,
                                           HippoBlock       *block)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(canvas_block);

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_description_changed),
                                             canvas_block);                                             
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_chat_id_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_image_url_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_image_dimension_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_title_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_title_link_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_icon_url_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_review_rating_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_review_title_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_list_name_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_list_link_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_list_item_comment_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_amazon_activity_parent_class)->set_block(canvas_block, block);

    g_object_set(block_amazon_activity->quipper,
                 "block", canvas_block->block,
                 NULL);
    g_object_set(block_amazon_activity->last_message_preview,
                 "block", canvas_block->block,
                 NULL);
    g_object_set(block_amazon_activity->chat_preview,
                 "block", canvas_block->block,
                 NULL);
    
    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::description",
                         G_CALLBACK(on_block_description_changed),
                         canvas_block);                         
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::chat-id",
                         G_CALLBACK(on_block_chat_id_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::image-url",
                         G_CALLBACK(on_block_image_url_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::image-width",
                         G_CALLBACK(on_block_image_dimension_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::image-height",
                         G_CALLBACK(on_block_image_dimension_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title",
                         G_CALLBACK(on_block_title_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title-link",
                         G_CALLBACK(on_block_title_link_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::icon-url",
                         G_CALLBACK(on_block_icon_url_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::review-rating",
                         G_CALLBACK(on_block_review_rating_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::review-title",
                         G_CALLBACK(on_block_review_title_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::list-name",
                         G_CALLBACK(on_block_list_name_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::list-link",
                         G_CALLBACK(on_block_list_link_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::list-item-comment",
                         G_CALLBACK(on_block_list_item_comment_changed),
                         canvas_block);
    }

    on_user_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_description_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_chat_id_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_image_url_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_image_dimension_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_title_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_title_link_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_icon_url_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_review_rating_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_review_title_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_list_name_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_list_link_changed(canvas_block->block, NULL, block_amazon_activity);
    on_block_list_item_comment_changed(canvas_block->block, NULL, block_amazon_activity);
}

static void
hippo_canvas_block_amazon_activity_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;

    if (canvas_block->block == NULL)
        return;
        
    actions = hippo_canvas_block_get_actions(canvas_block);

    hippo_actions_open_url(actions, hippo_block_get_title_link(canvas_block->block));
}

static void
hippo_canvas_block_amazon_activity_stack_reason_changed(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(canvas_block);

    if (HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_amazon_activity_parent_class)->stack_reason_changed)
        HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_amazon_activity_parent_class)->stack_reason_changed(canvas_block);
    
    update_visibility(block_amazon_activity);
}

static void
hippo_canvas_block_amazon_activity_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_amazon_activity_parent_class)->expand(canvas_block);
    
    update_visibility(block_amazon_activity);
}

static void
hippo_canvas_block_amazon_activity_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockAmazonActivity *block_amazon_activity = HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_amazon_activity_parent_class)->unexpand(canvas_block);
    
    update_visibility(block_amazon_activity);    
}
