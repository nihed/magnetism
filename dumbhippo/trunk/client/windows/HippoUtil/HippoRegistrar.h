/* HippoRegistrar.h: Utility class for registering stuff in the windows registry
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <ComCat.h>

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

class DLLEXPORT HippoRegistrar
{
public:
    HippoRegistrar(const WCHAR *dllName);
    ~HippoRegistrar();

    WCHAR *getModulePath();
    HRESULT registerTypeLib();
    HRESULT registerClassImplCategories(const CLSID &classID, 
                                        ULONG        cCategories,
                                        CATID        categories[]);
    HRESULT registerInprocServer(const CLSID &classID,
                                 const WCHAR *title);
    HRESULT registerBrowserHelperObject(const CLSID &classID,
                                        const WCHAR *title);
    HRESULT registerStartupProgram(const WCHAR *key,
                                   const WCHAR *commandline);
    HRESULT unregisterStartupProgram(const WCHAR *key);
    HRESULT registerGlobalShellCtxMenu(const CLSID &classID,
                                       const WCHAR *title);
    HRESULT unregisterGlobalShellCtxMenu(const WCHAR *title);

private:
    WCHAR *modulePath_;
};