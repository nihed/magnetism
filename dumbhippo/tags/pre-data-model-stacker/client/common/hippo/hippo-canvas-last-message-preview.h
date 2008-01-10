/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_LAST_MESSAGE_PREVIEW_H__
#define __HIPPO_CANVAS_LAST_MESSAGE_PREVIEW_H__

/* A canvas item that displays the most recent message for a block
 */

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasLastMessagePreview      HippoCanvasLastMessagePreview;
typedef struct _HippoCanvasLastMessagePreviewClass HippoCanvasLastMessagePreviewClass;

#define HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW              (hippo_canvas_last_message_preview_get_type ())
#define HIPPO_CANVAS_LAST_MESSAGE_PREVIEW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW, HippoCanvasLastMessagePreview))
#define HIPPO_CANVAS_LAST_MESSAGE_PREVIEW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW, HippoCanvasLastMessagePreviewClass))
#define HIPPO_IS_CANVAS_LAST_MESSAGE_PREVIEW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW))
#define HIPPO_IS_CANVAS_LAST_MESSAGE_PREVIEW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW))
#define HIPPO_CANVAS_LAST_MESSAGE_PREVIEW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW, HippoCanvasLastMessagePreviewClass))

GType            hippo_canvas_last_message_preview_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_LAST_MESSAGE_PREVIEW_H__ */
