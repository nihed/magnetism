/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-cookies-linux.h"
#include <string.h>

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
    g_free(path);

    path = g_build_filename(homedir, ".mozilla/firefox", NULL);
    hippo_cookie_locator_add_directory(locator, path, HIPPO_BROWSER_FIREFOX);
    g_free(path);
    
    path = g_build_filename(homedir, ".firefox", NULL);
    hippo_cookie_locator_add_directory(locator, path, HIPPO_BROWSER_FIREFOX);
    g_free(path);

    cookies = hippo_cookie_locator_load_cookies(locator, domain, port, name);
    
    g_debug("Loaded %d cookies matching domain '%s' port %d cookie name '%s'",
            g_slist_length(cookies), domain, port, name);

    hippo_cookie_locator_destroy(locator);
    
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
