/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include "apps.h"
#include <string.h>

struct App
{
    int refcount;
    DDMDataResource *resource;

};

App*
app_new(DDMDataResource *resource)
{
    App *app;

    app = g_new0(App, 1);
    app->refcount = 1;
    app->resource = resource;

    return app;
}

void
app_ref(App *app)
{
    g_return_if_fail(app->refcount > 0);

    app->refcount += 1;
}

void
app_unref(App *app)
{
    g_return_if_fail(app->refcount > 0);

    app->refcount -= 1;
    if (app->refcount == 0) {

        g_free(app);
    }
}

const char*
app_get_tooltip(App *app)
{
    /* FIXME */
    return "Foo";
}

GdkPixbuf*
app_get_icon(App *app)
{
    /* FIXME */
    return NULL;
}
