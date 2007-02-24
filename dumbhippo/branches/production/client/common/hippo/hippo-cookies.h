/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_COOKIES_H__
#define __HIPPO_COOKIES_H__

#include <glib-object.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct HippoCookie HippoCookie;


HippoCookie* hippo_cookie_new    (HippoBrowserKind origin_browser,
                                  const char      *domain,
                                  int              port,
                                  gboolean         all_hosts_match,
                                  const char      *path,
                                  gboolean         secure_connection_required,
                                  GTime            timestamp,
                                  const char      *name,
                                  const char      *value);

void         hippo_cookie_ref    (HippoCookie *cookie);
void         hippo_cookie_unref  (HippoCookie *cookie);

/* Cookie equals/hash doesn't include the timestamp or value, 
 * that is two cookies are equal if setting one would overwrite
 * the other one - so this equals/hash can be used to merge cookies
 * files.
 */
gboolean     hippo_cookie_equals (HippoCookie *first,
                                  HippoCookie *second);
guint        hippo_cookie_hash   (HippoCookie *cookie);

HippoBrowserKind hippo_cookie_get_origin_browser        (HippoCookie *cookie);
const char *hippo_cookie_get_domain                     (HippoCookie *cookie);
int         hippo_cookie_get_port                       (HippoCookie *cookie);
gboolean    hippo_cookie_get_all_hosts_match            (HippoCookie *cookie);
const char *hippo_cookie_get_path                       (HippoCookie *cookie);
gboolean    hippo_cookie_get_secure_connection_required (HippoCookie *cookie);
/* timestamp is the expiration time */
GTime       hippo_cookie_get_timestamp                  (HippoCookie *cookie);
const char *hippo_cookie_get_name                       (HippoCookie *cookie);
const char *hippo_cookie_get_value                      (HippoCookie *cookie);


typedef struct HippoCookieLocator HippoCookieLocator;

HippoCookieLocator *hippo_cookie_locator_new(void);

void hippo_cookie_locator_destroy(HippoCookieLocator *locator);

/* Add a directory to scan for cookies.txt files */
void hippo_cookie_locator_add_directory(HippoCookieLocator *locator,
                                        const char         *directory,
                                        HippoBrowserKind    browser);

/* Add cookies.txt file */
void hippo_cookie_locator_add_file(HippoCookieLocator *locator,
                                   const char         *file,
                                   HippoBrowserKind    browser);

/* You have to hippo_cookie_unref() the returned cookies and g_slist_free()
 * the list. NULL domain, NULL name, -1 port act as "wildcard" for this function, or 
 * specify them to filter.
 */
GSList *hippo_cookie_locator_load_cookies(HippoCookieLocator *locator,
                                          const char         *domain,
                                          int                 port,
                                          const char         *name);

G_END_DECLS

#endif /* __HIPPO_COOKIES_H__ */
