/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_WINDOW_WRAPPER_H__
#define __HIPPO_WINDOW_WRAPPER_H__

/* Implementation of HippoWindow for GTK+ */

#include <hippo/hippo-window.h>

G_BEGIN_DECLS

typedef struct _HippoWindowWrapper      HippoWindowWrapper;
typedef struct _HippoWindowWrapperClass HippoWindowWrapperClass;

#define HIPPO_TYPE_WINDOW_WRAPPER              (hippo_window_wrapper_get_type ())
#define HIPPO_WINDOW_WRAPPER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_WINDOW_WRAPPER, HippoWindowWrapper))
#define HIPPO_WINDOW_WRAPPER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_WINDOW_WRAPPER, HippoWindowWrapperClass))
#define HIPPO_IS_WINDOW_WRAPPER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_WINDOW_WRAPPER))
#define HIPPO_IS_WINDOW_WRAPPER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_WINDOW_WRAPPER))
#define HIPPO_WINDOW_WRAPPER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_WINDOW_WRAPPER, HippoWindowWrapperClass))

GType        	 hippo_window_wrapper_get_type               (void) G_GNUC_CONST;

HippoWindow* hippo_window_wrapper_new    (void);


G_END_DECLS

#endif /* __HIPPO_WINDOW_WRAPPER_H__ */
