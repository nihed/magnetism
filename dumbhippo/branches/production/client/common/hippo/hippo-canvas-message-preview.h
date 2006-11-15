/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_MESSAGE_PREVIEW_H__
#define __HIPPO_CANVAS_MESSAGE_PREVIEW_H__

/* A canvas item that displays short version of a single chat item.
 *
 * This is exposed separate from HippoCanvasChatPreview so we can
 * use it inside HippoCanvasChatPreview, and also use it standalone
 * when we want to preview only a single line of chat.
 *
 * A different, and perhaps better way of doing things would be
 * to have HippoCanvasChatPreview have two modes - single line or
 * full and toggle a preview between those modes depending on the
 * state of the block.
 */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-chat-room.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasMessagePreview      HippoCanvasMessagePreview;
typedef struct _HippoCanvasMessagePreviewClass HippoCanvasMessagePreviewClass;

#define HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW              (hippo_canvas_message_preview_get_type ())
#define HIPPO_CANVAS_MESSAGE_PREVIEW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW, HippoCanvasMessagePreview))
#define HIPPO_CANVAS_MESSAGE_PREVIEW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW, HippoCanvasMessagePreviewClass))
#define HIPPO_IS_CANVAS_MESSAGE_PREVIEW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW))
#define HIPPO_IS_CANVAS_MESSAGE_PREVIEW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW))
#define HIPPO_CANVAS_MESSAGE_PREVIEW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW, HippoCanvasMessagePreviewClass))

GType            hippo_canvas_message_preview_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_MESSAGE_PREVIEW_H__ */
