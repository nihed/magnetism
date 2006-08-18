/* HippoIE.cpp: Embed an instance if the IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoutil.h"
#include <mshtml.h>

#import <msxml3.dll>  named_guids

#include <mshtml.h>
#include <mshtmhst.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include <HippoArray.h>
#include <HippoUtil.h>
#include <HippoURLParser.h>
#include "HippoExternal.h"
#include "HippoIE.h"
#include "HippoInvocation.h"

using namespace MSXML2;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

class HippoIEImpl : 
    public HippoIE,
    public IStorage,
    public IOleInPlaceFrame, // <- IOleInplaceUIWindow <- IOleWindow
    public IOleClientSite,
    public IOleInPlaceSite, // <- IOleWindow
    public IOleContainer, // <- IParseDisplayName
    public IDocHostUIHandler,
    public DWebBrowserEvents2, // <- IDispatch
    public IServiceProvider
#if 0
    public IOleCommandTarget,
#endif
{
public:
    // Only sets up object
    HippoIEImpl(HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *application);
    ~HippoIEImpl(void);

    void setXsltTransform(WCHAR *styleSrc, ...);
    void setThreeDBorder(bool threeDBorder);
    void embedBrowser();
    void shutdown();
    void setLocation(const HippoBSTR &location);
    IWebBrowser2 *getBrowser();
    void resize(RECT *rect);
    HippoInvocation createInvocation(const HippoBSTR &functionName);

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IDispatch methods
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

    // IServiceProvider methods
    STDMETHODIMP QueryService(const GUID &, const IID &, void **);

#if 0
    // We don't have a use for the following interface at the current
    // time, but keeping the skeleton code around in case we need to implement
    // it later.

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
#endif

private:
    bool handleNavigation(IDispatch *targetDispatch,
                          BSTR       url,
                          bool       isPost);

    HippoIECallback *callback_;
    HWND window_;
    HippoPtr<IOleObject> ie_;
    HippoPtr<IDispatch> external_;
    HippoPtr<IWebBrowser2> browser_;

    HippoBSTR docSrc_;

    HippoPtr<IConnectionPoint> connectionPoint_; // connection point for DWebBrowserEvents2
    DWORD connectionCookie_; // cookie for DWebBrowserEvents2 connection

    HippoPtr<ITypeInfo> eventsTypeInfo_;

    bool haveTransform_;
    HippoBSTR styleSrc_;
    HippoArray<HippoBSTR> styleParamNames_;
    HippoArray<HippoBSTR> styleParamValues_;

    bool threeDBorder_;

    bool inNavigation_; // Set temporarily while we are navigating to a new location programmatically

    DWORD refCount_;
};

HippoIE *
HippoIE::create(HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *application)
{
    return new HippoIEImpl(window, src, cb, application);
}

HippoIEImpl::HippoIEImpl(HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *application)
{
    refCount_ = 1;
    window_ = window;
    docSrc_ = src;
    callback_ = cb;
    haveTransform_ = false;
    threeDBorder_ = true;
    inNavigation_ = false;

    HippoExternal *external = new HippoExternal();
    if (external)
        external->setApplication(application);
    external_ = external;
    external->Release();

    hippoLoadRegTypeInfo(LIBID_SHDocVw, 1, 1,
                         &DIID_DWebBrowserEvents2, &eventsTypeInfo_, 
                         NULL);
}

HippoIEImpl::~HippoIEImpl(void)
{
    hippoDebugLogW(L"Finalizing HippoIEImpl");
    // Really we should never get here until we are already shut down because
    // of circular references, but in case we do, do the shutdown stuff anyways
    shutdown();
}

HippoInvocation
HippoIEImpl::createInvocation(const HippoBSTR &functionName) 
{
    HippoPtr<IDispatch> docDispatch;
    browser_->get_Document(&docDispatch);
    assert(docDispatch != NULL);
    HippoQIPtr<IHTMLDocument2> doc(docDispatch);
    assert(doc != NULL);
    HippoPtr<IDispatch> script;
    doc->get_Script(&script);
    assert(script != NULL);

    return HippoInvocation(script, functionName);
}

IWebBrowser2 *
HippoIEImpl::getBrowser()
{
    return browser_;
}
void
HippoIEImpl::setXsltTransform(WCHAR *stylesrc, ...)
{
    va_list vap;
    va_start(vap, stylesrc);

    haveTransform_ = true;

    styleSrc_ = stylesrc;
    {
        WCHAR* key;
        WCHAR* val;
        while ((key = va_arg (vap, WCHAR *)) != NULL) {
            val = va_arg (vap, WCHAR *);
            styleParamNames_.append(HippoBSTR(key));
            styleParamValues_.append(HippoBSTR(val));
        }
    }
    va_end(vap);
}

void 
HippoIEImpl::setThreeDBorder(bool threeDBorder)
{
    threeDBorder_ = threeDBorder;
}
    
void
HippoIEImpl::embedBrowser()
{
    RECT rect;
    GetClientRect(window_,&rect);
    OleCreate(CLSID_WebBrowser,IID_IOleObject,OLERENDER_DRAW,0,this,this,(void**)&ie_);
    ie_->SetHostNames(L"Web Host",L"Web View");
    OleSetContainedObject(ie_,TRUE);
    ie_->DoVerb(OLEIVERB_SHOW,NULL,this,-1,window_,&rect);

    HippoQIPtr<IWebBrowser2> browser(ie_);
    browser_ = browser;

    HippoBSTR targetUrl;
    if (!haveTransform_)
        targetUrl = docSrc_;
    else
        targetUrl = L"about:blank";
    variant_t vTargetUrl(targetUrl.m_str);
    variant_t vEmpty;
    vEmpty.vt = VT_EMPTY;
    browser->Navigate2(&vTargetUrl, &vEmpty, &vEmpty, &vEmpty, &vEmpty);

    HippoQIPtr<IConnectionPointContainer> container(ie_);
    if (container)
    {
        if (SUCCEEDED(container->FindConnectionPoint(DIID_DWebBrowserEvents2,
            &connectionPoint_))) 
        {
            HippoQIPtr<IUnknown> unknown(static_cast<DWebBrowserEvents2 *>(this));
            connectionPoint_->Advise(unknown, &connectionCookie_);
        }
    }

    if (!haveTransform_)
        return;
    variant_t xmlResult;
    MSXML2::IXMLDOMDocumentPtr xmlsrc(MSXML2::CLSID_DOMDocument);
    MSXML2::IXMLDOMDocumentPtr clientXSLT(CLSID_FreeThreadedDOMDocument);
    try {
        xmlResult = xmlsrc->load(variant_t(docSrc_.m_str));
        xmlResult = clientXSLT->load(variant_t(styleSrc_.m_str));
    } catch(_com_error &e) {
        hippoDebugLogW(L"HippIE: Error loading XML files : %s\n",  HippoBSTR(e.Description()).m_str);
        return;
    }

    IXSLProcessorPtr processor;
    IXSLTemplatePtr xsltTemplate(CLSID_XSLTemplate);
    try{
        xmlResult = xsltTemplate->putref_stylesheet(clientXSLT);
        processor = xsltTemplate->createProcessor();
    } catch(_com_error &e) {
        hippoDebugLogW(L"HippoIE: Error setting XSL style sheet : %s\n", HippoBSTR(e.Description()).m_str);
        return;
    }
    IStream *iceCream;
    CreateStreamOnHGlobal(NULL,TRUE,&iceCream); // This is equivalent to Java's StringWriter
    processor->put_output(_variant_t(iceCream));

    xmlResult = processor->put_input(_variant_t(static_cast<IUnknown*>(xmlsrc)));

    // Append the specified parameters
    for (UINT i = 0; i < styleParamNames_.length(); i++) {
        processor->addParameter(_bstr_t(styleParamNames_[i].m_str), variant_t(styleParamValues_[i].m_str), _bstr_t(L""));
    }
    processor->transform();

    // Append NUL character so we can treat it as C string
    iceCream->Write((void const*)"\0",1,0);
    // Retrieve buffer, written in whatever encoding the XSLT stylesheet specified
    HGLOBAL hg = NULL;
    void *buf;
    GetHGlobalFromStream(iceCream, &hg);
    buf = GlobalLock(hg);

    HippoPtr<IDispatch> docDispatch;
    browser->get_Document(&docDispatch);
    HippoQIPtr<IHTMLDocument2> doc(docDispatch);
    HRESULT hresult = S_OK;
    VARIANT *param;
    SAFEARRAY *sfArray;
    HippoBSTR actualData;

    // The XSLT must have written out UTF-8.  Otherwise we lose...
    actualData.setUTF8((LPSTR) buf);

    // I have no idea why the write method takes an array of variants...crack!
    // We create a singleton array of a variant with a BSTR value
    sfArray = SafeArrayCreateVector(VT_VARIANT, 0, 1);
    hresult = SafeArrayAccessData(sfArray,(LPVOID*) & param);
    param->vt = VT_BSTR;
    param->bstrVal = actualData.stealContents();
    hresult = SafeArrayUnaccessData(sfArray);
    // Append the transformed XML to the document
    hresult = doc->write(sfArray);
    hresult = doc->close();
    
    SafeArrayDestroy(sfArray);
    iceCream->Release();
}

void
HippoIEImpl::shutdown()
{
    if (connectionPoint_) {
        if (connectionCookie_) {
            connectionPoint_->Unadvise(connectionCookie_);
            connectionCookie_ = 0;
        }
        connectionPoint_ = NULL;
    }

    if (ie_) {
        ie_->Close(OLECLOSE_NOSAVE);
        ie_ = NULL;
    }

    external_ = NULL;
    browser_ = NULL;
}

void 
HippoIEImpl::setLocation(const HippoBSTR &location)
{
    if (!browser_) {
        docSrc_ = location;
        return;
    }

    inNavigation_ = true;

    variant_t flags;
    variant_t targetFrameName(L"_self");
    variant_t postData;
    variant_t headers;
    browser_->Navigate(location.m_str, &flags, &targetFrameName, &postData, &headers);
}

void
HippoIEImpl::resize(RECT *rect)
{
    HippoQIPtr<IOleInPlaceObject> inPlace = ie_;
    inPlace->SetObjectRects(rect, rect);
}

bool 
HippoIEImpl::handleNavigation(IDispatch *targetDispatch,
                              BSTR       url,
                              bool       isPost)
{
    // Cases here:
    //
    // 0. If we're navigating programmatically via setLocation(), allow it
    //
    // 1. If it's a POST to our site, allow it
    //
    // 2. If it's normal navigation (of the toplevel window, not a post),
    //    to a normal looking URL (http, https, ftp protocol), then open
    //    the link in a new IE window, and cancel navigation.
    //
    // 3. If it's navigation not of the toplevel window (of an internal
    //    frame) and the link points to our site (via http or https)
    //    then allow the navigation.
    //
    // 4. If it's a javascript: URL, then it is code in the page and
    //    not really a link at all, so allow navigation.
    //
    // 5. Otherwise, cancel navigation
    
    HippoQIPtr<IWebBrowser2> targetWebBrowser = targetDispatch;
    bool toplevelNavigation = (IWebBrowser2 *)targetWebBrowser == (IWebBrowser2 *)browser_;

    bool ourSite = false;
    bool normalURL = false;
    bool javascript = false;

    HippoURLParser parser(url);
    if (parser.ok()) {
        INTERNET_SCHEME scheme = parser.getScheme();

        // Note that this blocks things like itms: and aim:, we may need to loosen
        // that eventually for desired functionality, but be safe for now.
        if (scheme == INTERNET_SCHEME_HTTP ||
            scheme == INTERNET_SCHEME_HTTPS ||
            scheme == INTERNET_SCHEME_FTP)
            normalURL = true;
        else if (scheme == INTERNET_SCHEME_JAVASCRIPT)
            javascript = true;

        if (scheme == INTERNET_SCHEME_HTTP ||
            scheme == INTERNET_SCHEME_HTTPS) 
        {
            
            ourSite = callback_->isOurServer(parser.getHostName());
        }
    }

    hippoDebugLogW(L"handleNavigation: %ls\r\n   oursite=%d, normalURL=%d, javascript=%d, toplevelNavigation=%d, inNavigation=%d", 
                   url, ourSite, normalURL, javascript, toplevelNavigation, inNavigation_);

    if (ourSite && toplevelNavigation && inNavigation_) {
        hippoDebugLogW(L"   Allowing navigation via SetLocation");
        inNavigation_ = false;
        return false;
    } if (ourSite && isPost) {
        hippoDebugLogW(L"   Allowing POST navigation to our site");
        return false;
    } else if (toplevelNavigation && !isPost && normalURL) {
        hippoDebugLogW(L"   Opening URL in separate window");
        callback_->launchBrowser(url);
        return true;
    } else if (!toplevelNavigation && ourSite) {
        hippoDebugLogW(L"   Allowing internal frame navigation");
        return false;
    } else if (javascript) {
        hippoDebugLogW(L"   Allowing navigation to javascript URL");
        return false;
    } else if (wcscmp(url, L"about:blank#") == 0) {
        hippoDebugLogW(L"   Denying navigation to about:blank (this is a bug workaround)");
        return true;
    } else {
        hippoDebugLogW(L"   Denying navigation");
        return true;
    }
}

// IDocHostUIHandler
STDMETHODIMP 
HippoIEImpl::EnableModeless(BOOL enable)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::FilterDataObject(IDataObject *dobj, IDataObject **dobjRet)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::GetDropTarget(IDropTarget *dropTarget, IDropTarget **dropTargetRet)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::GetExternal(IDispatch **dispatch)
{
    *dispatch = external_;
    if (*dispatch) {
        (*dispatch)->AddRef();
        return S_OK;
    } else {
        return E_OUTOFMEMORY;
    }
}

STDMETHODIMP 
HippoIEImpl::GetHostInfo(DOCHOSTUIINFO *info)
{
    if (info->cbSize < sizeof(DOCHOSTUIINFO))
        return E_FAIL;

    info->dwFlags = threeDBorder_ ? 0 : DOCHOSTUIFLAG_NO3DBORDER;
    info->dwDoubleClick = DOCHOSTUIDBLCLK_DEFAULT;
    info->pchHostCss = NULL;
    info->pchHostNS = NULL;

    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::GetOptionKeyPath(LPOLESTR *chKey, DWORD dw)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::HideUI(VOID)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::OnDocWindowActivate(BOOL activate)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::OnFrameWindowActivate(BOOL activate)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::ResizeBorder(LPCRECT border, IOleInPlaceUIWindow *uiWindow, BOOL frameWindow)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::ShowContextMenu(DWORD id, POINT *pt, IUnknown *cmdtReserved, IDispatch *dispReserved)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::ShowUI(DWORD id, IOleInPlaceActiveObject *activeObject, IOleCommandTarget *commandTarget, IOleInPlaceFrame *frame, IOleInPlaceUIWindow *doc)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::TranslateAccelerator(LPMSG msg, const GUID *guidCmdGroup, DWORD cmdID)
{
    return S_FALSE;
}

STDMETHODIMP 
HippoIEImpl::TranslateUrl(DWORD translate, OLECHAR *chURLIn, OLECHAR **chURLOut)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::UpdateUI(VOID)
{
    return S_OK;
}

// IStorage
STDMETHODIMP 
HippoIEImpl::CreateStream(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStream ** ppstm)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::OpenStream(const WCHAR * pwcsName,void * reserved1,DWORD grfMode,DWORD reserved2,IStream ** ppstm)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::CreateStorage(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStorage ** ppstg)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::OpenStorage(const WCHAR * pwcsName,IStorage * pstgPriority,DWORD grfMode,SNB snbExclude,DWORD reserved,IStorage ** ppstg)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::CopyTo(DWORD ciidExclude,IID const * rgiidExclude,SNB snbExclude,IStorage * pstgDest)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::MoveElementTo(const OLECHAR * pwcsName,IStorage * pstgDest,const OLECHAR* pwcsNewName,DWORD grfFlags)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::Commit(DWORD grfCommitFlags)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::Revert(void)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::EnumElements(DWORD reserved1,void * reserved2,DWORD reserved3,IEnumSTATSTG ** ppenum)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::DestroyElement(const OLECHAR * pwcsName)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::RenameElement(const WCHAR * pwcsOldName,const WCHAR * pwcsNewName)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::SetElementTimes(const WCHAR * pwcsName,FILETIME const * pctime,FILETIME const * patime,FILETIME const * pmtime)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::SetClass(REFCLSID clsid)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::SetStateBits(DWORD grfStateBits,DWORD grfMask)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::Stat(STATSTG * pstatstg,DWORD grfStatFlag)
{
    NOTIMPLEMENTED;
}

// IOleWindow

STDMETHODIMP 
HippoIEImpl::GetWindow(HWND FAR* lphwnd)
{
    *lphwnd = window_;

    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::ContextSensitiveHelp(BOOL fEnterMode)
{
    NOTIMPLEMENTED;
}

// IOleInPlaceUIWindow
STDMETHODIMP 
HippoIEImpl::GetBorder(LPRECT lprectBorder)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::RequestBorderSpace(LPCBORDERWIDTHS pborderwidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::SetBorderSpace(LPCBORDERWIDTHS pborderwidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::SetActiveObject(IOleInPlaceActiveObject *pActiveObject,LPCOLESTR pszObjName)
{
    return S_OK;
}


// IOleInPlaceFrame
STDMETHODIMP 
HippoIEImpl::InsertMenus(HMENU hmenuShared,LPOLEMENUGROUPWIDTHS lpMenuWidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::SetMenu(HMENU hmenuShared,HOLEMENU holemenu,HWND hwndActiveObject)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::RemoveMenus(HMENU hmenuShared)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::SetStatusText(LPCOLESTR pszStatusText)
{
    return S_OK;
}

// EnableModeless already covered

STDMETHODIMP HippoIEImpl::TranslateAccelerator(  LPMSG lpmsg,WORD wID)
{
    NOTIMPLEMENTED;
}

// IOleClientSite

STDMETHODIMP 
HippoIEImpl::SaveObject()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::GetMoniker(DWORD dwAssign,DWORD dwWhichMoniker,IMoniker ** ppmk)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::GetContainer(LPOLECONTAINER FAR* ppContainer)
{
    // We are a simple object and don't support a container.
    *ppContainer = NULL;

    return E_NOINTERFACE;
}

STDMETHODIMP 
HippoIEImpl::ShowObject()
{
    return NOERROR;
}

STDMETHODIMP 
HippoIEImpl::OnShowWindow(BOOL fShow)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::RequestNewObjectLayout()
{
    NOTIMPLEMENTED;
}

// IOleInPlaceSite
STDMETHODIMP 
HippoIEImpl::CanInPlaceActivate()
{
    // Yes we can
    return S_OK;
}

STDMETHODIMP
HippoIEImpl::OnInPlaceActivate()
{
    // Why disagree.
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::OnUIActivate()
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::GetWindowContext(
    LPOLEINPLACEFRAME FAR* ppFrame,
    LPOLEINPLACEUIWINDOW FAR* ppDoc,
    LPRECT prcPosRect,
    LPRECT prcClipRect,
    LPOLEINPLACEFRAMEINFO lpFrameInfo)
{
    AddRef();
    *ppFrame = this;
    *ppDoc = NULL;
    GetClientRect(window_,prcPosRect);
    GetClientRect(window_,prcClipRect);

    lpFrameInfo->fMDIApp = FALSE;
    lpFrameInfo->hwndFrame = window_;
    lpFrameInfo->haccel = NULL;
    lpFrameInfo->cAccelEntries = 0;

    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::Scroll(SIZE scrollExtent)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::OnUIDeactivate(BOOL fUndoable)
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::OnInPlaceDeactivate()
{
    return S_OK;
}

STDMETHODIMP 
HippoIEImpl::DiscardUndoState()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::DeactivateAndUndo()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::OnPosRectChange(LPCRECT lprcPosRect)
{
    return S_OK;
}

// IParseDisplayName
STDMETHODIMP 
HippoIEImpl::ParseDisplayName(IBindCtx *pbc,LPOLESTR pszDisplayName,ULONG *pchEaten,IMoniker **ppmkOut)
{
    NOTIMPLEMENTED;
}

// IOleContainer
STDMETHODIMP 
HippoIEImpl::EnumObjects(DWORD grfFlags,IEnumUnknown **ppenum)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIEImpl::LockContainer(BOOL fLock)
{
    NOTIMPLEMENTED;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoIEImpl::QueryInterface(const IID &ifaceID, 
                            void    **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IDocHostUIHandler*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch))
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IStorage))
        *result = static_cast<IStorage *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleWindow))
        *result = static_cast<IOleWindow *>(static_cast<IOleInPlaceSite *>(this));
    else if (IsEqualIID(ifaceID, IID_IOleInPlaceUIWindow))
        *result = static_cast<IOleInPlaceUIWindow *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleInPlaceFrame))
        *result = static_cast<IOleInPlaceFrame *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleClientSite))
        *result = static_cast<IOleClientSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleInPlaceSite)) // || riid == IID_IOleInPlaceSiteEx || riid == IID_IOleInPlaceSiteWindowless)
        *result = static_cast<IOleInPlaceSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IParseDisplayName))
        *result = static_cast<IParseDisplayName *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleContainer))
        *result = static_cast<IOleContainer *>(this);
    else if (IsEqualIID(ifaceID, IID_IDocHostUIHandler))
        *result = static_cast<IDocHostUIHandler *>(this);
    else if (IsEqualIID(ifaceID, DIID_DWebBrowserEvents2))
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IServiceProvider))
        *result = static_cast<IServiceProvider *>(this);
#if 0
    else if (IsEqualIID(ifaceID, IID_IOleCommandTarget))
        *result = static_cast<IOleCommandTarget *>(this);
#endif
    else {
#if 0
        // Can be used to print out interfaces that are being
        // queried for that we don't implement; some known
        // interfaces that are queried for against this object
        // are IOleCommandTarget and IOleControlSite

        WCHAR *ifaceStr;
        if (SUCCEEDED(StringFromIID(ifaceID, &ifaceStr))) {
            hippoDebugLogW(L"QI for interface: %s\n", ifaceStr);
            CoTaskMemFree(ifaceStr);
        }
#endif
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoIEImpl)


//////////////////////// IDispatch implementation ///////////////////

STDMETHODIMP
HippoIEImpl::GetIDsOfNames (const IID   &iid,
                             OLECHAR    **names,  
                             unsigned int cNames,          
                             LCID         lcid,                   
                             DISPID *     dispID)
{
    return DispGetIDsOfNames(eventsTypeInfo_, names, cNames, dispID);
}

STDMETHODIMP
HippoIEImpl::GetTypeInfo (unsigned int infoIndex,  
                          LCID         lcid,                  
                          ITypeInfo  **ppTInfo)
{
   if (ppTInfo == NULL)
      return E_INVALIDARG;

   *ppTInfo = NULL;

   if (infoIndex != 0)
      return DISP_E_BADINDEX;

   eventsTypeInfo_->AddRef();
   *ppTInfo = eventsTypeInfo_;

   return S_OK;
}

STDMETHODIMP 
HippoIEImpl::GetTypeInfoCount (unsigned int *pcTInfo)
{
    if (pcTInfo == NULL)
        return E_INVALIDARG;

    *pcTInfo = 1;

    return S_OK;
}
  
STDMETHODIMP
HippoIEImpl::Invoke (DISPID        member,
                     const IID    &iid,
                     LCID          lcid,              
                     WORD          flags,
                     DISPPARAMS   *dispParams,
                     VARIANT      *result,
                     EXCEPINFO    *excepInfo,  
                     unsigned int *argErr)
{
    switch (member) {
        case DISPID_BEFORENAVIGATE2:
            if (dispParams->cArgs != 7)
                return DISP_E_BADPARAMCOUNT;
            if (!(dispParams->rgvarg[6].vt == VT_DISPATCH &&
                  dispParams->rgvarg[5].vt == (VT_BYREF | VT_VARIANT) &&
                  dispParams->rgvarg[5].pvarVal->vt == VT_BSTR &&
                  dispParams->rgvarg[3].vt == (VT_BYREF | VT_VARIANT) &&
                  dispParams->rgvarg[3].pvarVal->vt == VT_BSTR &&
                  dispParams->rgvarg[2].vt == (VT_BYREF | VT_VARIANT) &&
                  dispParams->rgvarg[2].pvarVal->vt == (VT_BYREF | VT_VARIANT) && 
                  (dispParams->rgvarg[2].pvarVal->pvarVal->vt == (VT_ARRAY | VT_UI1) ||
                   dispParams->rgvarg[2].pvarVal->pvarVal->vt == VT_EMPTY) &&
                  dispParams->rgvarg[1].vt == (VT_BYREF | VT_VARIANT) &&
                  dispParams->rgvarg[1].pvarVal->vt == VT_BSTR &&
                  dispParams->rgvarg[0].vt == (VT_BYREF | VT_BOOL))) 
            {
                // If we get here, most likely the above code is just buggy, so do the 
                // safe thing and cancel navigation since we can't understand the request
                if (dispParams->rgvarg[0].vt == (VT_BYREF | VT_BOOL)) {
                    *dispParams->rgvarg[0].pboolVal = true;
                    return S_OK;
                } else
                    return DISP_E_BADVARTYPE;
            }

            // Check to see if navigation should be allowed; if the function returns TRUE
            // either handleNavigation() has handled the navigation itself by opening
            // an IE window for the URL or navigation should be blocked entirely.
            *dispParams->rgvarg[0].pboolVal = handleNavigation(dispParams->rgvarg[6].pdispVal,
                                                               dispParams->rgvarg[5].pvarVal->bstrVal,
                                                               dispParams->rgvarg[2].pvarVal->pvarVal->vt != VT_EMPTY);

            return S_OK;
        case DISPID_DOCUMENTCOMPLETE:
            if (dispParams->cArgs != 2)
                return DISP_E_BADPARAMCOUNT;
            if (!(dispParams->rgvarg[1].vt == VT_DISPATCH &&
                  dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT)))
                return DISP_E_BADVARTYPE;
            callback_->onDocumentComplete();
            return S_OK;
        case DISPID_WINDOWCLOSING:
            if (dispParams->cArgs != 2)
                return DISP_E_BADPARAMCOUNT;
            if (!(dispParams->rgvarg[1].vt == VT_BOOL &&
                  dispParams->rgvarg[0].vt == (VT_BYREF | VT_BOOL)))
                return DISP_E_BADVARTYPE;

            // window.close() was called from a script; storing TRUE
            // in the out parameter means "cancel", and then we handle
            // it ourselves by hiding the window. If we stored FALSE,
            // IE would ask the user whether they wanted to close the
            // window, then (AFAIK) do nothing, since the web browser
            // control doesn't have any ability to "quit".
            *dispParams->rgvarg[0].pboolVal = VARIANT_TRUE;

            callback_->onClose();                
            return S_OK;
        default:
            return DISP_E_MEMBERNOTFOUND; // Or S_OK
     }
 }

////////////////// IServiceProvider implementation ///////////////////

STDMETHODIMP 
HippoIEImpl::QueryService(const GUID &serviceID, 
                          const IID  &ifaceID, 
                          void      **result)
{
    // Query service is called on the HTML with a wide range of 
    // Service ID's .... IID_IHttpNegiotiate2, IID_ITargetFrame2,
    // any many others. We just chain this one to our embedder
    // for now.

    if (IsEqualGUID(serviceID, SID_STopLevelBrowser))
        return callback_->getToplevelBrowser(ifaceID, result);
    else
        return E_UNEXPECTED;
}

#if 0

/////////////////// IOleCommandTarget implementation ///////////////////

/* We implement IOleCommandTarget to handle requests that the control
 * (or Javascript in the control) makes of the target, like "close
 * the window". The set of possible commands isn't really documented
 * very well; we can get some of the standard OLE commands, like 
 * OLECMD_ID ... in the default group represented by commandGroup === NULL
 * but we may also get explorer-specific commands with 
 * commandGroup == CGID_Explorer. We hope that we can get away with
 * simply not supporting these commands, since they aren't documented.
 */

// Query for information about a particular command. It's very unlikely
// this is needed, but we provide an implementation anyways for
// correctness sake.
STDMETHODIMP
HippoIEImpl::QueryStatus (const GUID *commandGroup,
                          ULONG       nCommands,
                          OLECMD     *commands,
                          OLECMDTEXT *commandText)
{
    for (ULONG i = 0; i < nCommands; i++) {
        if (commandGroup == NULL && commands[i].cmdID == OLECMDID_CLOSE) {
            commands[i].cmdf = OLECMDF_SUPPORTED | OLECMDF_ENABLED;

            if (commandText) {
                if ((commandText->cmdtextf & OLECMDTEXTF_NAME) != 0) {
                    commandText->cwActual = (ULONG)wcslen(L"Close");
                    StringCchCopy(commandText->rgwz, commandText->cwBuf, L"Close");
                } else if ((commandText->cmdtextf & OLECMDTEXTF_STATUS) != 0) {
                    commandText->cwActual = (ULONG)wcslen(L"");
                    StringCchCopy(commandText->rgwz, commandText->cwBuf, L"");
                }

                commandText = NULL; // the text result is for the first supported command
            }
        } else {
            commands[i].cmdf = 0;
        }
    }

    return S_OK;
}

STDMETHODIMP
HippoIEImpl::Exec (const GUID *commandGroup,
                   DWORD       commandId,
                   DWORD       nCommandExecOptions,
                   VARIANTARG *commandInput,
                   VARIANTARG *commandOutput)
{
    // OLECMDID_CLOSE isn't actually invoked, even when IWebBrowser2->Quit()
    // is called; we leave it in here as a stub; quite a few other commands
    // are invoked, such as OLECMDID_UPDATECOMMANDS
    //
    if (commandGroup == NULL) {
        if (commandId == OLECMDID_CLOSE) {
            hippoDebugDialog(L"Close the window, it's cold in here");
            return S_OK;
        } else {
            return OLECMDERR_E_NOTSUPPORTED;
        }
    } else {
        return OLECMDERR_E_UNKNOWNGROUP;
    }
}
#endif
