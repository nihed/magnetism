// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#define UNICODE
#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
#define _WIN32_IE 0x0600                // Get newest shell icon API
#define _WIN32_WINNT 0x0500             // For GetLastInputInfo

// Windows Header Files:
#include <windows.h>
#import <mshtml.tlb>
#include <shellapi.h>
#include <shlwapi.h>
#include <wininet.h>

// C RunTime Header Files
#include <stdlib.h>
#include <malloc.h>
#include <memory.h>
#include <tchar.h>

