/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DISTRIBUTION_H__
#define __HIPPO_DISTRIBUTION_H__

/* 
 * Monitor idleness
 */

#include <glib.h>

G_BEGIN_DECLS

typedef struct HippoDistribution HippoDistribution;

typedef void (*HippoCheckPackageCallback)    (gboolean    is_installed,
                                              gboolean    is_installable,
                                              const char *installed_version,
                                              void       *data);
typedef void (*HippoCheckApplicationCallback) (gboolean   is_runnable,
                                               void      *data);
typedef void (*HippoAsyncCallback)            (GError    *error,
                                               void      *data);

typedef enum {
    HIPPO_DISTRIBUTION_ERROR_FAILED,
    HIPPO_DISTRIBUTION_ERROR_NO_SOURCE,
    HIPPO_DISTRIBUTION_ERROR_CANNOT_INSTALL,
    HIPPO_DISTRIBUTION_ERROR_NO_APPLICATION
} HippoDistributionError;

#define HIPPO_DISTRIBUTION_ERROR hippo_distribution_error_quark()

GQuark hippo_distribution_error_quark(void);

HippoDistribution *hippo_distribution_get (void);

const char *hippo_distribution_get_name        (HippoDistribution *distro);
const char *hippo_distribution_get_version     (HippoDistribution *distro);
const char *hippo_distribution_get_architecture(HippoDistribution *distro);

void hippo_distribution_check_package     (HippoDistribution             *distro,
                                           const char                    *package_names,
                                           HippoCheckPackageCallback      callback,
                                           void                          *callback_data);
void hippo_distribution_install_package   (HippoDistribution             *distro,
                                           const char                    *package_names,
                                           HippoAsyncCallback             callback,
                                           void                          *callback_data);
void hippo_distribution_check_application (HippoDistribution             *distro,
                                           const char                    *desktop_names,
                                           HippoCheckApplicationCallback  callback,
                                           void                          *callback_data);
void hippo_distribution_run_application   (HippoDistribution             *distro,
                                           const char                    *desktop_names,
                                           guint32                        launch_time,
                                           HippoAsyncCallback             callback,
                                           void                          *callback_data);

G_END_DECLS

#endif /* __HIPPO_DISTRIBUTION_H__ */
