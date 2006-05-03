#ifndef __HIPPO_BASICS_H__
#define __HIPPO_BASICS_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_INSTANCE_NORMAL,
    HIPPO_INSTANCE_DOGFOOD,
    HIPPO_INSTANCE_DEBUG
} HippoInstanceType;

#define HIPPO_DEFAULT_MESSAGE_HOST   "messages.dumbhippo.com"
#define HIPPO_DEFAULT_MESSAGE_PORT   5222
#define HIPPO_DEFAULT_MESSAGE_SERVER HIPPO_DEFAULT_MESSAGE_HOST ":5222"
#define HIPPO_DEFAULT_WEB_HOST       "dumbhippo.com"
#define HIPPO_DEFAULT_WEB_PORT       80
#define HIPPO_DEFAULT_WEB_SERVER     HIPPO_DEFAULT_WEB_HOST ":80"

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

G_END_DECLS

#endif /* __HIPPO_BASICS_H__ */
