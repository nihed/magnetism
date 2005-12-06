#pragma once

#include "stdafx.h"
#include <shlobj.h>
#include <mshtml.h>
#include <mshtmhst.h>
#include <HippoUtil.h>
#include <HippoArray.h>

class HippoIECallback
{
public:
    virtual void onDocumentComplete() = 0;
    virtual void onError(WCHAR *errText) = 0;
};

/*
 DANGER DANGER DANGER
 This class displays content in the Local Machine IE security zone; this
 means the content can do almost anything, like reading local files
 and instantiating random COM components.  AT NO POINT SHOULD IT READ UNTRUSTED
 CONTENT.  This means for example that not only can you not point it at
 http://randomsite.com, you can't do http://randomsite.com in a frame either.
 External images should be fine though.

 Currently using this on remote trusted sites is a lot like downloading a .exe
 from that site and executing it on the fly.  This means it's vulnerable to 
 MITM attacks without some external integrity mechanism such as SSL or 
 SHA1 sum checking.
 DANGER DANGER DANGER
*/
class HippoIE :
    public IDocHostUIHandler,
    public IStorage,
    public IOleInPlaceFrame,
    public IOleClientSite,
    public IOleInPlaceSite,
    public IOleContainer,
    public DWebBrowserEvents2
{
public:

    // Only sets up object
    HippoIE(HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *external);

    // Optional, apply an XSLT stylesheet to source
    void setXsltTransform(WCHAR *styleSrc, ...);

    // Actually instantiate
    void create();

    // Return IWebBrowser2 interface, not reffed
    IWebBrowser2 *getBrowser();

    void resize(RECT *rect);
    HRESULT invokeJavascript(WCHAR * funcName, VARIANT *invokeResult, int nargs, ...);
    HRESULT invokeJavascript(WCHAR * funcName, VARIANT *invokeResult, int nargs, va_list args);

    ~HippoIE(void);

    // Following is the not useful stuff

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    //IDispatch methods
   STDMETHOD (GetIDsOfNames) (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
   STDMETHOD (GetTypeInfo) (unsigned int, LCID, ITypeInfo **);                    
   STDMETHOD (GetTypeInfoCount) (unsigned int *);
   STDMETHOD (Invoke) (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                       VARIANT *, EXCEPINFO *, unsigned int *);

    // IStorage
    STDMETHODIMP CreateStream(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStream ** ppstm);
    STDMETHODIMP OpenStream(const WCHAR * pwcsName,void * reserved1,DWORD grfMode,DWORD reserved2,IStream ** ppstm);
    STDMETHODIMP CreateStorage(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStorage ** ppstg);
    STDMETHODIMP OpenStorage(const WCHAR * pwcsName,IStorage * pstgPriority,DWORD grfMode,SNB snbExclude,DWORD reserved,IStorage ** ppstg);
    STDMETHODIMP CopyTo(DWORD ciidExclude,IID const * rgiidExclude,SNB snbExclude,IStorage * pstgDest);
    STDMETHODIMP MoveElementTo(const OLECHAR * pwcsName,IStorage * pstgDest,const OLECHAR* pwcsNewName,DWORD grfFlags);
    STDMETHODIMP Commit(DWORD grfCommitFlags);
    STDMETHODIMP Revert(void);
    STDMETHODIMP EnumElements(DWORD reserved1,void * reserved2,DWORD reserved3,IEnumSTATSTG ** ppenum);
    STDMETHODIMP DestroyElement(const OLECHAR * pwcsName);
    STDMETHODIMP RenameElement(const WCHAR * pwcsOldName,const WCHAR * pwcsNewName);
    STDMETHODIMP SetElementTimes(const WCHAR * pwcsName,FILETIME const * pctime,FILETIME const * patime,FILETIME const * pmtime);
    STDMETHODIMP SetClass(REFCLSID clsid);
    STDMETHODIMP SetStateBits(DWORD grfStateBits,DWORD grfMask);
    STDMETHODIMP Stat(STATSTG * pstatstg,DWORD grfStatFlag);

    // IOleWindow
    STDMETHODIMP GetWindow(HWND FAR* lphwnd);
    STDMETHODIMP ContextSensitiveHelp(BOOL fEnterMode);
    // IOleInPlaceUIWindow
    STDMETHODIMP GetBorder(LPRECT lprectBorder);
    STDMETHODIMP RequestBorderSpace(LPCBORDERWIDTHS pborderwidths);
    STDMETHODIMP SetBorderSpace(LPCBORDERWIDTHS pborderwidths);
    STDMETHODIMP SetActiveObject(IOleInPlaceActiveObject *pActiveObject,LPCOLESTR pszObjName);
    // IOleInPlaceFrame
    STDMETHODIMP InsertMenus(HMENU hmenuShared,LPOLEMENUGROUPWIDTHS lpMenuWidths);
    STDMETHODIMP SetMenu(HMENU hmenuShared,HOLEMENU holemenu,HWND hwndActiveObject);
    STDMETHODIMP RemoveMenus(HMENU hmenuShared);
    STDMETHODIMP SetStatusText(LPCOLESTR pszStatusText);
    STDMETHODIMP TranslateAccelerator(  LPMSG lpmsg,WORD wID);

    // IOleClientSite
    STDMETHODIMP SaveObject();
    STDMETHODIMP GetMoniker(DWORD dwAssign,DWORD dwWhichMoniker,IMoniker ** ppmk);
    STDMETHODIMP GetContainer(LPOLECONTAINER FAR* ppContainer);
    STDMETHODIMP ShowObject();
    STDMETHODIMP OnShowWindow(BOOL fShow);
    STDMETHODIMP RequestNewObjectLayout();

    // IOleInPlaceSite methods
    STDMETHODIMP CanInPlaceActivate();
    STDMETHODIMP OnInPlaceActivate();
    STDMETHODIMP OnUIActivate();
    STDMETHODIMP GetWindowContext(LPOLEINPLACEFRAME FAR* lplpFrame,LPOLEINPLACEUIWINDOW FAR* lplpDoc,LPRECT lprcPosRect,LPRECT lprcClipRect,LPOLEINPLACEFRAMEINFO lpFrameInfo);
    STDMETHODIMP Scroll(SIZE scrollExtent);
    STDMETHODIMP OnUIDeactivate(BOOL fUndoable);
    STDMETHODIMP OnInPlaceDeactivate();
    STDMETHODIMP DiscardUndoState();
    STDMETHODIMP DeactivateAndUndo();
    STDMETHODIMP OnPosRectChange(LPCRECT lprcPosRect);
    // IParseDisplayName
    STDMETHODIMP ParseDisplayName(IBindCtx *pbc,LPOLESTR pszDisplayName,ULONG *pchEaten,IMoniker **ppmkOut);
    // IOleContainer
    STDMETHODIMP EnumObjects(DWORD grfFlags,IEnumUnknown **ppenum);
    STDMETHODIMP LockContainer(BOOL fLock);

    // IDocHostUIHandler
    STDMETHODIMP EnableModeless(BOOL enable);
    STDMETHODIMP FilterDataObject(IDataObject *dobj, IDataObject **dobjRet);
    STDMETHODIMP GetDropTarget(IDropTarget *dropTarget, IDropTarget **dropTargetRet);
    STDMETHODIMP GetExternal(IDispatch **dispatch);
    STDMETHODIMP GetHostInfo(DOCHOSTUIINFO *info);
    STDMETHODIMP GetOptionKeyPath(LPOLESTR *chKey, DWORD dw);
    STDMETHODIMP HideUI(VOID);
    STDMETHODIMP OnDocWindowActivate(BOOL activate);
    STDMETHODIMP OnFrameWindowActivate(BOOL activate);
    STDMETHODIMP ResizeBorder(LPCRECT border, IOleInPlaceUIWindow *uiWindow, BOOL frameWindow);
    STDMETHODIMP ShowContextMenu(DWORD id, POINT *pt, IUnknown *cmdtReserved, IDispatch *dispReserved);
    STDMETHODIMP ShowUI(DWORD id, IOleInPlaceActiveObject *activeObject, IOleCommandTarget *commandTarget, IOleInPlaceFrame *frame, IOleInPlaceUIWindow *doc);
    STDMETHODIMP TranslateAccelerator(LPMSG msg, const GUID *guidCmdGroup, DWORD cmdID);
    STDMETHODIMP TranslateUrl(DWORD translate, OLECHAR *chURLIn, OLECHAR **chURLOut);
    STDMETHODIMP UpdateUI(VOID);

private:
    HippoIECallback *callback_;
    HWND window_;
    HippoPtr<IOleObject> ie_;
    IDispatch *external_;
    HippoPtr<IWebBrowser2> browser_;

    HippoBSTR docSrc_;

    HippoPtr<IConnectionPoint> connectionPoint_; // connection point for DWebBrowserEvents2
    DWORD connectionCookie_; // cookie for DWebBrowserEvents2 connection

    HippoPtr<ITypeInfo> eventsTypeInfo_;

    bool haveTransform_;
    HippoBSTR styleSrc_;
    HippoArray<HippoBSTR> styleParamNames_;
    HippoArray<HippoBSTR> styleParamValues_;

    DWORD refCount_;

    void signalError(WCHAR *text, ...);
};
