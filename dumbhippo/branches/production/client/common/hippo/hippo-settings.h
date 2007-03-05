/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_SETTINGS_H__
#define __HIPPO_SETTINGS_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef void (* HippoSettingArrivedFunc) (const char    *key,
                                          const char    *value,
                                          void          *data);

typedef struct _HippoSettings      HippoSettings;
typedef struct _HippoSettingsClass HippoSettingsClass;

#define HIPPO_TYPE_SETTINGS              (hippo_settings_get_type ())
#define HIPPO_SETTINGS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_SETTINGS, HippoSettings))
#define HIPPO_SETTINGS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_SETTINGS, HippoSettingsClass))
#define HIPPO_IS_SETTINGS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_SETTINGS))
#define HIPPO_IS_SETTINGS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_SETTINGS))
#define HIPPO_SETTINGS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_SETTINGS, HippoSettingsClass))

GType            hippo_settings_get_type               (void) G_GNUC_CONST;

HippoSettings* hippo_settings_new         (HippoConnection         *connection);
HippoSettings* hippo_settings_get_and_ref (HippoConnection         *connection);
void           hippo_settings_set         (HippoSettings           *settings,
                                           const char              *key,
                                           const char              *value);
void           hippo_settings_get         (HippoSettings           *settings,
                                           const char              *key,
                                           HippoSettingArrivedFunc  func,
                                           void                    *data);
gboolean       hippo_settings_get_ready   (HippoSettings           *settings);

G_END_DECLS

#endif /* __HIPPO_SETTINGS_H__ */
