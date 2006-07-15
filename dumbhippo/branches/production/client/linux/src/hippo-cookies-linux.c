#include "hippo-cookies-linux.h"
#include <string.h>

#define MAX_FILES 10

static void
check_cookie_file(const char       *cookie_file,
                  HippoCookiesFile *files,
                  int              *n_files)
{
    if (g_file_test(cookie_file, G_FILE_TEST_EXISTS)) {
        files[*n_files].filename = g_strdup(cookie_file);
        files[*n_files].browser = HIPPO_BROWSER_FIREFOX;
        /* g_debug("  using as cookie file %d", *n_files); */
        (*n_files)++;
    }
}

/*
 * Look for cookies files.  They can either be included in
 * ~/.mozilla/firefox (modern versions of the browser) or they might
 * be included in ~/.firefox.  Additionally, there might be salted
 * directory names in the profile directory so we have to search to at
 * least a depth of one directory to find the profiles.
 */

static void
check_firefox_dir(const char       *firefox_dir,
                  HippoCookiesFile *files,
                  int              *n_files)
{
    GDir *dir;

    dir = g_dir_open(firefox_dir, 0, NULL); /* ignore errors */
    if (dir != NULL) {
        const char *subdir;

        while ((subdir = g_dir_read_name(dir)) != NULL) {
            char *subdirfull;
            char *cookie_file;

            /* g_debug("Reading firefox subdir/file '%s'", subdir); */

            if (*n_files >= MAX_FILES)
                break;

            if (strcmp(subdir, "Cache") == 0) {
                /* this saves a lot of IO */
                /* g_debug("Skipping firefox cache dir"); */
                continue;
            }

            cookie_file = g_build_filename(firefox_dir, subdir, "cookies.txt", NULL);
            /* g_debug("Checking for cookies file '%s'\n", cookie_file); */
            check_cookie_file(cookie_file, files, n_files);
            g_free(cookie_file);

            /* Also check for salted directories in the mozilla profile
               directories and discover the joy of recursion. */
            subdirfull = g_build_filename(firefox_dir, subdir, NULL);
            /* g_debug("checking if '%s' is a directory...", subdirfull); */
            if (g_file_test(subdirfull, G_FILE_TEST_IS_DIR)) {
                char *subsubdir = g_build_filename(firefox_dir, subdir, NULL);
                /* g_debug("'%s' is a directory...", subsubdir); */
                check_firefox_dir(subsubdir, files, n_files);
                g_free(subsubdir);
            }
            g_free(subdirfull);
        }
        g_dir_close(dir);
    }
}

GSList*
hippo_load_cookies(const char *domain,
                   int         port,
                   const char *name)
{
    HippoCookiesFile files[MAX_FILES];
    int n_files;
    GSList *cookies;
    const char *homedir;
    char *firefox_dir;
    
    homedir = g_get_home_dir();

    n_files = 0;

    /* We load the epiphany cookies, and the cookies from all profiles of firefox; 
     * not really clear what we "should" do, how do we know which browser is someone's 
     * "main" or "current"? I guess if any browser has our login cookie, the user's 
     * account is effectively logged in from a security standpoint...
     */
    files[n_files].filename = g_build_filename(homedir,
        ".gnome2/epiphany/mozilla/epiphany/cookies.txt", NULL);
    files[n_files].browser = HIPPO_BROWSER_EPIPHANY;
    ++n_files;

    firefox_dir = g_build_filename(homedir, ".mozilla/firefox", NULL);
    check_firefox_dir(firefox_dir, files, &n_files);
    g_free(firefox_dir);
    
    firefox_dir = g_build_filename(homedir, ".firefox", NULL);
    check_firefox_dir(firefox_dir, files, &n_files);
    g_free(firefox_dir);

    cookies = hippo_load_cookies_files(files, n_files, domain, port, name);
    
    g_debug("Loaded %d cookies matching domain '%s' port %d cookie name '%s'",
            g_slist_length(cookies), domain, port, name);
    
    do {
        --n_files;
        g_free(files[n_files].filename);
    } while (n_files > 0);
    
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
