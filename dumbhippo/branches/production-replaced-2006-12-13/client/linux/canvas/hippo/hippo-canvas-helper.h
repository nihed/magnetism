/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_HELPER_H__
#define __HIPPO_CANVAS_HELPER_H__

/* A helper object for hooking up a canvas to a widget */

#include <gtk/gtkcontainer.h>
#include <hippo/hippo-canvas.h>
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasHelper      HippoCanvasHelper;
typedef struct _HippoCanvasHelperClass HippoCanvasHelperClass;

#define HIPPO_TYPE_CANVAS_HELPER              (hippo_canvas_helper_get_type ())
#define HIPPO_CANVAS_HELPER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_HELPER, HippoCanvasHelper))
#define HIPPO_CANVAS_HELPER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_HELPER, HippoCanvasHelperClass))
#define HIPPO_IS_CANVAS_HELPER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_HELPER))
#define HIPPO_IS_CANVAS_HELPER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_HELPER))
#define HIPPO_CANVAS_HELPER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_HELPER, HippoCanvasHelperClass))

GType        	 hippo_canvas_helper_get_type               (void) G_GNUC_CONST;

HippoCanvasHelper* hippo_canvas_helper_new (GtkContainer *base_container);
void         hippo_canvas_helper_set_root (HippoCanvasHelper *helper,
                                           HippoCanvasItem   *root);

void         hippo_canvas_helper_set_load_image_hook(HippoCanvasLoadImageHook hook);

/* Caller must chain up to GtkContainer::expose after calling */
gboolean  hippo_canvas_helper_expose_event        (HippoCanvasHelper *widget,
            	       	                           GdkEventExpose    *event);
void      hippo_canvas_helper_size_request        (HippoCanvasHelper *widget,
            	       	                           GtkRequisition    *requisition);
/* Caller should set widget->allocation and move windows before calling */
void      hippo_canvas_helper_size_allocate       (HippoCanvasHelper *widget,
            	       	                           GtkAllocation     *allocation);
gboolean  hippo_canvas_helper_button_press        (HippoCanvasHelper *widget,
            	       	                           GdkEventButton    *event);
gboolean  hippo_canvas_helper_button_release      (HippoCanvasHelper *widget,
            	       	                           GdkEventButton    *event);
gboolean  hippo_canvas_helper_enter_notify        (HippoCanvasHelper *widget,
            	       	                           GdkEventCrossing  *event);
gboolean  hippo_canvas_helper_leave_notify        (HippoCanvasHelper *widget,
            	       	                           GdkEventCrossing  *event);
gboolean  hippo_canvas_helper_motion_notify       (HippoCanvasHelper *widget,
            	       	                           GdkEventMotion    *event);

void  hippo_canvas_helper_realize           (HippoCanvasHelper    *widget);
/* Caller should chain up to GtkContainer::unmap after calling */
void  hippo_canvas_helper_unmap             (HippoCanvasHelper    *widget);
/* Caller should chain up to GtkContainer::hierarchy_changed after calling */
void  hippo_canvas_helper_hierarchy_changed (HippoCanvasHelper *helper,
                                             GtkWidget         *old_toplevel);

void  hippo_canvas_helper_add               (HippoCanvasHelper *helper,
                                             GtkWidget         *widget);
void  hippo_canvas_helper_remove            (HippoCanvasHelper *helper,
                                             GtkWidget         *widget);
void  hippo_canvas_helper_forall            (HippoCanvasHelper *helper,
                                             gboolean           include_internals,
                                             GtkCallback        callback,
                                             gpointer           callback_data);
GType hippo_canvas_helper_child_type        (HippoCanvasHelper *helper);

G_END_DECLS

#endif /* __HIPPO_CANVAS_HELPER_H__ */
