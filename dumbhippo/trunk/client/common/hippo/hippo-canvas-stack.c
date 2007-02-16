/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include "hippo-canvas-stack.h"
#include "hippo-canvas-block.h"
#include "hippo-block-music-person.h"
#include "hippo-block-music-chat.h"
#include <hippo/hippo-canvas-box.h>
#include "hippo-actions.h"

static void      hippo_canvas_stack_init                (HippoCanvasStack       *stack);
static void      hippo_canvas_stack_class_init          (HippoCanvasStackClass  *klass);
static void      hippo_canvas_stack_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_stack_dispose             (GObject                *object);
static void      hippo_canvas_stack_finalize            (GObject                *object);

static void hippo_canvas_stack_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_stack_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);

struct _HippoCanvasStack {
    HippoCanvasBox box;

    HippoActions *actions;
    gint64 min_timestamp;
    int max_blocks;
    gboolean pin_messages;
};

struct _HippoCanvasStackClass {
    HippoCanvasBoxClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_ACTIONS,
    PROP_MAX_BLOCKS,
    PROP_PIN_MESSAGES
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasStack, hippo_canvas_stack, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_stack_iface_init));

static void
hippo_canvas_stack_init(HippoCanvasStack *stack)
{

    stack->max_blocks = G_MAXINT;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_stack_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);


}

static void
hippo_canvas_stack_class_init(HippoCanvasStackClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_stack_set_property;
    object_class->get_property = hippo_canvas_stack_get_property;

    object_class->dispose = hippo_canvas_stack_dispose;
    object_class->finalize = hippo_canvas_stack_finalize;

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));

    g_object_class_install_property(object_class,
                                    PROP_MAX_BLOCKS,
                                    g_param_spec_int("max-blocks",
                                                     _("Max blocks"),
                                                     _("Maximum number of blocks to show, -1 for unset"),
                                                     0, G_MAXINT,
                                                     G_MAXINT,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_PIN_MESSAGES,
                                    g_param_spec_boolean("pin-messages",
                                                         _("Pin Messages"),
                                                         _("Whether to pin system messages to the top of the stack"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

}

static void
set_actions(HippoCanvasStack *stack,
            HippoActions     *actions)
{
    if (actions == stack->actions)
        return;
    
    if (stack->actions) {
        g_object_unref(stack->actions);
        stack->actions = NULL;
    }

    if (actions) {
        stack->actions = actions;
        g_object_ref(stack->actions);
    }

    g_object_notify(G_OBJECT(stack), "actions");
}

static void
hippo_canvas_stack_dispose(GObject *object)
{
    HippoCanvasStack *stack = HIPPO_CANVAS_STACK(object);

    set_actions(stack, NULL);

    G_OBJECT_CLASS(hippo_canvas_stack_parent_class)->finalize(object);
}

static void
hippo_canvas_stack_finalize(GObject *object)
{
    /* HippoCanvasStack *stack = HIPPO_CANVAS_STACK(object); */


    G_OBJECT_CLASS(hippo_canvas_stack_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_stack_new(void)
{
    HippoCanvasStack *stack = g_object_new(HIPPO_TYPE_CANVAS_STACK, NULL);

    HIPPO_CANVAS_BOX(stack)->background_color_rgba = 0xffffffff;
    
    return HIPPO_CANVAS_ITEM(stack);
}

static void
remove_extra_children(HippoCanvasStack *canvas_stack)
{
    if (canvas_stack->max_blocks < G_MAXINT) {
        GList *children;
        int count;
        children = hippo_canvas_box_get_children(HIPPO_CANVAS_BOX(canvas_stack));
        count = g_list_length(children);
        if (count > canvas_stack->max_blocks)
            children = g_list_reverse(children);
        while (count > canvas_stack->max_blocks) {
            g_assert(children != NULL);
            hippo_canvas_box_remove(HIPPO_CANVAS_BOX(canvas_stack), children->data);
            children = g_list_remove(children, children->data);
            --count;
        }
        g_list_free(children);
    }
}

static void
hippo_canvas_stack_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasStack *stack;

    stack = HIPPO_CANVAS_STACK(object);

    switch (prop_id) {
    case PROP_ACTIONS:
        set_actions(stack,
                    (HippoActions*) g_value_get_object(value));
        break;
    case PROP_MAX_BLOCKS:
        {
            int new_max = g_value_get_int(value);
            if (new_max != stack->max_blocks) {
                stack->max_blocks = new_max;
                remove_extra_children(stack);
            }
        }
        break;
    case PROP_PIN_MESSAGES:
        {
            gboolean new_pin_messages = g_value_get_boolean(value);
            if (new_pin_messages != stack->pin_messages) {
                stack->pin_messages = new_pin_messages;
                /* Theoretically, we need to resort here; practically
                 * speaking, we don't change this property on the fly
                 */
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_stack_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasStack *stack;

    stack = HIPPO_CANVAS_STACK (object);

    switch (prop_id) {
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) stack->actions);
        break;
    case PROP_MAX_BLOCKS:
        g_value_set_int(value, stack->max_blocks);
        break;
    case PROP_PIN_MESSAGES:
        g_value_set_boolean(value, stack->pin_messages);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

typedef struct {
    HippoBlock *block;
    HippoCanvasItem *found;
} FindItemData;

static void
foreach_find_item(HippoCanvasItem *child,
                  void            *data)
{
    FindItemData *fid = data;
    HippoBlock *child_block = NULL;

    if (fid->found)
        return;
    
    g_object_get(G_OBJECT(child), "block", &child_block, NULL);
    if (child_block == fid->block) {
        fid->found = child;
    }

    if (child_block)
        g_object_unref(child_block);
}

static HippoCanvasItem*
find_block_item(HippoCanvasStack *canvas_stack,
                HippoBlock       *block)
{
    FindItemData fid;

    fid.block = block;
    fid.found = NULL;
    
    hippo_canvas_box_foreach(HIPPO_CANVAS_BOX(canvas_stack),
                             foreach_find_item,
                             &fid);

    return fid.found;
}

static int
canvas_block_compare(HippoCanvasItem *a,
                     HippoCanvasItem *b,
                     void            *data)
{
    HippoCanvasStack *stack = data;
    HippoBlock *block_a;
    HippoBlock *block_b;
    int result;

    g_object_get(G_OBJECT(a), "block", &block_a, NULL);
    g_object_get(G_OBJECT(b), "block", &block_b, NULL);

    if (stack->pin_messages && block_a->pinned != block_b->pinned) {
        if (block_a->pinned)
            result = -1;
        else
            result = 1;
    } else {
        result = hippo_block_compare_newest_first(block_a, block_b);
    }

    if (block_a)
        g_object_unref(block_a);
    if (block_b)
        g_object_unref(block_b);

    return result;
}

void
hippo_canvas_stack_add_block(HippoCanvasStack *canvas_stack,
                             HippoBlock       *block,
                             gboolean          visible)
{
    HippoCanvasItem *item;
    gint64 sort_timestamp;

    sort_timestamp = hippo_block_get_sort_timestamp(block);
    if (sort_timestamp < canvas_stack->min_timestamp)
        return;

    item = find_block_item(canvas_stack, block);

    if (item != NULL) {
        g_object_ref(item);
        hippo_canvas_box_remove(HIPPO_CANVAS_BOX(canvas_stack), item);
    } else {
        item = hippo_canvas_block_new(hippo_block_get_block_type(block),
                                      canvas_stack->actions);
        g_object_ref(item);
    }

    g_object_set(G_OBJECT(item), "block", block, NULL);
    hippo_canvas_box_insert_sorted(HIPPO_CANVAS_BOX(canvas_stack), item, 0,
                                   canvas_block_compare, canvas_stack);

    hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(canvas_stack), item, visible);
    remove_extra_children(canvas_stack);
    
    g_object_unref(item);

    /* Updating possibly visible blocks at this point doesn't take
     * into account the possibility of a block changing in such a way
     * to cause other blocks to be hidden without being restacked;
     * (for example, a MUSIC_CHAT block changing play_id to match
     * a MUSIC_PERSON block further down the stack). This is unlikely
     * to happen currently.
     */
    hippo_canvas_stack_update_hidden_blocks(canvas_stack);
}

void
hippo_canvas_stack_remove_block(HippoCanvasStack *canvas_stack,
                                HippoBlock       *block)
{
    HippoCanvasItem *item;

    item = find_block_item(canvas_stack, block);

    if (item != NULL) {
        g_object_set(G_OBJECT(item),
                     "block", NULL,
                     NULL);
        hippo_canvas_box_remove(HIPPO_CANVAS_BOX(canvas_stack), item);
    }
}

typedef struct {
    HippoCanvasStack *canvas_stack;
    GHashTable *chatted;
} UpdateHiddenData;

static void
foreach_update_hidden(HippoCanvasItem *child,
                      void            *data)
{
    UpdateHiddenData *uhd = data;
    HippoBlock *child_block = NULL;

    g_object_get(G_OBJECT(child), "block", &child_block, NULL);

    if (HIPPO_IS_BLOCK_MUSIC_CHAT(child_block)) {
        const char *play_id = NULL;
        HippoTrack *track = NULL;
        g_object_get(child_block, "track", &track, NULL);

        if (track) {
            play_id = hippo_track_get_play_id(track);
            g_object_unref(track);
        }

        if (play_id)
            g_hash_table_insert(uhd->chatted, (void *)play_id, GUINT_TO_POINTER(1));
        
    } else if (HIPPO_IS_BLOCK_MUSIC_PERSON(child_block)) {
        const char *play_id = NULL;
        GSList *track_history = NULL;
        gboolean seen_track = FALSE;
        
        g_object_get(child_block, "track_history", &track_history, NULL);

        if (track_history) {
            HippoTrack *track = track_history->data;
            play_id = hippo_track_get_play_id(track);
        }

        if (play_id)
            seen_track = g_hash_table_lookup(uhd->chatted, play_id) != NULL;

        hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(uhd->canvas_stack), 
                                           child, !seen_track);
    }

    if (child_block)
        g_object_unref(child_block);
}

void 
hippo_canvas_stack_update_hidden_blocks(HippoCanvasStack *canvas_stack)
{
    UpdateHiddenData uhd;

    /* The algorithm here isn't efficient; we walk through the entire
     * stack each time a block is added or restacked and accumulate
     * the set of music tracks that have been chatted on. Any music person
     * block about these tracks that occurs after the music chat block
     * is suppressed. This makes initial construction of the stack
     * an O(N^2) operation, though the individual walks are pretty
     * fast.
     *
     * A more efficient thing to do would be to keep a permanent list
     * of chatted track ids for the stack, and update visibility only
     * when a new track is added to that list or a music person block
     * is restacked (and thus has a new play id).
     */
    uhd.canvas_stack = canvas_stack;
    uhd.chatted = g_hash_table_new(g_str_hash, g_str_equal);
    
    hippo_canvas_box_foreach(HIPPO_CANVAS_BOX(canvas_stack),
                             foreach_update_hidden,
                             &uhd);
    
    g_hash_table_destroy(uhd.chatted);
}

static void
foreach_update_min_timestamp(HippoCanvasItem *child,
                             void            *data)
{
    HippoCanvasStack *stack = data;
    HippoBlock *child_block = NULL;
    gint64 sort_timestamp;

    g_object_get(G_OBJECT(child), "block", &child_block, NULL);

    sort_timestamp = hippo_block_get_sort_timestamp(child_block);
    if (sort_timestamp < stack->min_timestamp)
        hippo_canvas_box_remove(HIPPO_CANVAS_BOX(stack), child);

    g_object_unref(child_block);
}

void
hippo_canvas_stack_set_min_timestamp(HippoCanvasStack *canvas_stack,
                                     gint64            min_timestamp)
{
    if (canvas_stack->min_timestamp == min_timestamp)
        return;

    canvas_stack->min_timestamp = min_timestamp;

    hippo_canvas_box_foreach(HIPPO_CANVAS_BOX(canvas_stack),
                             foreach_update_min_timestamp,
                             canvas_stack);
}
