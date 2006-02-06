/* HippoIE.cpp: Embed an instance if the IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoIE.h"
#include <mshtml.h>

#import <msxml3.dll>  named_guids

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include <HippoUtil.h>
#include "HippoExternal.h"
#include "HippoLogWindow.h"
#include "HippoUI.h"

using namespace MSXML2;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoIE::HippoIE(HippoUI *ui, HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *application)
{
    ui_ = ui;
    window_ = window;
    docSrc_ = src;
    callback_ = cb;
    haveTransform_ = false;
    threeDBorder_ = true;

    HippoExternal *external = new HippoExternal();
    if (external)
        external->setApplication(application);
    external_ = external;
    external->Release();

    hippoLoadRegTypeInfo(LIBID_SHDocVw, 1, 1,
                         &DIID_DWebBrowserEvents2, &eventsTypeInfo_, 
                         NULL);
}

HippoIE::~HippoIE(void)
{
    if (connectionPoint_) {
        if (connectionCookie_) {
            connectionPoint_->Unadvise(connectionCookie_);
            connectionCookie_ = 0;
        }
        connectionPoint_ = NULL;
    }
    ie_->Release();
}

HRESULT
HippoIE::invokeJavascript(WCHAR * funcName, VARIANT *invokeResult, int nargs, ...)
{
    va_list vap;
    va_start(vap, nargs);
    HRESULT result = invokeJavascript(funcName, invokeResult, nargs, vap);
    va_end(vap);
    return result;
}

HRESULT 
HippoIE::invokeJavascript(WCHAR * funcName, VARIANT *invokeResult, int nargs, va_list vap)
{    
    HippoBSTR funcNameStr(funcName);
    VARIANT *arg;
    int argc;
    HippoPtr<IDispatch> docDispatch;
    browser_->get_Document(&docDispatch);
    assert(docDispatch != NULL);
    HippoQIPtr<IHTMLDocument2> doc(docDispatch);
    assert(doc != NULL);
    HippoPtr<IDispatch> script;
    doc->get_Script(&script);
    assert(script != NULL);

    DISPID id = NULL;
    HRESULT result = script->GetIDsOfNames(IID_NULL,&(funcNameStr.m_str),1,LOCALE_SYSTEM_DEFAULT,&id);
    if (FAILED(result))
        return result;

    DISPPARAMS args;
    ZeroMemory(&args, sizeof(args));
    args.rgvarg = new VARIANT[nargs];
    args.cNamedArgs = 0;

    argc = 0;
    for (int argc = 0; argc < nargs; argc++) {
        arg = va_arg (vap, VARIANT *);
        // This has to be in reverse order for some reason...CRACK
        VARIANT *destArg = &(args.rgvarg[(nargs-argc)-1]);
        VariantInit(destArg);
        result = VariantCopy(destArg, arg);
        if (FAILED(result))
            return result;
    }
    args.cArgs = nargs;

    EXCEPINFO excep;
    ZeroMemory(&excep, sizeof(excep));
    UINT argErr;
    result = script->Invoke(id, IID_NULL, 0, DISPATCH_METHOD,
                            &args, invokeResult, &excep, &argErr);
    delete [] args.rgvarg;
    return result;
}

IWebBrowser2 *
HippoIE::getBrowser()
{
    return browser_;
}
void
HippoIE::setXsltTransform(WCHAR *stylesrc, ...)
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
HippoIE::setThreeDBorder(bool threeDBorder)
{
    threeDBorder_ = threeDBorder;
}
    
void
HippoIE::signalError(WCHAR *text, ...)
{
    WCHAR buf[1024];
    va_list args;
    va_start(args, text);
    
    StringCchVPrintfW(buf, sizeof(buf)/sizeof(buf[0]), text, args);
    callback_->onError(buf);

    va_end(args);
}

void
HippoIE::create()
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
        signalError(L"Error loading XML files : %s\n",
            HippoBSTR(e.Description()).m_str);
        return;
    }

    IXSLProcessorPtr processor;
    IXSLTemplatePtr xsltTemplate(CLSID_XSLTemplate);
    try{
        xmlResult = xsltTemplate->putref_stylesheet(clientXSLT);
        processor = xsltTemplate->createProcessor();
    } catch(_com_error &e) {
        signalError(L"Error setting XSL style sheet : %s\n", HippoBSTR(e.Description()).m_str);
        return;
    }
    IStream *iceCream;
    CreateStreamOnHGlobal(NULL,TRUE,&iceCream); // This is equivalent to Java's StringWriter
    processor->put_output(_variant_t(iceCream));

    xmlResult = processor->put_input(_variant_t(static_cast<IUnknown*>(xmlsrc)));

    // Append the specified parameters
    for (UINT i = 0; i < styleParamNames_.length(); i++) {
        processor->addParameter(_bstr_t(styleParamNames_[i].m_str), variant_t(styleParamValues_[i].m_str), _bstr_t(""));
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
    param->bstrVal = actualData;
    hresult = SafeArrayUnaccessData(sfArray);
    // Append the transformed XML to the document
    hresult = doc->write(sfArray);
    hresult = doc->close();

    iceCream->Release();
}

void
HippoIE::resize(RECT *rect)
{
    HippoQIPtr<IOleInPlaceObject> inPlace = ie_;
    inPlace->SetObjectRects(rect, rect);
}

bool 
HippoIE::handleNavigation(IDispatch *targetDispatch,
                          BSTR       url,
                          bool       isPost)
{
    // Cases here:
    //
    // 1. If it's normal navigation (of the toplevel window, not a post),
    //    to a normal looking URL (http, https, ftp protocol), then open
    //    the link in a new IE window, and cancel navigation.
    //
    //    We are also allowing javascript: for now; in principle our HTML
    //    is all from our server and these should be safe. javascript: links
    //    don't open in a new window.
    //
    // 2. If it's navigation not of the toplevel window (of an internal
    //    frame) and the link points to our site (via http or https)
    //    then allow the navigation.
    //
    // 3. If it's a javascript: URL, then it is code in the page and
    //    not really a link at all, so allow navigation.
    //
    // 4. Otherwise, cancel navigation
    
    HippoQIPtr<IWebBrowser2> targetWebBrowser = targetDispatch;
    bool toplevelNavigation = (IWebBrowser2 *)targetWebBrowser == (IWebBrowser2 *)browser_;

    bool ourSite = false;
    bool normalURL = false;
    bool javascript = false;
    
    URL_COMPONENTS components;
    ZeroMemory(&components, sizeof(components));
    components.dwStructSize = sizeof(components);

    // The case where lpszHostName is NULL and dwHostNameLength is non-0 means
    // to return pointers into the passed in URL along with lengths. The 
    // specific non-zero value is irrelevant
    components.dwHostNameLength = 1;
    components.dwUserNameLength = 1;
    components.dwPasswordLength = 1;
    components.dwUrlPathLength = 1;
    components.dwExtraInfoLength = 1;

    if (InternetCrackUrl(url, 0, 0, &components)) {
        // javascript: special case; just code in the page
        if (components.nScheme == INTERNET_SCHEME_JAVASCRIPT) {
            hippoDebugLogW(L"Allowing navigation to javascript URL");
            return false;
        }

        // Note that this blocks things like itms: and aim:, we may need to loosen
        // that eventually for desired functionality, but be safe for now.
        if (components.nScheme == INTERNET_SCHEME_HTTP ||
            components.nScheme == INTERNET_SCHEME_HTTPS ||
            components.nScheme == INTERNET_SCHEME_FTP)
            normalURL = true;
        else if (components.nScheme == INTERNET_SCHEME_JAVASCRIPT)
            javascript = true;

        HippoBSTR webServer;
        unsigned int port;
        ui_->getPreferences()->parseWebServer(&webServer, &port);

        if ((components.nScheme == INTERNET_SCHEME_HTTP ||
            components.nScheme == INTERNET_SCHEME_HTTPS) &&
            components.dwHostNameLength == webServer.Length() &&
            wcsncmp(components.lpszHostName, webServer, components.dwHostNameLength) == 0)
            ourSite = true;
    }

    if (toplevelNavigation && !isPost && normalURL) {
        ui_->launchBrowser(url);
        return true;
    } else if (javascript) {
        hippoDebugLogW(L"Allowing javascript href %ls", url);
        return false;
    } else if (!toplevelNavigation && ourSite) {
        hippoDebugLogW(L"Allowing internal frame navigation to %ls", url);
        return false;
    } else {
        hippoDebugLogW(L"Denying navigation to %ls", url);
        return true;
    }
}

// IDocHostUIHandler
STDMETHODIMP 
HippoIE::EnableModeless(BOOL enable)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::FilterDataObject(IDataObject *dobj, IDataObject **dobjRet)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::GetDropTarget(IDropTarget *dropTarget, IDropTarget **dropTargetRet)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::GetExternal(IDispatch **dispatch)
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
HippoIE::GetHostInfo(DOCHOSTUIINFO *info)
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
HippoIE::GetOptionKeyPath(LPOLESTR *chKey, DWORD dw)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::HideUI(VOID)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::OnDocWindowActivate(BOOL activate)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::OnFrameWindowActivate(BOOL activate)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::ResizeBorder(LPCRECT border, IOleInPlaceUIWindow *uiWindow, BOOL frameWindow)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::ShowContextMenu(DWORD id, POINT *pt, IUnknown *cmdtReserved, IDispatch *dispReserved)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::ShowUI(DWORD id, IOleInPlaceActiveObject *activeObject, IOleCommandTarget *commandTarget, IOleInPlaceFrame *frame, IOleInPlaceUIWindow *doc)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::TranslateAccelerator(LPMSG msg, const GUID *guidCmdGroup, DWORD cmdID)
{
    return S_FALSE;
}

STDMETHODIMP 
HippoIE::TranslateUrl(DWORD translate, OLECHAR *chURLIn, OLECHAR **chURLOut)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::UpdateUI(VOID)
{
    return S_OK;
}

// IStorage
STDMETHODIMP 
HippoIE::CreateStream(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStream ** ppstm)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::OpenStream(const WCHAR * pwcsName,void * reserved1,DWORD grfMode,DWORD reserved2,IStream ** ppstm)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::CreateStorage(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStorage ** ppstg)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::OpenStorage(const WCHAR * pwcsName,IStorage * pstgPriority,DWORD grfMode,SNB snbExclude,DWORD reserved,IStorage ** ppstg)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::CopyTo(DWORD ciidExclude,IID const * rgiidExclude,SNB snbExclude,IStorage * pstgDest)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::MoveElementTo(const OLECHAR * pwcsName,IStorage * pstgDest,const OLECHAR* pwcsNewName,DWORD grfFlags)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::Commit(DWORD grfCommitFlags)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::Revert(void)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::EnumElements(DWORD reserved1,void * reserved2,DWORD reserved3,IEnumSTATSTG ** ppenum)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::DestroyElement(const OLECHAR * pwcsName)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::RenameElement(const WCHAR * pwcsOldName,const WCHAR * pwcsNewName)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::SetElementTimes(const WCHAR * pwcsName,FILETIME const * pctime,FILETIME const * patime,FILETIME const * pmtime)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::SetClass(REFCLSID clsid)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::SetStateBits(DWORD grfStateBits,DWORD grfMask)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::Stat(STATSTG * pstatstg,DWORD grfStatFlag)
{
    NOTIMPLEMENTED;
}

// IOleWindow

STDMETHODIMP 
HippoIE::GetWindow(HWND FAR* lphwnd)
{
    *lphwnd = window_;

    return S_OK;
}

STDMETHODIMP 
HippoIE::ContextSensitiveHelp(BOOL fEnterMode)
{
    NOTIMPLEMENTED;
}

// IOleInPlaceUIWindow
STDMETHODIMP 
HippoIE::GetBorder(LPRECT lprectBorder)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::RequestBorderSpace(LPCBORDERWIDTHS pborderwidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::SetBorderSpace(LPCBORDERWIDTHS pborderwidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::SetActiveObject(IOleInPlaceActiveObject *pActiveObject,LPCOLESTR pszObjName)
{
    return S_OK;
}


// IOleInPlaceFrame
STDMETHODIMP 
HippoIE::InsertMenus(HMENU hmenuShared,LPOLEMENUGROUPWIDTHS lpMenuWidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::SetMenu(HMENU hmenuShared,HOLEMENU holemenu,HWND hwndActiveObject)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::RemoveMenus(HMENU hmenuShared)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::SetStatusText(LPCOLESTR pszStatusText)
{
    return S_OK;
}

// EnableModeless already covered

STDMETHODIMP HippoIE::TranslateAccelerator(  LPMSG lpmsg,WORD wID)
{
    NOTIMPLEMENTED;
}

// IOleClientSite

STDMETHODIMP 
HippoIE::SaveObject()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::GetMoniker(DWORD dwAssign,DWORD dwWhichMoniker,IMoniker ** ppmk)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::GetContainer(LPOLECONTAINER FAR* ppContainer)
{
    // We are a simple object and don't support a container.
    *ppContainer = NULL;

    return E_NOINTERFACE;
}

STDMETHODIMP 
HippoIE::ShowObject()
{
    return NOERROR;
}

STDMETHODIMP 
HippoIE::OnShowWindow(BOOL fShow)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::RequestNewObjectLayout()
{
    NOTIMPLEMENTED;
}

// IOleInPlaceSite
STDMETHODIMP 
HippoIE::CanInPlaceActivate()
{
    // Yes we can
    return S_OK;
}

STDMETHODIMP
HippoIE::OnInPlaceActivate()
{
    // Why disagree.
    return S_OK;
}

STDMETHODIMP 
HippoIE::OnUIActivate()
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::GetWindowContext(
    LPOLEINPLACEFRAME FAR* ppFrame,
    LPOLEINPLACEUIWINDOW FAR* ppDoc,
    LPRECT prcPosRect,
    LPRECT prcClipRect,
    LPOLEINPLACEFRAMEINFO lpFrameInfo)
{
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
HippoIE::Scroll(SIZE scrollExtent)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::OnUIDeactivate(BOOL fUndoable)
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::OnInPlaceDeactivate()
{
    return S_OK;
}

STDMETHODIMP 
HippoIE::DiscardUndoState()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::DeactivateAndUndo()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::OnPosRectChange(LPCRECT lprcPosRect)
{
    return S_OK;
}

// IParseDisplayName
STDMETHODIMP 
HippoIE::ParseDisplayName(IBindCtx *pbc,LPOLESTR pszDisplayName,ULONG *pchEaten,IMoniker **ppmkOut)
{
    NOTIMPLEMENTED;
}

// IOleContainer
STDMETHODIMP 
HippoIE::EnumObjects(DWORD grfFlags,IEnumUnknown **ppenum)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoIE::LockContainer(BOOL fLock)
{
    NOTIMPLEMENTED;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoIE::QueryInterface(const IID &ifaceID, 
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
#if 0
    else if (IsEqualIID(ifaceID, IID_IServiceProvider))
        *result = static_cast<IServiceProvider *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleCommandTarget))
        *result = static_cast<IOleCommandTarget *>(this);
#endif
    else {
#if 0
        // Can be used to print out interfaces that are being
        // queried for that we don't implement; some known
        // interfaces that are queried for against this object
        // are IServiceProvider, IOleCommandTarget, and 
        // IOleControlSite

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

HIPPO_DEFINE_REFCOUNTING(HippoIE)


//////////////////////// IDispatch implementation ///////////////////

STDMETHODIMP
HippoIE::GetIDsOfNames (const IID   &iid,
                             OLECHAR    **names,  
                             unsigned int cNames,          
                             LCID         lcid,                   
                             DISPID *     dispID)
{
    return DispGetIDsOfNames(eventsTypeInfo_, names, cNames, dispID);
}

STDMETHODIMP
HippoIE::GetTypeInfo (unsigned int infoIndex,  
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
HippoIE::GetTypeInfoCount (unsigned int *pcTInfo)
{
    if (pcTInfo == NULL)
        return E_INVALIDARG;

    *pcTInfo = 1;

    return S_OK;
}
  
STDMETHODIMP
HippoIE::Invoke (DISPID        member,
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

#if 0
////////////////// IServiceProvider implementation ///////////////////

STDMETHODIMP 
HippoIE::QueryService(const GUID &serviceID, 
                      const IID  &ifaceID, 
                      void      **result)
{
    // Query service is called on the HTML with a wide range of 
    // Service ID's .... IID_IHttpNegiotiate2, IID_ITargetFrame2,
    // SID_STopLevelBrowser, and many others. We don't need to
    // handle any of them currently.

    WCHAR *serviceStr;

    if (SUCCEEDED(StringFromIID(serviceID, &serviceStr))) {
        hippoDebugLogW(L"QueryService for:%s\n", serviceStr);
        CoTaskMemFree(serviceStr);
    }

    return E_UNEXPECTED;
}

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
HippoIE::QueryStatus (const GUID *commandGroup,
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
HippoIE::Exec (const GUID *commandGroup,
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
            hippoDebug(L"Close the window, it's cold in here");
            return S_OK;
        } else {
            return OLECMDERR_E_NOTSUPPORTED;
        }
    } else {
        return OLECMDERR_E_UNKNOWNGROUP;
    }
}
#endif
