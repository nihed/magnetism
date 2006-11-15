/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx-hippoexplorer.h"
#include <HippoURLParser.h>
#include "HippoExplorerBar.h"
#include "HippoExplorerUtil.h"
#include <HippoInvocation.h>
#include "Guid.h"
#include "Globals.h"
#include "Resource.h"
#include <ExDispid.h>
#include <strsafe.h>

static const int MIN_HEIGHT = 125;
static const int MAX_HEIGHT = 125; // -1 is no max
static const int DEFAULT_HEIGHT = 125;
static const WCHAR *TITLE = L"Mugshot";
static const TCHAR *CLASS_NAME = TEXT("HippoExplorerBarClass");

HippoExplorerBar::HippoExplorerBar()
{
    hasFocus_ = false;
    window_ = 0;

    refCount_ = 1;
    dllRefCount++;
}

HippoExplorerBar::~HippoExplorerBar()
{
    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoExplorerBar::QueryInterface(const IID &ifaceID, 
                                 void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IDeskBand *>(this));
    else if (IsEqualIID(ifaceID, IID_IOleWindow)) 
        *result = static_cast<IOleWindow *>(this);
    else if (IsEqualIID(ifaceID, IID_IDockingWindow)) 
        *result = static_cast<IDockingWindow *>(this);
    else if (IsEqualIID(ifaceID, IID_IDeskBand))
        *result = static_cast<IDeskBand *>(this);
    else if (IsEqualIID(ifaceID, IID_IInputObject))
        *result = static_cast<IInputObject *>(this);
    else if (IsEqualIID(ifaceID, IID_IObjectWithSite))
        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IPersistStream))
        *result = static_cast<IPersistStream *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch))
        *result = static_cast<IDispatch *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}

HIPPO_DEFINE_REFCOUNTING(HippoExplorerBar)

/////////////////////// IOleWindow implementation /////////////////////

STDMETHODIMP 
HippoExplorerBar::GetWindow(HWND *pWindow)
{
    if (!pWindow)
        return E_INVALIDARG;
    
    *pWindow = window_;

    return S_OK;
}

STDMETHODIMP 
HippoExplorerBar::ContextSensitiveHelp(BOOL enterMode)
{
    return S_OK;
}

////////////////////// IDockingWindow Implementation ////////////////

STDMETHODIMP 
HippoExplorerBar::ShowDW(BOOL show)
{
    if (!window_)
        return S_OK;

    ShowWindow(window_, show ? SW_SHOW : SW_HIDE);

    return S_OK;
}

STDMETHODIMP 
HippoExplorerBar::CloseDW(DWORD reserved)
{
    if (!window_)
        return S_OK;

    DestroyWindow(window_);
    window_ = NULL;

    ie_->shutdown();
    ie_ = NULL;

    return S_OK;
}

STDMETHODIMP 
HippoExplorerBar::ResizeBorderDW(const RECT *border, 
                                 IUnknown   *site, 
                                 BOOL        reserved)
{
    // Will never be called for an explorer bar
    return E_NOTIMPL;
}

/////////////////////// IInputObject Implementation ////////////////////////

STDMETHODIMP
HippoExplorerBar::UIActivateIO(BOOL  activate, 
                               MSG  *message)
{
    if (!window_)
        return E_FAIL;

    if (!SetFocus(window_))
        return E_FAIL;

    return S_OK;
}

STDMETHODIMP
HippoExplorerBar::HasFocusIO(void)
{
    return hasFocus_ ? S_OK : S_FALSE;
}

STDMETHODIMP
HippoExplorerBar::TranslateAcceleratorIO(LPMSG pMsg)
{
    // No accelerators
    return S_FALSE;
}

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoExplorerBar::SetSite(IUnknown *site)
{
    site_ = NULL;
    
    if (site) 
    {
        // Get the window from the parent
        HippoQIPtr<IOleWindow> oleWindow(site);
        if (!oleWindow)
            return E_FAIL;

        HWND parentWindow = NULL;
        HRESULT hr = oleWindow->GetWindow(&parentWindow);
        if (FAILED (hr))
            return hr;
         if (!parentWindow)
            return E_FAIL;

        if (FAILED(site->QueryInterface<IInputObjectSite>(&site_)))
            return E_FAIL;

        HippoQIPtr<IServiceProvider> serviceProvider = site_;
        if (serviceProvider) 
            serviceProvider->QueryService<IWebBrowser2>(SID_SWebBrowserApp, &browser_);

        if (browser_) {
            HippoQIPtr<IConnectionPointContainer> container(browser_);
            if (container)
            {
                if (SUCCEEDED(container->FindConnectionPoint(DIID_DWebBrowserEvents2,
                                                            &connectionPoint_))) 
                {
                    // The COM-safe downcast here is a little overkill ... 
                    // we actually just need to disambiguate
                    HippoQIPtr<IUnknown> unknown(static_cast<IDispatch *>(this));
                    connectionPoint_->Advise(unknown, &connectionCookie_);
                }
            }
        }

        if (!createWindow(parentWindow)) {
            site_ = NULL;
            return E_FAIL;
        }

        createIE();
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoExplorerBar::GetSite(const IID &iid, 
                          void     **result)
{
    if (!site_) {
        *result = NULL;
        return E_FAIL;
    }

    return site_->QueryInterface(iid, result);
}

///////////////////// IDeskBand implementation ////////////////////////

STDMETHODIMP
HippoExplorerBar::GetBandInfo(DWORD          bandID, 
                               DWORD         viewMode, 
                               DESKBANDINFO *deskBandInfo)
{
    if (deskBandInfo->dwMask & DBIM_MINSIZE) {
        deskBandInfo->ptMinSize.x = 0;
        deskBandInfo->ptMinSize.y = MIN_HEIGHT;
    }

    if (deskBandInfo->dwMask & DBIM_MAXSIZE) {
        deskBandInfo->ptMaxSize.x = (LONG)-1;
        deskBandInfo->ptMaxSize.y = MAX_HEIGHT;
    }

    if (deskBandInfo->dwMask & DBIM_INTEGRAL) {
        deskBandInfo->ptIntegral.x = 1;
        deskBandInfo->ptIntegral.y = 1;
    }

    if (deskBandInfo->dwMask & DBIM_ACTUAL) {
        deskBandInfo->ptActual.x = 0;              // Not clear what to use here
         deskBandInfo->ptActual.y = DEFAULT_HEIGHT;
    }

    if (deskBandInfo->dwMask & DBIM_TITLE) {
        StringCchCopyW(deskBandInfo->wszTitle, sizeof(deskBandInfo->wszTitle) / sizeof(WCHAR), TITLE);
    }

    if (deskBandInfo->dwMask & DBIM_MODEFLAGS) {
        deskBandInfo->dwModeFlags = DBIMF_VARIABLEHEIGHT | DBIMF_BKCOLOR;
    }

    if (deskBandInfo->dwMask & DBIM_BKCOLOR) {
        deskBandInfo->crBkgnd = RGB(255, 255, 255); // White. Does this matter, since we create a window?
    }

    return S_OK;
}

//////////////////// IPersistStream implementation ////////////////////////////////

/* It's somewhat unclear whether we need to implement this and what it means. 
 * The available docs seem to indicate that returning the class ID is important to
 * prevent duplicate entries, even if we don't implement any of the rest of it.
 */

STDMETHODIMP 
HippoExplorerBar::GetClassID(CLSID *classID)
{
    if (!classID)
        return E_INVALIDARG;

    *classID = CLSID_HippoExplorerBar;

    return S_OK;
}

STDMETHODIMP 
HippoExplorerBar::IsDirty()
{
    return S_FALSE;
}

STDMETHODIMP 
HippoExplorerBar::Load(IStream *stream)
{
    return S_OK;
}

STDMETHODIMP
HippoExplorerBar::Save(IStream *stream, 
                       BOOL     clearDirty)
{
    return S_OK;
}

STDMETHODIMP
HippoExplorerBar::GetSizeMax(ULARGE_INTEGER *pul)
{
    return 0;
}

//////////////////// IDispatch implementation ////////////////////////

STDMETHODIMP
HippoExplorerBar::GetIDsOfNames (const IID   &iid,
                                 OLECHAR    **names,  
                                 unsigned int cNames,          
                                 LCID         lcid,                   
                                 DISPID *     dispID)
{
    return DISP_E_UNKNOWNNAME;
}

STDMETHODIMP
HippoExplorerBar::GetTypeInfo (unsigned int infoIndex,  
                               LCID         lcid,                  
                               ITypeInfo  **ppTInfo)
{
   if (ppTInfo == NULL)
      return E_INVALIDARG;

    return DISP_E_BADINDEX;
}

 STDMETHODIMP 
 HippoExplorerBar::GetTypeInfoCount (unsigned int *pcTInfo)
 {
    if (pcTInfo == NULL)
      return E_INVALIDARG;

    *pcTInfo = 0;

    return S_OK;
 }
  
 STDMETHODIMP
 HippoExplorerBar::Invoke (DISPID        member,
                           const IID    &iid,
                           LCID          lcid,              
                           WORD          flags,
                           DISPPARAMS   *dispParams,
                           VARIANT      *result,
                           EXCEPINFO    *excepInfo,  
                           unsigned int *argErr)
 {
      /* See note in HippoTracker::Invoke() for why we listen to 
       * both DocumentComplete and TitleChange
       */
      switch (member) {
        case DISPID_DOCUMENTCOMPLETE:
             if (dispParams->cArgs == 2 &&
                 dispParams->rgvarg[1].vt == VT_DISPATCH &&
                 dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT))
             {
                 checkPageChange();

                 return S_OK;
             } else {
                 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
             }
             break;
        case DISPID_TITLECHANGE:
             if (dispParams->cArgs == 1 &&
                 dispParams->rgvarg[0].vt == VT_BSTR)
             {
                 checkPageChange();

                 return S_OK;
             } else {
                 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
             }
             break;
         default:
             return DISP_E_MEMBERNOTFOUND; // Or S_OK
     }
}

//////////////////// HippoIECallback implementation ////////////////////////

void 
HippoExplorerBar::onClose()
{
}

void 
HippoExplorerBar::onDocumentComplete()
{
}

void 
HippoExplorerBar::launchBrowser(const HippoBSTR &url)
{
    if (browser_) {
        variant_t flags;
        variant_t targetFrameName(L"_self");
        variant_t postData;
        variant_t headers;
        browser_->Navigate(url.m_str, &flags, &targetFrameName, &postData, &headers);
    }
}

bool 
HippoExplorerBar::isOurServer(const HippoBSTR &host)
{
    return hippoIsOurServer(host);
}

HRESULT 
HippoExplorerBar::getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser)
{
    HippoQIPtr<IServiceProvider> serviceProvider = site_;
    if (!serviceProvider)
        return E_UNEXPECTED;

    return serviceProvider->QueryService(SID_STopLevelBrowser, ifaceID, toplevelBrowser);
}

///////////////////////////////////////////////////////////////////////////

bool
HippoExplorerBar::createWindow(HWND parentWindow)
{
    RECT parentRect;

    if (!GetClientRect(parentWindow, &parentRect))
        return false;

    if (!registerWindowClass())
        return false;

    window_ = CreateWindow(CLASS_NAME, 
                           NULL, // No title
                           WS_CHILD | WS_CLIPSIBLINGS,
                           0,                0,
                           parentRect.right, parentRect.bottom,
                           parentWindow,
                           NULL, // No menu
                           dllInstance,
                           NULL); // lpParam
    if (!window_)
        return false;

    hippoSetWindowData<HippoExplorerBar>(window_, this);

    return true;
}

bool 
HippoExplorerBar::registerWindowClass()
{
    WNDCLASS windowClass;

    if (GetClassInfo(dllInstance, CLASS_NAME, &windowClass))
        return true;  // Already registered

    windowClass.style = CS_HREDRAW | CS_VREDRAW;
    windowClass.lpfnWndProc = windowProc;
    windowClass.cbClsExtra = 0;
    windowClass.cbWndExtra = 0;
    windowClass.hInstance = dllInstance;
    windowClass.hIcon = 0;
    windowClass.hCursor = LoadCursor(NULL, IDC_ARROW);
    windowClass.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    windowClass.lpszMenuName = NULL;
    windowClass.lpszClassName = CLASS_NAME;    

    return RegisterClass(&windowClass) != 0;
}

bool
HippoExplorerBar::createIE() 
{
    if (!browser_)
        return false;

    if (FAILED(browser_->get_LocationURL(&currentUrl_)))
        return false;

    HippoBSTR framerUrl = getFramerUrl(currentUrl_);
    if (!framerUrl)
        framerUrl = L"about:blank";

    ie_ = HippoIE::create(window_, framerUrl.m_str, this, NULL);
    ie_->Release();
    ie_->embedBrowser();

    return true;
}

HippoBSTR 
HippoExplorerBar::getFramerUrl(const HippoBSTR &pageUrl)
{
    // We look at the toplevel URL of the page (it should be /visit?post=<postId>)
    // to figure out what URL we want to navigate to

    HippoURLParser parser(pageUrl);
    if (!parser.ok())
        return NULL;

    if (parser.getScheme() != INTERNET_SCHEME_HTTP)
        return NULL;
    
    if (!hippoIsOurServer(parser.getHostName()))
        return NULL;

    HippoBSTR urlPath = parser.getUrlPath();
    if (!urlPath || wcscmp(urlPath.m_str, L"/visit") != 0)
        return NULL;

    HippoBSTR extraInfo = parser.getExtraInfo();
    if (!extraInfo || wcsncmp(extraInfo.m_str, L"?post=", 6) != 0)
        return NULL;

    HippoBSTR postId(extraInfo.m_str + 6);
    if (!hippoVerifyGuid(postId))
        return NULL;

    HippoBSTR framerUrl(L"http://");
    framerUrl.Append(parser.getHostName());
    if (parser.getPort() != 80) {
        WCHAR buf[32];
        StringCchPrintf(buf, sizeof(buf) / sizeof(buf[0]), L":%d", parser.getPort());
        framerUrl.Append(buf);
    }
    framerUrl.Append(L"/framer?browserBar=true&postId=");
    framerUrl.Append(postId);

    return framerUrl;
}

void
HippoExplorerBar::checkPageChange()
{
    if (!browser_)
        return;
    
    HippoBSTR url;
    if (FAILED(browser_->get_LocationURL(&url)))
        return;

    if (!url)
        return;

    if (url == currentUrl_)
        return;

    currentUrl_ = url;

    HippoBSTR framerUrl = getFramerUrl(currentUrl_);
    if (framerUrl) {
        ie_->setLocation(framerUrl);
    }
}

bool
HippoExplorerBar::processMessage(UINT   message,
                                 WPARAM wParam,
                                 LPARAM lParam)
{
    switch (message) {
        case WM_SHOWWINDOW:
            if (!(BOOL)wParam && ie_) {
                // Notify the contents of the explorer bar that we've
                // been closed so that we leave the chatroom; the
                // page contents are kept around when the bar is closed,
                // so the normal handling when the controls are removed
                // doesn't work. We might want to do something on Show
                // if we wanted to handle the case when the user reopens the
                // bar manual from the menu, but probably we'd want to show
                // some content such as recent links in that case instead
                // of just rejoining the chatroom.
                HRESULT hr = ie_->createInvocation(L"dhBarClosed").run();
            }
            return false;
        case WM_SETFOCUS:
            setHasFocus(true);
            return true;
        case WM_KILLFOCUS:
            setHasFocus(false);
            return true;
        case WM_SIZE:
            if (ie_) {
                RECT rect = { 0, 0, LOWORD(lParam), HIWORD(lParam) };
                ie_->resize(&rect);
            }
        default:
            return false;
    }
}

void
HippoExplorerBar::setHasFocus(bool hasFocus)
{
    if (hasFocus != hasFocus_) {
        hasFocus_ = hasFocus;
    }

    site_->OnFocusChangeIS(static_cast<IInputObject *>(this), hasFocus);

    InvalidateRect(window_, NULL, FALSE);
}

void
HippoExplorerBar::onPaint()
{
    HDC dc;
    PAINTSTRUCT ps;

    dc = BeginPaint(window_, &ps);

    if (hasFocus_) 
        FillRect(dc, &ps.rcPaint, (HBRUSH)GetStockObject(BLACK_BRUSH));
    else
        FillRect(dc, &ps.rcPaint, (HBRUSH)GetStockObject(WHITE_BRUSH));
    
    EndPaint(window_, &ps);
}

LRESULT CALLBACK 
HippoExplorerBar::windowProc(HWND   window,
                             UINT   message,
                             WPARAM wParam,
                             LPARAM lParam)
{
    // Our only content is the IE browser, and it erases everything itself
    // on repaint, so we tell windows not to repaint by returning 1. This
    // prevents flicker on resize.
    if (message == WM_ERASEBKGND)
        return 1;

    HippoPtr<HippoExplorerBar> explorerBar = hippoGetWindowData<HippoExplorerBar>(window);
    if (explorerBar) {
        if (explorerBar->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}