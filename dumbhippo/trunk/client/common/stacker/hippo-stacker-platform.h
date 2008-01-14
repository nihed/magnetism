/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_STACKER_PLATFORM_H__
#define __HIPPO_STACKER_PLATFORM_H__

#include <hippo/hippo-basics.h>
#include <hippo/hippo-graphics.h>
#include <hippo/hippo-track.h>
#include <stacker/hippo-window.h>

G_BEGIN_DECLS

typedef struct _HippoStackerPlatform      HippoStackerPlatform;
typedef struct _HippoStackerPlatformClass HippoStackerPlatformClass;

#define HIPPO_TYPE_STACKER_PLATFORM              (hippo_stacker_platform_get_type ())
#define HIPPO_STACKER_PLATFORM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_STACKER_PLATFORM, HippoStackerPlatform))
#define HIPPO_STACKER_PLATFORM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_STACKER_PLATFORM, HippoStackerPlatformClass))
#define HIPPO_IS_STACKER_PLATFORM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_STACKER_PLATFORM))
#define HIPPO_IS_STACKER_PLATFORM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_STACKER_PLATFORM))
#define HIPPO_STACKER_PLATFORM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_STACKER_PLATFORM, HippoStackerPlatformClass))

/* Used currently for chat windows, but probably should also be used
 * to replace HippoWindow::active HippoWindow::onscreen with
 * HippoWindow::window-state.
 */
typedef enum {
    HIPPO_WINDOW_STATE_CLOSED, /* nonexistent, or "withdrawn" */
    HIPPO_WINDOW_STATE_HIDDEN, /* iconified, on another desktop, or obscured */
    HIPPO_WINDOW_STATE_ONSCREEN, /* some portion of the window is visible */
    HIPPO_WINDOW_STATE_ACTIVE /* the window the user is actively working with */
} HippoWindowState;

/* if content_type == NULL then we failed to get any content and the string is an error
 * message. If content_type != NULL then the string is the content.
 */
typedef void (* HippoHttpFunc) (const char *content_type,
                                GString    *content_or_error,
                                void       *data);

struct _HippoStackerPlatformClass {
    GTypeInterface base_iface;

    HippoWindow* (* create_window) (HippoStackerPlatform *platform);

    void (* get_screen_info) (HippoStackerPlatform *platform,
                              HippoRectangle       *monitor_rect_p,
                              HippoRectangle       *tray_icon_rect_p,
                              HippoOrientation     *tray_icon_orientation_p);
    
    gboolean  (* get_pointer_position) (HippoStackerPlatform *platform,
                                        int                  *x_p,
                                        int                  *y_p);
    
    void (* http_request) (HippoStackerPlatform *platform,
                           const char           *url,
                           HippoHttpFunc         func,
                           void                 *data);

    void (* show_chat_window) (HippoStackerPlatform *platform,
                               const char            *chat_id);
    
    HippoWindowState (* get_chat_window_state) (HippoStackerPlatform    *platform,
                                                const char       *chat_id);
    
    gboolean (* can_play_song_download) (HippoStackerPlatform *platform,
                                         HippoSongDownload    *song_download);
};

GType            hippo_stacker_platform_get_type               (void) G_GNUC_CONST;

HippoWindow*     hippo_stacker_platform_create_window          (HippoStackerPlatform    *platform);

/* monitor_rect is the portion of the "work area" (the area for client
 *    windows) on the same monitor as the tray icon
 */
void             hippo_stacker_platform_get_screen_info        (HippoStackerPlatform *platform,
                                                                HippoRectangle       *monitor_rect_p,
                                                                HippoRectangle       *tray_icon_rect_p,
                                                                HippoOrientation     *tray_icon_orientation_p);

/* Returns false if the pointer isn't on the same screen as the the tray icon; x_p/y_p
 * will be set to arbitrary values in that case. You probably can ignore the case, since
 * multiple non-combined screens (an X concept) is vanishingly rare.
 */
gboolean         hippo_stacker_platform_get_pointer_position   (HippoStackerPlatform    *platform,
                                                                int                     *x_p,
                                                                int                     *y_p);

void             hippo_stacker_platform_show_chat_window       (HippoStackerPlatform   *platform,
                                                                const char             *chat_id);
HippoWindowState hippo_stacker_platform_get_chat_window_state  (HippoStackerPlatform    *platform,
                                                               const char               *chat_id);

void             hippo_stacker_platform_http_request           (HippoStackerPlatform   *platform,
                                                                const char             *url,
                                                                HippoHttpFunc           func,
                                                                void                   *data);

gboolean         hippo_stacker_platform_can_play_song_download (HippoStackerPlatform   *platform,
                                                                HippoSongDownload      *song_download);
    
G_END_DECLS

#endif /* __HIPPO_STACKER_PLATFORM_H__ */
