/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-block-account-question.h"
#include "hippo-common-internal.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-account-question.h"
#include "hippo-canvas-url-link.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-timestamp.h>
#include <hippo/hippo-canvas-widgets.h>

static void      hippo_canvas_block_account_question_init                (HippoCanvasBlockAccountQuestion       *block);
static void      hippo_canvas_block_account_question_class_init          (HippoCanvasBlockAccountQuestionClass  *klass);
static void      hippo_canvas_block_account_question_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_account_question_dispose             (GObject                *object);
static void      hippo_canvas_block_account_question_finalize            (GObject                *object);

static void hippo_canvas_block_account_question_set_property (GObject      *object,
                                                              guint         prop_id,
                                                              const GValue *value,
                                                              GParamSpec   *pspec);
static void hippo_canvas_block_account_question_get_property (GObject      *object,
                                                              guint         prop_id,
                                                              GValue       *value,
                                                              GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_account_question_append_content_items (HippoCanvasBlock *canvas_block,
                                                                      HippoCanvasBox   *parent_box);
static void hippo_canvas_block_account_question_append_right_items   (HippoCanvasBlock *canvas_block,
                                                                      HippoCanvasBox   *parent_box);
static void hippo_canvas_block_account_question_set_block       (HippoCanvasBlock *canvas_block,
                                                                 HippoBlock       *block);
static void hippo_canvas_block_account_question_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_account_question_unexpand (HippoCanvasBlock *canvas_block);

/* Internals */
static void hippo_canvas_block_account_question_update_visibility(HippoCanvasBlockAccountQuestion *block_account_question);

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

struct _HippoCanvasBlockAccountQuestion {
    HippoCanvasBlock parent;

    HippoCanvasItem *description_item;
    HippoCanvasItem *read_more_item;
    HippoCanvasBox *timestamp_parent;
    HippoCanvasItem *timestamp_item;
    
    HippoCanvasBox *buttons_box;
};

struct _HippoCanvasBlockAccountQuestionClass {
    HippoCanvasBlockClass parent_class;

};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockAccountQuestion, hippo_canvas_block_account_question, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_account_question_iface_init));

static void
hippo_canvas_block_account_question_init(HippoCanvasBlockAccountQuestion *block_account_question)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_account_question);

    block->required_type = HIPPO_BLOCK_TYPE_ACCOUNT_QUESTION;
    block->expandable = FALSE;
    block->message_block = TRUE;
    block->linkify_title = FALSE;
    block->skip_lock = TRUE;
    block->skip_standard_right = TRUE;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_account_question_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_account_question_class_init(HippoCanvasBlockAccountQuestionClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_account_question_set_property;
    object_class->get_property = hippo_canvas_block_account_question_get_property;

    object_class->dispose = hippo_canvas_block_account_question_dispose;
    object_class->finalize = hippo_canvas_block_account_question_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_account_question_append_content_items;
    canvas_block_class->append_right_items = hippo_canvas_block_account_question_append_right_items;
    canvas_block_class->set_block = hippo_canvas_block_account_question_set_block;
    canvas_block_class->expand = hippo_canvas_block_account_question_expand;
    canvas_block_class->unexpand = hippo_canvas_block_account_question_unexpand;
}

static void
hippo_canvas_block_account_question_dispose(GObject *object)
{
    HippoCanvasBlockAccountQuestion *block_account_question;

    block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(object);

    G_OBJECT_CLASS(hippo_canvas_block_account_question_parent_class)->dispose(object);
}

static void
hippo_canvas_block_account_question_finalize(GObject *object)
{
    /* HippoCanvasBlockAccountQuestion *block = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(object); */

    G_OBJECT_CLASS(hippo_canvas_block_account_question_parent_class)->finalize(object);
}

static void
hippo_canvas_block_account_question_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasBlockAccountQuestion *block_account_question;

    block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_account_question_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasBlockAccountQuestion *block_account_question;

    block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_account_question_append_content_items (HippoCanvasBlock *block,
                                                          HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockAccountQuestion *block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(block);
    HippoCanvasBox *box;

    block_account_question->description_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                            "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                                                            "xalign", HIPPO_ALIGNMENT_START,
                                                            "padding-top", 2,
                                                            NULL);
    hippo_canvas_box_append(parent_box, block_account_question->description_item, 0);

    /* Create a box to control the expansion of the "Read more" link */
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(box), 0);

    block_account_question->read_more_item = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                                          "actions", block->actions,
                                                          "text", "Read more",
                                                          "tooltip", "More information about this",
                                                          "xalign", HIPPO_ALIGNMENT_START,
                                                          NULL);
    hippo_canvas_box_append(box, block_account_question->read_more_item, 0);

    block_account_question->buttons_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                                       "xalign", HIPPO_ALIGNMENT_CENTER,
                                                       "spacing", 20,
                                                       NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(block_account_question->buttons_box), 0);

    hippo_canvas_block_account_question_update_visibility(block_account_question);
}

static void
hippo_canvas_block_account_question_append_right_items (HippoCanvasBlock *block,
                                                        HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockAccountQuestion *block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(block);

    block_account_question->timestamp_parent = parent_box;
    block_account_question->timestamp_item = g_object_new(HIPPO_TYPE_CANVAS_TIMESTAMP,
                                                          "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                                                          "actions", block->actions,
                                                          NULL);
    hippo_canvas_box_append(parent_box, block_account_question->timestamp_item, HIPPO_PACK_FLOAT_RIGHT);
    hippo_canvas_box_set_child_visible(parent_box, block_account_question->timestamp_item, FALSE);
}

static void
on_button_activated(HippoCanvasItem                 *item,
                    HippoCanvasBlockAccountQuestion *block_account_question)
{
    const char *response = g_object_get_data(G_OBJECT(item), "response");
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_account_question);
    const char *block_id = hippo_block_get_guid(block->block);

    hippo_actions_send_account_question_response(block->actions, block_id, response);
}

static void
disconnect_buttons(HippoCanvasBlockAccountQuestion *block_account_question)
{
    GList *children;
    GList *l;
    
    children = hippo_canvas_box_get_children(block_account_question->buttons_box);
    for (l = children; l; l = l->next) {
        HippoCanvasItem *button_item = l->data;

        g_signal_handlers_disconnect_by_func(button_item,
                                             (void *)on_button_activated,
                                             block_account_question);
    }

    hippo_canvas_box_remove_all(block_account_question->buttons_box);
}

static void
on_title_changed(HippoBlock                      *block,
                 GParamSpec                      *arg, /* null when first calling this */
                 HippoCanvasBlockAccountQuestion *block_account_question)
{
    char *title = NULL;
    
    g_object_get(block,
                 "title", &title,
                 NULL);

    hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(block_account_question), title, NULL, FALSE);

    g_free(title);
}

static void
on_description_changed(HippoBlock                      *block,
                       GParamSpec                      *arg, /* null when first calling this */
                       HippoCanvasBlockAccountQuestion *block_account_question)
{
    char *description = NULL;
    
    g_object_get(block,
                 "description", &description,
                 NULL);

    g_object_set(block_account_question->description_item,
                 "text", description,
                 NULL);

    g_free(description);
}

static void
on_buttons_changed(HippoBlock                      *block,
                   GParamSpec                      *arg, /* null when first calling this */
                   HippoCanvasBlockAccountQuestion *block_account_question)
{
    GSList *buttons = NULL;
    GSList *l;
    
    g_object_get(block,
                 "buttons", &buttons,
                 NULL);

    disconnect_buttons(block_account_question);
    
    for (l = buttons; l; l = l->next) {
        HippoAccountQuestionButton *button = l->data;
        HippoCanvasItem *button_item = hippo_canvas_button_new();
        const char *text = hippo_account_question_button_get_text(button);
        const char *response = hippo_account_question_button_get_response(button);
        
        g_object_set(button_item,
                     "text", text,
                     NULL);
        g_object_set_data_full(G_OBJECT(button_item),
                               "response", g_strdup(response),
                               (GDestroyNotify)g_free);
            g_signal_connect(button_item, "activated",
                             G_CALLBACK(on_button_activated), block_account_question);
            
            hippo_canvas_box_append(block_account_question->buttons_box, button_item, 0);
    }
}

static void
on_more_link_changed(HippoBlock                      *block,
                     GParamSpec                      *arg, /* null when first calling this */
                     HippoCanvasBlockAccountQuestion *block_account_question)
{
    char *more_link = NULL;
    
    g_object_get(block,
                 "more-link", &more_link,
                 NULL);

    g_object_set(block_account_question->read_more_item,
                 "url", more_link,
                 NULL);

    g_free(more_link);
}

static void
on_answer_changed(HippoBlock                      *block,
                  GParamSpec                      *arg, /* null when first calling this */
                  HippoCanvasBlockAccountQuestion *block_account_question)
{
    char *answer = NULL;
    
    g_object_get(block,
                 "answer", &answer,
                 NULL);

    /* We only show a timestamp if the user has answered the question */
    hippo_canvas_box_set_child_visible(block_account_question->timestamp_parent, block_account_question->timestamp_item, answer != NULL);

    g_free(answer);
}

static void
on_timestamp_changed(HippoBlock                      *block,
                     GParamSpec                      *arg, /* null when first calling this */
                     HippoCanvasBlockAccountQuestion *block_account_question)
{
    gint64 timestamp = 0;
    
    g_object_get(block,
                 "timestamp", &timestamp,
                 NULL);

    g_object_set(block_account_question->timestamp_item,
                 "time", (int)(timestamp / 1000),
                 NULL);
}

static void
hippo_canvas_block_account_question_set_block(HippoCanvasBlock *canvas_block,
                                              HippoBlock       *block)
{
    HippoCanvasBlockAccountQuestion *block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(canvas_block);

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        disconnect_buttons(block_account_question);
        
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_title_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_description_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_buttons_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_more_link_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_answer_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_timestamp_changed),
                                             canvas_block);
    }


    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_account_question_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(canvas_block->block, "notify::title",
                         G_CALLBACK(on_title_changed), block_account_question);
        g_signal_connect(canvas_block->block, "notify::description",
                         G_CALLBACK(on_description_changed), block_account_question);
        g_signal_connect(canvas_block->block, "notify::buttons",
                         G_CALLBACK(on_buttons_changed), block_account_question);
        g_signal_connect(canvas_block->block, "notify::more_link",
                         G_CALLBACK(on_more_link_changed), block_account_question);
        g_signal_connect(canvas_block->block, "notify::answer",
                         G_CALLBACK(on_answer_changed), block_account_question);
        g_signal_connect(canvas_block->block, "notify::timestamp",
                         G_CALLBACK(on_timestamp_changed), block_account_question);

        on_title_changed(block, NULL, block_account_question);
        on_description_changed(block, NULL, block_account_question);
        on_buttons_changed(block, NULL, block_account_question);
        on_more_link_changed(block, NULL, block_account_question);
        on_answer_changed(block, NULL, block_account_question);
        on_timestamp_changed(block, NULL, block_account_question);
    }
}

static void
hippo_canvas_block_account_question_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockAccountQuestion *block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_account_question_parent_class)->expand(canvas_block);

    hippo_canvas_block_account_question_update_visibility(block_account_question);
}

static void
hippo_canvas_block_account_question_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockAccountQuestion *block_account_question = HIPPO_CANVAS_BLOCK_ACCOUNT_QUESTION(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_account_question_parent_class)->unexpand(canvas_block);
    
    hippo_canvas_block_account_question_update_visibility(block_account_question);
}

static void
hippo_canvas_block_account_question_update_visibility(HippoCanvasBlockAccountQuestion *block_account_question)
{
#if 0
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_account_question);
#endif
}
