/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_UI_H__
#define __HIPPO_UI_H__

#include <glib-object.h>
#include <hippo/hippo-basics.h>
#include "main.h"
#include "hippo-dbus-server.h"

G_BEGIN_DECLS

typedef struct HippoUI HippoUI;

HippoUI*         hippo_ui_new                  (HippoDataCache           *cache,
                                                HippoDBus                *dbus);
void             hippo_ui_free                 (HippoUI                  *ui);
void             hippo_ui_show                 (HippoUI                  *ui);
void             hippo_ui_get_screen_info      (HippoUI                  *ui,
                                                HippoRectangle           *monitor_rect_p,
                                                HippoRectangle           *tray_icon_rect_p,
                                                HippoOrientation         *tray_icon_orientation_p);
gboolean         hippo_ui_get_pointer_position (HippoUI                  *ui,
                                                int                      *x_p,
                                                int                      *y_p);
void             hippo_ui_set_idle             (HippoUI                  *ui,
                                                gboolean                  idle);
void             hippo_ui_show_about           (HippoUI                  *ui);
void             hippo_ui_join_chat            (HippoUI                  *ui,
                                                const char               *chat_id);
HippoWindowState hippo_ui_get_chat_state       (HippoUI                  *ui,
                                                const char               *chat_id);
void             hippo_ui_load_photo           (HippoUI                  *ui,
                                                HippoEntity              *entity,
                                                HippoPixbufCacheLoadFunc  func,
                                                void                     *data);


G_END_DECLS

#endif /* __HIPPO_UI_H__ */
