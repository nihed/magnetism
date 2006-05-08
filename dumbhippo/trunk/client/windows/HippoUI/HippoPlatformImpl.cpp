#include <stdafx.h>

#include "HippoPlatformImpl.h"

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);
static gboolean  hippo_platform_impl_read_login_cookie   (HippoPlatform           *platform,
                                                          char                   **username_p,
                                                          char                   **password_p);
static void      hippo_platform_impl_delete_login_cookie (HippoPlatform           *platform);                                                          
static char*     hippo_platform_impl_get_jabber_resource (HippoPlatform           *platform);

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


}

HippoPlatform*
hippo_platform_impl_new(HippoInstanceType instance)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(g_object_new(HIPPO_TYPE_PLATFORM_IMPL, NULL));
    impl->instance = instance;
    return HIPPO_PLATFORM(impl);
}

static gboolean
hippo_platform_impl_read_login_cookie(HippoPlatform *platform,
                                      char         **username_p,
                                      char         **password_p)
{
    /* FIXME */
    return FALSE;
}

static void
hippo_platform_impl_delete_login_cookie(HippoPlatform *platform)
{
    /* FIXME */
}

static char*
hippo_platform_impl_get_jabber_resource(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);
    
    if (impl->jabber_resource == NULL) {
        /* FIXME */
        g_debug("jabber resource: '%s'", impl->jabber_resource);
    }
    return impl->jabber_resource;
}


static char*
hippo_platform_impl_get_message_server(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    /* FIXME */

    if (impl->instance == HIPPO_INSTANCE_DOGFOOD)
        return g_strdup("dogfood.dumbhippo.com:21020");
    else
        return g_strdup(HIPPO_DEFAULT_MESSAGE_SERVER);
}

static char*
hippo_platform_impl_get_web_server(HippoPlatform *platform)
{
    HippoPlatformImpl *impl = HIPPO_PLATFORM_IMPL(platform);

    /* FIXME */

    if (impl->instance == HIPPO_INSTANCE_DOGFOOD)
        return g_strdup("dogfood.dumbhippo.com:9080");
    else
        return g_strdup(HIPPO_DEFAULT_WEB_SERVER);
}

static gboolean
hippo_platform_impl_get_signin(HippoPlatform *platform)
{

    /* FIXME */
    return TRUE;
}

static void
hippo_platform_impl_set_message_server(HippoPlatform  *platform,
                                       const char     *value)
{

    /* FIXME */
}

static void
hippo_platform_impl_set_web_server(HippoPlatform  *platform,
                                   const char     *value)
{

    /* FIXME */
}

static void
hippo_platform_impl_set_signin(HippoPlatform  *platform,
                               gboolean        value)
{

    /* FIXME */
}
