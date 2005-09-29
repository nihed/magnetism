/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoExplorerBar.h"
#include "Guid.h"
#include "Globals.h"
#include "Resource.h"
#include <strsafe.h>

static const int MIN_HEIGHT = 10;
static const int DEFAULT_HEIGHT = 50; // The registry value will be used instead, I think
static const WCHAR *TITLE = L"Hippo Explorer Bar";
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

	if (!createWindow(parentWindow)) {
	    site_ = NULL;
	    return E_FAIL;
	}
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
	deskBandInfo->ptMaxSize.y = (LONG)-1;
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
    windowClass.hIcon = (HICON)LoadImage(dllInstance, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	                                 IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    windowClass.hCursor = LoadCursor(NULL, IDC_ARROW);
    windowClass.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    windowClass.lpszMenuName = NULL;
    windowClass.lpszClassName = CLASS_NAME;    

    return RegisterClass(&windowClass) != 0;
}

bool
HippoExplorerBar::processMessage(UINT   message,
				 WPARAM wParam,
				 LPARAM lParam)
{
    switch (message) {
	case WM_SETFOCUS:
	    setHasFocus(true);
	    return true;
	case WM_KILLFOCUS:
	    setHasFocus(false);
	    return true;
	case WM_PAINT:
	    onPaint();
	    return true;
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
    HippoExplorerBar *explorerBar = hippoGetWindowData<HippoExplorerBar>(window);
    if (explorerBar) {
	if (explorerBar->processMessage(message, wParam, lParam))
	    return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}