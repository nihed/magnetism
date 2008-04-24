/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-stacker-platform-impl.h"
#include "hippo-window-wrapper.h"
#include "hippo-status-icon.h"
#include "hippo-http.h"
#include <hippo/hippo-canvas-theme.h>
#include "main.h"
#include <string.h>
#include <errno.h>

static void      hippo_stacker_platform_impl_init                (HippoStackerPlatformImpl       *impl);
static void      hippo_stacker_platform_impl_class_init          (HippoStackerPlatformImplClass  *klass);
static void      hippo_stacker_platform_impl_iface_init          (HippoStackerPlatformClass      *klass);

static void      hippo_stacker_platform_impl_dispose             (GObject                 *object);
static void      hippo_stacker_platform_impl_finalize            (GObject                 *object);


static HippoWindow* hippo_stacker_platform_impl_create_window       (HippoStackerPlatform     *platform);
static void         hippo_stacker_platform_impl_get_screen_info     (HippoStackerPlatform     *platform,
                                                                     HippoRectangle           *monitor_rect_p,
                                                                     HippoRectangle           *tray_icon_rect_p,
                                                                     HippoOrientation         *tray_icon_orientation_p);
static gboolean     hippo_stacker_platform_impl_get_pointer_position (HippoStackerPlatform     *platform,
                                                                      int                      *x_p,
                                                                      int                      *y_p);
static void         hippo_stacker_platform_impl_http_request        (HippoStackerPlatform     *platform,
                                                                     const char        *url,
                                                                     HippoHttpFunc      func,
                                                                     void              *data);

static void             hippo_stacker_platform_impl_show_chat_window      (HippoStackerPlatform *platform,
                                                                           const char           *chat_id);
static HippoWindowState hippo_stacker_platform_impl_get_chat_window_state (HippoStackerPlatform *platform,
                                                                           const char           *chat_id);

static gboolean     hippo_stacker_platform_impl_can_play_song_download (HippoStackerPlatform     *platform,
                                                                        HippoSongDownload        *song_download);

#if 0
typedef struct Dialogs Dialogs;

static Dialogs* dialogs_get                        (HippoConnection *connection);
static void     dialogs_destroy                    (Dialogs         *dialogs);
static void     dialogs_update_disconnected_window (Dialogs         *dialogs,
                                                    gboolean         show_if_not_showing);
static void     dialogs_update_login               (Dialogs         *dialogs,
                                                    gboolean         show_if_not_showing);
static void     dialogs_update_status              (Dialogs         *dialogs,
                                                    gboolean         show_if_not_showing);
#endif

struct _HippoStackerPlatformImpl {
    GObject parent;
    HippoInstanceType instance;
    HippoCanvasTheme *theme;
};

struct _HippoStackerPlatformImplClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE_WITH_CODE(HippoStackerPlatformImpl, hippo_stacker_platform_impl, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_STACKER_PLATFORM, hippo_stacker_platform_impl_iface_init));
                       

static void
hippo_stacker_platform_impl_iface_init(HippoStackerPlatformClass *klass)
{
    klass->create_window = hippo_stacker_platform_impl_create_window;
    klass->get_screen_info = hippo_stacker_platform_impl_get_screen_info;
    klass->get_pointer_position = hippo_stacker_platform_impl_get_pointer_position;
    klass->http_request = hippo_stacker_platform_impl_http_request;
    klass->show_chat_window = hippo_stacker_platform_impl_show_chat_window;
    klass->get_chat_window_state = hippo_stacker_platform_impl_get_chat_window_state;
    klass->can_play_song_download = hippo_stacker_platform_impl_can_play_song_download;
}

static void
hippo_stacker_platform_impl_init(HippoStackerPlatformImpl       *impl)
{
    const char *theme_stylesheet;

    if (hippo_stacker_app_is_uninstalled())
        theme_stylesheet = ABSOLUTE_TOP_SRCDIR "/../common/stacker/stacker.css";
    else
        theme_stylesheet = HIPPO_DATA_DIR "/stacker.css";
    
    impl->theme = hippo_canvas_theme_new(NULL, NULL, theme_stylesheet, NULL);
}

static void
hippo_stacker_platform_impl_class_init(HippoStackerPlatformImplClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_stacker_platform_impl_finalize;
    object_class->dispose = hippo_stacker_platform_impl_dispose;
}

HippoStackerPlatform*
hippo_stacker_platform_impl_new(void)
{
    HippoStackerPlatformImpl *impl;
    GError *error;
    
    impl = g_object_new(HIPPO_TYPE_STACKER_PLATFORM_IMPL, NULL);

    error = NULL;

    return HIPPO_STACKER_PLATFORM(impl);
}

static void
hippo_stacker_platform_impl_dispose(GObject *object)
{
    /*    HippoStackerPlatformImpl *impl = HIPPO_STACKER_PLATFORM_IMPL(object); */

    G_OBJECT_CLASS(hippo_stacker_platform_impl_parent_class)->finalize(object);
}

static void
hippo_stacker_platform_impl_finalize(GObject *object)
{
    /* HippoStackerPlatformImpl *impl = HIPPO_STACKER_PLATFORM_IMPL(object); */

    G_OBJECT_CLASS(hippo_stacker_platform_impl_parent_class)->finalize(object);
}

static HippoWindow*
hippo_stacker_platform_impl_create_window(HippoStackerPlatform *platform)
{
    HippoStackerPlatformImpl *impl = HIPPO_STACKER_PLATFORM_IMPL(platform);
    HippoWindow *window = hippo_window_wrapper_new();
    hippo_window_set_theme(window, impl->theme);

    return window;
}

static void
hippo_stacker_platform_impl_get_screen_info(HippoStackerPlatform    *platform,
                                            HippoRectangle   *monitor_rect_p,
                                            HippoRectangle   *tray_icon_rect_p,
                                            HippoOrientation *tray_icon_orientation_p)
{
    hippo_stacker_app_get_screen_info(hippo_get_stacker_app(), monitor_rect_p, tray_icon_rect_p,
                                      tray_icon_orientation_p);
}

static gboolean
hippo_stacker_platform_impl_get_pointer_position (HippoStackerPlatform     *platform,
                                                  int               *x_p,
                                                  int               *y_p)
{
    return hippo_stacker_app_get_pointer_position(hippo_get_stacker_app(), x_p, y_p);
}

static void
hippo_stacker_platform_impl_http_request(HippoStackerPlatform   *platform,
                                         const char      *url,
                                         HippoHttpFunc    func,
                                         void            *data)
{
    hippo_http_get(url, func, data);
}

static void
hippo_stacker_platform_impl_show_chat_window (HippoStackerPlatform     *platform,
                                              const char        *chat_id)
{
    hippo_stacker_app_join_chat(hippo_get_stacker_app(), chat_id);
}

HippoWindowState 
hippo_stacker_platform_impl_get_chat_window_state (HippoStackerPlatform  *platform,
                                                   const char            *chat_id)
{
    return hippo_stacker_app_get_chat_state(hippo_get_stacker_app(), chat_id);
}

static gboolean
hippo_stacker_platform_impl_can_play_song_download(HippoStackerPlatform     *platform,
                                                   HippoSongDownload *song_download)
{
    switch (hippo_song_download_get_source(song_download)) {
    case HIPPO_SONG_DOWNLOAD_ITUNES:
	return FALSE;
    case HIPPO_SONG_DOWNLOAD_YAHOO:
	return FALSE;
    case HIPPO_SONG_DOWNLOAD_RHAPSODY:
	return TRUE;
    }

    return TRUE;
}

#if 0
/* We want to show either a login dialog or a dialog with connection status
 * when the tray icon is clicked but we aren't signed in
 */
struct Dialogs {
    HippoConnection *connection;
    guint connection_state_id;
    GtkWidget *login_dialog;
    GtkWidget *connection_status_dialog;
};

static void
dialogs_connection_destroyed(Dialogs *dialogs)
{
    dialogs_destroy(dialogs);

    /* this isn't needed since the connection is already disposed */
    /* g_signal_handler_disconnect(dialogs->connection,
       dialogs->connection_state_id); */
    
    g_free(dialogs);
}

static void
state_changed_cb(HippoConnection *connection,
                 Dialogs         *dialogs)
{
    /* update dialog text and/or which dialog is showing */
    dialogs_update_disconnected_window(dialogs, FALSE);
}

static Dialogs*
dialogs_get(HippoConnection *connection)
{
    Dialogs *dialogs;

    dialogs = g_object_get_data(G_OBJECT(connection), "status-dialogs");
    if (dialogs == NULL) {
        dialogs = g_new0(Dialogs, 1);
        dialogs->connection = connection;
        g_object_set_data_full(G_OBJECT(connection), "status-dialogs", dialogs,
                               (GDestroyNotify) dialogs_connection_destroyed);
        dialogs->connection_state_id = g_signal_connect(G_OBJECT(dialogs->connection),
                                                        "state-changed",
                                                        G_CALLBACK(state_changed_cb),
                                                        dialogs);
    }
    
    return dialogs;
}

static void
dialogs_update_disconnected_window (Dialogs *dialogs,
                                    gboolean show_if_not_showing)
{
    HippoConnection *connection = dialogs->connection;
    
    if (hippo_connection_get_need_login(connection)) {
        dialogs_update_login(dialogs, show_if_not_showing);
    } else if (!hippo_connection_get_connected(connection)) {
        dialogs_update_status(dialogs, show_if_not_showing);
    } else {
        dialogs_destroy(dialogs);
    }
}

static void
dialogs_destroy(Dialogs *dialogs)
{
    if (dialogs->login_dialog) {
        g_object_run_dispose(G_OBJECT(dialogs->login_dialog));
        dialogs->login_dialog = NULL;
    }

    if (dialogs->connection_status_dialog) {
        g_object_run_dispose(G_OBJECT(dialogs->connection_status_dialog));
        dialogs->connection_status_dialog = NULL;
    }
}

static void
login_response_cb(GtkDialog *dialog,
                  int        response_id,
                  void      *data)
{
    Dialogs *dialogs = data;
    
    if (response_id == GTK_RESPONSE_ACCEPT) {
        hippo_connection_open_maybe_relative_url(dialogs->connection,
                                                 "/who-are-you");

        /* Though we don't expect this to succeed immediately (the
         * user has not yet had the chance to login) calling this
         * here has the side-effect of putting us into the mode where
         * we check for a new auth token every 5 seconds for a
         * few minutes.
         */
        hippo_connection_signin(dialogs->connection);
    }
    g_object_run_dispose(G_OBJECT(dialog));
}


static void
dialogs_update_login(Dialogs *dialogs,
                     gboolean show_if_not_showing)
{    
    if (dialogs->login_dialog == NULL && show_if_not_showing) {
        dialogs_destroy(dialogs); /* Kill any other dialogs */
        
        dialogs->login_dialog = gtk_message_dialog_new(NULL, 0,
                                                       GTK_MESSAGE_INFO,
                                                       GTK_BUTTONS_NONE,
                                                       _("You need to log in to mugshot.org"));

        gtk_window_set_title(GTK_WINDOW(dialogs->login_dialog), _("Mugshot Login"));
        
        gtk_dialog_add_buttons(GTK_DIALOG(dialogs->login_dialog),
                               _("Cancel"), GTK_RESPONSE_REJECT,
                               _("Open Login Page"), GTK_RESPONSE_ACCEPT,
                               NULL);
        gtk_dialog_set_default_response(GTK_DIALOG(dialogs->login_dialog), GTK_RESPONSE_ACCEPT);
        
        g_signal_connect(G_OBJECT(dialogs->login_dialog), "response",
                         G_CALLBACK(login_response_cb), dialogs);
        
        g_signal_connect(G_OBJECT(dialogs->login_dialog), "destroy",
                         G_CALLBACK(gtk_widget_destroyed), &dialogs->login_dialog);
    }

    if (dialogs->login_dialog)
        gtk_window_present(GTK_WINDOW(dialogs->login_dialog));
}


/* the not-logged-in states shouldn't display here, since we should show the
 * login dialog in that case instead
 */
static const char*
get_status_message(HippoConnection *connection)
{
    const char *msg;

    msg = NULL;

    if (hippo_connection_get_need_login(connection))
        return _("Mugshot is not connected - please log in to mugshot.org");
    
    switch (hippo_connection_get_state(connection)) {
    case HIPPO_STATE_SIGNED_OUT:
    case HIPPO_STATE_RETRYING:
        msg = _("Mugshot is not connected, but will try reconnecting soon");
        break;
    case HIPPO_STATE_SIGN_IN_WAIT:
    case HIPPO_STATE_AUTH_WAIT:
        msg = _("Mugshot is not connected - please log in to mugshot.org");
        break;
    case HIPPO_STATE_CONNECTING:
    case HIPPO_STATE_REDIRECTING:
    case HIPPO_STATE_AUTHENTICATING:
        msg = _("Mugshot is trying to connect to mugshot.org");
        break;    
    case HIPPO_STATE_AWAITING_CLIENT_INFO:
        msg = _("Mugshot is checking for new versions");
        break;
    case HIPPO_STATE_AUTHENTICATED:
        msg = _("Mugshot is connected!");
        break;
    }

    return msg;
}

static void
set_state_text(Dialogs *dialogs)
{
    if (dialogs->connection_status_dialog) {
        g_object_set(G_OBJECT(dialogs->connection_status_dialog),
                     "text", get_status_message(dialogs->connection),
                     NULL);
    }
}

static void
status_response_cb(GtkDialog *dialog,
                   int        response_id,
                   void      *data)
{
    g_object_run_dispose(G_OBJECT(dialog));
}

static void
dialogs_update_status(Dialogs *dialogs,
                      gboolean show_if_not_showing)
{
    if (dialogs->connection_status_dialog == NULL && show_if_not_showing) {
        dialogs_destroy(dialogs); /* Kill any other dialogs */
        
        dialogs->connection_status_dialog = gtk_message_dialog_new(NULL, 0,
                                                                   GTK_MESSAGE_INFO,
                                                                   GTK_BUTTONS_OK,
                                                                   "%s", get_status_message(dialogs->connection));

        gtk_window_set_title(GTK_WINDOW(dialogs->connection_status_dialog), _("Mugshot Status"));
        
        g_signal_connect(G_OBJECT(dialogs->connection_status_dialog), "response",
                         G_CALLBACK(status_response_cb), dialogs);
        
        g_signal_connect(G_OBJECT(dialogs->connection_status_dialog), "destroy",
                         G_CALLBACK(gtk_widget_destroyed), &dialogs->connection_status_dialog);
    } else {
        set_state_text(dialogs);
    }
    
    if (dialogs->connection_status_dialog)
        gtk_window_present(GTK_WINDOW(dialogs->connection_status_dialog));
}
#endif
