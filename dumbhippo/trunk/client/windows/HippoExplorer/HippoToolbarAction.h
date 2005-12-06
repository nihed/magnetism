/* HippoToolbarAction.h: COM object called by toolbar button
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <shlobj.h>
#include <HippoUtil.h>

class HippoToolbarAction :
    public IObjectWithSite,
    public IOleCommandTarget
{
public:
    HippoToolbarAction(void);
    ~HippoToolbarAction(void);

   // IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, LPVOID*);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   // IObjectWithSite methods
   STDMETHODIMP SetSite (IUnknown*);
   STDMETHODIMP GetSite (const IID &, void **);

   // IOleCommandTarget methods
   STDMETHODIMP QueryStatus (const GUID *commandGroup,
                             ULONG nCommands,
                             OLECMD *commands,
                             OLECMDTEXT *commandText);
   STDMETHODIMP Exec (const GUID *commandGroup,
                      DWORD       commandId,
                      DWORD       nCommandExecOptions,
                      VARIANTARG *commandInput,
                      VARIANTARG *commandOutput);

protected:
    DWORD refCount_;

private:
    void clearSite();
    bool createWindow();
    bool registerWindowClass();

    HRESULT getUI(IHippoUI **ui);
    bool spawnUI();
    void uiWaitTimer();
    void updateCommandEnabled();
    void stopUIWait();

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

    HippoPtr<IServiceProvider> site_;
    HippoPtr<IWebBrowser2> browser_;

    HWND window_;

    unsigned uiWaitCount_;
    HippoBSTR shareUrl_;
    HippoBSTR shareTitle_;
};
