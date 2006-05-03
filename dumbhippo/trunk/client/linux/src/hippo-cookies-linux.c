#include "hippo-cookies-linux.h"

GSList*
hippo_load_cookies(const char *domain,
                   int         port,
                   const char *name)
{
    GSList *cookies;
    GSList *files;
    const char *homedir;
    GDir *dir;
    char *firefox_dir;
    
    files = NULL;
    homedir = g_get_home_dir();

    /* We load the epiphany cookies, and the cookies from all copies of firefox; 
     * not really clear what we "should" do, how do we know which browser is someone's 
     * "main" or "current"? I guess if any browser has our login cookie, the user's 
     * account is effectively logged in from a security standpoint...
     */

    files = g_slist_prepend(files,
        g_build_filename(homedir,
        ".gnome2/epiphany/mozilla/epiphany/cookies.txt", NULL));

    firefox_dir = g_build_filename(homedir, ".mozilla/firefox", NULL);
    dir = g_dir_open(firefox_dir, 0, NULL); /* ignore errors */
    if (dir != NULL) {
        const char *subdir;
        while ((subdir = g_dir_read_name(dir)) != NULL) {
            char *cookie_file = g_build_filename(firefox_dir, subdir, "cookies.txt", NULL);
            if (g_file_test(cookie_file, G_FILE_TEST_EXISTS))
                files = g_slist_prepend(files, cookie_file);
        }
        g_dir_close(dir);
    }
    g_free(firefox_dir);
    
    cookies = hippo_load_cookies_files(files, domain, port, name);
    
    g_slist_foreach(files, (GFunc) g_free, NULL);
    g_slist_free(files);
    
    return cookies;
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
