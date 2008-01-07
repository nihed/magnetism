/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-cookies-linux.h"
#include <string.h>
#include <libgnomevfs/gnome-vfs.h>

typedef struct {
    HippoCookiesMonitorFunc func;
    void                   *data;    
} CookieMonitor;

typedef struct {
    char *path;
    GnomeVFSMonitorHandle *handle;
} MonitoredCookieFile;

static int cookie_monitors_serial = 0;
static GSList *cookie_monitors = NULL;
static GHashTable *monitored_files = NULL;

static void
cookie_monitors_notify(void)
{
    GSList *l;
    int start_serial;
        
    start_serial = cookie_monitors_serial;
    for (l = cookie_monitors; l != NULL; l = l->next) {
        CookieMonitor *cm = l->data;

        g_assert(cm != NULL);
        
        (* cm->func) (cm->data);
        
        if (start_serial != cookie_monitors_serial) {
            /* This is not supposed to happen, the warning is here in case we
             * ever accidentally create the bug
             */
            g_warning("Cookie monitor added/removed while notifying cookie monitors");
            return;
        }
    }
}

static void
on_cookie_file_changed(GnomeVFSMonitorHandle *handle,
                       const gchar *monitor_uri,
                       const gchar *info_uri,
                       GnomeVFSMonitorEventType event_type,
                       gpointer user_data)
{
    cookie_monitors_notify();
}

static void
add_monitored_cookie_file(const char *path)
{
    MonitoredCookieFile *mcf;
    GnomeVFSResult result;
    
    if (monitored_files == NULL) {
        monitored_files = g_hash_table_new(g_str_hash, g_str_equal);
    }

    if (g_hash_table_lookup(monitored_files, path) != NULL) {
        /* already monitored */
        return;
    }
    
    /* This is idempotent and fairly cheap, so do it here to avoid initializing
     * gnome-vfs on application startup
     */
    gnome_vfs_init();

    mcf = g_new0(MonitoredCookieFile, 1);

    mcf->path = g_strdup(path);

    result = gnome_vfs_monitor_add(&mcf->handle,
                                   mcf->path,
                                   GNOME_VFS_MONITOR_FILE,
                                   on_cookie_file_changed,
                                   NULL);
    if (result != GNOME_VFS_OK) {
        g_warning("Failed to monitor cookie file '%s'", mcf->path);
        g_free(mcf->path);
        g_free(mcf);
        return;
    }
    
    g_hash_table_replace(monitored_files, mcf->path, mcf);
}

GSList*
hippo_load_cookies(const char *domain,
                   int         port,
                   const char *name)
{
    HippoCookieLocator *locator = hippo_cookie_locator_new();
    const char *homedir = g_get_home_dir();
    char *path;
    GSList *cookies;

    /* We load the epiphany cookies, and the cookies from all profiles of firefox; 
     * not really clear what we "should" do, how do we know which browser is someone's 
     * "main" or "current"? I guess if any browser has our login cookie, the user's 
     * account is effectively logged in from a security standpoint...
     */
    path = g_build_filename(homedir,
                            ".gnome2/epiphany/mozilla/epiphany/cookies.txt",
                            NULL);
    hippo_cookie_locator_add_file(locator, path, HIPPO_BROWSER_EPIPHANY);
    add_monitored_cookie_file(path);
    g_free(path);

    path = g_build_filename(homedir,
                            ".galeon/mozilla/galeon/cookies.txt",
                            NULL);
    hippo_cookie_locator_add_file(locator, path, HIPPO_BROWSER_GALEON);
    add_monitored_cookie_file(path);
    g_free(path);

    path = g_build_filename(homedir,
                            ".mozilla/microb/cookies.txt",
                            NULL);
    hippo_cookie_locator_add_file(locator, path, HIPPO_BROWSER_MAEMO);
    add_monitored_cookie_file(path);
    g_free(path);
    
    path = g_build_filename(homedir, ".mozilla/firefox", NULL);
    hippo_cookie_locator_add_directory(locator, path, HIPPO_BROWSER_FIREFOX);
    add_monitored_cookie_file(path);
    g_free(path);
    
    path = g_build_filename(homedir, ".firefox", NULL);
    hippo_cookie_locator_add_directory(locator, path, HIPPO_BROWSER_FIREFOX);
    add_monitored_cookie_file(path);
    g_free(path);

    cookies = hippo_cookie_locator_load_cookies(locator, domain, port, name);
    
    g_debug("Loaded %d cookies matching domain '%s' port %d cookie name '%s'",
            g_slist_length(cookies), domain, port, name);

    hippo_cookie_locator_destroy(locator);
    
    return cookies;
}

void
hippo_cookie_monitor_add (HippoCookiesMonitorFunc  func,
                          void                    *data)
{
    CookieMonitor *cm;

    cm = g_new0(CookieMonitor, 1);
    cm->func = func;
    cm->data = data;
    cookie_monitors = g_slist_append(cookie_monitors, cm);

    ++cookie_monitors_serial;
}

void
hippo_cookie_monitor_remove (HippoCookiesMonitorFunc  func,
                             void                    *data)
{
    GSList *l;

    for (l = cookie_monitors; l != NULL; l = l->next) {
        CookieMonitor *cm = l->data;

        if (cm->func == func && cm->data == data) {
            cookie_monitors = g_slist_remove(cookie_monitors, cm);
            ++cookie_monitors_serial;
            return;
        }
    }

    g_warning("Attempt to remove cookie monitor that was not found");
}

#if 0
static void
print_and_eat_cookies(GSList *cookies)
{
    int count;
    
    count = 1;
    while (cookies != NULL) {
        HippoCookie *cookie = cookies->data;

        g_print("%d '%s:%d' all_hosts=%d path='%s' secure=%d time=%lu name='%s' value='%s'\n", count,
            hippo_cookie_get_domain(cookie),
            hippo_cookie_get_port(cookie),
            hippo_cookie_get_all_hosts_match(cookie),
            hippo_cookie_get_path(cookie),
            hippo_cookie_get_secure_connection_required(cookie),
            (unsigned long) hippo_cookie_get_timestamp(cookie),
            hippo_cookie_get_name(cookie),
            hippo_cookie_get_value(cookie));
        
        count += 1;
        
        cookies = g_slist_remove(cookies, cookies->data);
        hippo_cookie_unref(cookie);
    }
}

int
main(int argc, char **argv)
{
    GSList *cookies;
    int i;
    
    cookies = hippo_load_cookies(NULL, -1, NULL);
    print_and_eat_cookies(cookies);
    
    for (i = 1; i < argc; ++i) {
        GError *error = NULL;
        cookies = hippo_load_cookies_file(argv[i], NULL, -1, NULL, &error);
        if (error != NULL) {
            g_printerr("Failed to load '%s': %s\n", argv[i], error->message);
            g_error_free(error);
        }
        
        g_print("=== %s === ", argv[i]);   
        print_and_eat_cookies(cookies);
    }
    
    return 0;
}
#endif
