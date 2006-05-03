#include <hippo/hippo-common.h>
#include "hippo-platform-impl.h"

int
main(int argc, char **argv)
{
    HippoPlatform *platform;
    char *username;
    char *password;
    
    g_type_init ();
    
    platform = hippo_platform_impl_new(HIPPO_INSTANCE_DOGFOOD);
    
    username = NULL;
    password = NULL;
    if (hippo_platform_read_login_cookie(platform, &username, &password))
        g_print("Login info '%s' '%s'\n", username, password);
    else
        g_print("No login info found\n");
    
    g_free (username);
    g_free (password);

    return 0;
}
