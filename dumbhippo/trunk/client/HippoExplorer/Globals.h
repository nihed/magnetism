/* Globals.h: DLL-wide global variables
 *
 * Copyright Red Hat, Inc. 2005
 *
 * Partially based on MSDN BandObjs sample:
 *  Copyright 1997 Microsoft Corporation.  All Rights Reserved.
 **/

extern HINSTANCE  g_hInst;
extern UINT       g_DllRefCount;

#define ARRAYSIZE(a)    (sizeof(a)/sizeof(a[0]))
