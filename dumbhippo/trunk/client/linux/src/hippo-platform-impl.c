/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-platform-impl.h"
#include "hippo-cookies-linux.h"
#include "hippo-window-wrapper.h"
#include "hippo-status-icon.h"
#include "hippo-http.h"
#include "main.h"
#include <string.h>

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);

static void      hippo_platform_impl_dispose             (GObject                 *object);
static void      hippo_platform_impl_finalize            (GObject                 *object);


static void         hippo_platform_impl_get_platform_info   (HippoPlatform     *platform,
                                                             HippoPlatformInfo *info);
static HippoWindow* hippo_platform_impl_create_window       (HippoPlatform     *platform);
static void         hippo_platform_impl_get_screen_info     (HippoPlatform     *platform,
                                                             HippoRectangle    *monitor_rect_p,
                                                             HippoRectangle    *tray_icon_rect_p,
                                                             HippoOrientation  *tray_icon_orientation_p);
static gboolean     hippo_platform_impl_get_pointer_position (HippoPlatform     *platform,
                                                              int               *x_p,
                                                              int               *y_p);
static gboolean     hippo_platform_impl_read_login_cookie   (HippoPlatform     *platform,
                                                             HippoBrowserKind  *origin_browser_p,
                                                             char             **username_p,
                                                             char             **password_p);
static void         hippo_platform_impl_delete_login_cookie (HippoPlatform     *platform);
static const char*  hippo_platform_impl_get_jabber_resource (HippoPlatform     *platform);
static void         hippo_platform_impl_open_url            (HippoPlatform     *platform,
                                                             HippoBrowserKind   browser,
                                                             const char        *url);
static void         hippo_platform_impl_http_request        (HippoPlatform     *platform,
                                                             const char        *url,
                                                             HippoHttpFunc      func,
                                                             void              *data);

static void             hippo_platform_impl_show_chat_window      (HippoPlatform *platform,
                                                                   const char    *chat_id);
static HippoWindowState hippo_platform_impl_get_chat_window_state (HippoPlatform *platform,
                                                                   const char    *chat_id);

static gboolean     hippo_platform_impl_can_play_song_download (HippoPlatform     *platform,
                                                                HippoSongDownload *song_download);
static void         hippo_platform_impl_show_disconnected_window (HippoPlatform *platform,
                                                                  HippoConnection *connection);
static HippoNetworkStatus hippo_platform_impl_get_network_status (HippoPlatform *platform);

static HippoInstanceType hippo_platform_impl_get_instance_type (HippoPlatform  *platform);
static char*        hippo_platform_impl_get_message_server  (HippoPlatform     *platform);
static char*        hippo_platform_impl_get_web_server      (HippoPlatform     *platform);
static gboolean     hippo_platform_impl_get_signin          (HippoPlatform     *platform);
static void         hippo_platform_impl_set_message_server  (HippoPlatform     *platform,
                                                             const char        *value);
static void         hippo_platform_impl_set_web_server      (HippoPlatform     *platform,
                                                             const char        *value);
static void         hippo_platform_impl_set_signin          (HippoPlatform     *platform,
                                                             gboolean           value);

typedef struct Dialogs Dialogs;

static Dialogs* dialogs_get                        (HippoConnection *connection);
static void     dialogs_destroy                    (Dialogs         *dialogs);
static void     dialogs_update_disconnected_window (Dialogs         *dialogs,
                                                    gboolean         show_if_not_showing);
static void     dialogs_update_login               (Dialogs         *dialogs,
                                                    gboolean         show_if_not_showing);
static void     dialogs_update_status              (Dialogs         *dialogs,
                                                    gboolean         show_if_not_showing);


struct _HippoPlatformImpl {
    GObject parent;
    HippoInstanceType instance;
    char *jabber_resource;
};

struct _HippoPlatformImplClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE_WITH_CODE(HippoPlatformImpl, hippo_platform_impl, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_PLATFORM, hippo_platform_impl_iface_init));
                       

static void
hippo_platform_impl_iface_init(HippoPlatformClass *klass)
{
    klass->get_platform_info = hippo_platform_impl_get_platform_info;
    klass->create_window = hippo_platform_impl_create_window;
    klass->get_screen_info = hippo_platform_impl_get_screen_info;
    klass->get_pointer_position = hippo_platform_impl_get_pointer_position;
    klass->read_login_cookie = hippo_platform_impl_read_login_cookie;
    klass->delete_login_cookie = hippo_platform_impl_delete_login_cookie;
    klass->get_jabber_resource = hippo_platform_impl_get_jabber_resource;
    klass->open_url = hippo_platform_impl_open_url;
    klass->http_request = hippo_platform_impl_http_request;
    klass->show_chat_window = hippo_platform_impl_show_chat_window;
    klass->get_chat_window_state = hippo_platform_impl_get_chat_window_state;
    klass->can_play_song_download = hippo_platform_impl_can_play_song_download;
    klass->show_disconnected_window = hippo_platform_impl_show_disconnected_window;
    klass->get_network_status = hippo_platform_impl_get_network_status;
    
    klass->get_instance_type = hippo_platform_impl_get_instance_type;
    klass->get_message_server = hippo_platform_impl_get_message_server;
    klass->get_web_server = hippo_platform_impl_get_web_server;
    klass->get_signin = hippo_platform_impl_get_signin;
    klass->set_message_server = hippo_platform_impl_set_message_server;
    klass->set_web_server = hippo_platform_impl_set_web_server;
    klass->set_signin = hippo_platform_impl_set_signin;
}

static void
hippo_platform_impl_init(HippoPlatformImpl       *impl)
{

}

static void
hippo_platform_impl_class_init(HippoPlatformImplClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_platform_impl_finalize;
    object_class->dispose = hippo_platform_impl_dispose;
}

HippoPlatform*
hippo_platform_impl_new(HippoInstanceType instance)
{
    HippoPlatformImpl *impl = g_object_new(HIPPO_TYPE_PLATFORM_IMPL, NULL);
    impl->instance = instance;
    return HIPPO_PLATFORM(impl);
}

static void
hippo_platform_impl_dispose(GObject *object)
{
    /* HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(object); */

    g_debug("Disposing platform impl");
    
    G_OBJECT_CLASS(hippo_platform_impl_parent_class)->finalize(object);
}

static void
hippo_platform_impl_finalize(GObject *object)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(object);

    g_debug("Finalizing platform impl");

    g_free(impl->jabber_resource);
    
    G_OBJECT_CLASS(hippo_platform_impl_parent_class)->finalize(object);
}

static const char*
get_distribution(void)
{
    static const char* distribution = NULL;

    if (distribution == NULL) {
        char *contents = NULL;
        gsize len = 0;
        GError *error = NULL;
        if (!g_file_get_contents("/etc/fedora-release", &contents, &len, &error)) {
            g_debug("Failed to get /etc/fedora-release contents: %s", error->message);
            g_error_free(error);
        } else {
            if (strstr(contents, " 5 ") != NULL)
                distribution = "fedora5";
            else if (strstr(contents, " 6 ") != NULL)
                distribution = "fedora6";

            g_free(contents);
        }
    }

    return distribution;
}

static void
hippo_platform_impl_get_platform_info(HippoPlatform     *platform,
                                      HippoPlatformInfo *info)
{
    info->name = "linux";
    info->distribution = get_distribution();
}

static HippoWindow*
hippo_platform_impl_create_window(HippoPlatform *platform)
{
    return HIPPO_WINDOW(hippo_window_wrapper_new());
}

static void
hippo_platform_impl_get_screen_info(HippoPlatform    *platform,
                                    HippoRectangle   *monitor_rect_p,
                                    HippoRectangle   *tray_icon_rect_p,
                                    HippoOrientation *tray_icon_orientation_p)
{
    hippo_app_get_screen_info(hippo_get_app(), monitor_rect_p, tray_icon_rect_p,
                              tray_icon_orientation_p);
}

static gboolean
hippo_platform_impl_get_pointer_position (HippoPlatform     *platform,
                                          int               *x_p,
                                          int               *y_p)
{
    return hippo_app_get_pointer_position(hippo_get_app(), x_p, y_p);
}

static gboolean
hippo_platform_impl_read_login_cookie(HippoPlatform    *platform,
                                      HippoBrowserKind *origin_browser_p,
                                      char            **username_p,
                                      char            **password_p)
{
    GSList *cookies;
    char *web_host;
    int web_port;
    gboolean success;
    HippoCookie *cookie;
    char *value;

    hippo_platform_get_web_host_port(platform, &web_host, &web_port);
    
    g_debug("Looking for login to %s:%d", web_host, web_port);
    
    /* We load cookies with -1 (wildcard) for the port because 
     * the port doesn't seem to get saved in cookies.txt ...
     */
    cookies = hippo_load_cookies(web_host, -1, "auth");
    
    if (cookies == NULL) {
        g_free(web_host);
        return FALSE;
    }        

    /* Extract value from first cookie and free the rest of them 
     * (we only expect to have one, though)
     */
    
    cookie = cookies->data;
    /* in theory the cookie value could be NULL, which is OK, but be aware */        
    value = g_strdup(hippo_cookie_get_value(cookie));
    
    if (origin_browser_p)
        *origin_browser_p = hippo_cookie_get_origin_browser(cookie);
    
    g_debug("Parsing cookie value '%s' from browser %d",
        value ? value : "NULL", hippo_cookie_get_origin_browser(cookie));    
    
    /* Free cookies! */
    g_slist_foreach(cookies, (GFunc) hippo_cookie_unref, NULL);
    g_slist_free(cookies);

    /* Parse the value and return username/password
     * hippo_parse_login_cookie allows a NULL value
     */
     
    success = hippo_parse_login_cookie(value, web_host, username_p, password_p);
    g_free(value);
    g_free(web_host);
    return success;
}

static void
hippo_platform_impl_delete_login_cookie(HippoPlatform *platform)
{
    /* FIXME this is going to be a serious headache. 
     * For browsers that aren't running we have to blow the cookie
     * out of cookies.txt, for running browsers we have to hook into them
     * and export an API to drop the cookie or something.
     */

}

static const char*
hippo_platform_impl_get_jabber_resource(HippoPlatform *platform)
{
    /* On Windows we're using the hardware profile ID. Linux doesn't have 
     * such a thing; arguably the resource should be per-user or per-session
     * anyway. 
     */
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    if (impl->jabber_resource == NULL) {
        /* OK, this is pretty lame FIXME but it should get 
         * us a unique per-session identifier most of the time.
         * Technically .Xauthority can change during the session though.
         * anyway, FIXME
         */
        unsigned int dbus_session_hash = 0;
        unsigned int xauthority_hash = 0;
        const char *dbus_session;
        char *xauthority_file;
        char *xauthority;
        gsize len;
        
        dbus_session = g_getenv("DBUS_SESSION_BUS_ADDRESS");
        if (dbus_session)
            dbus_session_hash = g_str_hash(dbus_session);
        
        xauthority_file = g_build_filename(g_get_home_dir(), ".Xauthority", NULL);
        if (g_file_get_contents(xauthority_file, &xauthority, &len, NULL)) {
            gsize i;
            
            /* g_str_hash assumes nul termination, this is a binary file */
            xauthority_hash = 17;
            for (i = 0; i < len; ++i) {
                xauthority_hash = xauthority_hash * 37 + xauthority[i];
            }
            g_free(xauthority);
        }
        g_free(xauthority_file);
        
        impl->jabber_resource = g_strdup_printf("%u-%u-%u",
            dbus_session_hash, xauthority_hash, g_str_hash(g_get_user_name()));
            
        g_debug("jabber resource: '%s'", impl->jabber_resource);
    }
    return impl->jabber_resource;
}

static void
hippo_platform_impl_open_url(HippoPlatform     *platform,
                             HippoBrowserKind   browser,
                             const char        *url)
{
    char *command;
    char *quoted;
    GError *error;
    
    g_debug("Opening url '%s'", url);
    
    quoted = g_shell_quote(url);
    
    switch (browser) {
    case HIPPO_BROWSER_EPIPHANY:
        command = g_strdup_printf("epiphany %s", quoted);
        break;
    case HIPPO_BROWSER_FIREFOX:
    case HIPPO_BROWSER_UNKNOWN: /* FIXME get user's default from gnome */
    default:
        command = g_strdup_printf("firefox %s", quoted);    
        break;
    }

    error = NULL;
    if (!g_spawn_command_line_async(command, &error)) {
        GtkWidget *dialog;
        
        dialog = gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_ERROR,
                                        GTK_BUTTONS_CLOSE,
                                        _("Couldn't start your web browser!"));
        gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(dialog), "%s", error->message);
        g_signal_connect(dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
        
        gtk_widget_show(dialog);
        
        g_debug("Failed to launch browser: %s", error->message);
        g_error_free(error);
    }
    
    g_free(command);
    g_free(quoted);
}

static void
hippo_platform_impl_http_request(HippoPlatform   *platform,
                                 const char      *url,
                                 HippoHttpFunc    func,
                                 void            *data)
{
    hippo_http_get(url, func, data);
}

static void
hippo_platform_impl_show_chat_window (HippoPlatform     *platform,
                                      const char        *chat_id)
{
    hippo_app_join_chat(hippo_get_app(), chat_id);
}

HippoWindowState 
hippo_platform_impl_get_chat_window_state (HippoPlatform    *platform,
                                           const char       *chat_id)
{
    return hippo_app_get_chat_state(hippo_get_app(), chat_id);
}

static gboolean
hippo_platform_impl_can_play_song_download(HippoPlatform     *platform,
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

static void
hippo_platform_impl_show_disconnected_window(HippoPlatform   *platform,
                                             HippoConnection *connection)
{
    /* HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform); */
    Dialogs *dialogs;

    dialogs = dialogs_get(connection);
    
    dialogs_update_disconnected_window(dialogs, TRUE);
}

static HippoNetworkStatus
hippo_platform_impl_get_network_status (HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    return HIPPO_NETWORK_STATUS_UNKNOWN;
}

static HippoInstanceType
hippo_platform_impl_get_instance_type(HippoPlatform  *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    return impl->instance;
}

static const char *
get_debug_server(void)
{
    const char *debug_server = g_getenv("HIPPO_DEBUG_SERVER");
    if (debug_server)
        return debug_server;
    else
	return "localinstance.mugshot.org";
}

static char*
hippo_platform_impl_get_message_server(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    const char *server;

    server = g_getenv("HIPPO_MESSAGE_SERVER");
    if (server)
	return g_strdup(server);

    /* FIXME */

    if (impl->instance == HIPPO_INSTANCE_DOGFOOD)
        return g_strdup("dogfood.mugshot.org:21020");
    else if (impl->instance == HIPPO_INSTANCE_DEBUG)
        return g_strconcat(get_debug_server(), ":21020", NULL);
    else
        return g_strdup(HIPPO_DEFAULT_MESSAGE_SERVER);
}

static char*
hippo_platform_impl_get_web_server(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    const char *server;

    server = g_getenv("HIPPO_WEB_SERVER");
    if (server)
	return g_strdup(server);

    /* FIXME */

    if (impl->instance == HIPPO_INSTANCE_DOGFOOD)
        return g_strdup("dogfood.mugshot.org:9080");
    else if (impl->instance == HIPPO_INSTANCE_DEBUG)
        return g_strconcat(get_debug_server(), ":8080", NULL);
    else
        return g_strdup(HIPPO_DEFAULT_WEB_SERVER);
}

static gboolean
hippo_platform_impl_get_signin(HippoPlatform *platform)
{

    /* FIXME */
    return TRUE;
}

static void
hippo_platform_impl_set_message_server(HippoPlatform  *platform,
                                       const char     *value)
{

    /* FIXME */
}

static void
hippo_platform_impl_set_web_server(HippoPlatform  *platform,
                                   const char     *value)
{

    /* FIXME */
}

static void
hippo_platform_impl_set_signin(HippoPlatform  *platform,
                               gboolean        value)
{

    /* FIXME */
}

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
