/* HippoUI.cpp: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include "HippoUI.h"
#include <stdio.h>
#include <strsafe.h>
#include <exdisp.h>
#include <HippoUtil.h>
#include <HippoUtil_i.c>
#include "Resource.h"

// GUID definition
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include "Guid.h"
#pragma data_seg()

using namespace MSHTML;

static const int MAX_LOADSTRING = 100;
static const TCHAR *CLASS_NAME = TEXT("HippoUIClass");

HippoUI::HippoUI()
{
    refCount_ = 1;

    HippoPtr<ITypeLib> typeLib;
    HRESULT hr = LoadRegTypeLib(LIBID_HippoUtil, 
				0, 1, /* Version */
				0,    /* LCID */
				&typeLib);
    if (SUCCEEDED (hr))
	typeLib->GetTypeInfoOfGuid(IID_IHippoUI, &uiTypeInfo_);
    else
	hippoDebug(L"Failed to load type lib: %x\n", hr);

    notificationIcon_.setUI(this);
}


HippoUI::~HippoUI()
{
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoUI::QueryInterface(const IID &ifaceID, 
    			         void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
	*result = static_cast<IUnknown *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
	*result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoUI)) 
	*result = static_cast<IHippoUI *>(this);
    else {
	*result = NULL;
	return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}

HIPPO_DEFINE_REFCOUNTING(HippoUI)

////////////////////////// IDispatch implementation ///////////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoUI::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
	return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoUI::GetTypeInfo(UINT        iTInfo,
       		     LCID        lcid,
    		     ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
	return E_INVALIDARG;
    if (!uiTypeInfo_)
	return E_OUTOFMEMORY;
    if (iTInfo != 0)
	return DISP_E_BADINDEX;

    uiTypeInfo_->AddRef();
    *ppTInfo = uiTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoUI::GetIDsOfNames (REFIID    riid,
		        LPOLESTR *rgszNames,
			UINT      cNames,
			LCID	   lcid,
			DISPID   *rgDispId)
 {
     if (!uiTypeInfo_) 
	 return E_OUTOFMEMORY;
    
     return  DispGetIDsOfNames(uiTypeInfo_, rgszNames, cNames, rgDispId);
 }
        
STDMETHODIMP 
HippoUI::Invoke (DISPID      dispIdMember,
		 REFIID      riid,
		 LCID        lcid,
		 WORD        wFlags,
		 DISPPARAMS *pDispParams,
                 VARIANT    *pVarResult,
                 EXCEPINFO  *pExcepInfo,
                 UINT       *puArgErr)
{
    if (!uiTypeInfo_) 
	 return E_OUTOFMEMORY;

    HippoQIPtr<IHippoUI> hippoUI(this);
    return DispInvoke(hippoUI, uiTypeInfo_, dispIdMember, wFlags, 
	              pDispParams, pVarResult, pExcepInfo, puArgErr);
}

//////////////////////// IHippoTracker implementation //////////////////////

STDMETHODIMP 
HippoUI::Log(BSTR message)
{
    currentURL_ = message;
    notificationIcon_.showURL(message);
    return S_OK;
}

/////////////////////////////////////////////////////////////////////////////

bool
HippoUI::create(HINSTANCE instance)
{
    instance_ = instance;

    smallIcon_ = (HICON)LoadImage(instance_, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	                          IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    bigIcon_ = (HICON)LoadImage(instance_, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	                        IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);

    if (!registerClass())
	return false;

    if (!registerActive())
	return false;


    if (!createWindow()) {
	revokeActive();
	return false;
    }

    notificationIcon_.setIcon(smallIcon_);
    if (!notificationIcon_.create(window_)) {
	revokeActive();
	return false;
    }

    return true;
}

void
HippoUI::destroy()
{
    notificationIcon_.destroy();
    
    revokeActive();
}

// Show a window offering to share the given URL
void 
HippoUI::showShareWindow(BSTR url)
{
    HippoBSTR shareURL;
    
    if (!SUCCEEDED (getAppletURL(HippoBSTR(L"shareURL.htm"), &shareURL)))
	return;

    if (!SUCCEEDED (shareURL.Append(L"?url=")))
	return;

    if (!SUCCEEDED (shareURL.Append(url)))
	return;

    HippoPtr<IWebBrowser2> webBrowser;
    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
	             IID_IWebBrowser2, (void **)&webBrowser);

    if (!webBrowser)
	return;

    VARIANT missing;
    missing.vt = VT_NULL;

    webBrowser->Navigate(shareURL,
   		         &missing, &missing, &missing, &missing);
    webBrowser->put_AddressBar(VARIANT_FALSE);
    webBrowser->put_MenuBar(VARIANT_FALSE);
    webBrowser->put_StatusBar(VARIANT_FALSE);
    webBrowser->put_ToolBar(VARIANT_FALSE);
    webBrowser->put_Width(500);
    webBrowser->put_Height(500);

    RECT workArea;
    if (::SystemParametersInfo(SPI_GETWORKAREA, 0, &workArea, 0)) {
	webBrowser->put_Left((workArea.left + workArea.right - 500) / 2);
	webBrowser->put_Top((workArea.bottom + workArea.top - 500) / 2);
    }

    HippoPtr<IDispatch> dispDocument;	
    webBrowser->get_Document(&dispDocument);
    HippoQIPtr<IHTMLDocument2> document(dispDocument);

    if (document) {
	HippoPtr<IHTMLElement> bodyElement;
	document->get_body(&bodyElement);
	HippoQIPtr<IHTMLBodyElement> body(bodyElement);

	if (body)
	    body->put_scroll(HippoBSTR(L"no"));
    }

    webBrowser->put_Visible(VARIANT_TRUE);
}

// Show a window when the user clicks on a shared link
void 
HippoUI::showURL(BSTR url)
{
    HRESULT hr;

    HippoBSTR shareURL;

    HippoPtr<IWebBrowser2> webBrowser;
    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
	             IID_IWebBrowser2, (void **)&webBrowser);

    if (!webBrowser)
	return;

    VARIANT missing;
    missing.vt = VT_EMPTY;

    webBrowser->Navigate(url,
   		         &missing, &missing, &missing, &missing);

    /* Something like the following should activate a explorer bar,
     * (see Q255920) but doesn't seem to work.
     */
#if 0
    HippoBSTR barIDString(L"{A65AC703-C186-4e93-9022-AF8B92C726C8}");
    VARIANT barID;
    barID.vt = VT_BSTR;
    barID.bstrVal = barIDString;

    VARIANT show;
    barID.vt = VT_BOOL;
    barID.boolVal = VARIANT_TRUE;

    hr = webBrowser->ShowBrowserBar(&barID, &show, 0);
    if (!SUCCEEDED (hr)) 
	hippoDebug(L"Couldn't show browser bar: %X", hr);
#endif

    webBrowser->put_Visible(VARIANT_TRUE);
}

// Tries to register as the singleton HippoUI, returns true on success
bool 
HippoUI::registerActive()
{
    IHippoUI *pHippoUI;
 
    QueryInterface(IID_IHippoUI, (LPVOID *)&pHippoUI);
    HRESULT hr = RegisterActiveObject(pHippoUI, CLSID_HippoUI, ACTIVEOBJECT_WEAK, &registerHandle_);
    pHippoUI->Release();

    if (FAILED(hr)) {
        MessageBox(NULL, TEXT("Error registering Dumb Hippo"), NULL, MB_OK);
        return false;
    } else if (hr == MK_S_MONIKERALREADYREGISTERED) {
	// Duplicates are actually succesfully registered them, so have to be removed
	revokeActive();
	MessageBox(NULL, TEXT("Dumb Hippo is already running"), NULL, MB_OK);
	return false;
    }
    
    return true;
}

// Removes previous registration via registerActive()
void
HippoUI::revokeActive()
{
    RevokeActiveObject(registerHandle_, NULL);
}

bool
HippoUI::registerClass()
{
    WNDCLASSEX wcex;

    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style		= CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc	= windowProc;
    wcex.cbClsExtra	= 0;
    wcex.cbWndExtra	= 0;
    wcex.hInstance	= instance_;
    wcex.hIcon		= bigIcon_;
    wcex.hCursor	= LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground	= (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName	= NULL;
    wcex.lpszClassName	= CLASS_NAME;
    wcex.hIconSm	= smallIcon_;

    return RegisterClassEx(&wcex) != 0;
}

bool
HippoUI::createWindow(void)
{
    WCHAR title[MAX_LOADSTRING];
    LoadString(instance_, IDS_APP_TITLE, title, MAX_LOADSTRING);

    window_ = CreateWindow(CLASS_NAME, title, WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, NULL, NULL, instance_, NULL);
    
    if (!window_)
	return false;

    SetWindowLongPtr(window_, GWLP_USERDATA, (::LONG_PTR)this);

    return true;
}

// Find the pathname for a HTML file, based on the location of the .exe
// We could alternatively use res: URIs and embed the HTML files in the
// executable, but this is probably more flexible
HRESULT
HippoUI::getAppletURL(BSTR filename, BSTR *url)
{
    HRESULT hr;

    // XXX can theoretically truncate if we have a \?\\foo\bar\...
    // path which isn't limited to the short Windows MAX_PATH
    // Could use dynamic allocation here
    WCHAR baseBuf[MAX_PATH];

    if (!GetModuleFileName(instance_, baseBuf, sizeof(baseBuf) / sizeof(baseBuf[0])))
	return E_FAIL;

    for (size_t i = wcslen(baseBuf); i > 0; i--)
	if (baseBuf[i - 1] == '\\')
	    break;

    if (i == 0)  // No \ in path?
	return E_FAIL;

    HippoBSTR path((UINT)i, baseBuf);
    hr = path.Append(L"applets\\");
    if (!SUCCEEDED (hr))
	return hr;

    hr = path.Append(filename);
    if (!SUCCEEDED (hr))
	return hr;

    WCHAR urlBuf[INTERNET_MAX_URL_LENGTH];
    DWORD urlLength = INTERNET_MAX_URL_LENGTH;
    hr = UrlCreateFromPath(path, urlBuf, &urlLength, NULL);
    if (!SUCCEEDED (hr))
	return hr;

    *url = SysAllocString(urlBuf);
    return *url ? S_OK : E_OUTOFMEMORY;
}

bool
HippoUI::processMessage(UINT   message,
			WPARAM wParam,
			LPARAM lParam)
{
    int wmId, wmEvent;

    // Messages sent from the notification icon
    if (message == notificationIcon_.getMessage())
    {
	notificationIcon_.processMessage(wParam, lParam);
	return true;
    }

    switch (message) 
    {
    case WM_COMMAND:
	wmId    = LOWORD(wParam); 
	wmEvent = HIWORD(wParam);
	switch (wmId)
	{
	case IDM_SHARE:
	    showShareWindow(currentURL_);
	    break;
	case IDM_EXIT:
	    DestroyWindow(window_);
	    return true;
	}
	break;
    case WM_DESTROY:
	PostQuitMessage(0);
	return true;
    }

    return false;
}

LRESULT CALLBACK 
HippoUI::windowProc(HWND   window,
		    UINT   message,
		    WPARAM wParam,
		    LPARAM lParam)
{
    HippoUI *ui = (HippoUI *)GetWindowLongPtr(window, GWLP_USERDATA);
    if (ui) {
	if (ui->processMessage(message, wParam, lParam))
	    return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

/* Finds all IE and Explorer windows on the system. Needs some refinement
 * to distinguish the two.
 */
#if 0
static void
findExplorerWindows()
{
    HippoPtr<IShellWindows> shellWindows;
    HRESULT hr = CoCreateInstance(CLSID_ShellWindows, NULL, CLSCTX_ALL, IID_IShellWindows, (void **)&shellWindows);
    if (FAILED(hr)) {
	hippoDebug(L"Couldn't create: %x", hr);
	return;
    }

    LONG count;
    shellWindows->get_Count(&count);
    hippoDebug(L"%d", count);
    for (LONG i = 0; i < count; i++) {
	HippoPtr<IDispatch> dispatch;
	VARIANT item;
	item.vt = VT_I4;
	item.intVal = i;
	hr = shellWindows->Item(item, &dispatch);
	if (SUCCEEDED(hr)) {
	    HippoQIPtr<IWebBrowser2> browser(dispatch);

	    if (browser) {
		HippoBSTR browserURL;
	    	browser->get_LocationURL(&browserURL);

		if (browserURL)
		    hippoDebug(L"URL: %ls\n", (WCHAR *)browserURL);
	    }
	}
    }

}
#endif

int APIENTRY 
WinMain(HINSTANCE hInstance,
	HINSTANCE hPrevInstance,
	LPSTR     lpCmdLine,
	int       nCmdShow)
{
    MSG msg;
    HippoUI *ui;

    // Initialize COM
    CoInitialize(NULL);

    ui = new HippoUI();
    if (!ui->create(hInstance))
	return 0;

    // Main message loop:
    while (GetMessage(&msg, NULL, 0, 0)) 
    {
	TranslateMessage(&msg);
	DispatchMessage(&msg);
    }

    ui->destroy();
    ui->Release();

    CoUninitialize();

    return (int)msg.wParam;
}

