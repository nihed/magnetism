// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once
#include "HippoStdAfx.h"

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
#import <mshtml.tlb>
#include <ole2.h>
