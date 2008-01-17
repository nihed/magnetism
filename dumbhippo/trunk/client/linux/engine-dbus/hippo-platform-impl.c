/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-platform-impl.h"
#include "hippo-cookies-linux.h"
#include "hippo-distribution.h"
#include "hippo-dbus-system.h"
#include "main.h"
#include <dbus/dbus.h>
#include <string.h>
#include <errno.h>

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);

static void      hippo_platform_impl_dispose             (GObject                 *object);
static void      hippo_platform_impl_finalize            (GObject                 *object);


static void         hippo_platform_impl_get_platform_info   (HippoPlatform     *platform,
                                                             HippoPlatformInfo *info);
static gboolean     hippo_platform_impl_read_login_cookie   (HippoPlatform     *platform,
                                                             HippoServerType    web_server_type,
                                                             HippoBrowserKind  *origin_browser_p,
                                                             char             **username_p,
                                                             char             **password_p);
static void         hippo_platform_impl_delete_login_cookie (HippoPlatform     *platform);
static const char*  hippo_platform_impl_get_jabber_resource (HippoPlatform     *platform);
static void         hippo_platform_impl_open_url            (HippoPlatform     *platform,
                                                             HippoBrowserKind   browser,
                                                             const char        *url);

static HippoNetworkStatus hippo_platform_impl_get_network_status (HippoPlatform *platform);

static HippoInstanceType hippo_platform_impl_get_instance_type (HippoPlatform  *platform);
static char*        hippo_platform_impl_get_message_server  (HippoPlatform     *platform,
                                                             HippoServerType    server_type);
static char*        hippo_platform_impl_get_web_server      (HippoPlatform     *platform,
                                                             HippoServerType    server_type);
static gboolean     hippo_platform_impl_get_signin          (HippoPlatform     *platform);
static void         hippo_platform_impl_set_message_server  (HippoPlatform     *platform,
                                                             const char        *value);
static void         hippo_platform_impl_set_web_server      (HippoPlatform     *platform,
                                                             const char        *value);
static void         hippo_platform_impl_set_signin          (HippoPlatform     *platform,
                                                             gboolean           value);

static char *hippo_platform_impl_make_cache_filename (HippoPlatform  *platform,
                                                      const char     *server,
                                                      const char     *user_id);

struct _HippoPlatformImpl {
    GObject parent;
    HippoInstanceType instance;
    char *jabber_resource;
    HippoSystemDBus *system_dbus;
    HippoNetworkStatus network_status;

    /* This is a hack to simulate forgetting the username password after
     * an authentication failure without having to actually worry about
     * how to change cookies.txt if there is currently a browser running.
     * 
     * Instead of deleting the cookie, we just remember that the last
     * value we read is "deleted" and ignore it if we read it again.
     *
     * This case is only hit in the relatively rare case of rejected
     * authentication.
     */
    char *last_username;
    char *last_password;
    gboolean forget_auth;
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
    klass->read_login_cookie = hippo_platform_impl_read_login_cookie;
    klass->delete_login_cookie = hippo_platform_impl_delete_login_cookie;
    klass->get_jabber_resource = hippo_platform_impl_get_jabber_resource;
    klass->open_url = hippo_platform_impl_open_url;
    klass->get_network_status = hippo_platform_impl_get_network_status;
    
    klass->get_instance_type = hippo_platform_impl_get_instance_type;
    klass->get_message_server = hippo_platform_impl_get_message_server;
    klass->get_web_server = hippo_platform_impl_get_web_server;
    klass->get_signin = hippo_platform_impl_get_signin;
    klass->set_message_server = hippo_platform_impl_set_message_server;
    klass->set_web_server = hippo_platform_impl_set_web_server;
    klass->set_signin = hippo_platform_impl_set_signin;
    
    klass->make_cache_filename = hippo_platform_impl_make_cache_filename;
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

static void
on_network_status_changed(HippoSystemDBus   *system_dbus,
                          HippoNetworkStatus status,
                          HippoPlatformImpl *impl)
{
    if (status != impl->network_status) {
        impl->network_status = status;
        hippo_platform_emit_network_status_changed(HIPPO_PLATFORM(impl),
                                                   impl->network_status);
    }
}

static void
on_cookie_monitor_notification(void *data)
{
    HippoPlatformImpl *impl;

    impl = HIPPO_PLATFORM_IMPL(data);

    hippo_platform_emit_cookies_maybe_changed(HIPPO_PLATFORM(impl));
}

HippoPlatform*
hippo_platform_impl_new(HippoInstanceType instance)
{
    HippoPlatformImpl *impl;
    GError *error;
    
    impl = g_object_new(HIPPO_TYPE_PLATFORM_IMPL, NULL);
    impl->instance = instance;

    error = NULL;
    impl->system_dbus = hippo_system_dbus_open(&error);
    if (impl->system_dbus) {
        g_signal_connect(G_OBJECT(impl->system_dbus), "network-status-changed",
                         G_CALLBACK(on_network_status_changed), impl);
    } else {
        g_debug("Failed to open system dbus: %s", error->message);
        g_error_free(error);
    }
    impl->network_status = HIPPO_NETWORK_STATUS_UNKNOWN;

    hippo_cookie_monitor_add(on_cookie_monitor_notification, impl);
    
    return HIPPO_PLATFORM(impl);
}

static void
hippo_platform_impl_dispose(GObject *object)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(object);

    g_debug("Disposing platform impl");

    if (impl->system_dbus) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(impl->system_dbus),
                                             G_CALLBACK(on_network_status_changed), impl);
        g_object_unref(impl->system_dbus);
        impl->system_dbus = NULL;
    }

    hippo_cookie_monitor_remove(on_cookie_monitor_notification, impl);
    
    G_OBJECT_CLASS(hippo_platform_impl_parent_class)->finalize(object);
}

static void
hippo_platform_impl_finalize(GObject *object)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(object);

    g_debug("Finalizing platform impl");

    g_free(impl->jabber_resource);
    g_free(impl->last_username);
    g_free(impl->last_password);
    
    G_OBJECT_CLASS(hippo_platform_impl_parent_class)->finalize(object);
}

static void
hippo_platform_impl_get_platform_info(HippoPlatform     *platform,
                                      HippoPlatformInfo *info)
{
    HippoDistribution *distro = hippo_distribution_get();
    
    info->name = "linux";
    info->distribution = hippo_distribution_get_name(distro);
    info->version = hippo_distribution_get_version(distro);
    info->architecture = hippo_distribution_get_architecture(distro);

    /* Backwards compatibility hack: versions of the server prior to 2007-03-13
     * expected distribution/version to be combined into one string and
     * handled "fedora5" and "fedora6" specially. Newer server versions
     * expand "fedora5/fedora6" back into the correct two fields.
     */
    if (info->distribution && strcmp(info->distribution, "Fedora") == 0) {
        if (info->version && strcmp(info->version, "5") == 0)
            info->distribution = "fedora5";
        else if (info->version && strcmp(info->version, "6") == 0)
            info->distribution = "fedora6";
    }
}

static gboolean
hippo_platform_impl_read_login_cookie(HippoPlatform    *platform,
                                      HippoServerType   web_server_type,
                                      HippoBrowserKind *origin_browser_p,
                                      char            **username_p,
                                      char            **password_p)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    GSList *cookies;
    char *web_host;
    int web_port;
    gboolean success = FALSE;
    HippoCookie *cookie;
    char *value;
    char *username = NULL;
    char *password = NULL;

    hippo_platform_get_web_host_port(platform, web_server_type, &web_host, &web_port);
    
    g_debug("Looking for login to %s:%d", web_host, web_port);
    
    /* We load cookies with -1 (wildcard) for the port because 
     * the port doesn't seem to get saved in cookies.txt ...
     */
    cookies = hippo_load_cookies(web_host, -1, "auth");
    
    if (cookies == NULL) {
        g_free(web_host);
        goto out;
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

    success = hippo_parse_login_cookie(value, web_host, &username, &password);
    g_free(value);
    g_free(web_host);

 out:
    if (success && impl->forget_auth &&
        (username == impl->last_username ||
         (username && impl->last_username && strcmp(username, impl->last_username) == 0)) &&
        (password == impl->last_password ||
         (password && impl->last_password && strcmp(password, impl->last_password) == 0)))
    {
        g_free(impl->last_username);
        g_free(impl->last_password);
        
        impl->last_username = username;
        impl->last_password = password;

        username = NULL;
        password = NULL;
        success = FALSE;
    } else {
        impl->forget_auth = FALSE;
        
        g_free(impl->last_username);
        g_free(impl->last_password);
        
        impl->last_username = g_strdup(username);
        impl->last_password = g_strdup(password);
    }

    if (username_p)
        *username_p = username;
    else
        g_free(username);

    if (password_p)
        *password_p = password;
    else
        g_free(password);
    
    return success;
}

static void
hippo_platform_impl_delete_login_cookie(HippoPlatform *platform)
{
    /* Hack; don't delete the cookie, just remember that we should
     * have deleted it; see comment in the HippoPlatformImpl structure.
     */
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    impl->forget_auth = TRUE;
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
    case HIPPO_BROWSER_GALEON:
        command = g_strdup_printf("galeon %s", quoted);
        break;
    case HIPPO_BROWSER_MAEMO:
        command = g_strdup_printf("browser --url=%s", quoted);
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

static HippoNetworkStatus
hippo_platform_impl_get_network_status (HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    return impl->network_status;
}

static HippoInstanceType
hippo_platform_impl_get_instance_type(HippoPlatform  *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    return impl->instance;
}

static char*
hippo_platform_impl_get_message_server(HippoPlatform  *platform,
                                       HippoServerType server_type)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    const char *server;
    
    /* on Windows this looks up a preference, this function
     * is pretty pointless on Linux
     */
    
    server = hippo_get_default_server(impl->instance, server_type, HIPPO_SERVER_PROTOCOL_MESSAGE);
    return g_strdup(server);
}

static char*
hippo_platform_impl_get_web_server(HippoPlatform  *platform,
                                   HippoServerType server_type)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    const char *server;

    /* on Windows this looks up a preference, this function
     * is pretty pointless on Linux
     */
    
    server = hippo_get_default_server(impl->instance, server_type, HIPPO_SERVER_PROTOCOL_WEB);
    return g_strdup(server);
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

static char *
hippo_platform_impl_make_cache_filename (HippoPlatform  *platform,
                                         const char     *server,
                                         const char     *user_id)
{
    char *directory;
    char *filename;
    char *path;
    char *machine_id;

    directory = g_build_filename(g_get_home_dir(), ".online-data-cache", NULL);
    if (!g_file_test (directory, G_FILE_TEST_IS_DIR)) {
        if (g_mkdir_with_parents(directory, 0700) != 0) {
            g_warning("Can't create directory for online data cache: %s", g_strerror(errno));
            return NULL;
        }
    }
    
    machine_id = dbus_get_local_machine_id();

    filename = g_strdup_printf("%s-%s-%s.db", server, user_id, machine_id);
    path = g_build_filename(g_get_home_dir(), ".online-data-cache", filename, NULL);

    dbus_free(machine_id);
    g_free(directory);
    g_free(filename);

    return path;
}
