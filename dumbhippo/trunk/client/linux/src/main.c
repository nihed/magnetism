#include <hippo/hippo-common.h>
#include "hippo-platform-impl.h"

int
main(int argc, char **argv)
{
    HippoPlatform *platform;
    HippoOptions options;
    GMainLoop *loop;
    HippoConnection *connection;
    HippoDataCache *cache;
    
    g_thread_init(NULL);
    g_type_init ();

    if (!hippo_parse_options(&argc, &argv, &options))
        return 1;
    
    platform = hippo_platform_impl_new(options.instance_type);

    hippo_options_free_fields(&options);

    connection = hippo_connection_new(platform);
    cache = hippo_data_cache_new(connection);
    
    if (hippo_connection_signin(connection))
        g_debug("Waiting for user to sign in");
    else
        g_debug("Found login cookie");

    loop = g_main_loop_new(NULL, FALSE);
    
    g_main_loop_run(loop);
    
    g_main_loop_unref(loop);

    return 0;
}
