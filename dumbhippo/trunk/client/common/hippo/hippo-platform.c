#include "hippo-platform.h"

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

char*
hippo_platform_read_login_cookie(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM(platform), NULL);
    
    return HIPPO_PLATFORM_GET_CLASS(platform)->read_login_cookie(platform);
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
