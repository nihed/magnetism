// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#define _WIN32_IE 0x0600                // Get newest shell icon API
#define _WIN32_WINNT 0x0501             // For GetLastInputInfo, CS_DROPSHADOW
#include "HippoStdAfx.h"

// Windows Header Files:
#import <mshtml.tlb>
#include <shellapi.h>
#include <shlwapi.h>
#include <wininet.h>

// Generated from HippUI.idl
#include "HippoUI_h.h"