/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-block.h"
#include <string.h>

/* === HippoBlock implementation === */

static void     hippo_block_finalize             (GObject *object);

struct _HippoBlock {
    GObject parent;
    char   *guid;
    HippoBlockType type;
    GTime  update_time;
    gint64 server_timestamp;
    gint64 timestamp;
    gint64 clicked_timestamp;
    gint64 ignored_timestamp;
    int clicked_count;
    guint clicked : 1;
    guint ignored : 1;
};

struct _HippoBlockClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoBlock, hippo_block, G_TYPE_OBJECT);

enum {
    CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_block_init(HippoBlock *block)
{
}

static void
hippo_block_class_init(HippoBlockClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    signals[CHANGED] =
        g_signal_new ("changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
          
    object_class->finalize = hippo_block_finalize;
}

static void
hippo_block_finalize(GObject *object)
{
    HippoBlock *block = HIPPO_BLOCK(object);

    g_free(block->guid);

    G_OBJECT_CLASS(hippo_block_parent_class)->finalize(object); 
}

static void 
hippo_block_emit_changed(HippoBlock *block)
{
    g_signal_emit(block, signals[CHANGED], 0);
}

/* === HippoBlock exported API === */

HippoBlock*
hippo_block_new(const char *guid)
{
    HippoBlock *block = g_object_new(HIPPO_TYPE_BLOCK, NULL);
    
    block->guid = g_strdup(guid);
    
    return block;
}

const char*
hippo_block_get_guid(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);
    return block->guid;
}

GTime
hippo_block_get_update_time(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->update_time;
}

void
hippo_block_set_update_time(HippoBlock *block,
                            GTime       value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->update_time) {
        block->update_time = value;
        hippo_block_emit_changed(block);
    }
}

gint64
hippo_block_get_server_timestamp(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->server_timestamp;
}

void
hippo_block_set_server_timestamp (HippoBlock *block,
                                  gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->server_timestamp) {
        block->server_timestamp = value;
        hippo_block_emit_changed(block);
    }
}

gint64
hippo_block_get_timestamp(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->timestamp;
}

void
hippo_block_set_timestamp (HippoBlock *block,
                           gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->timestamp) {
        block->timestamp = value;
        hippo_block_emit_changed(block);
    }
}

gint64
hippo_block_get_clicked_timestamp (HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->clicked_timestamp;
}

void
hippo_block_set_clicked_timestamp (HippoBlock *block,
                                   gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->clicked_timestamp) {
        block->clicked_timestamp = value;
        hippo_block_emit_changed(block);
    }
}

gint64
hippo_block_get_ignored_timestamp (HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->ignored_timestamp;
}

void
hippo_block_set_ignored_timestamp (HippoBlock *block,
                                   gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->ignored_timestamp) {
        block->ignored_timestamp = value;
        hippo_block_emit_changed(block);
    }
}

gint64
hippo_block_get_sort_timestamp(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    if (block->ignored)
        return block->ignored_timestamp;
    else
        return block->timestamp;
}

int
hippo_block_get_clicked_count(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), 0);

    return block->clicked_count;
}

void
hippo_block_set_clicked_count(HippoBlock *block,
                              int         value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->clicked_count) {
        block->clicked_count = value;
        hippo_block_emit_changed(block);
    }
}

gboolean
hippo_block_get_clicked(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->clicked;
}

void
hippo_block_set_clicked(HippoBlock *block,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    value = value != FALSE;
    if (value != block->clicked) {
        block->clicked = value;
        hippo_block_emit_changed(block);
    }
}

gboolean
hippo_block_get_ignored(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->ignored;
}

void
hippo_block_set_ignored(HippoBlock *block,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->ignored) {
        block->ignored = value;
        hippo_block_emit_changed(block);
    }
}
