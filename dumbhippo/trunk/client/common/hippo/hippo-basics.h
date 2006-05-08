#ifndef __HIPPO_BASICS_H__
#define __HIPPO_BASICS_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_INSTANCE_NORMAL,
    HIPPO_INSTANCE_DOGFOOD,
    HIPPO_INSTANCE_DEBUG
} HippoInstanceType;

typedef enum {
    HIPPO_HOTNESS_COLD,
    HIPPO_HOTNESS_COOL,
    HIPPO_HOTNESS_WARM,
    HIPPO_HOTNESS_GETTING_HOT,
    HIPPO_HOTNESS_HOT,
    HIPPO_HOTNESS_UNKNOWN
} HippoHotness;

#define HIPPO_DEFAULT_MESSAGE_HOST     "messages.dumbhippo.com"
#define HIPPO_DEFAULT_MESSAGE_PORT     5222
#define HIPPO_DEFAULT_MESSAGE_SERVER   HIPPO_DEFAULT_MESSAGE_HOST ":5222"
#define HIPPO_DEFAULT_WEB_HOST         "dumbhippo.com"
#define HIPPO_DEFAULT_WEB_PORT         80
#define HIPPO_DEFAULT_WEB_SERVER       HIPPO_DEFAULT_WEB_HOST ":80"

#ifdef G_OS_WIN32
#define HIPPO_DEFAULT_MESSAGE_HOST_L   L"messages.dumbhippo.com"
#define HIPPO_DEFAULT_MESSAGE_SERVER_L HIPPO_DEFAULT_MESSAGE_HOST_L L":5222"
#define HIPPO_DEFAULT_WEB_HOST_L       L"dumbhippo.com"
#define HIPPO_DEFAULT_WEB_SERVER_L     HIPPO_DEFAULT_WEB_HOST_L L":80"
#endif

typedef struct {
    HippoInstanceType instance_type;
    guint config_flag : 1;
    guint install_launch : 1;
    guint replace_existing : 1;
    guint quit_existing : 1;
    guint initial_debug_share : 1;
} HippoOptions;

gboolean hippo_parse_server          (const char *server,
                                      char      **host,
                                      int        *port);                 
void     hippo_parse_message_server  (const char *server,
                                      char      **host,
                                      int        *port);
void     hippo_parse_web_server      (const char *server,
                                      char      **host,
                                      int        *port);

gboolean hippo_parse_login_cookie    (const char *cookie_value,
                                      const char *required_host,
                                      char      **username_p,
                                      char      **password_p);

char*    hippo_id_to_jabber_id       (const char *guid);
char*    hippo_id_from_jabber_id     (const char *jid);

gboolean hippo_parse_options         (int          *argc_p,
                                      char       ***argv_p,
                                      HippoOptions *results);
void     hippo_options_free_fields   (HippoOptions *options);

const char* hippo_hotness_debug_string(HippoHotness hotness);

G_END_DECLS

#endif /* __HIPPO_BASICS_H__ */
