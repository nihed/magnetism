/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_MAIN_H__
#define __HIPPO_MAIN_H__

#include <config.h>
#include <engine/hippo-engine.h>

#include <gtk/gtk.h>
#include <glib/gi18n-lib.h>

G_BEGIN_DECLS

typedef struct _HippoDBus      HippoDBus;

typedef struct HippoEngineApp HippoEngineApp;

HippoEngineApp* hippo_get_engine_app(void);

void       hippo_engine_app_quit           (HippoEngineApp   *app);

HippoDataCache *hippo_engine_app_get_data_cache (HippoEngineApp *app);
HippoDBus      *hippo_engine_app_get_dbus       (HippoEngineApp *app);
DDMDataModel   *hippo_engine_app_get_data_model (HippoEngineApp *app);

void       hippo_engine_app_show_home      (HippoEngineApp   *app);
void       hippo_engine_app_open_url       (HippoEngineApp   *app,
                                            gboolean    use_login_browser,
                                            const char *url);

void              hippo_engine_app_join_chat     (HippoEngineApp   *app,
                                                  const char       *chat_id);
HippoWindowState hippo_engine_app_get_chat_state (HippoEngineApp   *app,
                                                  const char       *chat_id);

G_END_DECLS

#endif /* __HIPPO_MAIN_H__ */
