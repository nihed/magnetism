#include "hippo-platform-impl.h"
#include <hippo/hippo-connection.h>

int
main(int argc, char **argv)
{
    HippoPlatform *platform;
    char *cookie;
    
    g_type_init ();
    
    platform = g_object_new(HIPPO_TYPE_PLATFORM_IMPL, NULL);
    
    cookie = hippo_platform_read_login_cookie(platform);
    g_print("Login cookie is '%s'\n", cookie);
    
    g_free (cookie);

    return 0;
}
