#include "hippo-platform-impl.h"
#include "hippo-cookies-linux.h"

static void      hippo_platform_impl_init                (HippoPlatformImpl       *impl);
static void      hippo_platform_impl_class_init          (HippoPlatformImplClass  *klass);
static void      hippo_platform_impl_iface_init          (HippoPlatformClass      *klass);
static char*     hippo_platform_impl_read_login_cookie   (HippoPlatform           *platform);

struct _HippoPlatformImpl {
    GObject parent;

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
}

static void
hippo_platform_impl_init(HippoPlatformImpl       *impl)
{

}

static void
hippo_platform_impl_class_init(HippoPlatformImplClass  *klass)
{


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
