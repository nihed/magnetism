/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_JPEG_LOADER_H__
#define __HIPPO_JPEG_LOADER_H__

#include <cairo.h>

G_BEGIN_DECLS

cairo_surface_t* hippo_parse_jpeg (const char *data,
                                   int         data_len,
                                   GError    **error);

G_END_DECLS

#endif /* __HIPPO_JPEG_LOADER_H__ */
