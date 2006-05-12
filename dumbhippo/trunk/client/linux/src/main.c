#include "main.h"
#include "hippo-platform-impl.h"
#include "hippo-status-icon.h"

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoStatusIcon *icon;
};

void
hippo_app_quit(HippoApp *app)
{
    g_main_loop_quit(app->loop);
}


static HippoApp *the_app;

HippoApp*
hippo_get_app(void)
{
    return the_app;
}

int
main(int argc, char **argv)
{
    HippoOptions options;
     
    g_thread_init(NULL);
    gtk_init(&argc, &argv);

    the_app = g_new0(HippoApp, 1);

    if (!hippo_parse_options(&argc, &argv, &options))
        return 1;
    
    the_app->platform = hippo_platform_impl_new(options.instance_type);

    hippo_options_free_fields(&options);

    the_app->loop = g_main_loop_new(NULL, FALSE);

    the_app->connection = hippo_connection_new(the_app->platform);
    the_app->cache = hippo_data_cache_new(the_app->connection);
    the_app->icon = hippo_status_icon_new(the_app->cache);
    
    if (hippo_connection_signin(the_app->connection))
        g_debug("Waiting for user to sign in");
    else
        g_debug("Found login cookie");
    
    gtk_status_icon_set_visible(the_app->icon, TRUE);
    
    g_main_loop_run(the_app->loop);
    
    g_main_loop_unref(the_app->loop);
    g_free(the_app);

    return 0;
}
