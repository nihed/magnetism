/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-post.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-post.h"
#include "hippo-canvas-quipper.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-gradient.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-message-preview.h>
#include <hippo/hippo-canvas-chat-preview.h>

static void      hippo_canvas_block_post_init                (HippoCanvasBlockPost       *block);
static void      hippo_canvas_block_post_class_init          (HippoCanvasBlockPostClass  *klass);
static void      hippo_canvas_block_post_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_post_dispose             (GObject                *object);
static void      hippo_canvas_block_post_finalize            (GObject                *object);

static void hippo_canvas_block_post_set_property (GObject      *object,
                                                  guint         prop_id,
                                                  const GValue *value,
                                                  GParamSpec   *pspec);
static void hippo_canvas_block_post_get_property (GObject      *object,
                                                  guint         prop_id,
                                                  GValue       *value,
                                                  GParamSpec   *pspec);

/* Canvas item methods */
static void     hippo_canvas_block_post_paint              (HippoCanvasItem *item,
                                                            cairo_t         *cr,
                                                            HippoRectangle  *damaged_box);

/* Canvas block methods */
static void hippo_canvas_block_post_append_content_items (HippoCanvasBlock *canvas_block,
                                                          HippoCanvasBox   *parent_box);
static void hippo_canvas_block_post_set_block       (HippoCanvasBlock *canvas_block,
                                                     HippoBlock       *block);

static void hippo_canvas_block_post_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_post_clicked_count_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_post_significant_clicked_count_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_post_stack_reason_changed (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_post_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_post_unexpand (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_post_hush     (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_post_unhush   (HippoCanvasBlock *canvas_block);

/* Our own methods */
static void hippo_canvas_block_post_set_post (HippoCanvasBlockPost *canvas_block_post,
                                              HippoPost            *post);

static void hippo_canvas_block_post_update_visibility(HippoCanvasBlockPost *block_post);

/* Callbacks */
#if 0
static void on_faves_activated                 (HippoCanvasItem      *button_or_link,
                                                HippoCanvasBlockPost *canvas_block_post);
#endif


struct _HippoCanvasBlockPost {
    HippoCanvasBlock canvas_block;
    HippoPost *post;
    HippoCanvasItem *description_item;
    HippoCanvasItem *reason_item;
    HippoCanvasItem *quipper;
    HippoCanvasItem *single_message_preview;
    HippoCanvasItem *clicked_count_item;
    HippoCanvasItem *details_box;
    HippoCanvasItem *chat_preview;
    HippoCanvasItem *faves_link;

    guint have_messages : 1;
};

struct _HippoCanvasBlockPostClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockPost, hippo_canvas_block_post, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_post_iface_init));

static void
hippo_canvas_block_post_init(HippoCanvasBlockPost *block_post)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_post);
    
    block->required_type = HIPPO_BLOCK_TYPE_POST;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_post_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_block_post_paint;
}

static void
hippo_canvas_block_post_class_init(HippoCanvasBlockPostClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);
    
    object_class->set_property = hippo_canvas_block_post_set_property;
    object_class->get_property = hippo_canvas_block_post_get_property;
    
    object_class->dispose = hippo_canvas_block_post_dispose;
    object_class->finalize = hippo_canvas_block_post_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_post_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_post_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_post_title_activated;
    canvas_block_class->clicked_count_changed = hippo_canvas_block_post_clicked_count_changed;
    canvas_block_class->significant_clicked_count_changed = hippo_canvas_block_post_significant_clicked_count_changed;
    canvas_block_class->stack_reason_changed = hippo_canvas_block_post_stack_reason_changed;
    canvas_block_class->expand = hippo_canvas_block_post_expand;
    canvas_block_class->unexpand = hippo_canvas_block_post_unexpand;
    canvas_block_class->hush = hippo_canvas_block_post_hush;
    canvas_block_class->unhush = hippo_canvas_block_post_unhush;
}

static void
hippo_canvas_block_post_dispose(GObject *object)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(object);

    hippo_canvas_block_post_set_post(block_post, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_post_parent_class)->dispose(object);
}

static void
hippo_canvas_block_post_finalize(GObject *object)
{
    /* HippoCanvasBlockPost *block = HIPPO_CANVAS_BLOCK_POST(object); */

    G_OBJECT_CLASS(hippo_canvas_block_post_parent_class)->finalize(object);
}

static void
hippo_canvas_block_post_set_property(GObject         *object,
                                     guint            prop_id,
                                     const GValue    *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasBlockPost *block_post;

    block_post = HIPPO_CANVAS_BLOCK_POST(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_post_get_property(GObject         *object,
                                     guint            prop_id,
                                     GValue          *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasBlockPost *block_post;

    block_post = HIPPO_CANVAS_BLOCK_POST (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_post_append_content_items (HippoCanvasBlock *block,
                                              HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(block);

    /* Skip this for now; with feed posts it's just confusing, we need to be able to give
     * the feed name for those and say "Web Swarm" only for manual shares
     */
    /* hippo_canvas_block_set_heading(block, _("Web Swarm")); */
    
    block_post->reason_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                           "xalign", HIPPO_ALIGNMENT_START,
                                           "text", NULL,
                                           "font", "Italic 11px",
                                           NULL);
    hippo_canvas_box_append(parent_box, block_post->reason_item, 0);
    hippo_canvas_box_set_child_visible(parent_box,
                                       block_post->reason_item,
                                       FALSE);

    block_post->description_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                                "xalign", HIPPO_ALIGNMENT_START,
                                                "yalign", HIPPO_ALIGNMENT_START,
                                                "text", NULL,
                                                NULL);
    hippo_canvas_box_append(parent_box, block_post->description_item, 0);

    block_post->quipper = g_object_new(HIPPO_TYPE_CANVAS_QUIPPER,
                                       "actions", hippo_canvas_block_get_actions(block),
                                       NULL);
    hippo_canvas_box_append(parent_box, block_post->quipper, 0);
    hippo_canvas_box_set_child_visible(parent_box,
                                       block_post->quipper,
                                       FALSE); /* no messages yet */

    block_post->single_message_preview = g_object_new(HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW,
                                                      "actions", hippo_canvas_block_get_actions(block),
                                                      NULL);
    hippo_canvas_box_append(parent_box, block_post->single_message_preview, 0);
    hippo_canvas_box_set_child_visible(parent_box,
                                       block_post->single_message_preview,
                                       FALSE); /* no messages yet */

    block_post->details_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                           "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                                           NULL);
    hippo_canvas_box_append(parent_box, block_post->details_box, 0);
    hippo_canvas_box_set_child_visible(parent_box,
                                       block_post->details_box,
                                       FALSE); /* not expanded at first */
    
    block_post->clicked_count_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                  "text", NULL,
                                                  NULL);
#if 0
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block_post->details_box), block_post->clicked_count_item, 0);
#endif

#if 0
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", " | ",
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block_post->details_box), item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text", "Add to Faves",
                        "color-cascade", HIPPO_CASCADE_MODE_NONE,
                        "tooltip", "Save this item in your favorites list",
                        NULL);
    block_post->faves_link = item;
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block_post->details_box), item, 0);

    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_faves_activated), block_post);
#endif

    block_post->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                            "actions", hippo_canvas_block_get_actions(block),
                                            "padding-top", 8,
                                            NULL);
    hippo_canvas_box_append(parent_box,
                            block_post->chat_preview, 0);
    hippo_canvas_box_set_child_visible(parent_box,
                                       block_post->chat_preview,
                                       FALSE); /* not expanded at first */
}

static void
hippo_canvas_block_post_paint(HippoCanvasItem *item,
                              cairo_t         *cr,
                              HippoRectangle  *damaged_box)
{
    /* HippoCanvasBlockPost *block = HIPPO_CANVAS_BLOCK_POST(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr, damaged_box);
}

static void
on_block_post_post_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when first calling this */
                           void       *data)
{
    HippoCanvasBlockPost *canvas_block_post = HIPPO_CANVAS_BLOCK_POST(data);
    HippoPost *post;

    post = NULL;
    g_object_get(G_OBJECT(block),
                 "post", &post,
                 NULL);

    g_debug("canvas block post notified of new post %s",
            post ? hippo_post_get_guid(post) : "null");
    
    hippo_canvas_block_post_set_post(canvas_block_post, post);

    if (post)
        g_object_unref(post);
}

static void
on_block_post_recent_messages_changed(HippoBlock *block,
                                      GParamSpec *arg, /* null when first calling this */
                                      void       *data)
{
    HippoCanvasBlockPost *canvas_block_post = HIPPO_CANVAS_BLOCK_POST(data);
    HippoChatMessage *last_message;
    GSList *messages;

    last_message = NULL;
    messages = NULL;
    g_object_get(G_OBJECT(block),
                 "recent-messages", &messages,
                 NULL);
        
    if (messages)
        last_message = messages->data;
    
    g_object_set(G_OBJECT(canvas_block_post->chat_preview),
                 "recent-messages", messages,
                 NULL);
    g_object_set(G_OBJECT(canvas_block_post->single_message_preview),
                 "message", last_message,
                 NULL);
        
    canvas_block_post->have_messages = last_message != NULL;

    hippo_canvas_block_post_update_visibility(canvas_block_post);
}

static void
on_message_count_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         HippoCanvasBlockPost *canvas_block_post)
{
    int message_count = -1;

    g_object_get(block, "message-count", &message_count, NULL);
    
    g_object_set(G_OBJECT(canvas_block_post->chat_preview),
                 "message-count", message_count,
                 NULL);
}

static void
hippo_canvas_block_post_set_block(HippoCanvasBlock *canvas_block,
                                  HippoBlock       *block)
{
    /* g_debug("canvas-block-post set block %p", block); */
    
    if (block == canvas_block->block)
        return;
    
    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_post_post_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_post_recent_messages_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_message_count_changed),
                                             canvas_block);
    }
    
    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_post_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::post",
                         G_CALLBACK(on_block_post_post_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::recent-messages",
                         G_CALLBACK(on_block_post_recent_messages_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::message-count",
                         G_CALLBACK(on_message_count_changed),
                         canvas_block);
        
        on_block_post_post_changed(canvas_block->block, NULL, canvas_block);
        on_block_post_recent_messages_changed(canvas_block->block, NULL, canvas_block);
        on_message_count_changed(canvas_block->block, NULL,
                                 HIPPO_CANVAS_BLOCK_POST(canvas_block));
    }
}

static void
update_post(HippoCanvasBlockPost *canvas_block_post)
{
    HippoPost *post;
    
    post = canvas_block_post->post;

    if (post == NULL) {
        hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                     NULL, NULL, FALSE);
        hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                      NULL);
        hippo_canvas_block_set_sent_to(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                       NULL);
        hippo_canvas_block_set_original_age(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                            0);
        g_object_set(G_OBJECT(canvas_block_post->description_item),
                     "text", NULL,
                     NULL);
        g_object_set(G_OBJECT(canvas_block_post->chat_preview),
                     "chat-id", NULL,
                     NULL);
        g_object_set(G_OBJECT(canvas_block_post->quipper),
                     "chat-id", NULL,
                     "title", NULL,
                     NULL);
    } else {
        hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                     hippo_post_get_title(post),
                                     hippo_post_get_url(post), 
                                     hippo_post_get_have_viewed(post));
        hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                      hippo_post_get_sender(post));
        hippo_canvas_block_set_sent_to(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                       hippo_post_get_recipients(post));
        hippo_canvas_block_set_original_age(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                            hippo_post_get_date(post));
        g_object_set(G_OBJECT(canvas_block_post->description_item),
                     "text", hippo_post_get_description(post),
                     NULL);

        g_object_set(G_OBJECT(canvas_block_post->chat_preview),
                     "chat-id", hippo_post_get_guid(post),
                     NULL);
        g_object_set(G_OBJECT(canvas_block_post->quipper),
                     "chat-id", hippo_post_get_guid(post),
                     "title", hippo_post_get_title(post),
                     NULL);
    }
    
    hippo_canvas_block_post_update_visibility(canvas_block_post);
}

static void
on_post_changed(HippoPost *post,
                void      *data)
{
    HippoCanvasBlockPost *canvas_block_post = HIPPO_CANVAS_BLOCK_POST(data);

    g_assert(post == canvas_block_post->post);
    
    update_post(canvas_block_post);
}

static void
hippo_canvas_block_post_set_post (HippoCanvasBlockPost *canvas_block_post,
                                  HippoPost            *post)
{
#if 0
    g_debug("setting canvas block post to %s from %s",
            post ? hippo_post_get_guid(post) : "null",
            canvas_block_post->post ? hippo_post_get_guid(canvas_block_post->post) :
            "null");
#endif
    
    if (post == canvas_block_post->post)
        return;
    
    if (canvas_block_post->post) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block_post->post),
                                             G_CALLBACK(on_post_changed),
                                             canvas_block_post);
        g_object_unref(canvas_block_post->post);
        canvas_block_post->post = NULL;
    }

    if (post != NULL) {
        g_object_ref(post);
        g_signal_connect(G_OBJECT(post),
                         "changed",
                         G_CALLBACK(on_post_changed),
                         canvas_block_post);
        canvas_block_post->post = post;
    }
    
    update_post(canvas_block_post);
}

static void
hippo_canvas_block_post_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoPost *post;

    if (canvas_block->block == NULL)
        return;
    
    actions = hippo_canvas_block_get_actions(canvas_block);

    post = NULL;
    g_object_get(G_OBJECT(canvas_block->block),
                 "post", &post,
                 NULL);

    if (post == NULL)
        return;
    
    hippo_actions_visit_post(actions, post);

    g_object_unref(post);
}

static void
hippo_canvas_block_post_clicked_count_changed (HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockPost *canvas_block_post = HIPPO_CANVAS_BLOCK_POST(canvas_block);
    char *s;
    
    s = g_strdup_printf(_("%d views"), hippo_block_get_clicked_count(canvas_block->block));
    
    g_object_set(G_OBJECT(canvas_block_post->clicked_count_item),
                 "text", s,
                 NULL);
    g_free(s);
}

static void
block_post_update_reason_item(HippoCanvasBlockPost *canvas_block_post)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(canvas_block_post);
    HippoStackReason stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    
    switch (stack_reason) {
    case HIPPO_STACK_VIEWER_COUNT:
        {
            int significant_count = hippo_block_get_significant_clicked_count(canvas_block->block);
            char *s;
            if (significant_count == 1)
                s = g_strdup(_("1 person has now viewed this post."));
            else
                s = g_strdup_printf(_("%d people have now viewed this post."), significant_count);
            g_object_set(G_OBJECT(canvas_block_post->reason_item),
                         "text", s,
                         NULL);
            g_free(s);
            break;
        }
    default:
        break;
    }

    hippo_canvas_block_post_update_visibility(canvas_block_post);
}

static void 
hippo_canvas_block_post_significant_clicked_count_changed(HippoCanvasBlock *canvas_block)
{
    block_post_update_reason_item(HIPPO_CANVAS_BLOCK_POST(canvas_block));
}

static void 
hippo_canvas_block_post_stack_reason_changed(HippoCanvasBlock *canvas_block)
{
    block_post_update_reason_item(HIPPO_CANVAS_BLOCK_POST(canvas_block));
}

static void
hippo_canvas_block_post_update_visibility(HippoCanvasBlockPost *block_post)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_post);
    HippoStackReason stack_reason;
    gboolean show_description;
    gboolean show_single_message;
    gboolean show_reason;

    if (canvas_block->block)
        stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    else
        stack_reason = HIPPO_STACK_BLOCK_UPDATE;

    /* the details box / chat preview /quipper  show iff. we are expanded
     */
    hippo_canvas_box_set_child_visible(canvas_block->main_box,
                                       block_post->details_box,
                                       canvas_block->expanded);

    hippo_canvas_box_set_child_visible(canvas_block->main_box,
                                       block_post->chat_preview,
                                       canvas_block->expanded);

    hippo_canvas_box_set_child_visible(canvas_block->main_box,
                                       block_post->quipper,
                                       canvas_block->expanded);

    /* The description is always visible when expanded, otherwise we sometimes
     * show a single line summary
     */
    g_object_set(G_OBJECT(block_post->description_item),
                 "size-mode", canvas_block->expanded ? HIPPO_CANVAS_SIZE_WRAP_WORD : HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                 NULL);
    
    show_single_message = !canvas_block->expanded && stack_reason == HIPPO_STACK_CHAT_MESSAGE && block_post->have_messages;
    show_reason = stack_reason == HIPPO_STACK_VIEWER_COUNT;
    show_description = canvas_block->expanded || (!show_single_message && !show_reason);

    hippo_canvas_box_set_child_visible(canvas_block->main_box,
                                       block_post->description_item,
                                       show_description);
    hippo_canvas_box_set_child_visible(canvas_block->main_box,
                                       block_post->reason_item,
                                       show_reason);
    hippo_canvas_box_set_child_visible(canvas_block->main_box,
                                       block_post->single_message_preview,
                                       show_single_message);
}

static void
hippo_canvas_block_post_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_post_parent_class)->expand(canvas_block);

    hippo_canvas_block_post_update_visibility(block_post);
}

static void
hippo_canvas_block_post_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_post_parent_class)->unexpand(canvas_block);
    
    hippo_canvas_block_post_update_visibility(block_post);
}


static void
hippo_canvas_block_post_hush(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_post_parent_class)->hush(canvas_block);

#if 0
    g_object_set(G_OBJECT(block_post->faves_link),
                 "color-cascade", HIPPO_CASCADE_MODE_INHERIT,
                 NULL);
#endif
    
    hippo_canvas_chat_preview_set_hushed(HIPPO_CANVAS_CHAT_PREVIEW(block_post->chat_preview),
                                         TRUE);
}

static void
hippo_canvas_block_post_unhush(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_post_parent_class)->unhush(canvas_block);

#if 0
    g_object_set(G_OBJECT(block_post->faves_link),
                 "color-cascade", HIPPO_CASCADE_MODE_NONE,
                 NULL);
#endif
    
    hippo_canvas_chat_preview_set_hushed(HIPPO_CANVAS_CHAT_PREVIEW(block_post->chat_preview),
                                         FALSE);
}

#if 0
static void
on_faves_activated(HippoCanvasItem      *button_or_link,
                   HippoCanvasBlockPost *canvas_block_post)
{
    HippoCanvasBlock *canvas_block;

    canvas_block = HIPPO_CANVAS_BLOCK(canvas_block_post);

    if (canvas_block->actions && canvas_block->block)
        hippo_actions_add_to_faves(canvas_block->actions, canvas_block->block);
}
#endif
