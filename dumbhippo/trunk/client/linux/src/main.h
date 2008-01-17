/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_MAIN_H__
#define __HIPPO_MAIN_H__

#include <config.h>
#include <stacker/hippo-stacker-platform.h>
#include <stacker/hippo-stack-manager.h>

/* avoiding gtk/gtk.h because of the internal gtk/ copy stuff,
 * this plays it safer. maybe it speeds up compilation a tiny 
 * bit too, who knows.
 */
#include <gtk/gtkwidget.h>
#include <gtk/gtkmain.h>
#include <gtk/gtkimage.h>
#include <gtk/gtkeventbox.h>
#include <gtk/gtklabel.h>
#include <gtk/gtkstock.h>
#include <gtk/gtknotebook.h>
#include <gtk/gtkimagemenuitem.h>
#include <gtk/gtkseparatormenuitem.h>
#include <gtk/gtkicontheme.h>
#include <gtk/gtkaboutdialog.h>
#include <gtk/gtkmessagedialog.h>
#include <glib/gi18n-lib.h>

G_BEGIN_DECLS

typedef struct HippoStackerApp HippoStackerApp;

HippoStackerApp* hippo_get_stacker_app(void);

void       hippo_stacker_app_quit           (HippoStackerApp   *app);

DDMDataModel   *      hippo_stacker_app_get_data_model       (HippoStackerApp *app);
HippoStackerPlatform *hippo_stacker_app_get_stacker_platform (HippoStackerApp *app);
HippoStackManager*    hippo_stacker_app_get_stack            (HippoStackerApp *app);

void hippo_stacker_app_show_about (HippoStackerApp *app);
void hippo_stacker_app_show_home  (HippoStackerApp *app);
void hippo_stacker_app_open_url   (HippoStackerApp *app,
                                   gboolean         use_login_browser,
                                   const char      *url);

void             hippo_stacker_app_join_chat      (HippoStackerApp *app,
                                                   const char      *chat_id);
HippoWindowState hippo_stacker_app_get_chat_state (HippoStackerApp *app,
                                                   const char      *chat_id);

void     hippo_stacker_app_get_screen_info      (HippoStackerApp  *app,
                                                 HippoRectangle   *monitor_rect_p,
                                                 HippoRectangle   *tray_icon_rect_p,
                                                 HippoOrientation *tray_icon_orientation_p);
gboolean hippo_stacker_app_get_pointer_position (HippoStackerApp  *app,
                                                 int              *x_p,
                                                 int              *y_p);

G_END_DECLS

#endif /* __HIPPO_MAIN_H__ */
