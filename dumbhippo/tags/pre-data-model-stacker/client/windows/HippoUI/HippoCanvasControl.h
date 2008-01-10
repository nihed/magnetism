/* HippoCanvasControl.h: canvas item to hold a Windows window
 *
 * Copyright Red Hat, Inc. 2006
 **/

#ifndef __HIPPO_CANVAS_CONTROL_H__
#define __HIPPO_CANVAS_CONTROL_H__

class HippoAbstractControl;

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>

typedef struct _HippoCanvasControl      HippoCanvasControl;
typedef struct _HippoCanvasControlClass HippoCanvasControlClass;

#define HIPPO_TYPE_CANVAS_CONTROL              (hippo_canvas_control_get_type ())
#define HIPPO_CANVAS_CONTROL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_CONTROL, HippoCanvasControl))
#define HIPPO_CANVAS_CONTROL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_CONTROL, HippoCanvasControlClass))
#define HIPPO_IS_CANVAS_CONTROL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_CONTROL))
#define HIPPO_IS_CANVAS_CONTROL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_CONTROL))
#define HIPPO_CANVAS_CONTROL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_CONTROL, HippoCanvasControlClass))

struct _HippoCanvasControl {
    HippoCanvasBox box;
    HippoAbstractControl *control;
};

struct _HippoCanvasControlClass {
    HippoCanvasBoxClass parent_class;
};

GType            hippo_canvas_control_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_control_new    (void);

#endif /* __HIPPO_CANVAS_CONTROL_H__ */

