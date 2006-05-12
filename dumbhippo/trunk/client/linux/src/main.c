#include "main.h"
#include "hippo-platform-impl.h"
#include "hippo-status-icon.h"

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
};

void
hippo_app_quit(HippoApp *app)
{
    g_debug("Quitting main loop");
    g_main_loop_quit(app->loop);
}

void
hippo_app_show_about(HippoApp *app)
{
    if (app->about_dialog == NULL) {
        app->about_dialog = g_object_new(GTK_TYPE_ABOUT_DIALOG,
            "name", "Mugshot",
            "version", VERSION,
            "copyright", "Copyright 2006 Red Hat, Inc. and others",
            "website", "http://mugshot.org",
            NULL);
        g_signal_connect(app->about_dialog, "destroy",
            G_CALLBACK(gtk_widget_destroyed), &app->about_dialog);
    }
    
    gtk_window_present(GTK_WINDOW(app->about_dialog));
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
    
    gtk_status_icon_set_visible(GTK_STATUS_ICON(the_app->icon), TRUE);
    
    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    if (the_app->about_dialog)
        gtk_object_destroy(GTK_OBJECT(the_app->about_dialog));
    g_object_unref(the_app->icon);
    g_main_loop_unref(the_app->loop);
    g_free(the_app);

    return 0;
}
