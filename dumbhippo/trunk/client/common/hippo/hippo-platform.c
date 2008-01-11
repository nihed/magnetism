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

enum {
    NETWORK_STATUS_CHANGED,
    COOKIES_MAYBE_CHANGED,
    LAST_SIGNAL
};
static int signals[LAST_SIGNAL];

static void
hippo_platform_base_init(void *klass)
{
    static gboolean initialized = FALSE;
    
    if (!initialized) {
        /* create signals in here */      

        signals[NETWORK_STATUS_CHANGED] =
            g_signal_new ("network-status-changed",
                          HIPPO_TYPE_PLATFORM,
                          G_SIGNAL_RUN_LAST,
                          0,
                          NULL, NULL,
                          g_cclosure_marshal_VOID__INT,
                          G_TYPE_NONE, 1, G_TYPE_INT);        
        
        signals[COOKIES_MAYBE_CHANGED] =
            g_signal_new ("cookies-maybe-changed",
                          HIPPO_TYPE_PLATFORM,
                          G_SIGNAL_RUN_LAST,
                          0,
                          NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);
        
        initialized = TRUE;   
    }
}

void
hippo_platform_get_platform_info (HippoPlatform     *platform,
                                  HippoPlatformInfo *info)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));
    
    HIPPO_PLATFORM_GET_CLASS(platform)->get_platform_info(platform, info);
}

gboolean
hippo_platform_read_login_cookie(HippoPlatform    *platform,
                                 HippoServerType   web_server_type,
                                 HippoBrowserKind *origin_browser_p,
                                 char            **username_p,
                                 char            **password_p)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), FALSE);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->read_login_cookie(platform, web_server_type,
                                                                 origin_browser_p, username_p, password_p);
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

HippoInstanceType 
hippo_platform_get_instance_type (HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), HIPPO_INSTANCE_NORMAL);
    return HIPPO_PLATFORM_GET_CLASS(platform)->get_instance_type(platform);
}

char*
hippo_platform_get_message_server(HippoPlatform  *platform,
                                  HippoServerType server_type)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->get_message_server(platform, server_type);    
}

char*
hippo_platform_get_web_server(HippoPlatform  *platform,
                              HippoServerType server_type)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->get_web_server(platform, server_type); 
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

char *
hippo_platform_make_cache_filename (HippoPlatform  *platform,
                                    const char     *server,
                                    const char     *user_id)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);

    if (HIPPO_PLATFORM_GET_CLASS(platform)->make_cache_filename)
        return HIPPO_PLATFORM_GET_CLASS(platform)->make_cache_filename(platform, server, user_id);
    else
        return NULL;
}

void
hippo_platform_get_message_host_port(HippoPlatform  *platform,
                                     HippoServerType server_type,
                                     char          **host_p,
                                     int            *port_p)
{
    char *server = hippo_platform_get_message_server(platform, server_type);
    hippo_parse_message_server(server, hippo_platform_get_instance_type(platform),
                               server_type, host_p, port_p);
    g_free(server);
}

void
hippo_platform_get_web_host_port(HippoPlatform  *platform,
                                 HippoServerType server_type,
                                 char          **host_p,
                                 int            *port_p)
{
    char *server = hippo_platform_get_web_server(platform, server_type);
    hippo_parse_web_server(server, hippo_platform_get_instance_type(platform),
                           server_type, host_p, port_p);
    g_free(server);
}

HippoNetworkStatus
hippo_platform_get_network_status(HippoPlatform *platform)
{
    HippoPlatformClass *klass;
    
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), HIPPO_NETWORK_STATUS_UNKNOWN);

    klass = HIPPO_PLATFORM_GET_CLASS(platform);
    
    if (klass->get_network_status != NULL) {
        return (* klass->get_network_status) (platform);
    } else {
        return HIPPO_NETWORK_STATUS_UNKNOWN;
    }
}

void
hippo_platform_emit_network_status_changed (HippoPlatform *platform,
                                            HippoNetworkStatus status)
{
    g_signal_emit(G_OBJECT(platform), signals[NETWORK_STATUS_CHANGED], 0, status);
}

void
hippo_platform_emit_cookies_maybe_changed (HippoPlatform *platform)
{
    g_signal_emit(G_OBJECT(platform), signals[COOKIES_MAYBE_CHANGED], 0);
}
