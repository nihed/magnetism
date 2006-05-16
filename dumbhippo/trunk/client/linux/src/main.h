#ifndef __HIPPO_MAIN_H__
#define __HIPPO_MAIN_H__

#include <config.h>
#include <hippo/hippo-common.h>
#include <gtk/gtk.h>
#include <glib/gi18n-lib.h>
#include "hippo-image-cache.h"

G_BEGIN_DECLS

typedef struct HippoApp HippoApp;

HippoApp* hippo_get_app(void);

void       hippo_app_quit           (HippoApp   *app);
void       hippo_app_show_about     (HippoApp   *app);
void       hippo_app_show_home      (HippoApp   *app);
void       hippo_app_open_url       (HippoApp   *app,
                                     gboolean    use_login_browser,
                                     const char *url);
void       hippo_app_visit_post     (HippoApp   *app,
                                     HippoPost  *post);
void       hippo_app_join_chat      (HippoApp   *app,
                                     const char *chat_id);
/* use this only for user and group photos, caching is weird 
 * if the cache for lots of image types interacts
 */
void       hippo_app_load_photo     (HippoApp               *app,
                                     HippoEntity            *entity,
                                     HippoImageCacheLoadFunc func,
                                     void                   *data);

G_END_DECLS

#endif /* __HIPPO_MAIN_H__ */
