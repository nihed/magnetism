// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#define _WIN32_WINNT 0x0500             // For CoInitializeEx (don't need quite this version)
#include <HippoStdAfx.h>
#include <comutil.h>

/* We want to use __uuidof() for type-safe QueryInterface, but a bunch
 * of __declspec(uuid) declarations are only found in the PlatformSDK
 * ComDef.h rather than the compiler ComDef.h. So force that to be 
 * included first.
 */
#if _MSC_VER < 1400
#include <../PlatformSDK/Include/ComDef.h>
#else
#include <ComDef.h>
#endif
#include <ole2.h>
