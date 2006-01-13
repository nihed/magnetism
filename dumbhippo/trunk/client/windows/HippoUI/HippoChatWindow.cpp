/* HippoChatWindow.cpp: Window displaying a chat room for a post
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
#include "HippoChatWindow.h"
#include "Guid.h"

static const TCHAR *CLASS_NAME = TEXT("HippoChatWindowClass");
static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoChatWindow::HippoChatWindow(void)
{
    refCount_ = 1;
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    ie_ = NULL;
    chatRoom_ = NULL;

    ieCallback_ = new HippoChatWindowIECallback(this);

    hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoChatWindow, &ifaceTypeInfo_, NULL);
}

HippoChatWindow::~HippoChatWindow(void)
{
    setChatRoom(NULL);

    DestroyWindow(window_);

    delete ieCallback_;
}

void 
HippoChatWindow::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoChatWindow::createWindow(void)
{
    window_ = CreateWindow(CLASS_NAME, L"Hippo Chat", WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                           NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoChatWindow>(window_, this);

    return true;
}

void HippoChatWindow::HippoChatWindowIECallback::onDocumentComplete()
{
    chatWindow_->ui_->debugLogW(L"HippoChatWindow document complete");
}

void
HippoChatWindow::HippoChatWindowIECallback::onError(WCHAR *text) 
{
    chatWindow_->ui_->debugLogW(L"HippoIE error: %s", text);
}

bool
HippoChatWindow::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    HippoBSTR srcURL;
    ui_->getAppletURL(L"chatwindow.xml", &srcURL);
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
    variant_t serverUrl(serverURLStr.m_str);
    variant_t appletUrl(appletURLStr.m_str);
    variant_t result;
    ie_->invokeJavascript(L"dhInit", &result, 2, &serverUrl, &appletUrl);

    return true;
}

bool
HippoChatWindow::create(void)
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
HippoChatWindow::invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...)
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
HippoChatWindow::registerClass()
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
HippoChatWindow::show(void) 
{   
    ui_->debugLogW(L"doing ChatWindow show");
    if (!ShowWindow(window_, SW_SHOW))
        ui_->logLastError(L"Failed to invoke ShowWindow");
    if (!RedrawWindow(window_, NULL, NULL, RDW_UPDATENOW))
        ui_->logLastError(L"Failed to invoke RedrawWindow");
    if (!BringWindowToTop(window_))
        ui_->logLastError(L"Failed to invoke BringWindowToTop");
}

void 
HippoChatWindow::setForegroundWindow()
{
    if (window_)
        SetForegroundWindow(window_);
}

void 
HippoChatWindow::setChatRoom(HippoChatRoom *chatRoom)
{
    if (chatRoom_)
        chatRoom_->removeListener(this);
     
    chatRoom_ = chatRoom;

    if (chatRoom_)
        chatRoom_->addListener(this);
}

HippoChatRoom *
HippoChatWindow::getChatRoom()
{
    return chatRoom_;
}

bool
HippoChatWindow::processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    switch (message) 
    {
    case WM_CLOSE:
        ui_->onChatWindowClosed(this);
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
HippoChatWindow::windowProc(HWND   window,
                            UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    HippoChatWindow *chatWindow = hippoGetWindowData<HippoChatWindow>(window);
    if (chatWindow) {
        if (chatWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

// IHippoChatWindow

STDMETHODIMP 
HippoChatWindow::SendMessage(BSTR message)
{
    chatRoom_->sendMessage(message);
    return S_OK;
}

STDMETHODIMP
HippoChatWindow::OpenExternalURL(BSTR url)
{
    ui_->launchBrowser(url);
    return S_OK;
}

HRESULT
HippoChatWindow::GetServerBaseUrl(BSTR *ret)
{
    HippoBSTR temp;
    ui_->getRemoteURL(L"", &temp);

    return temp.CopyTo(ret);
}

HRESULT
HippoChatWindow::GetSelfId(BSTR *ret)
{
    HippoBSTR temp;
    ui_->GetLoginId(&temp);

    return temp.CopyTo(ret);
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoChatWindow::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoChatWindow*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoChatWindow)) 
        *result = static_cast<IHippoChatWindow *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoChatWindow)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoChatWindow::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoChatWindow::GetTypeInfo(UINT        iTInfo,
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
HippoChatWindow::GetIDsOfNames (REFIID    riid,
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
HippoChatWindow::Invoke (DISPID        member,
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
    HippoQIPtr<IHippoChatWindow> chatWindow(static_cast<IHippoChatWindow *>(this));
    HRESULT hr = DispInvoke(chatWindow, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}

////////////////////////////// HippoChatRoomListener implementatin //////////////////////////////////

void 
HippoChatWindow::onUserJoin(HippoChatRoom *chatRoom, const HippoChatUser &user)
{
    variant_t userId(user.getUserId());
    variant_t version(user.getVersion);
    variant_t name(user.getName());
    variant_t result;

    ie_->invokeJavascript(L"dhChatAddPerson", &result, 3, &userId, &version, &name);
}

void 
HippoChatWindow::onUserLeave(HippoChatRoom *chatRoom, const HippoChatUser &user)
{
    variant_t userId(user.getUserId());
    variant_t result;

    ie_->invokeJavascript(L"dhChatRemovePerson", &result, 1, &userId);
}

void 
HippoChatWindow::onMessage(HippoChatRoom *chatRoom, const HippoChatMessage &message)
{
    const HippoChatUser &user = message.getUser();

    variant_t userId(user.getUserId());
    variant_t version(user.getVersion());
    variant_t name(user.getName());
    variant_t text(message.getText());
    variant_t result;

    ie_->invokeJavascript(L"dhChatAddMessage", &result, 4, &userId, &version, &name, &text);
}

void 
HippoChatWindow::onClear(HippoChatRoom *chatRoom)
{
    variant_t result;

    ie_->invokeJavascript(L"dhChatClear", &result, 0);
}
