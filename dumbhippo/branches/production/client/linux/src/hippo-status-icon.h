/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_STATUS_ICON_H__
#define __HIPPO_STATUS_ICON_H__

#include <hippo/hippo-common.h>
#include <gtk/gtkstatusicon.h>

G_BEGIN_DECLS

typedef struct _HippoStatusIcon      HippoStatusIcon;
typedef struct _HippoStatusIconClass HippoStatusIconClass;

#define HIPPO_TYPE_STATUS_ICON              (hippo_status_icon_get_type ())
#define HIPPO_STATUS_ICON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_STATUS_ICON, HippoStatusIcon))
#define HIPPO_STATUS_ICON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_STATUS_ICON, HippoStatusIconClass))
#define HIPPO_IS_STATUS_ICON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_STATUS_ICON))
#define HIPPO_IS_STATUS_ICON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_STATUS_ICON))
#define HIPPO_STATUS_ICON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_STATUS_ICON, HippoStatusIconClass))

GType        	 hippo_status_icon_get_type               (void) G_GNUC_CONST;

HippoStatusIcon* hippo_status_icon_new                    (HippoDataCache *cache);

G_END_DECLS

#endif /* __HIPPO_STATUS_ICON_H__ */
