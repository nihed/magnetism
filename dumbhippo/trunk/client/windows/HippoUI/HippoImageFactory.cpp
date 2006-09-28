/* HippoImageFactory.cpp: load images by name
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include "stdafx-hippoui.h"
#include "HippoUI.h"
#include <cairo.h>

class HippoImageFactory {
public:
    static HippoImageFactory* getInstance();


    HippoImageFactory();
    ~HippoImageFactory();
    cairo_surface_t* get(const char *name);
    void setUI(HippoUI *ui);

private:
    HippoUI *ui_;
    GHashTable *cache_;
};

static HippoImageFactory *instance_ = NULL;

HippoImageFactory::HippoImageFactory()
: ui_(0), cache_(0)
{
    cache_ = g_hash_table_new_full(g_str_hash, g_str_equal,
                        (GFreeFunc) g_free, (GFreeFunc) cairo_surface_destroy);
}

HippoImageFactory::~HippoImageFactory()
{
    g_hash_table_destroy(cache_);
}

cairo_surface_t*
HippoImageFactory::get(const char *name)
{
    g_return_val_if_fail(ui_ != NULL, NULL);

    cairo_surface_t *surface = (cairo_surface_t*) g_hash_table_lookup(cache_, name);
    if (surface != NULL)
        return surface;

    HippoBSTR bname = HippoBSTR::fromUTF8(name);
    bname.Append(L".png");
    HippoBSTR path;
    ui_->getImagePath(bname, &path);
    HippoUStr upath(path);
    surface = cairo_image_surface_create_from_png(upath.c_str());
    if (surface != NULL) {
        // the surface may be an "error" surface, if so we still cache it
        if (cairo_surface_status(surface) != CAIRO_STATUS_SUCCESS) {
            g_warning("Failed to load png '%s' (name %s)", upath.c_str(), name);
        }
        // pass surface refcount to the cache
        g_hash_table_replace(cache_, g_strdup(name), surface);
    }
    return surface;
}

void
HippoImageFactory::setUI(HippoUI *ui)
{
    ui_ = ui;
}

HippoImageFactory*
HippoImageFactory::getInstance()
{
    if (instance_ == NULL) {
        instance_ = new HippoImageFactory();
    }
    return instance_;
}

cairo_surface_t*
hippo_image_factory_get(const char *name)
{
    return HippoImageFactory::getInstance()->get(name);
}

void
hippo_image_factory_set_ui(HippoUI *ui)
{
    HippoImageFactory::getInstance()->setUI(ui);
}
