/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_ENGINE_BASICS_H__
#define __HIPPO_ENGINE_BASICS_H__

#include <config.h>
#include <glib-object.h>

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

/* The full server spec is a 3x2x2 matrix
 * (instance X stacker/desktop X web/xmpp)
 */
typedef enum {
    HIPPO_SERVER_DESKTOP,
    HIPPO_SERVER_STACKER
} HippoServerType;

typedef enum {
    HIPPO_SERVER_PROTOCOL_WEB,
    HIPPO_SERVER_PROTOCOL_MESSAGE
} HippoServerProtocol;

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

typedef enum {
    HIPPO_URI_ACTION_BROKEN,
    HIPPO_URI_ACTION_JOIN_CHAT
} HippoUriAction;

#define HIPPO_URI_SCHEME     "mugshot"
#define HIPPO_URI_SCHEME_LEN 7

typedef struct {
    HippoUriAction action;
    char *server;
    union {
        struct {
            char         *chat_id;
            HippoChatKind kind;
        } join_chat;
    } u;
} HippoUriActionData;

/* Default servers for Mugshot Stacker site */

#define HIPPO_DEFAULT_STACKER_MESSAGE_HOST     "message-router.mugshot.org"
#define HIPPO_DEFAULT_STACKER_MESSAGE_PORT     5222
#define HIPPO_DEFAULT_STACKER_MESSAGE_SERVER   HIPPO_DEFAULT_STACKER_MESSAGE_HOST ":5222"
#define HIPPO_DEFAULT_STACKER_WEB_HOST         "mugshot.org"
#define HIPPO_DEFAULT_STACKER_WEB_PORT         80
#define HIPPO_DEFAULT_STACKER_WEB_SERVER       HIPPO_DEFAULT_STACKER_WEB_HOST ":80"

#define HIPPO_DEFAULT_STACKER_DOGFOOD_HOST       "dogfood.mugshot.org"
#define HIPPO_DEFAULT_STACKER_DOGFOOD_WEB_PORT   9080
#define HIPPO_DEFAULT_STACKER_DOGFOOD_WEB_SERVER HIPPO_DEFAULT_STACKER_DOGFOOD_HOST ":9080"
#define HIPPO_DEFAULT_STACKER_DOGFOOD_MESSAGE_SERVER HIPPO_DEFAULT_STACKER_DOGFOOD_HOST ":21020"

#define HIPPO_DEFAULT_STACKER_LOCAL_HOST       "localinstance.mugshot.org"
#define HIPPO_DEFAULT_STACKER_LOCAL_WEB_PORT   8080
#define HIPPO_DEFAULT_STACKER_LOCAL_WEB_SERVER HIPPO_DEFAULT_STACKER_LOCAL_HOST ":8080"
#define HIPPO_DEFAULT_STACKER_LOCAL_MESSAGE_SERVER HIPPO_DEFAULT_STACKER_LOCAL_HOST ":21020"

#ifdef G_OS_WIN32

#define HIPPO_DEFAULT_STACKER_MESSAGE_HOST_L   L"message-router.mugshot.org"
#define HIPPO_DEFAULT_STACKER_MESSAGE_SERVER_L HIPPO_DEFAULT_STACKER_MESSAGE_HOST_L L":5222"
#define HIPPO_DEFAULT_STACKER_WEB_HOST_L       L"mugshot.org"
#define HIPPO_DEFAULT_STACKER_WEB_SERVER_L     HIPPO_DEFAULT_STACKER_WEB_HOST_L L":80"

#define HIPPO_DEFAULT_STACKER_DOGFOOD_HOST_L     L"dogfood.mugshot.org"
#define HIPPO_DEFAULT_STACKER_DOGFOOD_WEB_SERVER_L     HIPPO_DEFAULT_STACKER_DOGFOOD_HOST_L L":9080"
#define HIPPO_DEFAULT_STACKER_DOGFOOD_MESSAGE_SERVER_L HIPPO_DEFAULT_STACKER_DOGFOOD_HOST_L L":21020"

#define HIPPO_DEFAULT_STACKER_LOCAL_HOST_L     L"localinstance.mugshot.org"
#define HIPPO_DEFAULT_STACKER_LOCAL_WEB_SERVER_L     HIPPO_DEFAULT_STACKER_LOCAL_HOST_L L":8080"
#define HIPPO_DEFAULT_STACKER_LOCAL_MESSAGE_SERVER_L HIPPO_DEFAULT_STACKER_LOCAL_HOST_L L":21020"

#endif /* G_OS_WIN32 */

/* Default servers for GNOME online desktop site (for now XMPP server is the same)
 */

#define HIPPO_DEFAULT_DESKTOP_MESSAGE_HOST     "message-router.mugshot.org"
#define HIPPO_DEFAULT_DESKTOP_MESSAGE_PORT     5222
#define HIPPO_DEFAULT_DESKTOP_MESSAGE_SERVER   HIPPO_DEFAULT_DESKTOP_MESSAGE_HOST ":5222"
#define HIPPO_DEFAULT_DESKTOP_WEB_HOST         "online.gnome.org"
#define HIPPO_DEFAULT_DESKTOP_WEB_PORT         80
#define HIPPO_DEFAULT_DESKTOP_WEB_SERVER       HIPPO_DEFAULT_DESKTOP_WEB_HOST ":80"

#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_HOST       "dogfood-online.gnome.org"
#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_WEB_PORT   9080
#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_WEB_SERVER HIPPO_DEFAULT_DESKTOP_DOGFOOD_HOST ":9080"
#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_MESSAGE_SERVER HIPPO_DEFAULT_DESKTOP_DOGFOOD_HOST ":21020"

#define HIPPO_DEFAULT_DESKTOP_LOCAL_HOST       "localinstance-online.gnome.org"
#define HIPPO_DEFAULT_DESKTOP_LOCAL_WEB_PORT   8080
#define HIPPO_DEFAULT_DESKTOP_LOCAL_WEB_SERVER HIPPO_DEFAULT_DESKTOP_LOCAL_HOST ":8080"
#define HIPPO_DEFAULT_DESKTOP_LOCAL_MESSAGE_SERVER HIPPO_DEFAULT_DESKTOP_LOCAL_HOST ":21020"

#ifdef G_OS_WIN32

#define HIPPO_DEFAULT_DESKTOP_MESSAGE_HOST_L   L"message-router.mugshot.org"
#define HIPPO_DEFAULT_DESKTOP_MESSAGE_SERVER_L HIPPO_DEFAULT_DESKTOP_MESSAGE_HOST_L L":5222"
#define HIPPO_DEFAULT_DESKTOP_WEB_HOST_L       L"online.gnome.org"
#define HIPPO_DEFAULT_DESKTOP_WEB_SERVER_L     HIPPO_DEFAULT_DESKTOP_WEB_HOST_L L":80"

#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_HOST_L     L"dogfood-online.gnome.org"
#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_WEB_SERVER_L     HIPPO_DEFAULT_DESKTOP_DOGFOOD_HOST_L L":9080"
#define HIPPO_DEFAULT_DESKTOP_DOGFOOD_MESSAGE_SERVER_L HIPPO_DEFAULT_DESKTOP_DOGFOOD_HOST_L L":21020"

#define HIPPO_DEFAULT_DESKTOP_LOCAL_HOST_L     L"localinstance-online.gnome.org"
#define HIPPO_DEFAULT_DESKTOP_LOCAL_WEB_SERVER_L     HIPPO_DEFAULT_DESKTOP_LOCAL_HOST_L L":8080"
#define HIPPO_DEFAULT_DESKTOP_LOCAL_MESSAGE_SERVER_L HIPPO_DEFAULT_DESKTOP_LOCAL_HOST_L L":21020"

#endif /* G_OS_WIN32 */


#define HIPPO_JID_DOMAIN "dumbhippo.com"
#define HIPPO_ROOMS_JID_DOMAIN "rooms." HIPPO_JID_DOMAIN
#define HIPPO_ADMIN_JID "admin@" HIPPO_JID_DOMAIN


typedef struct {
    HippoInstanceType instance_type;
    guint install_launch : 1;
    guint replace_existing : 1;
    guint quit_existing : 1;
    guint initial_debug_share : 1;
    guint verbose : 1;
    guint verbose_xmpp : 1;
    guint debug_updates : 1;
    guint show_window : 1;
    char *crash_dump;
    char **restart_argv;
    int    restart_argc;
} HippoOptions;

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

gboolean hippo_parse_server          (const char       *server,
                                      char            **host,
                                      int              *port);                 
void     hippo_parse_message_server  (const char       *server,
                                      HippoInstanceType instance_type,
                                      HippoServerType   server_type,
                                      char            **host,
                                      int              *port);
void     hippo_parse_web_server      (const char       *server,
                                      HippoInstanceType instance_type,
                                      HippoServerType   server_type,                                      
                                      char            **host,
                                      int              *port);

const char*  hippo_get_default_server (HippoInstanceType   instance_type,
                                       HippoServerType     server_type,
                                       HippoServerProtocol protocol);


gboolean hippo_parse_login_cookie    (const char *cookie_value,
                                      const char *required_host,
                                      char      **username_p,
                                      char      **password_p);

char*    hippo_id_to_jabber_id       (const char *guid);
char*    hippo_id_from_jabber_id     (const char *jid);

void hippo_set_print_debug_func      (HippoPrintDebugFunc func);
void hippo_override_loudmouth_log    (void);

gboolean hippo_parse_options         (int          *argc_p,
                                      char       ***argv_p,
                                      HippoOptions *results);
void     hippo_options_free_fields   (HippoOptions *options);

gboolean hippo_parse_uri                   (const char         *uri,
                                            HippoUriActionData *data,
                                            GError            **error);
void     hippo_uri_action_data_free_fields (HippoUriActionData *data);

int      hippo_compare_versions            (const char *version_a,
                                            const char *version_b);

G_END_DECLS

#endif /* __HIPPO_ENGINE_BASICS_H__ */
