#include "hippo-platform-impl.h"
#include "hippo-cookies-linux.h"

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);
static char*     hippo_platform_impl_read_login_cookie   (HippoPlatform           *platform);
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


}

HippoPlatform*
hippo_platform_impl_new(HippoInstanceType instance)
{
    HippoPlatformImpl *impl = g_object_new(HIPPO_TYPE_PLATFORM_IMPL, NULL);
    impl->instance = instance;
    return HIPPO_PLATFORM(impl);
}

static char*
hippo_platform_impl_read_login_cookie(HippoPlatform *platform)
{
    GSList *cookies;
    
    cookies = hippo_load_cookies("dogfood.dumbhippo.com", 80, "auth");
    
    if (cookies != NULL) {
        HippoCookie *cookie = cookies->data;
        /* in theory the cookie value could be NULL, which is OK, but be aware */
        char *value = g_strdup(hippo_cookie_get_value(cookie));
        cookies = g_slist_remove_link(cookies, cookies);
        g_slist_foreach(cookies, (GFunc) hippo_cookie_unref, NULL);
        g_slist_free(cookies);
        hippo_cookie_unref(cookie);
        return value;
    }

    return NULL;
}

static char*
hippo_platform_impl_get_message_server(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM_IMPL(platform), NULL);
    /* FIXME */    
    return g_strdup(HIPPO_DEFAULT_MESSAGE_SERVER);
}

static char*
hippo_platform_impl_get_web_server(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM_IMPL(platform), NULL);
    /* FIXME */
    return g_strdup(HIPPO_DEFAULT_WEB_SERVER);
}

static gboolean
hippo_platform_impl_get_signin(HippoPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_PLATFORM_IMPL(platform), FALSE);
    /* FIXME */
    return TRUE;
}

static void
hippo_platform_impl_set_message_server(HippoPlatform  *platform,
                                       const char     *value)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));                                  
    /* FIXME */
}

static void
hippo_platform_impl_set_web_server(HippoPlatform  *platform,
                                   const char     *value)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));                                  
    /* FIXME */
}

static void
hippo_platform_impl_set_signin(HippoPlatform  *platform,
                               gboolean        value)
{
    g_return_if_fail(HIPPO_IS_PLATFORM(platform));                                  
    /* FIXME */
}
