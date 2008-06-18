/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_UI_H__
#define __HIPPO_UI_H__

#include <glib-object.h>
#include "main.h"
#include "hippo-dbus-stacker.h"

G_BEGIN_DECLS

typedef struct HippoUI HippoUI;

HippoUI*         hippo_ui_new                  (DDMDataModel             *model);
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

HippoStackManager*    hippo_ui_get_stack_manager    (HippoUI *ui);
HippoStackerPlatform *hippo_ui_get_stacker_platform (HippoUI *ui);

G_END_DECLS

#endif /* __HIPPO_UI_H__ */
