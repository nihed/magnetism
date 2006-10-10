/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_CHAT_PREVIEW_H__
#define __HIPPO_CANVAS_CHAT_PREVIEW_H__

/* A canvas item that displays a preview of an ongoing chat,
 * intended to be included in specific HippoCanvasBlock such as the
 * post and group blocks. May monitor a HippoChatRoom directly or
 * may just be a dumb display that the block sets info on, not sure
 * yet.
 */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-chat-room.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasChatPreview      HippoCanvasChatPreview;
typedef struct _HippoCanvasChatPreviewClass HippoCanvasChatPreviewClass;

#define HIPPO_TYPE_CANVAS_CHAT_PREVIEW              (hippo_canvas_chat_preview_get_type ())
#define HIPPO_CANVAS_CHAT_PREVIEW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_CHAT_PREVIEW, HippoCanvasChatPreview))
#define HIPPO_CANVAS_CHAT_PREVIEW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_CHAT_PREVIEW, HippoCanvasChatPreviewClass))
#define HIPPO_IS_CANVAS_CHAT_PREVIEW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_CHAT_PREVIEW))
#define HIPPO_IS_CANVAS_CHAT_PREVIEW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_CHAT_PREVIEW))
#define HIPPO_CANVAS_CHAT_PREVIEW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_CHAT_PREVIEW, HippoCanvasChatPreviewClass))

GType            hippo_canvas_chat_preview_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_chat_preview_new         (void);

void             hippo_canvas_chat_preview_set_hushed  (HippoCanvasChatPreview *chat_preview,
                                                        gboolean                value);

G_END_DECLS

#endif /* __HIPPO_CANVAS_CHAT_PREVIEW_H__ */
