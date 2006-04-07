/* HippoBubbleList.cpp: Window with a list of notification bubbles
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx.h"

#include <stdarg.h>
#include <HippoUtil.h>
#include "HippoBubbleList.h"
#include "HippoUI.h"

// These values basically don't matter, since the javascript code drives the size
static const int BASE_WIDTH = 400;
static const int BASE_HEIGHT = 150;

HippoBubbleList::HippoBubbleList(void)
{
    refCount_ = 1;
    desiredWidth_ = BASE_WIDTH;
    desiredHeight_ = BASE_HEIGHT;

    hippoLoadTypeInfo((WCHAR *)0, &IID_IHippoBubbleList, &ifaceTypeInfo_, NULL);

    setClassName(L"HippoBubbleListClass");
    setTitle(L"Recent Links");
    setWindowStyle(WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX);
    setApplication(this);
}

HippoBubbleList::~HippoBubbleList(void)
{
}

void
HippoBubbleList::moveResizeWindow() 
{
    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    RECT r = { 0, 0, desiredWidth_, desiredHeight_ };
    AdjustWindowRectEx(&r, windowStyle_, FALSE, extendedStyle_);
    int width = r.right - r.left;
    int height = r.bottom - r.top;

    moveResize(desktopRect.right - width, 
               (desktopRect.top + desktopRect.bottom - height) / 2,
               width, height);
}

HippoBSTR
HippoBubbleList::getURL()
{
    HippoBSTR srcURL;

    ui_->getAppletURL(L"bubbleList.xml", &srcURL);

    return srcURL;
}

void
HippoBubbleList::initializeWindow()
{
    moveResizeWindow();
}

void 
HippoBubbleList::initializeIE()
{
    HippoBSTR appletURL;
    ui_->getAppletURL(L"", &appletURL);
    HippoBSTR styleURL;
    ui_->getAppletURL(L"clientstyle.xml", &styleURL);
    ie_->setXsltTransform(styleURL, L"appleturl", appletURL.m_str, NULL);
}

void 
HippoBubbleList::initializeBrowser()
{
    // Kind of a hack
    HippoBSTR serverURL;
    ui_->getRemoteURL(HippoBSTR(L""), &serverURL);
    HippoBSTR appletURL;
    ui_->getAppletURL(HippoBSTR(L""), &appletURL);
    HippoBSTR selfID;
    ui_->GetLoginId(&selfID);

    // Find the maximum vertical size that we can make the window's contents
    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    RECT r = { 0, 0, 100, 100 };
    AdjustWindowRectEx(&r, windowStyle_, FALSE, extendedStyle_);
    int maxVerticalSize = (desktopRect.bottom - desktopRect.top) - (r.bottom - r.top) + 100;

    ui_->debugLogU("Invoking dhInit");
    ie_->createInvocation(L"dhInit")
        .add(serverURL)
        .add(appletURL)
        .add(selfID)
        .addLong(maxVerticalSize)
        .run();
}
    
void 
HippoBubbleList::onClose(bool fromScript)
{
    hide();
}

void 
HippoBubbleList::addLinkShare(HippoPost *share)
{
    if (!create())
        return;

    variant_t result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    // Note if you change the arguments to this function, you must change bubbleList.js
    ie_->createInvocation(L"dhAddLinkShare")
        .addDispatch(share)
        .getResult(&result);
}

void
HippoBubbleList::addMySpaceCommentNotification(long myId, long blogId, const HippoMySpaceBlogComment &comment)
{
    if (!create())
        return;

    ui_->debugLogW(L"Invoking dhAddMySpaceComment");

    // Note if you change the arguments to this function, you must change bubbleList.js
    ie_->createInvocation(L"dhAddMySpaceComment")
        .addLong(myId)
        .addLong(blogId)
        .addLong(comment.commentId)
        .addLong(comment.posterId)
        .add(comment.posterName)
        .add(comment.posterImgUrl)
        .add(comment.content)
        .run();
}

void 
HippoBubbleList::updatePost(HippoPost *post)
{
    if (!ie_)
        return;

    ie_->createInvocation(L"dhUpdatePost")
        .addDispatch(post)
        .run();
}

void 
HippoBubbleList::clear()
{
    if (!create())
        return;

    ui_->debugLogW(L"Invoking dhBubbleListClear");

    // Note if you change the arguments to this function, you must change bubbleList.js
    ie_->createInvocation(L"dhBubbleListClear").run();
}

// IHippoBubbleList

STDMETHODIMP 
HippoBubbleList::DisplaySharedLink(BSTR linkId, BSTR url)
{
    ui_->displaySharedLink(linkId, url);
    return S_OK;
}

STDMETHODIMP
HippoBubbleList::GetServerBaseUrl(BSTR *ret)
{
    HippoBSTR temp;
    ui_->getRemoteURL(L"", &temp);

    temp.CopyTo(ret);
    return S_OK;
}

STDMETHODIMP 
HippoBubbleList::Resize(int width, int height)
{
    if (width != desiredWidth_ || height != desiredHeight_) {
        desiredWidth_ = width;
        desiredHeight_ = height;
        if (window_)
            moveResizeWindow();
    }

    return S_OK;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoBubbleList::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoBubbleList*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoBubbleList)) 
        *result = static_cast<IHippoBubbleList *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoBubbleList)


//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoBubbleList::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoBubbleList::GetTypeInfo(UINT        iTInfo,
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
HippoBubbleList::GetIDsOfNames (REFIID    riid,
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
HippoBubbleList::Invoke (DISPID        member,
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
    HippoQIPtr<IHippoBubbleList> hippoBubbleList(static_cast<IHippoBubbleList *>(this));
    HRESULT hr = DispInvoke(hippoBubbleList, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
