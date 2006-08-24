/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-context.h"
#include <hippo/hippo-common-marshal.h>

static void     hippo_canvas_context_base_init (void                  *klass);

enum {
    LAST_SIGNAL
};
/* static int signals[LAST_SIGNAL]; */

GType
hippo_canvas_context_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoCanvasContextClass),
                hippo_canvas_context_base_init,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoCanvasContext",
                                      &info, 0);
    }
    
    return type;
}

static void
hippo_canvas_context_base_init(void *klass)
{
    static gboolean initialized = FALSE;

    if (!initialized) {
        /* create signals in here */

        initialized = TRUE;
    }
}

PangoLayout*
hippo_canvas_context_create_layout(HippoCanvasContext *context)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->create_layout(context);
}
