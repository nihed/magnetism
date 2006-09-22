/* HippoCanvasControl.cpp: canvas item to hold a Windows window
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include "HippoCanvasControl.h"
#include "HippoAbstractControl.h"

static void      hippo_canvas_control_init                (HippoCanvasControl       *control);
static void      hippo_canvas_control_class_init          (HippoCanvasControlClass  *klass);
static void      hippo_canvas_control_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_control_dispose             (GObject                *object);
static void      hippo_canvas_control_finalize            (GObject                *object);

static void hippo_canvas_control_set_property (GObject      *object,
                                               guint         prop_id,
                                               const GValue *value,
                                               GParamSpec   *pspec);
static void hippo_canvas_control_get_property (GObject      *object,
                                               guint         prop_id,
                                               GValue       *value,
                                               GParamSpec   *pspec);


/* Canvas item methods */
static void hippo_canvas_control_set_context (HippoCanvasItem    *item,
                                              HippoCanvasContext *context);
static void hippo_canvas_control_allocate    (HippoCanvasItem    *item,
                                              int                 width,
                                              int                 height);

/* Canvas box methods */
static void hippo_canvas_control_paint_below_children       (HippoCanvasBox  *box,
                                                             cairo_t         *cr);
static int  hippo_canvas_control_get_content_width_request  (HippoCanvasBox  *box);
static int  hippo_canvas_control_get_content_height_request (HippoCanvasBox  *box,
                                                             int              for_width);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_CONTROL
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasControl, hippo_canvas_control, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_control_iface_init));

static void
hippo_canvas_control_init(HippoCanvasControl *control)
{

}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_control_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = (HippoCanvasItemClass*) g_type_interface_peek_parent(item_class);

    item_class->set_context = hippo_canvas_control_set_context;
    item_class->allocate = hippo_canvas_control_allocate;
}

static void
hippo_canvas_control_class_init(HippoCanvasControlClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    object_class->set_property = hippo_canvas_control_set_property;
    object_class->get_property = hippo_canvas_control_get_property;

    object_class->dispose = hippo_canvas_control_dispose;
    object_class->finalize = hippo_canvas_control_finalize;

    box_class->paint_below_children = hippo_canvas_control_paint_below_children;
    box_class->get_content_width_request = hippo_canvas_control_get_content_width_request;
    box_class->get_content_height_request = hippo_canvas_control_get_content_height_request;

    g_object_class_install_property(object_class,
                                    PROP_CONTROL,
                                    g_param_spec_pointer("control",
                                                         "Control",
                                                         "Control to put in the canvas item",
                                                         GParamFlags(G_PARAM_READABLE | G_PARAM_WRITABLE)));
}

static void
hippo_canvas_control_dispose(GObject *object)
{
    HippoCanvasControl *control = HIPPO_CANVAS_CONTROL(object);

    if (control->control) {
        control->control->Release();
        control->control = NULL;
        g_object_notify(object, "control");
    }

    G_OBJECT_CLASS(hippo_canvas_control_parent_class)->dispose(object);
}

static void
hippo_canvas_control_finalize(GObject *object)
{
    /* HippoCanvasControl *control = HIPPO_CANVAS_CONTROL(object); */


    G_OBJECT_CLASS(hippo_canvas_control_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_control_new(void)
{
    HippoCanvasControl *control;
    
    control = HIPPO_CANVAS_CONTROL(g_object_new(HIPPO_TYPE_CANVAS_CONTROL, NULL));

    return HIPPO_CANVAS_ITEM(control);
}

static void
hippo_canvas_control_set_property(GObject         *object,
                                 guint            prop_id,
                                 const GValue    *value,
                                 GParamSpec      *pspec)
{
    HippoCanvasControl *control;

    control = HIPPO_CANVAS_CONTROL(object);

    switch (prop_id) {
    case PROP_CONTROL:
        {
            HippoAbstractControl *w = (HippoAbstractControl*) g_value_get_pointer(value);
            if (control->control != w) {
                if (w) {
                    w->AddRef();
                }
                if (control->control) {
                    control->control->Release();
                }
                control->control = w;

                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(control));
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_control_get_property(GObject         *object,
                                  guint            prop_id,
                                  GValue          *value,
                                  GParamSpec      *pspec)
{
    HippoCanvasControl *control;

    control = HIPPO_CANVAS_CONTROL (object);

    switch (prop_id) {
    case PROP_CONTROL:
        g_value_set_pointer(value, control->control);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_control_set_context(HippoCanvasItem    *item,
                                HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    if (context == box->context)
        return;

    if (box->context)
        hippo_canvas_context_unregister_widget_item(box->context, item);

    /* chain up, which invalidates our old context */
    item_parent_class->set_context(item, context);

    if (box->context)
        hippo_canvas_context_register_widget_item(box->context, item);
}

static void
hippo_canvas_control_allocate(HippoCanvasItem *item,
                              int              width,
                              int              height)
{
    int x, y, w, h;
    int control_x, control_y;
    HippoCanvasControl *control;
    HippoCanvasBox *box;

    control = HIPPO_CANVAS_CONTROL(item);
    box = HIPPO_CANVAS_BOX(item);
    
    /* get the box set up */
    item_parent_class->allocate(item, width, height);

    /* Now do the allocation for the child control */
    if (control->control == NULL)
        return;
    
    control->control->getLastRequest(&w, &h);

    hippo_canvas_box_align(box, w, h, &x, &y, &w, &h);

    control_x = 0;
    control_y = 0;
    if (box->context)
        hippo_canvas_context_translate_to_widget(box->context, item,
                                                 &control_x, &control_y);

    control->control->moveResize(control_x + x, control_y + y, w, h);
}

static void
hippo_canvas_control_paint_below_children(HippoCanvasBox  *box,
                                          cairo_t         *cr)
{
    HippoCanvasControl *control = HIPPO_CANVAS_CONTROL(box);

    if (control->control == NULL)
        return;

    /* For now the controls all draw themselves */
}

static int
hippo_canvas_control_get_content_width_request(HippoCanvasBox *box)
{
    HippoCanvasControl *control = HIPPO_CANVAS_CONTROL(box);
    int children_width;
    int control_width;
    
    children_width = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_control_parent_class)->get_content_width_request(box);

    if (control->control) {
        control_width = control->control->getWidthRequest();
    } else {
        control_width = 0;
    }

    return MAX(control_width, children_width);
}

static int
hippo_canvas_control_get_content_height_request(HippoCanvasBox  *box,
                                                int              for_width)
{
    HippoCanvasControl *control = HIPPO_CANVAS_CONTROL(box);
    int children_height;
    int control_height;
    
    /* get height of children and the box padding */
    children_height = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_control_parent_class)->get_content_height_request(box,
                                                                                                            for_width);
    
    if (control->control) {
        control_height = control->control->getHeightRequest(for_width);
    } else {
        control_height = 0;
    }
    
    return MAX(control_height, children_height);
}
