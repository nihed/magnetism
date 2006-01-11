/* HippoChatRoom.cpp: Window displaying a chatroom for a post
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include "HippoUI.h"
#include "HippoIE.h"
#include <HippoUtil.h>
#include "HippoChatRoom.h"
#include "Guid.h"

static const TCHAR *CLASS_NAME = TEXT("HippoChatRoomClass");
static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoChatRoom::HippoChatRoom(void)
{
    refCount_ = 1;
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    ie_ = NULL;

    ieCallback_ = new HippoChatRoomIECallback(this);

    hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoChatRoom, &ifaceTypeInfo_, NULL);
}

HippoChatRoom::~HippoChatRoom(void)
{
    delete ieCallback_;
}

void 
HippoChatRoom::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoChatRoom::createWindow(void)
{
    window_ = CreateWindow(CLASS_NAME, L"Hippo Chat", WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                           NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoChatRoom>(window_, this);

    return true;
}

void HippoChatRoom::HippoChatRoomIECallback::onDocumentComplete()
{
    chatRoom_->ui_->debugLogW(L"HippoChatRoom document complete");
}

void
HippoChatRoom::HippoChatRoomIECallback::onError(WCHAR *text) 
{
    chatRoom_->ui_->debugLogW(L"HippoIE error: %s", text);
}

bool
HippoChatRoom::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    HippoBSTR srcURL;
    ui_->getAppletURL(L"chatroom.xml", &srcURL);
    ie_ = new HippoIE(window_, srcURL, ieCallback_, this);
    ie_->setThreeDBorder(false);

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
    HippoBSTR selfIdStr;
    ui_->getLoginId(&selfIdStr);
    variant_t serverUrl(serverURLStr.m_str);
    variant_t appletUrl(appletURLStr.m_str);
    variant_t selfId(selfIdStr.m_str);
    variant_t result;
    ie_->invokeJavascript(L"dhInit", &result, 3, &serverUrl, &appletUrl, &selfId);

    return true;
}

bool
HippoChatRoom::create(void)
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

bool
HippoChatRoom::invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...)
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

bool
HippoChatRoom::registerClass()
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
HippoChatRoom::show(void) 
{   
    ui_->debugLogW(L"doing chatRoom show");
    if (!ShowWindow(window_, SW_SHOW))
        ui_->logLastError(L"Failed to invoke ShowWindow");
    if (!RedrawWindow(window_, NULL, NULL, RDW_UPDATENOW))
        ui_->logLastError(L"Failed to invoke RedrawWindow");
    if (!BringWindowToTop(window_))
        ui_->logLastError(L"Failed to invoke BringWindowToTop");
}

void
HippoChatRoom::close()
{
    ShowWindow(window_, SW_HIDE);
}

bool
HippoChatRoom::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    switch (message) 
    {
    case WM_CLOSE:
        close();
        return true;
    case WM_SIZE:
        {
            RECT rect = { 0, 0, LOWORD(lParam), HIWORD(lParam) };
            hippoDebugLogW(L"Now CLIENT is %d %d", rect.right, rect.bottom);
            ie_->resize(&rect);
            return true;
        }
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoChatRoom::windowProc(HWND   window,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    HippoChatRoom *chatRoomWindow = hippoGetWindowData<HippoChatRoom>(window);
    if (chatRoomWindow) {
        if (chatRoomWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

// IHippoChatRoom

STDMETHODIMP 
HippoChatRoom::SendMessage(BSTR message)
{
    return S_OK;
}

STDMETHODIMP
HippoChatRoom::OpenExternalURL(BSTR url)
{
    ui_->launchBrowser(url);
    return S_OK;
}

HRESULT
HippoChatRoom::GetServerBaseUrl(BSTR *ret)
{
    HippoBSTR temp;
    ui_->getRemoteURL(L"", &temp);
    *ret = ::SysAllocString(temp.m_str);
    return S_OK;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoChatRoom::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoChatRoom*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoChatRoom)) 
        *result = static_cast<IHippoChatRoom *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoChatRoom)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoChatRoom::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoChatRoom::GetTypeInfo(UINT        iTInfo,
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
HippoChatRoom::GetIDsOfNames (REFIID    riid,
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
HippoChatRoom::Invoke (DISPID        member,
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
    HippoQIPtr<IHippoChatRoom> hippoChatRoom(static_cast<IHippoChatRoom *>(this));
    HRESULT hr = DispInvoke(hippoChatRoom, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
