/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "stdafx-hippoui.h"

#include "HippoPlatformImpl.h"
#include "HippoUIUtil.h"
#include "HippoHttp.h"
#include "HippoUI.h"
#include "HippoWindowWin.h"
#include <HippoUtil.h>
#include <ShlObj.h>
#include <Windows.h>
#include <mshtml.h>
#include <hippo/hippo-basics.h>
#include <stacker/hippo-stacker-platform.h>

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);
static void      hippo_platform_impl_stacker_iface_init  (HippoStackerPlatformClass *klass);

static void      hippo_platform_impl_finalize            (GObject                 *object);

static void      hippo_platform_impl_get_platform_info   (HippoPlatform           *platform,
                                                          HippoPlatformInfo       *info);
static gboolean  hippo_platform_impl_read_login_cookie   (HippoPlatform           *platform,
                                                          HippoBrowserKind        *origin_browser_p,
                                                          char                   **username_p,
                                                          char                   **password_p);
static void      hippo_platform_impl_delete_login_cookie (HippoPlatform           *platform);                                                          
static const char* hippo_platform_impl_get_jabber_resource (HippoPlatform           *platform);

static HippoInstanceType hippo_platform_impl_get_instance_type (HippoPlatform *platform);

static char*     hippo_platform_impl_get_message_server  (HippoPlatform           *platform); 
static char*     hippo_platform_impl_get_web_server      (HippoPlatform           *platform); 
static gboolean  hippo_platform_impl_get_signin          (HippoPlatform           *platform);
static void      hippo_platform_impl_set_message_server  (HippoPlatform           *platform,
                                                          const char              *value); 
static void      hippo_platform_impl_set_web_server      (HippoPlatform           *platform,
                                                          const char              *value); 
static void      hippo_platform_impl_set_signin          (HippoPlatform           *platform,
                                                          gboolean                 value);
static HippoWindow* hippo_platform_impl_create_window       (HippoPlatform     *platform);
static void         hippo_platform_impl_get_screen_info     (HippoPlatform     *platform,
                                                             HippoRectangle    *monitor_rect_p,
                                                             HippoRectangle    *tray_icon_rect_p,
                                                             HippoOrientation  *tray_icon_orientation_p);
static gboolean     hippo_platform_impl_get_pointer_position (HippoPlatform    *platform,
                                                              int              *x_p,
                                                              int              *y_p);
static void         hippo_platform_impl_open_url            (HippoPlatform     *platform,
                                                             HippoBrowserKind   browser,
                                                             const char        *url);
static void         hippo_platform_impl_http_request        (HippoPlatform     *platform,
                                                             const char        *url,
                                                             HippoHttpFunc      func,
                                                             void              *data);

static void             hippo_platform_impl_show_chat_window    (HippoPlatform  *platform,
                                                                 const char     *chat_id);
static HippoWindowState hippo_platform_impl_get_chat_window_state(HippoPlatform *platform,
                                                                  const char    *chat_id);

static gboolean     hippo_platform_impl_can_play_song_download (HippoPlatform     *platform,
                                                                HippoSongDownload *song_download);


struct _HippoPlatformImpl {
    GObject parent;
    HippoInstanceType instance;
    char *jabber_resource;
    HippoPreferences *preferences;
    HippoHTTP *http;
    HippoUI *ui;
};

struct _HippoPlatformImplClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE_WITH_CODE(HippoPlatformImpl, hippo_platform_impl, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_PLATFORM, hippo_platform_impl_iface_init);
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_STACKER_PLATFORM, hippo_stacker_platform_impl_iface_init));

static void
hippo_platform_impl_iface_init(HippoPlatformClass *klass)
{
    klass->get_platform_info = hippo_platform_impl_get_platform_info;
    klass->read_login_cookie = hippo_platform_impl_read_login_cookie;
    klass->delete_login_cookie = hippo_platform_impl_delete_login_cookie;
    klass->get_jabber_resource = hippo_platform_impl_get_jabber_resource;
    klass->open_url = hippo_platform_impl_open_url;
    klass->get_instance_type = hippo_platform_impl_get_instance_type;
    klass->get_message_server = hippo_platform_impl_get_message_server;
    klass->get_web_server = hippo_platform_impl_get_web_server;
    klass->get_signin = hippo_platform_impl_get_signin;
    klass->set_message_server = hippo_platform_impl_set_message_server;
    klass->set_web_server = hippo_platform_impl_set_web_server;
    klass->set_signin = hippo_platform_impl_set_signin;
}

static void
hippo_platform_impl_stacker_iface_init(HippoStackerPlatformClass *klass)
{
    klass->create_window = hippo_platform_impl_create_window;
    klass->get_screen_info = hippo_platform_impl_get_screen_info;
    klass->get_pointer_position = hippo_platform_impl_get_pointer_position;
    klass->http_request = hippo_platform_impl_http_request;
    klass->show_chat_window = hippo_platform_impl_show_chat_window;
    klass->get_chat_window_state = hippo_platform_impl_get_chat_window_state;
    klass->can_play_song_download = hippo_platform_impl_can_play_song_download;
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

    if (impl->http)
        delete impl->http;

    G_OBJECT_CLASS(hippo_platform_impl_parent_class)->finalize(object);
}

void
hippo_platform_impl_set_ui(HippoPlatformImpl *impl,
                           HippoUI           *ui)
{
    impl->ui = ui;
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
read_ie_login_cookie(const char       *web_host,
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
    *cookieBuffer = 0;
    if (!InternetGetCookieEx(authUrl, 
                             L"auth",
                             cookieBuffer, &cookieSize,
                             0,
                             NULL))
    {
        HRESULT error = GetLastError();
        if (error == ERROR_INSUFFICIENT_BUFFER) {
            cookieBuffer = allocBuffer = new WCHAR[cookieSize];
            if (!cookieBuffer)
                goto out;
            goto retry;
        } else {
            hippoDebugLogW(L"Failed to get auth cookie %d", (int) error);
            cookieSize = 0;
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
    
        if (hippo_parse_login_cookie(cookieValue.c_str(), web_host, username_p, password_p)) {
            //hippoDebugLogU("using cookie '%s'", cookieValue.c_str());
            break;
        }

        //hippoDebugLogU("skipping cookie '%s'", cookieValue.c_str());
    }

out:
    delete[] allocBuffer;

    return (*username_p && *password_p);
}

static void
store_ie_login_cookie(const char  *web_host,
                      int          web_port,
                      const char  *username,
                      const char  *password)
{
    HippoBSTR authUrl;
    
    makeAuthUrl(web_host, &authUrl);

    char *value = g_strdup_printf("host=%s&name=%s&password=%s; expires=Thu, 31-Dec-2020 23:59:59 GMT; path=/",
                                  web_host, username, password);
    HippoBSTR valueW = HippoBSTR::fromUTF8(value);
    g_free(value);

    InternetSetCookie(authUrl.m_str, L"auth", valueW.m_str);
}

static char *
get_firefox_dir(void)
{
    WCHAR path[MAX_PATH];
    SHGetFolderPath(NULL, CSIDL_APPDATA, NULL, SHGFP_TYPE_CURRENT, path);

    HippoBSTR result(path);
    result.Append(L"\\Mozilla\\Firefox");
    
    HippoUStr resultU(result);
    
    return g_strdup(resultU.c_str());
}

static gboolean
read_firefox_login_cookie(const char       *web_host,
                          char            **username_p,
                          char            **password_p)
{
    HippoCookieLocator *locator = hippo_cookie_locator_new();
    const char *homedir = g_get_home_dir();
    char *path;
    GSList *cookies;
    gboolean result = FALSE;

    path = get_firefox_dir();
    hippo_cookie_locator_add_directory(locator, path, HIPPO_BROWSER_FIREFOX);
    g_free(path);

    cookies = hippo_cookie_locator_load_cookies(locator, web_host, -1, "auth");
    if (cookies) {
        HippoCookie *cookie = (HippoCookie *)cookies->data;
        const char *value = hippo_cookie_get_value(cookie);
        result = hippo_parse_login_cookie(value, web_host, username_p, password_p);
    }

    /* Free cookies! */
    g_slist_foreach(cookies, (GFunc) hippo_cookie_unref, NULL);
    g_slist_free(cookies);

    hippo_cookie_locator_destroy(locator);

    return result;
}

static gboolean
hippo_platform_impl_read_login_cookie(HippoPlatform    *platform,
                                      HippoBrowserKind *origin_browser_p,
                                      char            **username_p,
                                      char            **password_p)
{
    char *web_host;
    int web_port;

    hippo_platform_get_web_host_port(platform, &web_host, &web_port);
    
    g_debug("Looking for login to %s:%d", web_host, web_port);

    gboolean result = read_ie_login_cookie(web_host, username_p, password_p);

    if (result) {
        *origin_browser_p = HIPPO_BROWSER_IE;
    } else if (!result) {
        result = read_firefox_login_cookie(web_host, username_p, password_p);
        if (result) {
            *origin_browser_p = HIPPO_BROWSER_FIREFOX;

            /* Store the cookie we found from firefox into the system-wide
             * cookie store used by IE so that our embedded use of IE
             * (and direct calls to WinInet) pick it up.
             */
            store_ie_login_cookie(web_host, web_port, *username_p, *password_p);
        }
    }
    
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
    if (read_ie_login_cookie(to_web_host, &username, &password)) {
        g_free(username);
        g_free(password);

        return;
    }

    if (!read_ie_login_cookie(from_web_host, &username, &password))
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

static void
hippo_platform_impl_get_platform_info(HippoPlatform           *platform,
                                      HippoPlatformInfo       *info)
{
    info->name = "windows";
    info->distribution = NULL; // Unused for windows
    info->version = NULL;      // FIXME: should be "nt2000", "xp", "vista"
    info->architecture = NULL; // FIXME: should be "i386" for win32 or "x86_64" for win64
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

static HippoInstanceType
hippo_platform_impl_get_instance_type(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    return impl->instance;
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

static HippoWindow*
hippo_platform_impl_create_window(HippoPlatform     *platform)
{
    return hippo_window_win_new(HIPPO_PLATFORM_IMPL(platform)->ui);
}

struct TrayIconInfo {
    HippoOrientation orientation;
    RECT rect;
};

BOOL CALLBACK
enum_tray_icon_child(HWND child, LPARAM lParam)
{
    TrayIconInfo *info = (TrayIconInfo *)lParam;

    WCHAR className[256];
    GetClassName(child, className, 256);
    if (wcscmp(className, L"TrayNotifyWnd") == 0) {
        // Aha, found the notification tray, this is a better guess than
        // the whole taskbar. We stop here; the codeproject code goes further
        // and cuts out the clock (as below), but I think it actually looks 
        // a bit better this way ... going to the whole notification area
        // makes it clear to the user that it's just an approximation
        GetWindowRect(child, &info->rect);
        return FALSE;
    }
#if 0    
    else if (wcscmp(className, L"TrayClockWClass") == 0) {
        // The clock sits either to the right of the taskbar (horizontal orientation)
        // or above it (vertical orientation)
        RECT clockRect;
        GetWindowRect(child, &clockRect);
        if (info->orientation == HIPPO_ORIENTATION_HORIZONTAL)
            info->rect.right = clockRect.left;
        else
            info->rect.top = clockRect.bottom;

        return FALSE;
    }
#endif

    return TRUE; // Keep going
}

// The basic technique here comes from http://www.codeproject.com/shell/trayposition.asp
static gboolean
find_icon_tray_rect(RECT            *iconTrayRect,
                    HippoOrientation orientation)
{
    HWND trayWindow = FindWindow(L"Shell_TrayWnd", NULL);
    TrayIconInfo info;

    info.orientation = orientation;

    if (!trayWindow)
        return FALSE;

    // This gets the entire taskbar size, we use this as a fallback, if our
    // child window search fails
    GetWindowRect(trayWindow, &info.rect);

    // We refine the taskbar 
    EnumChildWindows(trayWindow, enum_tray_icon_child, (LPARAM)&info);

    *iconTrayRect = info.rect;
    
    return TRUE;
}

static void
hippo_platform_impl_get_screen_info(HippoPlatform     *platform,
                                    HippoRectangle    *monitor_rect_p,
                                    HippoRectangle    *tray_icon_rect_p,
                                    HippoOrientation  *tray_icon_orientation_p)
{
    APPBARDATA abd;
    abd.cbSize = sizeof(abd);
    if (!SHAppBarMessage(ABM_GETTASKBARPOS, &abd)) {
        g_warning("Failed to get task bar extents");
        return;
    }

    HippoOrientation orientation;
    switch (abd.uEdge) {
        case ABE_BOTTOM:
        case ABE_TOP:
            orientation = HIPPO_ORIENTATION_HORIZONTAL;
            break;
        case ABE_LEFT:
        case ABE_RIGHT:
            orientation = HIPPO_ORIENTATION_VERTICAL;
            break;
        default:
            g_warning("unknown tray icon orientation");
            break;
    }

    if (tray_icon_orientation_p)
        *tray_icon_orientation_p = orientation;

    RECT iconTrayRect;

    if (!find_icon_tray_rect(&iconTrayRect, orientation)) {
        // If this starts happening  regularly, we can refine
        // this code to make a better guess at that point.
        iconTrayRect = abd.rc;
    }

    if (tray_icon_rect_p) {
        tray_icon_rect_p->x = iconTrayRect.left;
        tray_icon_rect_p->width = iconTrayRect.right - iconTrayRect.left;
        tray_icon_rect_p->y = iconTrayRect.top;
        tray_icon_rect_p->height = iconTrayRect.bottom - iconTrayRect.top;
    }

    if (monitor_rect_p) {
        HMONITOR monitor = MonitorFromRect(&iconTrayRect, MONITOR_DEFAULTTONEAREST);
        MONITORINFO monitorInfo;
        monitorInfo.cbSize = sizeof(monitorInfo);
        if (GetMonitorInfo(monitor, &monitorInfo)) {
            monitor_rect_p->x = monitorInfo.rcWork.left;
            monitor_rect_p->y = monitorInfo.rcWork.top;
            monitor_rect_p->width = monitorInfo.rcWork.right - monitorInfo.rcWork.left;
            monitor_rect_p->height = monitorInfo.rcWork.bottom - monitorInfo.rcWork.top;
        } else {
            g_warning("GetMonitorInfo failed"); // Shouldn't happen, don't both with a fallback
        }
    }
}

static gboolean
hippo_platform_impl_get_pointer_position (HippoPlatform *platform,
                                          int           *x_p,
                                          int           *y_p)
{
    POINT point = { 0, 0 };

    bool result = GetCursorPos(&point) != 0;

    if (x_p)
        *x_p = point.x;
    if (y_p)
        *y_p = point.y;

    return result;
}

static void
hippo_platform_impl_open_url(HippoPlatform     *platform,
                             HippoBrowserKind   browser,
                             const char        *url)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    if (!impl->ui) {
        g_warning("trying to hippo_platform_open_url before ui set on platform object");
        return;
    }

    // FIXME this really is not right at all, it ignores the browser kind, 
    // but let's sort it out as part of getting firefox to work since 
    // I'm not even sure what API HippoPlatform should have here.

    impl->ui->LaunchBrowser(HippoBSTR::fromUTF8(url));
}

class HttpHandler : public HippoHTTPAsyncHandler
{
public:
    HttpHandler(HippoHttpFunc cCallback, void *cCallbackData)
        : cCallback_(cCallback), cCallbackData_(cCallbackData), errorCode_(S_OK) {
        
    }

    ~HttpHandler() {
        
    }

    virtual void handleError(HRESULT result) {
        errorCode_ = result;
        finish(NULL, 0);
    }
    virtual void handleGotSize(long responseSize) { };
    virtual void handleContentType(WCHAR *mimetype, WCHAR *charset) {
        contentType_.setUTF16(mimetype);
    }
    virtual void handleBytesRead(void *responseData, long responseBytes) { };
    virtual void handleComplete(void *responseData, long responseBytes) {
        finish(responseData, responseBytes);
    }

private:
    HippoHttpFunc cCallback_;
    void *cCallbackData_;
    HippoUStr contentType_;
    HRESULT errorCode_;

    // my reading of HippoHTTP.h is that exactly one of handleError or handleComplete
    // is guaranteed to be called, so if those each call finish() it's safe to 
    // "delete this" here.
    void finish(void *responseData, long responseBytes) {
        if (cCallback_ == NULL) // pointless since we'll self-delete anyway
            return;
        if (contentType_.c_str() != NULL && responseData) {
            GString *str = g_string_new_len((char*) responseData, responseBytes);
            (* cCallback_) (contentType_.c_str(), str, cCallbackData_);
            g_string_free(str, TRUE);
        } else {
            GString *str = g_string_new(NULL);
            g_string_append_printf(str, "HTTP error: %d", errorCode_);
            (* cCallback_) (NULL, str, cCallbackData_);
            g_string_free(str, TRUE);
        }
        cCallback_ = NULL;
        cCallbackData_ = NULL;

        delete this;
    }
};

static void
hippo_platform_impl_http_request(HippoPlatform     *platform,
                                 const char        *url,
                                 HippoHttpFunc      func,
                                 void              *data)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    if (impl->http == NULL) {
        impl->http = new HippoHTTP();
    }

    // we use the cache here, even though on Linux the http handler 
    // does not, so maybe we should not either. Also on linux 
    // we'll never use cookies from the browser while I bet we do 
    // here.
    HippoBSTR urlW = HippoBSTR::fromUTF8(url);
    impl->http->doGet(urlW, true /* use cache */, new HttpHandler(func, data));
}

static void
hippo_platform_impl_show_chat_window(HippoPlatform   *platform,
                                     const char      *chat_id)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    if (!impl->ui) {
        g_warning("trying to hippo_platform_show_chat_window before ui set on platform object");
        return;
    }

    impl->ui->ShowChatWindow(HippoBSTR::fromUTF8(chat_id));
}

static HippoWindowState 
hippo_platform_impl_get_chat_window_state(HippoPlatform *platform,
                                          const char    *chat_id)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    if (!impl->ui) {
        g_warning("trying to hippo_platform_get_chat_window_state before ui set on platform object");
        return HIPPO_WINDOW_STATE_CLOSED;
    }

    return impl->ui->getChatWindowState(HippoBSTR::fromUTF8(chat_id));
}


static gboolean
hippo_platform_impl_can_play_song_download(HippoPlatform     *platform,
                                           HippoSongDownload *song_download)
{
    switch (hippo_song_download_get_source(song_download)) {
    case HIPPO_SONG_DOWNLOAD_ITUNES:
        return TRUE;
    case HIPPO_SONG_DOWNLOAD_YAHOO:
        return TRUE;
    case HIPPO_SONG_DOWNLOAD_RHAPSODY:
        return TRUE;
    }

    return TRUE;
}
