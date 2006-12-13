/*
 * Stuff to be in stdafx.h for all projects in the solution
 */

#pragma once

#ifdef min
#error "min macro should not be defined"
#endif
#ifdef max
#error "max macro should not be defined"
#endif

#ifndef NOMINMAX
#define NOMINMAX
#endif
#define UNICODE
#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
// Windows Header Files:
#include <windows.h>
// C RunTime Header Files
#include <stdlib.h>
#include <malloc.h>
#include <memory.h>
#include <tchar.h>

// warning C4290: C++ exception specification ignored except to indicate a function is not __declspec(nothrow)
#pragma warning(disable:4290)
// too many standard headers create this warning
// warning C4995: 'foobar': name was marked as #pragma deprecated
#pragma warning(disable:4995)
