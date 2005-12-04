/* HippoBubble.cpp: Display notifications
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#import <msxml3.dll>  named_guids

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include "HippoUI.h"
#include "HippoIE.h"
#include <HippoUtil.h>
#include "HippoBubble.h"
#include "Guid.h"

static const TCHAR *CLASS_NAME = TEXT("HippoBubbleClass");
static const int BASE_WIDTH = 400;
static const int BASE_HEIGHT = 150;
static const UINT_PTR CHECK_MOUSE = 1;

using namespace MSXML2;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoBubble::HippoBubble(void)
{
    refCount_ = 1;
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    idle_ = FALSE;
    haveMouse_ = FALSE;
    effectiveIdle_ = FALSE;
    viewerSpace_ = 0;
    ie_ = NULL;

    ieCallback_ = new HippoBubbleIECallback(this);

    HippoPtr<ITypeLib> typeLib;
    HRESULT hr = LoadRegTypeLib(LIBID_HippoUtil, 
                                0, 1, /* Version */
                                0,    /* LCID */
                                &typeLib);
    if (SUCCEEDED (hr)) {
        typeLib->GetTypeInfoOfGuid(IID_IHippoBubble, &ifaceTypeInfo_);
        typeLib->GetTypeInfoOfGuid(CLSID_HippoBubble, &classTypeInfo_);
    } else
        hippoDebug(L"Failed to load type lib: %x\n", hr);
}

HippoBubble::~HippoBubble(void)
{
    delete ieCallback_;
}

void 
HippoBubble::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoBubble::createWindow(void)
{
    window_ = CreateWindowEx(WS_EX_TOPMOST, CLASS_NAME, L"Hippo Notification", WS_POPUP,
                             CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                             NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }
    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    moveResizeWindow();

    hippoSetWindowData<HippoBubble>(window_, this);

    return true;
}

void
HippoBubble::moveResizeWindow() 
{
    int width = BASE_WIDTH;
    int height = BASE_HEIGHT + viewerSpace_;

    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    MoveWindow(window_, 
               (desktopRect.right - width), (desktopRect.bottom - height), 
               width, height, 
               TRUE);

    if (ie_) {
        RECT rect;

        rect.top = 0;
        rect.left = 0;
        rect.bottom = height;
        rect.right = width;

        ie_->resize(&rect);
    }
}

void
HippoBubble::HippoBubbleIECallback::onError(WCHAR *text) 
{
    bubble_->ui_->debugLogW(L"HippoIE error: %s", text);
}

bool
HippoBubble::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    HippoBSTR srcURL;
    ui_->getAppletURL(L"notification.xml", &srcURL);
    ie_ = new HippoIE(window_, srcURL, ieCallback_, this);

    HippoBSTR appletURL;
    ui_->getAppletURL(L"", &appletURL);
    HippoBSTR styleURL;
    ui_->getAppletURL(L"clientstyle.xml", &styleURL);
    ie_->setXsltTransform(styleURL, L"appleturl", appletURL.m_str, NULL);
    ie_->create();
    browser_ = ie_->getBrowser();

    // Kind of a hack
    HippoBSTR serverURLStr;
    ui_->getRemoteURL(HippoBSTR(L""), &serverURLStr);
    HippoBSTR appletURLStr;
    ui_->getAppletURL(HippoBSTR(L""), &appletURLStr);
    variant_t serverUrl(serverURLStr.m_str);
    variant_t appletUrl(appletURLStr.m_str);
    variant_t result;
    ie_->invokeJavascript(L"dhInit", &result, 2, &serverUrl, &appletUrl);

    // Set the initial value of the idle state
    doSetIdle();

    return true;
}

bool
HippoBubble::create(void)
{
    if (window_ != NULL) {
        return true;
    }
    if (!registerClass()) {
        ui_->debugLogW(L"Failed to register window class");
        return false;
    }
    if (!createWindow()) {
        ui_->debugLogW(L"Failed to create window");
        return false;
    }
    if (!embedIE()) {
        ui_->debugLogW(L"Failed to embed IE");
        return false;
    }
    return true;
}

static SAFEARRAY *
hippoStrArrayToSafeArray(HippoArray<HippoBSTR> &args)
{
    // I swear the SAFEARRAY API was *designed* to be painful
    SAFEARRAYBOUND dim[1];
    dim[0].lLbound= 0;
    dim[0].cElements = args.length();
    SAFEARRAY *ret = SafeArrayCreate(VT_VARIANT, 1, dim);
    for (unsigned int i = 0; i < args.length(); i++) {
        VARIANT *data;
        VARIANT argv;
        SafeArrayAccessData(ret, (void**)&data);
        argv.vt = VT_BSTR;
        argv.bstrVal = args[i];
        VariantCopy(&(data[i]), &argv);
        SafeArrayUnaccessData(ret);
    }
    return ret;
}

void 
HippoBubble::setLinkNotification(HippoLinkShare &share)
{
    if (window_ == NULL) {
        ui_->debugLogW(L"Creating new window");
        if (!create()) {
            ui_->debugLogW(L"Failed to create window");
            return;
        }
    }

    variant_t senderName(share.senderName);
    variant_t senderId(share.senderId);
    variant_t postId(share.postId);
    variant_t linkTitle(share.title);
    variant_t linkURL(share.url);
    variant_t linkDescription(share.description);
    SAFEARRAY *personRecipients = hippoStrArrayToSafeArray(share.personRecipients);
    VARIANT personRecipientsArg;
    personRecipientsArg.vt = VT_ARRAY | VT_VARIANT;
    personRecipientsArg.parray = personRecipients;
    SAFEARRAY *groupRecipients = hippoStrArrayToSafeArray(share.groupRecipients);
    VARIANT groupRecipientsArg;
    groupRecipientsArg.vt = VT_ARRAY | VT_VARIANT;
    groupRecipientsArg.parray = groupRecipients;
    SAFEARRAY *viewers = hippoStrArrayToSafeArray(share.viewers);
    VARIANT viewersArg;
    viewersArg.vt = VT_ARRAY | VT_VARIANT;
    viewersArg.parray = viewers;

    VARIANT result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    invokeJavascript(L"dhAddLinkShare", &result, 9, &senderName,
                     &senderId, &postId, &linkTitle, &linkURL, &linkDescription,
                     &personRecipientsArg, &groupRecipientsArg, &viewersArg);
    SafeArrayDestroy(personRecipients);
    SafeArrayDestroy(groupRecipients);

    show();
}

bool
HippoBubble::invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...)
{
    va_list args;
    va_start (args, nargs);
    HRESULT result = ie_->invokeJavascript(funcName, invokeResult, nargs, args);
    bool ret = SUCCEEDED(result);
    if (!ret)
        ui_->logError(L"failed to invoke javascript", result);
    va_end (args);
    return ret;
}

void 
HippoBubble::setIdle(bool idle)
{
    idle_ = idle;
    updateIdle();
}

void
HippoBubble::updateIdle()
{
    bool effectiveIdle = idle_ || haveMouse_;
    if (effectiveIdle_ != effectiveIdle) {
        effectiveIdle_ = effectiveIdle;
        doSetIdle();
    }
}

void
HippoBubble::doSetIdle()
{
    if (window_) {
        variant_t idleVariant(effectiveIdle_);
        variant_t result;
    
        ie_->invokeJavascript(L"dhSetIdle", &result, 1, &idleVariant);
    }
}

bool
HippoBubble::registerClass()
{
    WNDCLASSEX wcex;

    ZeroMemory(&wcex, sizeof(WNDCLASSEX));
    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = windowProc;
    wcex.cbClsExtra = 0;
    wcex.cbWndExtra = 0;
    wcex.hInstance  = instance_;
    wcex.hCursor    = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = CLASS_NAME;

    if (RegisterClassEx(&wcex) == 0) {
        if (GetClassInfoEx(instance_, CLASS_NAME, &wcex) != 0)
            return true;
        return false;
    }
    return true;
}

void
HippoBubble::show(void) 
{   
    ui_->debugLogW(L"doing bubble show");
    //if (!AnimateWindow(window_, 400, AW_BLEND))
    //  ui_->logLastError(L"Failed to invoke AnimateWindow");
    if (!ShowWindow(window_, SW_SHOW))
        ui_->logLastError(L"Failed to invoke ShowWindow");
    if (!RedrawWindow(window_, NULL, NULL, RDW_UPDATENOW))
        ui_->logLastError(L"Failed to invoke RedrawWindow");
    if (!BringWindowToTop(window_))
        ui_->logLastError(L"Failed to invoke BringWindowToTop");

    SetTimer(window_, CHECK_MOUSE, 250 /* 0.25 second */, NULL);
    checkMouse();
}

/* We want to detect when the mouse pointer is over the bubble; this
 * is used to suspend the handling of the paging timeout; if
 * the mouse is over the bubble we keep the bubble up indefinitely
 * to let the user interact with it. As far as I know there is no
 * way to detect enters and leaves that go directly to a subwindow
 * (virtual enters/leaves in X terminology), so we check by simply
 * installing a timeout when the bubble is up.
 */
void
HippoBubble::checkMouse()
{
    POINT pt;
    RECT rect;

    GetCursorPos(&pt);
    ScreenToClient(window_, &pt);
    GetClientRect(window_, &rect);

    if (pt.x > 0 && pt.x < rect.right &&
        pt.y > 0 && pt.y < rect.bottom) 
    {
        if (!haveMouse_) {
            haveMouse_ = TRUE;
            updateIdle();
        }
    } 
    else
    {
        if (haveMouse_) {
            haveMouse_ = FALSE;
            updateIdle();
        }
    }
}

bool
HippoBubble::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    switch (message) 
    {
    case WM_TIMER:
        if (wParam == CHECK_MOUSE) {
            checkMouse();
            return true;
        } else {
            return false;
        }
    case WM_CLOSE:
        Close();
        return true;
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoBubble::windowProc(HWND   window,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    HippoBubble *bubbleWindow = hippoGetWindowData<HippoBubble>(window);
    if (bubbleWindow) {
        if (bubbleWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

// IHippoBubble

STDMETHODIMP
HippoBubble::DebugLog(BSTR str)
{
    ui_->debugLogW(L"%s", str);
    return S_OK;
}

STDMETHODIMP 
HippoBubble::DisplaySharedLink(BSTR linkId)
{
    ui_->displaySharedLink(linkId);
    return S_OK;
}

STDMETHODIMP
HippoBubble::OpenExternalURL(BSTR url)
{
    HippoPtr<IWebBrowser2> browser;
    ui_->launchBrowser(url, browser);
    return S_OK;
}

STDMETHODIMP
HippoBubble::Close()
{
    KillTimer(window_, CHECK_MOUSE);
    if (haveMouse_) {
        haveMouse_ = FALSE;
        updateIdle();
    }

    AnimateWindow(window_, 200, AW_BLEND | AW_HIDE);
    ui_->debugLogU("closing link notification");
    return S_OK;
}

STDMETHODIMP 
HippoBubble::SetViewerSpace(DWORD viewerSpace)
{
    if (viewerSpace != viewerSpace_) {
        viewerSpace_ = viewerSpace;
        if (window_)
            moveResizeWindow();
    }

    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetXmlHttp(IXMLHttpRequest **request)
{
    CoCreateInstance(CLSID_XMLHTTPRequest, NULL, CLSCTX_INPROC,
        IID_IXMLHTTPRequest, (void**) request);
    return S_OK;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoBubble::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoBubble*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoBubble)) 
        *result = static_cast<IHippoBubble *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoBubble)


//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

    STDMETHODIMP
HippoBubble::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetTypeInfo(UINT        iTInfo,
                         LCID        lcid,
                         ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoBubble::GetIDsOfNames (REFIID    riid,
                            LPOLESTR *rgszNames,
                            UINT      cNames,
                            LCID      lcid,
                            DISPID   *rgDispId)
{
    HRESULT ret;
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    
    ret = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
    return ret;
}
        
STDMETHODIMP
HippoBubble::Invoke (DISPID        member,
                     const IID    &iid,
                     LCID          lcid,              
                     WORD          flags,
                     DISPPARAMS   *dispParams,
                     VARIANT      *result,
                     EXCEPINFO    *excepInfo,  
                     unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    HippoQIPtr<IHippoBubble> hippoBubble(static_cast<IHippoBubble *>(this));
    HRESULT hr = DispInvoke(hippoBubble, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
