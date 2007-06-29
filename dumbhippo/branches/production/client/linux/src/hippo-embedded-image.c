/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include "hippo-embedded-image.h"
#include <gdk-pixbuf/gdk-pixdata.h>
/* the generated image data */
#include <hippo/hippo-embedded-image-data.h>

GdkPixbuf*
hippo_embedded_image_get(const char *name)
{
    int i;
    
    for (i = 0; catalog[i].name != NULL; ++i) {
        if (strcmp(catalog[i].name, name) == 0) {
            if (catalog[i].pixbuf == NULL) {
                catalog[i].pixbuf = gdk_pixbuf_from_pixdata(catalog[i].pixdata, FALSE, NULL);
                g_assert(catalog[i].pixbuf);
            }
            return catalog[i].pixbuf;
        }
    }
    g_warning("Requested embedded image '%s' that does not exist", name);
    return NULL;
}
