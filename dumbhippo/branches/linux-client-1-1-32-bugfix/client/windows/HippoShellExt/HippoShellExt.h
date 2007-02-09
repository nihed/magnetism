#pragma once

#include <HippoUtil.h>
#include <HippoArray.h>

class HippoShellExt : 
    public IShellExtInit,
    public IContextMenu3
{
public:
    HippoShellExt(void);
    ~HippoShellExt(void);

   // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IShellExtInit
    STDMETHODIMP Initialize(LPCITEMIDLIST folderId, LPDATAOBJECT dataObj, HKEY keyID);

    // IContextMenu3
    STDMETHODIMP QueryContextMenu(HMENU menu, UINT indexMenu, UINT cmdFirst, UINT cmdLast, UINT flags);
    STDMETHODIMP InvokeCommand(LPCMINVOKECOMMANDINFO cmi);
    STDMETHODIMP GetCommandString(UINT_PTR cmd, UINT flags, UINT *reserved, LPSTR name, UINT cmax);
    STDMETHODIMP HandleMenuMsg(UINT msg, WPARAM param, LPARAM lParam);
    STDMETHODIMP HandleMenuMsg2(UINT msg, WPARAM param, LPARAM lParam, LRESULT *result);

private:
    int refCount_;

    LPITEMIDLIST folderId_;
    HippoArray<HippoBSTR> fileNames_;
    HippoPtr<IDataObject> fileData_;
    HKEY fileRegKey_;

    void initUIConnection(void);

    HippoPtr<IHippoUI> ui_;

};
