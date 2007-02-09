/* HippoImageFactory.h: load images by name
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include "HippoUI.h"
#include <cairo.h>

// does not return a reference; i.e. we never unload these images, they are just
// kept statically.
cairo_surface_t* hippo_image_factory_get(const char *name);

void hippo_image_factory_set_ui(HippoUI *ui);
