#ifndef __HIPPO_MAIN_H__
#define __HIPPO_MAIN_H__

#include <hippo/hippo-common.h>
#include <gtk/gtk.h>

G_BEGIN_DECLS

typedef struct HippoApp HippoApp;

HippoApp* hippo_get_app(void);

void hippo_app_quit(HippoApp *app);

G_END_DECLS

#endif /* __HIPPO_MAIN_H__ */
