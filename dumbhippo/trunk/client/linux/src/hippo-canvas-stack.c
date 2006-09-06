/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-stack.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_stack_init                (HippoCanvasStack       *stack);
static void      hippo_canvas_stack_class_init          (HippoCanvasStackClass  *klass);
static void      hippo_canvas_stack_iface_init          (HippoCanvasItemClass   *item_class);
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
    PROP_0

};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasStack, hippo_canvas_stack, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_stack_iface_init));

static void
hippo_canvas_stack_init(HippoCanvasStack *stack)
{

}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_stack_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);


}

static void
hippo_canvas_stack_class_init(HippoCanvasStackClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_stack_set_property;
    object_class->get_property = hippo_canvas_stack_get_property;

    object_class->finalize = hippo_canvas_stack_finalize;
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
    HippoCanvasItem *item;

    HIPPO_CANVAS_BOX(stack)->background_color_rgba = 0xffffffff;

    item = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(stack), item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(stack), item, 0);
    
    return HIPPO_CANVAS_ITEM(stack);
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
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}
