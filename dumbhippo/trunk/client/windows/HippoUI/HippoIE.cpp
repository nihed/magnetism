#include "StdAfx.h"
#include ".\hippoie.h"
#include <mshtml.h>

#import <msxml3.dll>  named_guids

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include <HippoUtil.h>

using namespace MSXML2;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoIE::HippoIE(HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *external)
{
    window_ = window;
    docSrc_ = src;
    callback_ = cb;
    external_ = external;
}

HippoIE::~HippoIE(void)
{
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
    HippoQIPtr<IHTMLDocument2> doc(docDispatch);
    HippoPtr<IDispatch> script;
    doc->get_Script(&script);

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

    HippoBSTR blankURL(L"about:blank");
    VARIANT url;
    url.vt = VT_BSTR;
    url.bstrVal = blankURL;
    VARIANT vempty;
    vempty.vt = VT_EMPTY;

    browser->Navigate2(&url, &vempty, &vempty, &vempty, &vempty);
    browser->put_Resizable(VARIANT_FALSE);

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

    IDispatch *docDispatch;
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

    iceCream->Release();
}

void
HippoIE::resize(RECT *rect)
{
    HippoQIPtr<IOleInPlaceObject> inPlace = ie_;
    inPlace->SetObjectRects(rect, rect);
}

// IDocHostUIExternal
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
    }
    return E_NOTIMPL;
}

STDMETHODIMP 
HippoIE::GetHostInfo(DOCHOSTUIINFO *info)
{
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
    return S_OK;
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
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IDocHostUIHandler*>(this));
    else if (IsEqualIID(ifaceID, IID_IDocHostUIHandler))
        *result = static_cast<IDocHostUIHandler*>(this);
    else if (IsEqualIID(ifaceID, IID_IOleClientSite))
        *result = static_cast<IOleClientSite*>(this);
    else if (IsEqualIID(ifaceID, IID_IOleInPlaceSite)) // || riid == IID_IOleInPlaceSiteEx || riid == IID_IOleInPlaceSiteWindowless)
        *result = static_cast<IOleInPlaceSite*>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoIE)