#include "stdafx-hippoui.h"

#include "HippoPlatformImpl.h"
#include "HippoUIUtil.h"
#include <HippoUtil.h>
#include <Windows.h>
#include <mshtml.h>
#include <hippo/hippo-basics.h>

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);

static void      hippo_platform_impl_finalize            (GObject                 *object);

static gboolean  hippo_platform_impl_read_login_cookie   (HippoPlatform           *platform,
                                                          HippoBrowserKind        *origin_browser_p,
                                                          char                   **username_p,
                                                          char                   **password_p);
static void      hippo_platform_impl_delete_login_cookie (HippoPlatform           *platform);                                                          
static const char* hippo_platform_impl_get_jabber_resource (HippoPlatform           *platform);

static char*     hippo_platform_impl_get_message_server  (HippoPlatform           *platform); 
static char*     hippo_platform_impl_get_web_server      (HippoPlatform           *platform); 
static gboolean  hippo_platform_impl_get_signin          (HippoPlatform           *platform);
static void      hippo_platform_impl_set_message_server  (HippoPlatform           *platform,
                                                          const char              *value); 
static void      hippo_platform_impl_set_web_server      (HippoPlatform           *platform,
                                                          const char              *value); 
static void      hippo_platform_impl_set_signin          (HippoPlatform           *platform,
                                                          gboolean                 value);


struct _HippoPlatformImpl {
    GObject parent;
    HippoInstanceType instance;
    char *jabber_resource;
    HippoPreferences *preferences;
};

struct _HippoPlatformImplClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE_WITH_CODE(HippoPlatformImpl, hippo_platform_impl, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_PLATFORM, hippo_platform_impl_iface_init));
                       

static void
hippo_platform_impl_iface_init(HippoPlatformClass *klass)
{
    klass->read_login_cookie = hippo_platform_impl_read_login_cookie;
    klass->delete_login_cookie = hippo_platform_impl_delete_login_cookie;
    klass->get_jabber_resource = hippo_platform_impl_get_jabber_resource;
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
}

HippoPlatform*
hippo_platform_impl_new(HippoInstanceType  instance)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(g_object_new(HIPPO_TYPE_PLATFORM_IMPL, NULL));
    impl->instance = instance;

    impl->preferences = new HippoPreferences(instance);

    return HIPPO_PLATFORM(impl);
}

static void
hippo_platform_impl_finalize(GObject *object)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(object);

    g_free(impl->jabber_resource);
    delete impl->preferences;
    
    G_OBJECT_CLASS(hippo_platform_impl_parent_class)->finalize(object);
}

HippoPreferences*
hippo_platform_impl_get_preferences(HippoPlatformImpl *impl)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM_IMPL(impl), NULL);

    return impl->preferences;
}

static bool
startsWith(WCHAR *str, WCHAR *prefix)
{
    size_t prefixlen = wcslen(prefix);
    return wcsncmp(str, prefix, prefixlen) == 0;
}

static void
copySubstring(WCHAR *str, WCHAR *end, BSTR *to) 
{
    unsigned int length = (unsigned int)(end - str);
    HippoBSTR tmp(length, str);
    tmp.CopyTo(to);
}

static void
makeAuthUrl(const char *web_host,
            BSTR       *authUrl)
{
    // we're getting the cookies we would send to this url if we were 
    // sending this url an HTTP request. We ignore the web_port stuff
    // since browser behavior is unpredictable then, we just assume 
    // all servers have their own hostname here and elsewhere.
    HippoBSTR tmp(L"http://");
    tmp.appendUTF8(web_host, -1);
    tmp.Append(L"/");
    
    tmp.CopyTo(authUrl);
}

static void
getAuthUrl(HippoPlatform *platform,
           BSTR          *authUrl)
{
    char *web_host;
    int web_port;

    hippo_platform_get_web_host_port(platform, &web_host, &web_port);
    makeAuthUrl(web_host, authUrl);

    g_free(web_host);
}

static gboolean
do_read_login_cookie(const char       *web_host,
                     char            **username_p,
                     char            **password_p)
{
    WCHAR staticBuffer[1024];
    WCHAR *allocBuffer = NULL;
    WCHAR *cookieBuffer = staticBuffer;
    DWORD cookieSize = sizeof(staticBuffer) / sizeof(staticBuffer[0]);
    char *cookie = NULL;
    HippoBSTR authUrl;

    *username_p = NULL;
    *password_p = NULL;
    
    makeAuthUrl(web_host, &authUrl);

retry:
    if (!InternetGetCookieEx(authUrl, 
                             L"auth",
                             cookieBuffer, &cookieSize,
                             0,
                             NULL))
    {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            cookieBuffer = allocBuffer = new WCHAR[cookieSize];
            if (!cookieBuffer)
                goto out;
            goto retry;
        }
    }

    WCHAR *p = cookieBuffer;
    WCHAR *nextCookie = NULL;
    for (WCHAR *p = cookieBuffer; p < cookieBuffer + cookieSize; p = nextCookie + 1) {
        HippoBSTR host;
        HippoBSTR username;
        HippoBSTR password;

        nextCookie = wcschr(p, ';');
        if (!nextCookie)
            nextCookie = cookieBuffer + cookieSize;

        while (*p == ' ' || *p == '\t') // Skip whitespace after ;
            p++;

        if (!startsWith(p, L"auth="))
            continue;

        p += 5; // Skip 'auth='

        HippoUStr cookieValue(p, (int) (nextCookie - p));
        
        if (hippo_parse_login_cookie(cookieValue.c_str(),
                                     web_host, username_p, password_p))
            break;
    }

out:
    delete[] allocBuffer;

    return (*username_p && *password_p);
}

static gboolean
hippo_platform_impl_read_login_cookie(HippoPlatform    *platform,
                                      HippoBrowserKind *origin_browser_p,
                                      char            **username_p,
                                      char            **password_p)
{
    char *web_host;
    int web_port;

    *origin_browser_p = HIPPO_BROWSER_IE;

    hippo_platform_get_web_host_port(platform, &web_host, &web_port);
    
    g_debug("Looking for login to %s:%d", web_host, web_port);

    gboolean result = do_read_login_cookie(web_host, username_p, password_p);

    g_free(web_host);

    return result;
}

static void
hippo_platform_impl_delete_login_cookie(HippoPlatform *platform)
{
    HippoBSTR authUrl;
    getAuthUrl(platform, &authUrl);
    InternetSetCookie(authUrl, NULL,  L"auth=; Path=/");
}

void
hippo_platform_impl_windows_migrate_cookie(const char *from_web_host,
                                           const char *to_web_host)
{
    char *username;
    char *password;

    // See if we already have a cookie from the new host
    if (do_read_login_cookie(to_web_host, &username, &password)) {
        g_free(username);
        g_free(password);

        return;
    }

    if (!do_read_login_cookie(from_web_host, &username, &password))
        return;

    GDate *date = g_date_new();
    GTimeVal timeval;
    g_get_current_time(&timeval);
    g_date_set_time_val(date, &timeval);
    g_date_add_days(date, 5 * 365); // 5 years, more or less

    // Can't use g_date_strftime, since that would be unpredictably located
    // while we need fixed english-locale DAY, DD-MMM-YYYY HH:MM:SS GMT

    static const char *days[] = {
        "Mon", "Tue", "Wed", "The", "Fri", "Sat", "Sun"
    };

    static const char * const months[] = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    char *cookieUTF8 = g_strdup_printf("auth=host=%s&name=%s&password=%s; Path=/; expires = %s, %02d-%s-%04d 00:00:00 GMT", 
                                       to_web_host, 
                                       username, 
                                       password, 
                                       days[(int)g_date_get_weekday(date) - 1],
                                       g_date_get_day(date),
                                       months[(int)g_date_get_month(date) - 1],
                                       g_date_get_year(date));
    g_date_free(date);

    HippoBSTR cookie;
    cookie.setUTF8(cookieUTF8, -1);
    g_free(cookieUTF8);
    
    HippoBSTR authUrl;
    makeAuthUrl(to_web_host, &authUrl);
    InternetSetCookie(authUrl, NULL, cookie);

    g_free(username);
    g_free(password);
}

static const char*
hippo_platform_impl_get_jabber_resource(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    if (impl->jabber_resource == NULL) {
        // Create an XMPP resource identifier based on this machine's hardware
        // profile GUID.
        HW_PROFILE_INFO hwProfile;
        if (GetCurrentHwProfile(&hwProfile)) {
            HippoUStr guidUTF(hwProfile.szHwProfileGuid);
            impl->jabber_resource = g_strdup(guidUTF.c_str());
        } else {
            hippoDebugLogW(L"Failed to get hardware profile!");

            // uhhh... let's just make up a number, better than bombing out
            GTimeVal val;
            g_get_current_time(&val);
            impl->jabber_resource = g_strdup_printf("%d", val.tv_sec);
        }

        g_debug("jabber resource: '%s'", impl->jabber_resource);
    }
    return impl->jabber_resource;
}

static char*
hippo_platform_impl_get_message_server(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    HippoBSTR messageServer;

    impl->preferences->getMessageServer(&messageServer);

    HippoUStr messageServerUTF(messageServer);
    return g_strdup(messageServerUTF.c_str());
}

static char*
hippo_platform_impl_get_web_server(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    HippoBSTR webServer;

    impl->preferences->getWebServer(&webServer);

    HippoUStr webServerUTF(webServer);
    return g_strdup(webServerUTF.c_str());
}

static gboolean
hippo_platform_impl_get_signin(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    return impl->preferences->getSignIn();
}

static void
hippo_platform_impl_set_message_server(HippoPlatform  *platform,
                                       const char     *value)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    impl->preferences->setMessageServer(HippoBSTR::fromUTF8(value, -1));
}

static void
hippo_platform_impl_set_web_server(HippoPlatform  *platform,
                                   const char     *value)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    impl->preferences->setWebServer(HippoBSTR::fromUTF8(value, -1));
}

static void
hippo_platform_impl_set_signin(HippoPlatform  *platform,
                               gboolean        value)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    impl->preferences->setSignIn(value != FALSE);
}
