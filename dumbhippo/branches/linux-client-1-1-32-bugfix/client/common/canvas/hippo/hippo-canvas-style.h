/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_STYLE_H__
#define __HIPPO_CANVAS_STYLE_H__

#include "hippo-canvas-context.h"
#include "hippo-canvas-box.h"

/* HippoCanvasStyle contains styles that are inherited by children
 * from their container in a tree of canvas items.
 *
 * Right now this is an implementation detail of HippoCanvasBox, not
 * exposed in the HippoCanvasItem/HippoCanvasContext interfaces.
 */

G_BEGIN_DECLS

#define HIPPO_CANVAS_DEFAULT_COLOR 0x000000ff
#define HIPPO_CANVAS_DEFAULT_BACKGROUND_COLOR 0xffffff00

#define HIPPO_TYPE_CANVAS_STYLE              (hippo_canvas_style_get_type ())
#define HIPPO_CANVAS_STYLE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_STYLE, HippoCanvasStyle))
#define HIPPO_CANVAS_STYLE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_STYLE, HippoCanvasStyleClass))
#define HIPPO_IS_CANVAS_STYLE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_STYLE))
#define HIPPO_IS_CANVAS_STYLE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_STYLE))
#define HIPPO_CANVAS_STYLE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_STYLE, HippoCanvasStyleClass))

GType             hippo_canvas_style_get_type          (void) G_GNUC_CONST;

void hippo_canvas_style_affect_color     (HippoCanvasStyle     *style,
                                          guint32              *color_rgba_p);
void hippo_canvas_style_affect_font_desc (HippoCanvasStyle     *style,
                                          PangoFontDescription *font_desc);

G_END_DECLS

#endif /* __HIPPO_CANVAS_STYLE_H__ */
