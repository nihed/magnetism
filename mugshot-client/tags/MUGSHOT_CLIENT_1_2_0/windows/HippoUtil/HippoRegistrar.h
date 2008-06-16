/* HippoRegistrar.h: Utility class for registering stuff in the windows registry
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <ComCat.h>
#include <HippoUtilExport.h>

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
                                 const WCHAR *title,
                                 const WCHAR *versionIndependentProgId = 0,
                                 const WCHAR *progId = 0);
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