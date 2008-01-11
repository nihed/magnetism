/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_MAIN_H__
#define __HIPPO_MAIN_H__

#include <config.h>
#include <hippo/hippo-common.h>
#include <hippo/hippo-stacker-platform.h>

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

typedef struct _HippoDBus      HippoDBus;

typedef struct HippoApp HippoApp;

HippoApp* hippo_get_app(void);

void       hippo_app_quit           (HippoApp   *app);

HippoDataCache *hippo_app_get_data_cache (HippoApp *app);
HippoDBus      *hippo_app_get_dbus       (HippoApp *app);
DDMDataModel   *hippo_app_get_data_model (HippoApp *app);
HippoStackerPlatform *hippo_app_get_stacker_platform (HippoApp *app);

void       hippo_app_set_show_stacker (HippoApp *app,
                                       gboolean  value);

HippoStackManager* hippo_app_get_stack (HippoApp *app);

void       hippo_app_show_about     (HippoApp   *app);
void       hippo_app_show_home      (HippoApp   *app);
void       hippo_app_open_url       (HippoApp   *app,
                                     gboolean    use_login_browser,
                                     const char *url);

void              hippo_app_join_chat     (HippoApp   *app,
                                           const char *chat_id);
HippoWindowState hippo_app_get_chat_state (HippoApp   *app,
                                           const char *chat_id);

void       hippo_app_get_screen_info   (HippoApp         *app,
                                        HippoRectangle   *monitor_rect_p,
                                        HippoRectangle   *tray_icon_rect_p,
                                        HippoOrientation *tray_icon_orientation_p);
gboolean hippo_app_get_pointer_position (HippoApp *app,
                                         int      *x_p,
                                         int      *y_p);

/* FIXME just change all references to have the HIPPO_ */
#define ADD_WEAK(ptr)    HIPPO_ADD_WEAK(ptr)
#define REMOVE_WEAK(ptr) HIPPO_REMOVE_WEAK(ptr)

G_END_DECLS

#endif /* __HIPPO_MAIN_H__ */
