/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "hippo-canvas-layout.h"

GType
hippo_canvas_layout_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoCanvasLayoutIface),
                NULL /* base_init */,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoCanvasLayout",
                                      &info, 0);
    }

    return type;
}

void
hippo_canvas_layout_set_box(HippoCanvasLayout *layout,
                            HippoCanvasBox    *box)
{
    HippoCanvasLayoutIface *iface;
    
    g_return_if_fail(HIPPO_IS_CANVAS_LAYOUT(layout));

    iface = HIPPO_CANVAS_LAYOUT_GET_IFACE(layout);

    if (iface->set_box)
        HIPPO_CANVAS_LAYOUT_GET_IFACE(layout)->set_box(layout, box);
}

void
hippo_canvas_layout_get_width_request(HippoCanvasLayout *layout,
                                      int               *min_width_p,
                                      int               *natural_width_p)
{
    HippoCanvasLayoutIface *iface;
    
    g_return_if_fail(HIPPO_IS_CANVAS_LAYOUT(layout));
    
    iface = HIPPO_CANVAS_LAYOUT_GET_IFACE(layout);

    if (iface->get_width_request)
        iface->get_width_request(layout, min_width_p, natural_width_p);
    else {
        g_warning("HippoCanvasLayout implementor must implement get_width_request");
        if (min_width_p)
            *min_width_p = 0;
        if (natural_width_p)
            *natural_width_p = 0;
    }
}

void
hippo_canvas_layout_get_height_request (HippoCanvasLayout *layout,
                                        int                for_width,
                                        int               *min_height_p,
                                        int               *natural_height_p)
{
    HippoCanvasLayoutIface *iface;

    g_return_if_fail(HIPPO_IS_CANVAS_LAYOUT(layout));
    
    iface = HIPPO_CANVAS_LAYOUT_GET_IFACE(layout);

    if (iface->get_height_request)
        iface->get_height_request(layout, for_width, min_height_p, natural_height_p);
    else {
        g_warning("HippoCanvasLayout implementor must implement get_height_request");
        if (min_height_p)
            *min_height_p = 0;
        if (natural_height_p)
            *natural_height_p = 0;
    }
}

void
hippo_canvas_layout_allocate(HippoCanvasLayout *layout,
                             int                x,
                             int                y,
                             int                width,
                             int                height,
                             int                requested_width,
                             int                requested_height,
                             gboolean           origin_changed)
{
    HippoCanvasLayoutIface *iface;
    
    g_return_if_fail(HIPPO_IS_CANVAS_LAYOUT(layout));

    iface = HIPPO_CANVAS_LAYOUT_GET_IFACE(layout);
    
    if (iface->allocate)
        iface->allocate(layout,
                        x, y,
                        width, height,
                        requested_width, requested_height,
                        origin_changed);
    else
        g_warning("HippoCanvasLayout implementor must implement allocate");
}
