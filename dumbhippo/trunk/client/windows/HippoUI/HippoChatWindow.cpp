/* HippoChatWindow.cpp: Window displaying a chat room for a post
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include "HippoChatWindow.h"
#include "HippoUI.h"

static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

HippoChatWindow::HippoChatWindow(void)
{
    refCount_ = 1;
    hippoLoadTypeInfo((WCHAR *)0, &IID_IHippoChatWindow, &ifaceTypeInfo_, NULL);

    setClassName(L"HippoChatWindowClass");
    setTitle(L"Mugshot Chat");
    setApplication(this);
}

HippoChatWindow::~HippoChatWindow(void)
{
}

void 
HippoChatWindow::setChatId(BSTR chatId)
{
    chatId_ = chatId;

    HippoBSTR srcURL;
    ui_->getRemoteURL(L"chatwindow?chatId=", &srcURL);
    srcURL.Append(chatId_);

    setURL(srcURL);
}

BSTR
HippoChatWindow::getChatId()
{
    return chatId_.m_str;
}

void
HippoChatWindow::onClose(bool fromScript)
{
    ui_->onChatWindowClosed(this);
}

// IHippoChatWindow

STDMETHODIMP
HippoChatWindow::DemandAttention()
{
    FLASHWINFO flashInfo;
    flashInfo.cbSize = sizeof(flashInfo);
    flashInfo.hwnd = window_;
    flashInfo.dwTimeout = 0;
    flashInfo.dwFlags = FLASHW_TIMERNOFG | FLASHW_TRAY;
    flashInfo.uCount = 5;
    FlashWindowEx(&flashInfo);
    return S_OK;
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
    HippoQIPtr<IHippoChatWindow> hippoChatWindow(static_cast<IHippoChatWindow *>(this));
    HRESULT hr = DispInvoke(hippoChatWindow, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
