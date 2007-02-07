#ifndef __HIPPO_CANVAS_FILTER_AREA_H__
#define __HIPPO_CANVAS_FILTER_AREA_H__

/* Canvas item for the purple "base" of the stacker */

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasFilterArea      HippoCanvasFilterArea;
typedef struct _HippoCanvasFilterAreaClass HippoCanvasFilterAreaClass;

#define HIPPO_TYPE_CANVAS_FILTER_AREA              (hippo_canvas_filter_area_get_type ())
#define HIPPO_CANVAS_FILTER_AREA(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_FILTER_AREA, HippoCanvasFilterArea))
#define HIPPO_CANVAS_FILTER_AREA_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_FILTER_AREA, HippoCanvasFilterAreaClass))
#define HIPPO_IS_CANVAS_FILTER_AREA(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_FILTER_AREA))
#define HIPPO_IS_CANVAS_FILTER_AREA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_FILTER_AREA))
#define HIPPO_CANVAS_FILTER_AREA_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_FILTER_AREA, HippoCanvasFilterAreaClass))

GType            hippo_canvas_filter_area_get_type               (void) G_GNUC_CONST;

void             hippo_canvas_filter_area_set_nofeed_active      (HippoCanvasFilterArea *area,
                                                                  gboolean               active);
void             hippo_canvas_filter_area_set_noselfsource_active(HippoCanvasFilterArea *area,
                                                                  gboolean               active);                                                  

HippoCanvasItem* hippo_canvas_filter_area_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_FILTER_AREA_H__ */
