#include "stdafx-hipposhellext.h"

#include <iostream>
#include <shlobj.h>
#include <ShellAPI.h>
#include <HippoRegistrar.h>
#include <HippoUtil.h>

#include "ClassFactory.h"
#include "HippoShellExt.h"

// Definitions of GUIDs
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include <shlguid.h>
#include "Guid.h"
#pragma data_seg()

// We redefine these here to avoid a dependency on the HippoUI project
static const CLSID CLSID_HippoUI = {
    0xfd2d3bee, 0x477e, 0x4625, 0xb3, 0x5f, 0xbf, 0x49, 0x7f, 0xf6, 0x1, 0xd9
};

static const CLSID CLSID_HippoUI_Debug = {
    0xee8e46eb, 0xcdc7, 0x4f89, 0xa8, 0xae, 0xaf, 0x9, 0x94, 0x6c, 0x96, 0x85
};

static const WCHAR *knownFlickrPhotoTypes[] = {
    L"png",
    L"jpeg",
    L"jpg"
};

HINSTANCE dllInstance;
UINT dllRefCount;

HippoShellExt::HippoShellExt(void)
{
}

HippoShellExt::~HippoShellExt(void)
{
}

// IUnknown
STDMETHODIMP
HippoShellExt::QueryInterface(REFIID ifaceID, void **result)
{
   if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IShellExtInit *>(this));
    else if (IsEqualIID(ifaceID, IID_IShellExtInit)) 
        *result = static_cast<IShellExtInit *>(this);
    else if (IsEqualIID(ifaceID, IID_IContextMenu)) 
        *result = static_cast<IContextMenu *>(this);
    else if (IsEqualIID(ifaceID, IID_IContextMenu2)) 
        *result = static_cast<IContextMenu2 *>(this);
    else if (IsEqualIID(ifaceID, IID_IContextMenu3)) 
        *result = static_cast<IContextMenu3 *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;
}

HIPPO_DEFINE_REFCOUNTING(HippoShellExt);

// IShellExtInit
STDMETHODIMP 
HippoShellExt::Initialize(LPCITEMIDLIST folderId, IDataObject *dataObj, HKEY regKey)
{
    HippoPtr<IUnknown> unknown;
    ui_ = NULL;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&ui_);
    else {
        unknown = NULL;
        if (SUCCEEDED (GetActiveObject(CLSID_HippoUI_Debug, NULL, &unknown)))
            unknown->QueryInterface<IHippoUI>(&ui_);
        else
            return S_OK;
    }

    ILFree(folderId_);
    folderId_ = NULL;

    if (folderId) {
        folderId_ = ILClone(folderId);
    }

    fileData_ = dataObj;
    if (fileData_) {
        STGMEDIUM medium;
        FORMATETC fe = {CF_HDROP, NULL, DVASPECT_CONTENT, -1, TYMED_HGLOBAL};
        UINT count;

        if (SUCCEEDED(fileData_->GetData(&fe, &medium))) {
            count = DragQueryFile((HDROP)medium.hGlobal, (UINT)-1, NULL, 0);
            for (UINT i = 0; i < count; i++) {
                UINT bufsize = DragQueryFile((HDROP)medium.hGlobal, i, NULL, 0);
                if (bufsize > 0) {
                    UINT totalBuf = bufsize+1;
                    HippoBSTR fileName(totalBuf, L"");
                    DragQueryFile((HDROP)medium.hGlobal, i, fileName.m_str, 
                                  totalBuf);
                    WCHAR *suffix = wcsrchr(fileName.m_str, '.');
                    if (!suffix)
                        continue;
                    suffix++;
                    for (UINT j = 0; j < sizeof(knownFlickrPhotoTypes)/sizeof(knownFlickrPhotoTypes[0]); j++) {
                        if (wcsicmp(suffix, knownFlickrPhotoTypes[j]) == 0) {
                            fileNames_.append(fileName);
                            break;
                        }
                    }
                }
            }

            ReleaseStgMedium(&medium);
        }
        
    }
    if (regKey) 
        RegOpenKeyEx(regKey, NULL, 0L, 
                     MAXIMUM_ALLOWED, 
                     &fileRegKey_); 

    return S_OK;
}

STDMETHODIMP 
HippoShellExt::QueryContextMenu(HMENU menu, UINT indexMenu, UINT cmdFirst, UINT cmdLast, UINT flags)
{
    // Disabled for now
    if (true)
        return MAKE_HRESULT(SEVERITY_SUCCESS, 0, 0);
    if ((flags & CMF_DEFAULTONLY)
        || !ui_ || !fileData_ || fileNames_.length() == 0)
        return MAKE_HRESULT(SEVERITY_SUCCESS, 0, 0);

    WCHAR *text;
    if (fileNames_.length() > 1) // needs i18n
        text = L"Share Pictures";
    else 
        text = L"Share Picture";
    InsertMenu(menu, indexMenu, MF_STRING | MF_BYPOSITION,
               cmdFirst + 0, text);

    return MAKE_HRESULT(SEVERITY_SUCCESS, 0, 0 + 1);
}

STDMETHODIMP 
HippoShellExt::InvokeCommand(LPCMINVOKECOMMANDINFO cmi)
{
    for (UINT i = 0; i < fileNames_.length(); i++)
        ui_->BeginFlickrShare(fileNames_[i]);
    return S_OK;
}

STDMETHODIMP 
HippoShellExt::GetCommandString(UINT_PTR cmd, UINT flags, UINT *reserved, LPSTR nameSize, UINT cmax)
{
    return S_OK;
}

STDMETHODIMP 
HippoShellExt::HandleMenuMsg(UINT msg, WPARAM param, LPARAM lParam)
{
    return S_OK;
}

STDMETHODIMP 
HippoShellExt::HandleMenuMsg2(UINT msg, WPARAM param, LPARAM lParam, LRESULT *result)
{
    return S_OK;
}

// Global DLL functions
BOOL APIENTRY 
DllMain(HINSTANCE module, 
        DWORD reason, 
        LPVOID reserved)
{  
    switch(reason)
    {
        case DLL_PROCESS_ATTACH:
        dllInstance = module;
        break;
    }
    return TRUE;
}

STDAPI 
DllCanUnloadNow(void)
{
    return dllRefCount > 0 ? S_FALSE : S_OK;
}


STDAPI 
DllGetClassObject(const CLSID &classID, 
                  const IID   &ifaceID,
                  void       **result)
{
    HRESULT hr;

    ClassFactory *classFactory = new ClassFactory(CLSID_HippoShellExt);
    if (!classFactory) {
        *result = NULL;
        return E_OUTOFMEMORY;
    }
       
    hr = classFactory->QueryInterface(ifaceID, result);
    classFactory->Release();

    return hr;
}

STDAPI 
DllRegisterServer(void)
{
    HippoRegistrar registrar(L"HippoShellExt.dll");
    HRESULT hr;

    hr = registrar.registerInprocServer(CLSID_HippoShellExt,
                                        L"Mugshot Shell Extension");
    if (FAILED(hr))
        return hr;

    // *un*register, since this module isn't used at the moment
    hr = registrar.unregisterGlobalShellCtxMenu(// CLSID_HippoShellExt,
                                                L"Mugshot Shell Extension");
    if (FAILED(hr))
        return hr;

    // Notify shell we changed the registry
    SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, NULL, NULL);

    return S_OK;
}