// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once


#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers
#define UNICODE
// Windows Header Files:
#include <windows.h>
// C RunTime Header Files
#include <stdlib.h>
#include <malloc.h>
#include <memory.h>
#include <tchar.h>

/* We want to use __uuidof() for type-safe QueryInterface, but a bunch
 * of __declspec(uuid) declarations are only found in the PlatformSDK
 * ComDef.h rather than the compiler ComDef.h. So force that to be 
 * included first.
 */
#include <../PlatformSDK/Include/ComDef.h>
#import <mshtml.tlb>
#include <ole2.h>

