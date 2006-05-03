#ifndef __HIPPO_COOKIES_H__
#define __HIPPO_COOKIES_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef struct HippoCookie HippoCookie;

HippoCookie* hippo_cookie_new    (const char *domain,
                                  int         port,
                                  gboolean    all_hosts_match,
                                  const char *path,
                                  gboolean    secure_connection_required,
                                  GTime       timestamp,
                                  const char *name,
                                  const char *value);

void         hippo_cookie_ref    (HippoCookie *cookie);
void         hippo_cookie_unref  (HippoCookie *cookie);

const char *hippo_cookie_get_domain                     (HippoCookie *cookie);
int         hippo_cookie_get_port                       (HippoCookie *cookie);
gboolean    hippo_cookie_get_all_hosts_match            (HippoCookie *cookie);
const char *hippo_cookie_get_path                       (HippoCookie *cookie);
gboolean    hippo_cookie_get_secure_connection_required (HippoCookie *cookie);
GTime       hippo_cookie_get_timestamp                  (HippoCookie *cookie);
const char *hippo_cookie_get_name                       (HippoCookie *cookie);
const char *hippo_cookie_get_value                      (HippoCookie *cookie);



G_END_DECLS

#endif /* __HIPPO_COOKIES_H__ */
