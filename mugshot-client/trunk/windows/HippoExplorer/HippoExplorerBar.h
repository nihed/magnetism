/* HippoExplorerBar.h: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <shlobj.h>
#include <HippoIE.h>
#include <HippoUtil.h>

class HippoExplorerBar : public IDeskBand, 
                         public IInputObject, 
                         public IObjectWithSite,
                         public IPersistStream,
                         public IDispatch,
                         public HippoIECallback
{
public:
   HippoExplorerBar();
   ~HippoExplorerBar();

   //IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, void **);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   //IOleWindow methods
   STDMETHODIMP GetWindow(HWND *);
   STDMETHODIMP ContextSensitiveHelp(BOOL);

   //IDockingWindow methods
   STDMETHODIMP ShowDW(BOOL);
   STDMETHODIMP CloseDW(DWORD);
   STDMETHODIMP ResizeBorderDW(const RECT *, IUnknown *, BOOL);

   //IDeskBand methods
   STDMETHODIMP GetBandInfo(DWORD, DWORD, DESKBANDINFO*);

   //IInputObject methods
   STDMETHODIMP UIActivateIO(BOOL, MSG *);
   STDMETHODIMP HasFocusIO();
   STDMETHODIMP TranslateAcceleratorIO(MSG *);

   //IObjectWithSite methods
   STDMETHODIMP SetSite(IUnknown *);
   STDMETHODIMP GetSite(const IID &, void **);

   //IPersistStream methods
   STDMETHODIMP GetClassID(CLSID *);
   STDMETHODIMP IsDirty();
   STDMETHODIMP Load(IStream *);
   STDMETHODIMP Save(IStream *, BOOL);
   STDMETHODIMP GetSizeMax(ULARGE_INTEGER *);

    //IDispatch methods
   STDMETHOD (GetIDsOfNames) (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
   STDMETHOD (GetTypeInfo) (unsigned int, LCID, ITypeInfo **);                    
   STDMETHOD (GetTypeInfoCount) (unsigned int *);
   STDMETHOD (Invoke) (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                       VARIANT *, EXCEPINFO *, unsigned int *);

   // HippoIECallback methods
   void onClose();
   void onDocumentComplete();
   void launchBrowser(const HippoBSTR &url);
   bool isOurServer(const HippoBSTR &host);
   HRESULT getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser);

protected:
    DWORD refCount_;

private:
    HippoPtr<IInputObjectSite> site_;
    HippoPtr<IWebBrowser2> browser_;
    HippoPtr<IConnectionPoint> connectionPoint_; // connection point for DWebBrowserEvents2
    DWORD connectionCookie_; // cookie for DWebBrowserEvents2 connection

    HWND window_;
    bool hasFocus_;
    HippoPtr<HippoIE> ie_;
    HippoBSTR currentUrl_;

private:
    bool createWindow(HWND parentWindow);
    bool registerWindowClass();
    bool createIE();
    void checkPageChange();
    HippoBSTR getFramerUrl(const HippoBSTR &pageUrl);
    bool processMessage(UINT   message, 
                        WPARAM wParam,
                        LPARAM lParam);
    void setHasFocus(bool hasFocus);
    void onPaint();
    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);
};
