/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CHAT_WINDOW_H__
#define __HIPPO_CHAT_WINDOW_H__

#include <hippo/hippo-common.h>
#include <gtk/gtkwindow.h>

G_BEGIN_DECLS

typedef struct _HippoChatWindow      HippoChatWindow;
typedef struct _HippoChatWindowClass HippoChatWindowClass;

#define HIPPO_TYPE_CHAT_WINDOW              (hippo_chat_window_get_type ())
#define HIPPO_CHAT_WINDOW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CHAT_WINDOW, HippoChatWindow))
#define HIPPO_CHAT_WINDOW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CHAT_WINDOW, HippoChatWindowClass))
#define HIPPO_IS_CHAT_WINDOW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CHAT_WINDOW))
#define HIPPO_IS_CHAT_WINDOW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CHAT_WINDOW))
#define HIPPO_CHAT_WINDOW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CHAT_WINDOW, HippoChatWindowClass))

GType        	 hippo_chat_window_get_type               (void) G_GNUC_CONST;

HippoChatWindow* hippo_chat_window_new                    (HippoDataCache *cache,
                                                           HippoChatRoom  *room);

HippoChatRoom*   hippo_chat_window_get_room               (HippoChatWindow *window);
HippoWindowState hippo_chat_window_get_state              (HippoChatWindow *window);

G_END_DECLS

#endif /* __HIPPO_CHAT_WINDOW_H__ */
