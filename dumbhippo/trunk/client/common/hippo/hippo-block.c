/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-block.h"
#include <string.h>

/* === HippoBlock implementation === */

static void     hippo_block_finalize             (GObject *object);

struct _HippoBlock {
    GObject parent;
    char   *guid;
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
