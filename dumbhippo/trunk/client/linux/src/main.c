#include <hippo/hippo-common.h>
#include "hippo-platform-impl.h"

int
main(int argc, char **argv)
{
    HippoPlatform *platform;
    char *cookie;
    
    g_type_init ();
    
    platform = hippo_platform_impl_new(HIPPO_INSTANCE_DOGFOOD);
    
    cookie = hippo_platform_read_login_cookie(platform);
    g_print("Login cookie is '%s'\n", cookie);
    
    g_free (cookie);

    return 0;
}
