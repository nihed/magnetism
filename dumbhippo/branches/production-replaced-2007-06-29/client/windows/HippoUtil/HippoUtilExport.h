/* HippoUtilExport.h: Define DLLEXPORT
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

