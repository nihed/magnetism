/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* Based on GdkPixbuf library - JPEG image loader
 *
 * Copyright (C) 1999 Michael Zucchi
 * Copyright (C) 1999 The Free Software Foundation
 *
 * Progressive loading code Copyright (C) 1999 Red Hat, Inc.
 *
 * Authors: Michael Zucchi <zucchi@zedzone.mmc.com.au>
 *          Federico Mena-Quintero <federico@gimp.org>
 *          Michael Fulbright <drmike@redhat.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

#include "hippo-common-internal.h"
#include "hippo-jpeg-loader.h"
#include <string.h>
#include <setjmp.h>
#include <stdio.h> /* we don't use this bug jpeglib has FILE* in it */
#include <jpeglib.h>
#include <jerror.h>

#ifndef HAVE_SIGSETJMP
#ifdef sigsetjmp
#error "configure didn't detect sigsetjmp correctly"
#endif
#define sigjmp_buf jmp_buf
#define sigsetjmp(jb, x) setjmp(jb)
#define siglongjmp longjmp
#endif

static const char *
colorspace_name (const J_COLOR_SPACE jpeg_color_space)
{
    switch (jpeg_color_space) {
    case JCS_UNKNOWN: return "UNKNOWN";
    case JCS_GRAYSCALE: return "GRAYSCALE";
    case JCS_RGB: return "RGB";
    case JCS_YCbCr: return "YCbCr";
    case JCS_CMYK: return "CMYK";
    case JCS_YCCK: return "YCCK";
    default: return "invalid";
    }
}

/* error handler data */
typedef struct {
    struct jpeg_error_mgr pub;
    sigjmp_buf setjmp_buffer;
    GError **error;
} HippoJpegErrorData;

static void
fatal_error_handler (j_common_ptr cinfo)
{
    HippoJpegErrorData *errmgr;
    char buffer[JMSG_LENGTH_MAX];

    errmgr = (HippoJpegErrorData *) cinfo->err;

    /* Create the message */
    (* cinfo->err->format_message) (cinfo, buffer);

    /* broken check for *error == NULL for robustness against
     * crappy JPEG library
     */
    if (errmgr->error && *errmgr->error == NULL) {
        g_set_error (errmgr->error,
                     HIPPO_ERROR,
                     HIPPO_ERROR_FAILED,
                     _("Error interpreting JPEG image file (%s)"),
                     buffer);
    }

    siglongjmp (errmgr->setjmp_buffer, 1);

    g_assert_not_reached ();
}

static void
output_message_handler (j_common_ptr cinfo)
{
    /* This method keeps libjpeg from dumping crap to stderr */

    /* do nothing */
}

/* explode gray image data from jpeg library into rgb components in pixbuf */
static void
explode_gray_into_buf (struct jpeg_decompress_struct *cinfo,
                       guchar **lines)
{
    gint i, j;
    guint w;

    g_return_if_fail (cinfo != NULL);
    g_return_if_fail (cinfo->output_components == 1);
    g_return_if_fail (cinfo->out_color_space == JCS_GRAYSCALE);

    /* Expand grey->colour.  Expand from the end of the
     * memory down, so we can use the same buffer.
     */
    w = cinfo->output_width;
    for (i = cinfo->rec_outbuf_height - 1; i >= 0; i--) {
        guchar *from, *to;

        from = lines[i] + w - 1;
        to = lines[i] + (w - 1) * 4;
        for (j = w - 1; j >= 0; j--) {
#if G_BYTE_ORDER == G_LITTLE_ENDIAN
            /* MSB-last so ignore the last */
            to[0] = from[0];
            to[1] = from[0];
            to[2] = from[0];
#else
            /* MSB-first so ignore the first */
            to[1] = from[0];
            to[2] = from[0];
            to[3] = from[0];
#endif
            to -= 4;
            from--;
        }
    }
}

static void
pad_and_byteswap_rgb (struct jpeg_decompress_struct *cinfo,
                      guchar **lines)
{
    int i, j;
    int w;

    g_return_if_fail (cinfo != NULL);
    g_return_if_fail (cinfo->output_components == 3);
    g_return_if_fail (cinfo->out_color_space == JCS_RGB);

    /* Add the padding byte to each pixel, also flip the
     * byte order if we are little-endian.
     */
    w = cinfo->output_width;
    for (i = cinfo->rec_outbuf_height - 1; i >= 0; i--) {

        guchar *from, *to;

        from = lines[i] + (w - 1) * 3;
        to = lines[i] + (w - 1) * 4;
        for (j = w - 1; j >= 0; j--) {
            int r, g, b;

            g_assert(from >= lines[i]);
            g_assert(to >= lines[i]);
            g_assert(from <= to);

            r = from[0];
            g = from[1];
            b = from[2];

#if G_BYTE_ORDER == G_LITTLE_ENDIAN
            /* MSB-last so ignore the last - BGRP (P = pad) */
            to[0] = b;
            to[1] = g;
            to[2] = r;
#else
            /* MSB-first so ignore the first - PRGB (P = pad) */
            to[1] = r;
            to[2] = g;
            to[3] = b;
#endif
            to -= 4;
            from -= 3;
        }
    }
}

static void
convert_cmyk_to_rgb (struct jpeg_decompress_struct *cinfo,
                     guchar **lines)
{
    int i, j;

    g_return_if_fail (cinfo != NULL);
    g_return_if_fail (cinfo->output_components == 4);
    g_return_if_fail (cinfo->out_color_space == JCS_CMYK);

    for (i = cinfo->rec_outbuf_height - 1; i >= 0; i--) {
        guchar *p;

        p = lines[i];
        for (j = 0; j < (int) cinfo->output_width; j++) {
            int c, m, y, k, r, g, b;
            c = p[0];
            m = p[1];
            y = p[2];
            k = p[3];
            if (cinfo->saw_Adobe_marker) {
                r = k*c / 255;
                g = k*m / 255;
                b = k*y / 255;
            } else {
                r = (255 - k)*(255 - c) / 255;
                g = (255 - k)*(255 - m) / 255;
                b = (255 - k)*(255 - y) / 255;
            }

#if G_BYTE_ORDER == G_LITTLE_ENDIAN
            /* MSB-last so ignore the last - BGRP (P = pad) */
            p[0] = b;
            p[1] = g;
            p[2] = r;
#else
            /* MSB-first so ignore the first - PRGB (P = pad) */
            p[1] = r;
            p[2] = g;
            p[3] = b;
#endif

            p += 4;
        }
    }
}


static const unsigned char fake_jpeg_eoi[] = { 0xff, JPEG_EOI };

typedef struct {
    struct jpeg_source_mgr pub; /* public fields */

    const char *data;
    int   data_len;
} HippoJpegSource;

static void
init_source (j_decompress_ptr cinfo)
{
    /* HippoJpegSource *src = (HippoJpegSource*) cinfo->src; */
}

static boolean
fill_input_buffer (j_decompress_ptr cinfo)
{
    HippoJpegSource *src = (HippoJpegSource*) cinfo->src;

    if (src->data_len < 2) {
        src->pub.next_input_byte = &fake_jpeg_eoi[0];
        src->pub.bytes_in_buffer = sizeof(fake_jpeg_eoi);
    } else {
        src->pub.next_input_byte = (unsigned char*) src->data;
        src->pub.bytes_in_buffer = src->data_len;
    }

    return TRUE;
}

static void
skip_input_data (j_decompress_ptr cinfo, long num_bytes)
{
    HippoJpegSource *src = (HippoJpegSource*) cinfo->src;

    if (num_bytes > 0) {
        src->pub.next_input_byte += (size_t) num_bytes;
        src->pub.bytes_in_buffer -= (size_t) num_bytes;
    }
}

static void
term_source (j_decompress_ptr cinfo)
{
}

static const cairo_user_data_key_t pixels_key;

cairo_surface_t*
hippo_parse_jpeg (const char *data,
                  int         data_len,
                  GError    **error)
{

    gint i;
    guchar *dptr;
    guchar *lines[4]; /* Used to expand rows, via rec_outbuf_height,
                       * from the header file:
                       * " Usually rec_outbuf_height will be 1 or 2,
                       * at most 4."
                       */
    guchar **lptr;
    struct jpeg_decompress_struct cinfo;
    HippoJpegErrorData jerr;
    HippoJpegSource *src;
    cairo_surface_t *surface;
    int rowstride;

    /* setup error handler */
    cinfo.err = jpeg_std_error (&jerr.pub);
    jerr.pub.error_exit = fatal_error_handler;
    jerr.pub.output_message = output_message_handler;

    jerr.error = error;

    if (sigsetjmp (jerr.setjmp_buffer, 1)) {
        /* Whoops there was a jpeg error */

        jpeg_destroy_decompress (&cinfo);
        return NULL;
    }

    /* load header, setup */
    jpeg_create_decompress (&cinfo);

    cinfo.src = (struct jpeg_source_mgr *)
        (*cinfo.mem->alloc_small) ((j_common_ptr) &cinfo, JPOOL_PERMANENT,
                                   sizeof (HippoJpegSource));
    src = (HippoJpegSource*) cinfo.src;
    src->data = data;
    src->data_len = data_len;

    src->pub.init_source = init_source;
    src->pub.fill_input_buffer = fill_input_buffer;
    src->pub.skip_input_data = skip_input_data;
    src->pub.resync_to_restart = jpeg_resync_to_restart; /* use default method */
    src->pub.term_source = term_source;
    src->pub.bytes_in_buffer = 0; /* forces fill_input_buffer on first read */
    src->pub.next_input_byte = NULL; /* until buffer loaded */

    jpeg_read_header (&cinfo, TRUE);
    jpeg_start_decompress (&cinfo);
    cinfo.do_fancy_upsampling = FALSE;
    cinfo.do_block_smoothing = FALSE;


    /* cairo RGB24 is 32-bit with the high byte ignored */
    rowstride = cinfo.output_width * 4;

    dptr = g_try_malloc(rowstride * cinfo.output_height);

    /* We rely on the RGB24 really being 32 bpp, so we can read CMYK data into it */
    if (dptr)
        surface = cairo_image_surface_create_for_data(dptr, CAIRO_FORMAT_RGB24,
                                                      cinfo.output_width, cinfo.output_height,
                                                      rowstride);
    else
        surface = NULL;

    if (dptr == NULL || surface == NULL) {
        g_free(dptr);

        jpeg_destroy_decompress (&cinfo);

        /* broken check for *error == NULL for robustness against
         * crappy JPEG library
         */
        if (error && *error == NULL) {
            g_set_error (error,
                         HIPPO_ERROR,
                         HIPPO_ERROR_FAILED,
                         _("Insufficient memory to load image, try exiting some applications to free memory"));
        }

        return NULL;
    }

    /* Arrange to free the pixels */
    cairo_surface_set_user_data(surface, &pixels_key, dptr, g_free);

    /* decompress all the lines, a few at a time */
    while (cinfo.output_scanline < cinfo.output_height) {
        lptr = lines;
        for (i = 0; i < cinfo.rec_outbuf_height; i++) {
            *lptr++ = dptr;
            dptr += rowstride;
        }

        jpeg_read_scanlines (&cinfo, lines, cinfo.rec_outbuf_height);

        switch (cinfo.out_color_space) {
        case JCS_GRAYSCALE:
            explode_gray_into_buf (&cinfo, lines);
            break;
        case JCS_RGB:
            /* This is as in GdkPixbuf, so we have to convert */
            pad_and_byteswap_rgb(&cinfo, lines);
            break;
        case JCS_CMYK:
            convert_cmyk_to_rgb (&cinfo, lines);
            break;
        default:
            cairo_surface_destroy(surface);
            if (error && *error == NULL) {
                g_set_error (error,
                             HIPPO_ERROR,
                             HIPPO_ERROR_FAILED,
                             _("Unsupported JPEG color space (%s)"),
                             colorspace_name (cinfo.out_color_space));
            }

            jpeg_destroy_decompress (&cinfo);
            return NULL;
        }
    }

    jpeg_finish_decompress (&cinfo);
    jpeg_destroy_decompress (&cinfo);

    return surface;
}
