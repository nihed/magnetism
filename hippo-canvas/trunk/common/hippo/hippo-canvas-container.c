/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-container.h"
#include "hippo-canvas-marshal.h"

static void     hippo_canvas_container_base_init (void                  *klass);

/* 
enum {

    LAST_SIGNAL
};
static int signals[LAST_SIGNAL];
*/

GType
hippo_canvas_container_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoCanvasContainerIface),
                hippo_canvas_container_base_init,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoCanvasContainer",
                                      &info, 0);
    }

    return type;
}

static void
hippo_canvas_container_base_init(void *klass)
{
    static gboolean initialized = FALSE;

    if (!initialized) {
        /* create signals in here */
        
        initialized = TRUE;
    }
}

gboolean
hippo_canvas_container_get_child_visible(HippoCanvasContainer        *container,
                                         HippoCanvasItem             *child)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTAINER(container), FALSE);
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(child), FALSE);

    return HIPPO_CANVAS_CONTAINER_GET_IFACE(container)->get_child_visible(container, child) != FALSE;
}

/**
 * hippo_canvas_container_set_child_visible:
 * @container: the container item
 * @child: child that is inside the container
 * @visible: %FALSE if child should be given a 0x0 allocation and therefore be invisible
 *
 * 
 * This is the equivalent of gtk_widget_show() and gtk_widget_hide() for #GtkWidget.
 * Unlike #GtkWidget, in #HippoCanvas visibility is a property of the container-child
 * pair rather than a property of the child itself. Thus, if you remove
 * a child from a container its visibility state is lost.
 *
 * Internally, invisible children are just allocated a 0x0 size; they are not
 * otherwise told they are invisible.
 * 
 * Making this a "child property" on the container instead of a flag on
 * #HippoCanvasItem is perhaps a little surprising, but
 * is consistent with e.g. having the allocation origin in the container
 * also. The general theme is that #HippoCanvasItem has minimal knowledge
 * of its context - doesn't know its origin coords, parent item,
 * or whether it will be painted at all. Which makes it easier to
 * implement canvas items and easier to use them in different/multiple
 * contexts, but makes containers harder and more complex.
 *
 * An implementation convenience of this approach is that the
 * Windows and Linux canvas widgets need not handle the visibility
 * of their root items.
 */
void
hippo_canvas_container_set_child_visible (HippoCanvasContainer        *container,
                                          HippoCanvasItem             *child,
                                          gboolean                     visible)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTAINER(container));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    HIPPO_CANVAS_CONTAINER_GET_IFACE(container)->set_child_visible(container, child, visible != FALSE);
}
