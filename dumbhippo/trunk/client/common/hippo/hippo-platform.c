/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-platform.h"
#include "hippo-connection.h"

static void hippo_platform_base_init(void *klass);

GType
hippo_platform_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info = 
        {
            sizeof(HippoPlatformClass),
            hippo_platform_base_init,
            NULL /* base_finalize */
        };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoPlatform",
                                      &info, 0);
    }
    
    return type;
}

static void
hippo_platform_base_init(void *klass)
{
    static gboolean initialized = FALSE;
    
    if (!initialized) {
        /* create signals in here */      
    
        initialized = TRUE;   
    }
}

HippoWindow*
hippo_platform_create_window(HippoPlatform    *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->create_window(platform);
}

void
hippo_platform_get_screen_info(HippoPlatform    *platform,
                               HippoRectangle   *monitor_rect_p,
                               HippoRectangle   *tray_icon_rect_p,
                               HippoOrientation *tray_icon_orientation_p)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));
    
    HIPPO_PLATFORM_GET_CLASS(platform)->get_screen_info(platform,
                                                        monitor_rect_p,
                                                        tray_icon_rect_p,
                                                        tray_icon_orientation_p);
}

gboolean
hippo_platform_read_login_cookie(HippoPlatform    *platform,
                                 HippoBrowserKind *origin_browser_p,
                                 char            **username_p,
                                 char            **password_p)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), FALSE);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->read_login_cookie(platform, origin_browser_p, username_p, password_p);
}

void
hippo_platform_delete_login_cookie(HippoPlatform *platform)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));

    HIPPO_PLATFORM_GET_CLASS(platform)->delete_login_cookie(platform);
}

const char*
hippo_platform_get_jabber_resource(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);

    return HIPPO_PLATFORM_GET_CLASS(platform)->get_jabber_resource(platform);
}

void
hippo_platform_open_url(HippoPlatform   *platform,
                        HippoBrowserKind browser,
                        const char      *url)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));

    HIPPO_PLATFORM_GET_CLASS(platform)->open_url(platform,
                                                 browser,
                                                 url);
}

void
hippo_platform_http_request(HippoPlatform   *platform,
                            const char      *url,
                            HippoHttpFunc    func,
                            void            *data)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));

    HIPPO_PLATFORM_GET_CLASS(platform)->http_request(platform,
                                                     url,
                                                     func,
                                                     data);
}

void
hippo_platform_show_chat_window(HippoPlatform   *platform,
                                const char      *chat_id)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));

    HIPPO_PLATFORM_GET_CLASS(platform)->show_chat_window(platform,
                                                         chat_id);
}

char*
hippo_platform_get_message_server(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->get_message_server(platform);    
}

char*
hippo_platform_get_web_server(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->get_web_server(platform);    
}

gboolean
hippo_platform_get_signin(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), FALSE);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->get_signin(platform);    
}

void
hippo_platform_set_message_server(HippoPlatform  *platform,
                                  const char     *value)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));                                  
    HIPPO_PLATFORM_GET_CLASS(platform)->set_message_server(platform, value);                              
}

void
hippo_platform_set_web_server(HippoPlatform  *platform,
                              const char     *value)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));                                  
    HIPPO_PLATFORM_GET_CLASS(platform)->set_web_server(platform, value);                              
}

void
hippo_platform_set_signin(HippoPlatform  *platform,
                          gboolean        value)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));                                  
    HIPPO_PLATFORM_GET_CLASS(platform)->set_signin(platform, value);                              
}

void
hippo_platform_get_message_host_port(HippoPlatform  *platform,
                                     char          **host_p,
                                     int            *port_p)
{
    char *server = hippo_platform_get_message_server(platform);
    hippo_parse_message_server(server, host_p, port_p);
    g_free(server);
}

void
hippo_platform_get_web_host_port(HippoPlatform  *platform,
                                 char          **host_p,
                                 int            *port_p)
{
    char *server = hippo_platform_get_web_server(platform);
    hippo_parse_web_server(server, host_p, port_p);
    g_free(server);
}
