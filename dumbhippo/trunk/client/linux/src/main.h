/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_MAIN_H__
#define __HIPPO_MAIN_H__

#include <config.h>
#include <hippo/hippo-common.h>
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
#include "hippo-pixbuf-cache.h"

G_BEGIN_DECLS

typedef struct HippoApp HippoApp;

HippoApp* hippo_get_app(void);

void       hippo_app_quit           (HippoApp   *app);
HippoDataCache *hippo_app_get_data_cache (HippoApp *app);
void       hippo_app_show_about     (HippoApp   *app);
void       hippo_app_show_home      (HippoApp   *app);
void       hippo_app_open_url       (HippoApp   *app,
                                     gboolean    use_login_browser,
                                     const char *url);
void       hippo_app_visit_post     (HippoApp   *app,
                                     HippoPost  *post);
void       hippo_app_visit_post_id  (HippoApp   *app,
                                     const char *guid);                                     
void       hippo_app_ignore_post_id (HippoApp   *app,
                                     const char *guid);                                                                          
void       hippo_app_ignore_entity_id(HippoApp   *app,
                                      const char *guid);                                        
void       hippo_app_ignore_entity_chat_id(HippoApp   *app,
                                           const char *guid);                                       
void       hippo_app_visit_entity   (HippoApp    *app,
                                     HippoEntity *entity);
void       hippo_app_visit_entity_id(HippoApp    *app,
                                     const char  *guid);
void       hippo_app_invite_to_group(HippoApp   *app,
                                     const char *group_id,
                                     const char *user_id);                                     
void       hippo_app_join_chat      (HippoApp   *app,
                                     const char *chat_id);
gboolean   hippo_app_post_is_active (HippoApp   *app,
                                     const char *post_id);                                     
gboolean   hippo_app_chat_is_active (HippoApp   *app,
                                     const char *post_id);
/* use this only for user and group photos, caching is weird 
 * if the cache for lots of image types interacts
 */
void       hippo_app_load_photo     (HippoApp                *app,
                                     HippoEntity             *entity,
                                     HippoPixbufCacheLoadFunc func,
                                     void                    *data);
void       hippo_app_get_screen_info   (HippoApp         *app,
                                        HippoRectangle   *monitor_rect_p,
                                        HippoRectangle   *tray_icon_rect_p,
                                        HippoOrientation *tray_icon_orientation_p);

/* less typing */
#define ADD_WEAK(ptr)    g_object_add_weak_pointer(G_OBJECT(*(ptr)), (void**) (char*) (ptr))
#define REMOVE_WEAK(ptr) do { if (*ptr) { g_object_remove_weak_pointer(G_OBJECT(*(ptr)), (void**) (char*) (ptr)); *ptr = NULL; } } while(0)

G_END_DECLS

#endif /* __HIPPO_MAIN_H__ */
