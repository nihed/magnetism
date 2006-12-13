/* gdiplus.h requires min/max defined, but we don't want them normally */
#pragma once

#ifdef min
#error "min should not be defined here"
#endif

#define max(a,b)            (((a) > (b)) ? (a) : (b))
#define min(a,b)            (((a) < (b)) ? (a) : (b))

#include <gdiplus.h>

#undef min
#undef max
