/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_PLATFORM_H__
#define __HIPPO_PLATFORM_H__

#include <hippo/hippo-basics.h>
#include <hippo/hippo-post.h>
#include <hippo/hippo-graphics.h>

G_BEGIN_DECLS

typedef struct {
    const char *name;         /* "windows" or "linux" */
    const char *distribution; /* "fedora5", "fedora6", or NULL for unknown
                               * could also be "xp" or something maybe on Windows
                               * but is NULL right now
                               */
} HippoPlatformInfo;

typedef enum {
    HIPPO_NETWORK_STATUS_UNKNOWN,
    HIPPO_NETWORK_STATUS_DOWN,
    HIPPO_NETWORK_STATUS_UP
} HippoNetworkStatus;

typedef struct _HippoPlatform      HippoPlatform;
typedef struct _HippoPlatformClass HippoPlatformClass;

#define HIPPO_TYPE_PLATFORM              (hippo_platform_get_type ())
#define HIPPO_PLATFORM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PLATFORM, HippoPlatform))
#define HIPPO_PLATFORM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PLATFORM, HippoPlatformClass))
#define HIPPO_IS_PLATFORM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PLATFORM))
#define HIPPO_IS_PLATFORM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PLATFORM))
#define HIPPO_PLATFORM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_PLATFORM, HippoPlatformClass))

struct _HippoPlatformClass {
    GTypeInterface base_iface;

    void (* get_platform_info) (HippoPlatform     *platform,
                                HippoPlatformInfo *info);
    
    HippoWindow* (* create_window)    (HippoPlatform  *platform);

    void      (* get_screen_info)     (HippoPlatform    *platform,
                                       HippoRectangle   *monitor_rect_p,
                                       HippoRectangle   *tray_icon_rect_p,
                                       HippoOrientation *tray_icon_orientation_p);
    gboolean  (* get_pointer_position) (HippoPlatform   *platform,
                                        int             *x_p,
                                        int             *y_p);
    
    gboolean  (* read_login_cookie)   (HippoPlatform  *platform,
                                       HippoBrowserKind *origin_browser_p,
                                       char          **username,
                                       char          **password);
    void      (* delete_login_cookie) (HippoPlatform  *platform);                                   
    
    const char* (* get_jabber_resource) (HippoPlatform  *platform);

    void      (* open_url)            (HippoPlatform   *platform,
                                       HippoBrowserKind browser,
                                       const char      *url);

    void      (* http_request)        (HippoPlatform   *platform,
                                       const char      *url,
                                       HippoHttpFunc    func,
                                       void            *data);

    void      (* show_chat_window)   (HippoPlatform    *platform,
                                      const char       *chat_id);
    HippoWindowState (* get_chat_window_state) (HippoPlatform    *platform,
                                                const char       *chat_id);
    
    gboolean  (* can_play_song_download) (HippoPlatform     *platform,
                                          HippoSongDownload *song_download);

    void      (* show_disconnected_window) (HippoPlatform   *platform,
                                            HippoConnection *connection);

    HippoNetworkStatus (* get_network_status) (HippoPlatform *platform);
    
    /* Preferences */
    char*     (* get_message_server)  (HippoPlatform *platform);
    char*     (* get_web_server)      (HippoPlatform *platform);
    gboolean  (* get_signin)          (HippoPlatform *platform);
    
    void     (* set_message_server)  (HippoPlatform *platform, const char *value);
    void     (* set_web_server)      (HippoPlatform *platform, const char *value);
    void     (* set_signin)          (HippoPlatform *platform, gboolean    value);

    HippoInstanceType (* get_instance_type) (HippoPlatform *platform);
};

GType            hippo_platform_get_type               (void) G_GNUC_CONST;

void             hippo_platform_get_platform_info      (HippoPlatform     *platform,
                                                        HippoPlatformInfo *info);

HippoWindow*     hippo_platform_create_window          (HippoPlatform    *platform);

/* monitor_rect is the portion of the "work area" (the area for client
 *    windows) on the same monitor as the tray icon
 */
void             hippo_platform_get_screen_info        (HippoPlatform    *platform,
                                                        HippoRectangle   *monitor_rect_p,
                                                        HippoRectangle   *tray_icon_rect_p,
                                                        HippoOrientation *tray_icon_orientation_p);
/* Returns false if the pointer isn't on the same screen as the the tray icon; x_p/y_p
 * will be set to arbitrary values in that case. You probably can ignore the case, since
 * multiple non-combined screens (an X concept) is vanishingly rare.
 */
gboolean         hippo_platform_get_pointer_position   (HippoPlatform    *platform,
                                                        int              *x_p,
                                                        int              *y_p);

gboolean         hippo_platform_read_login_cookie      (HippoPlatform    *platform,
                                                        HippoBrowserKind *origin_browser_p,
                                                        char            **username_p,
                                                        char            **password_p);
void             hippo_platform_delete_login_cookie    (HippoPlatform *platform);                                       
const char*      hippo_platform_get_jabber_resource    (HippoPlatform *platform); 

void             hippo_platform_open_url               (HippoPlatform   *platform,
                                                        HippoBrowserKind browser,
                                                        const char      *url);

void             hippo_platform_show_chat_window       (HippoPlatform   *platform,
                                                        const char      *chat_id);
HippoWindowState hippo_platform_get_chat_window_state (HippoPlatform    *platform,
                                                       const char       *chat_id);

void             hippo_platform_http_request           (HippoPlatform   *platform,
                                                        const char      *url,
                                                        HippoHttpFunc    func,
                                                        void            *data);

gboolean         hippo_platform_can_play_song_download (HippoPlatform     *platform,
                                                        HippoSongDownload *song_download);

void             hippo_platform_show_disconnected_window (HippoPlatform   *platform,
                                                          HippoConnection *connection);
    

HippoNetworkStatus hippo_platform_get_network_status (HippoPlatform *platform);

void               hippo_platform_emit_network_status_changed (HippoPlatform *platform,
                                                               HippoNetworkStatus status);

/* Preferences */
HippoInstanceType hippo_platform_get_instance_type (HippoPlatform *platform);

char*            hippo_platform_get_message_server     (HippoPlatform *platform); 
char*            hippo_platform_get_web_server         (HippoPlatform *platform); 
gboolean         hippo_platform_get_signin             (HippoPlatform *platform); 

void             hippo_platform_set_message_server     (HippoPlatform  *platform,
                                                        const char     *value); 
void             hippo_platform_set_web_server         (HippoPlatform  *platform,
                                                        const char     *value); 
void             hippo_platform_set_signin             (HippoPlatform  *platform,
                                                        gboolean        value);
                                                       

/* Convenience wrappers on get_server stuff that parse the host/port */
void             hippo_platform_get_message_host_port  (HippoPlatform  *platform,
                                                        char          **host_p,
                                                        int            *port_p);
void             hippo_platform_get_web_host_port      (HippoPlatform  *platform,
                                                        char          **host_p,
                                                        int            *port_p);

G_END_DECLS

#endif /* __HIPPO_PLATFORM_H__ */
