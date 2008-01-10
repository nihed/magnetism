/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_QUIP_WINDOW_H__
#define __HIPPO_QUIP_WINDOW_H__

#include <hippo/hippo-data-cache.h>

G_BEGIN_DECLS

typedef struct _HippoQuipWindow      HippoQuipWindow;
typedef struct _HippoQuipWindowClass HippoQuipWindowClass;

#define HIPPO_TYPE_QUIP_WINDOW              (hippo_quip_window_get_type ())
#define HIPPO_QUIP_WINDOW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_QUIP_WINDOW, HippoQuipWindow))
#define HIPPO_QUIP_WINDOW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_QUIP_WINDOW, HippoQuipWindowClass))
#define HIPPO_IS_QUIP_WINDOW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_QUIP_WINDOW))
#define HIPPO_IS_QUIP_WINDOW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_QUIP_WINDOW))
#define HIPPO_QUIP_WINDOW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_QUIP_WINDOW, HippoQuipWindowClass))

GType            hippo_quip_window_get_type               (void) G_GNUC_CONST;

HippoQuipWindow* hippo_quip_window_new (HippoDataCache *data_cache);

void hippo_quip_window_set_chat      (HippoQuipWindow *quip_window,
                                      HippoChatKind    chat_kind,
                                      const char      *chat_id);
void hippo_quip_window_set_sentiment (HippoQuipWindow *quip_window,
                                      HippoSentiment   sentiment);
void hippo_quip_window_set_title     (HippoQuipWindow *quip_window,
                                      const char      *title);
void hippo_quip_window_show          (HippoQuipWindow *quip_window);

G_END_DECLS

#endif /* __HIPPO_QUIP_WINDOW_H__ */
