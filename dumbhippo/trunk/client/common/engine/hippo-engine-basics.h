/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_ENGINE_BASICS_H__
#define __HIPPO_ENGINE_BASICS_H__

#include <config.h>
#include <glib-object.h>

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

/* This can be used to specify which browser to use,
 * in which case UNKNOWN = use system default,
 * or to specify which browser was used (e.g. for cookies)
 * in which case UNKNOWN = don't know.
 */
typedef enum 
{
    HIPPO_BROWSER_UNKNOWN,
    HIPPO_BROWSER_IE,
    HIPPO_BROWSER_FIREFOX,
    HIPPO_BROWSER_EPIPHANY,
    HIPPO_BROWSER_GALEON,
    HIPPO_BROWSER_MAEMO
} HippoBrowserKind;

#define HIPPO_JID_DOMAIN "dumbhippo.com"
#define HIPPO_ROOMS_JID_DOMAIN "rooms." HIPPO_JID_DOMAIN
#define HIPPO_ADMIN_JID "admin@" HIPPO_JID_DOMAIN

typedef void (* HippoPrintDebugFunc) (const char *message);

gboolean hippo_parse_int32  (const char *s,
                             int        *result);
gboolean hippo_parse_int64  (const char *s,
                             gint64     *result);
gboolean hippo_parse_double (const char *s,
                             double     *result);


gboolean hippo_parse_http_url  (const char *url,
                                gboolean   *is_https,
                                char      **host,
                                int        *port);

gboolean hippo_parse_login_cookie    (const char *cookie_value,
                                      const char *required_host,
                                      char      **username_p,
                                      char      **password_p);

char*    hippo_id_to_jabber_id       (const char *guid);
char*    hippo_id_from_jabber_id     (const char *jid);

void hippo_set_print_debug_func      (HippoPrintDebugFunc func);
void hippo_initialize_logging        (HippoOptions *options);
void hippo_override_loudmouth_log    (void);

G_END_DECLS

#endif /* __HIPPO_ENGINE_BASICS_H__ */
