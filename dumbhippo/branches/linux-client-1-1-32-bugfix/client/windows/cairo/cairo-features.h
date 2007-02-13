/* Generated by configure.  Do not edit */
#ifndef CAIRO_FEATURES_H
#define CAIRO_FEATURES_H

#ifdef  __cplusplus
# define CAIRO_BEGIN_DECLS  extern "C" {
# define CAIRO_END_DECLS    }
#else
# define CAIRO_BEGIN_DECLS
# define CAIRO_END_DECLS
#endif

#ifdef BUILDING_CAIRO
#define cairo_public __declspec(dllexport)
#else
#define cairo_public __declspec(dllimport)
#endif

#define CAIRO_VERSION_MAJOR 1
#define CAIRO_VERSION_MINOR 2
#define CAIRO_VERSION_MICRO 6

#define CAIRO_VERSION_STRING "1.2.6"

#define CAIRO_HAS_PNG_FUNCTIONS 1
#define CAIRO_HAS_WIN32_SURFACE 1
#define CAIRO_HAS_WIN32_FONT 1

#endif