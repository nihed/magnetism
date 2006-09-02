/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BASICS_H__
#define __HIPPO_BASICS_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef struct _HippoWindow      HippoWindow;
typedef struct _HippoWindowClass HippoWindowClass;

typedef struct _HippoDataCache      HippoDataCache;
typedef struct _HippoDataCacheClass HippoDataCacheClass;

typedef struct _HippoConnection      HippoConnection;
typedef struct _HippoConnectionClass HippoConnectionClass;

typedef struct _HippoChatRoom      HippoChatRoom;
typedef struct _HippoChatRoomClass HippoChatRoomClass;

typedef enum {
    HIPPO_ERROR_FAILED
} HippoError;

#define HIPPO_ERROR hippo_error_quark()
GQuark hippo_error_quark(void);

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

typedef enum 
{
    HIPPO_BROWSER_IE,
    HIPPO_BROWSER_FIREFOX,
    HIPPO_BROWSER_EPIPHANY
} HippoBrowserKind;

typedef enum {
    HIPPO_CHAT_STATE_NONMEMBER,
    HIPPO_CHAT_STATE_VISITOR,
    HIPPO_CHAT_STATE_PARTICIPANT
} HippoChatState;

typedef enum {
    HIPPO_CHAT_KIND_UNKNOWN,
    HIPPO_CHAT_KIND_POST,
    HIPPO_CHAT_KIND_GROUP,
    HIPPO_CHAT_KIND_BROKEN
} HippoChatKind;

typedef enum 
{
    HIPPO_URI_ACTION_BROKEN,
    HIPPO_URI_ACTION_JOIN_CHAT
} HippoUriAction;

typedef enum
{
    HIPPO_ORIENTATION_VERTICAL,
    HIPPO_ORIENTATION_HORIZONTAL
} HippoOrientation;

#define HIPPO_URI_SCHEME     "mugshot"
#define HIPPO_URI_SCHEME_LEN 7

typedef struct {
    int x;
    int y;
    int width;
    int height;
} HippoRectangle;

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

#define HIPPO_DEFAULT_MESSAGE_HOST     "messages.mugshot.org"
#define HIPPO_DEFAULT_MESSAGE_PORT     5222
#define HIPPO_DEFAULT_MESSAGE_SERVER   HIPPO_DEFAULT_MESSAGE_HOST ":5222"
#define HIPPO_DEFAULT_WEB_HOST         "mugshot.org"
#define HIPPO_DEFAULT_WEB_PORT         80
#define HIPPO_DEFAULT_WEB_SERVER       HIPPO_DEFAULT_WEB_HOST ":80"
#define HIPPO_DEFAULT_LOCAL_HOST       "localinstance.mugshot.org"
#define HIPPO_DEFAULT_LOCAL_WEB_PORT   8080
#define HIPPO_DEFAULT_LOCAL_WEB_SERVER HIPPO_DEFAULT_LOCAL_HOST ":8080"
#define HIPPO_DEFAULT_LOCAL_MESSAGE_SERVER HIPPO_DEFAULT_LOCAL_HOST ":21020"

#ifdef G_OS_WIN32

#define HIPPO_DEFAULT_MESSAGE_HOST_L   L"messages.mugshot.org"
#define HIPPO_DEFAULT_MESSAGE_SERVER_L HIPPO_DEFAULT_MESSAGE_HOST_L L":5222"
#define HIPPO_DEFAULT_WEB_HOST_L       L"mugshot.org"
#define HIPPO_DEFAULT_WEB_SERVER_L     HIPPO_DEFAULT_WEB_HOST_L L":80"
#define HIPPO_DEFAULT_LOCAL_HOST_L     L"localinstance.mugshot.org"
#define HIPPO_DEFAULT_LOCAL_WEB_SERVER_L     HIPPO_DEFAULT_LOCAL_HOST_L L":8080"
#define HIPPO_DEFAULT_LOCAL_MESSAGE_SERVER_L HIPPO_DEFAULT_LOCAL_HOST_L L":21020"

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
    char **restart_argv;
    int    restart_argc;
} HippoOptions;

typedef void (* HippoPrintDebugFunc) (const char *message);

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

gboolean hippo_verify_guid           (const char      *possible_guid);
gboolean hippo_verify_guid_wide      (const gunichar2 *possible_guid);

void hippo_set_print_debug_func      (HippoPrintDebugFunc func);
void hippo_override_loudmouth_log    (void);

gboolean hippo_parse_options         (int          *argc_p,
                                      char       ***argv_p,
                                      HippoOptions *results);
void     hippo_options_free_fields   (HippoOptions *options);

const char* hippo_hotness_debug_string(HippoHotness hotness);

gboolean hippo_parse_uri                   (const char         *uri,
                                            HippoUriActionData *data,
                                            GError            **error);
void     hippo_uri_action_data_free_fields (HippoUriActionData *data);

/* same strings used in URIs, the xmpp protocol */
HippoChatKind hippo_parse_chat_kind        (const char   *str);
const char*   hippo_chat_kind_as_string    (HippoChatKind kind);

int      hippo_compare_versions            (const char *version_a,
                                            const char *version_b);

G_END_DECLS

#endif /* __HIPPO_BASICS_H__ */
