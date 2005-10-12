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
#include <Winsock2.h>
#include <urlmon.h>   // For CoInternetParseUrl
#include <wininet.h>  // for cookie retrieval
#include "Resource.h"

#include <glib.h>

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

    preferencesDialog_ = NULL;

    nextBrowserCookie_ = 0;

    rememberPassword_ = FALSE;
    passwordRemembered_ = FALSE;
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
HippoUI::RegisterBrowser(IWebBrowser2 *browser,
	                 DWORD        *cookie)
{
    HippoBrowserInfo info;

    info.browser = browser;
    *cookie = info.cookie = ++nextBrowserCookie_;

    browsers_.append(info);

    return S_OK;
}

STDMETHODIMP 
HippoUI::UnregisterBrowser(DWORD cookie)
{
    for (ULONG i = 0; i < browsers_.length(); i++) {
	if (browsers_[i].cookie == cookie) {
	    browsers_.remove(i);
	    return S_OK;
	}
    }

    return E_FAIL;
}

STDMETHODIMP 
HippoUI::UpdateBrowser(DWORD cookie, BSTR url, BSTR title)
{
    for (ULONG i = 0; i < browsers_.length(); i++) {
 	if (browsers_[i].cookie == cookie) {
	    browsers_[i].url = url;
	    browsers_[i].title = title;
	    return S_OK;
	}
    }

    return E_FAIL;
}

////////////////////////////////////////////////////////////////////////////

bool
HippoUI::create(HINSTANCE instance)
{
    instance_ = instance;

    smallIcon_ = (HICON)LoadImage(instance_, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	                          IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    bigIcon_ = (HICON)LoadImage(instance_, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	                        IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);

    menu_ = LoadMenu(instance, MAKEINTRESOURCE(IDR_NOTIFY));

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

    im_.setUI(this);
    if (preferences_.getSignIn()) {
	if (im_.signIn())
	    showSignInWindow();
    }

    return true;
}

void
HippoUI::destroy()
{
    notificationIcon_.destroy();
    
    revokeActive();
}

HippoPreferences *
HippoUI::getPreferences()
{
    return &preferences_;
}

void
HippoUI::showAppletWindow(BSTR url)
{
    HippoPtr<IWebBrowser2> webBrowser;
    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
	             IID_IWebBrowser2, (void **)&webBrowser);

    if (!webBrowser)
	return;

    VARIANT missing;
    missing.vt = VT_NULL;

    webBrowser->Navigate(url,
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

// Show a window offering to share the given URL
void 
HippoUI::showShareWindow(BSTR url)
{
    HippoBSTR shareURL;
    
    if (!SUCCEEDED (getAppletURL(HippoBSTR(L"jsf/addlink.faces"), &shareURL)))
	return;

    if (!SUCCEEDED (shareURL.Append(L"?url=")))
	return;

    if (!SUCCEEDED (shareURL.Append(url)))
	return;

    showAppletWindow(shareURL);
}

void
HippoUI::showSignInWindow()
{
    HippoBSTR signInURL;
    
    if (!SUCCEEDED (getAppletURL(HippoBSTR(L"jsf/addclient.faces"), &signInURL)))
	return;

    showAppletWindow(signInURL);
}

void
HippoUI::showMenu(UINT buttonFlag)
{
    POINT pt;
    HMENU popupMenu;

    updateMenu();

    // We:
    //  - Set the foreground window to our (non-shown) window so that clicking
    //    away elsewhere works
    //  - Send the dummy event to force a context switch to our app
    // See Microsoft knowledgebase Q135788

    GetCursorPos(&pt);
    popupMenu = GetSubMenu(menu_, 0);

    SetForegroundWindow(window_);
    TrackPopupMenu(popupMenu, buttonFlag, pt.x, pt.y, 0, window_, NULL);

    PostMessage(window_, WM_NULL, 0, 0);
}

// Show a window when the user clicks on a shared link
void 
HippoUI::showURL(BSTR url)
{
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

void 
HippoUI::onAuthFailure()
{
    showSignInWindow();
}

void
HippoUI::onAuthSuccess()
{
}

void 
HippoUI::onLinkMessage(const WCHAR *url,
	               const WCHAR *title,
		       const WCHAR *description)
{
    notificationIcon_.showURL(url, title, description);
}

// Tries to register as the singleton HippoUI, returns true on success
bool 
HippoUI::registerActive()
{
    IHippoUI *pHippoUI;
 
    QueryInterface(IID_IHippoUI, (LPVOID *)&pHippoUI);
    HRESULT hr = RegisterActiveObject(pHippoUI, CLSID_HippoUI, ACTIVEOBJECT_STRONG, &registerHandle_);
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
    
    // There might already be explorer windows open, so broadcast a message
    // that causes HippoTracker to recheck the active object table
    UINT uiStartedMessage = RegisterWindowMessage(TEXT("HippoUIStarted"));
    SendNotifyMessage(HWND_BROADCAST, uiStartedMessage, 0, 0);

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

    hippoSetWindowData<HippoUI>(window_, this);

    return true;
}

void 
HippoUI::showPreferences()
{
    if (!preferencesDialog_) {
	preferencesDialog_ = CreateDialogParam(instance_, MAKEINTRESOURCE(IDD_PREFERENCES),
	                                       window_, preferencesProc, (::LONG_PTR)this);
	if (!preferencesDialog_)
	    return;

	HippoBSTR messageServer;
	if (SUCCEEDED (preferences_.getMessageServer(&messageServer)))
	    SetDlgItemText(preferencesDialog_, IDC_MESSAGE_SERVER, messageServer);

	HippoBSTR webServer;
	if (SUCCEEDED (preferences_.getWebServer(&webServer)))
	    SetDlgItemText(preferencesDialog_, IDC_WEB_SERVER, webServer);
    }
    
    updateForgetPassword();
    ShowWindow(preferencesDialog_, SW_SHOW);
}

void 
HippoUI::updateForgetPassword()
{
    if (!preferencesDialog_)
	return;

    HWND forgetPassButton = GetDlgItem(preferencesDialog_, IDC_FORGETPASSWORD);
    if (forgetPassButton)
	EnableWindow(forgetPassButton, im_.hasAuth());
}

static bool
urlIsLocal(const WCHAR *url)
{
    WCHAR schemaBuf[64];
    DWORD schemaSize;

    if (CoInternetParseUrl(url, PARSE_SCHEMA, 0,
  		           schemaBuf, sizeof(schemaBuf) / sizeof(schemaBuf[0]), 
			   &schemaSize, 0) != S_OK)
	return false;

    return wcscmp(schemaBuf, L"file") == 0;
}

void
HippoUI::updateMenu()
{
    HMENU popupMenu = GetSubMenu(menu_, 0);

    // Delete previous dynamic menuitems
    while (TRUE) {
    	int id = GetMenuItemID(popupMenu, 0);
        if (id >= IDM_SHARE0 && id <= IDM_SIGN_OUT)
	    RemoveMenu(popupMenu, 0, MF_BYPOSITION);
	else
	    break;
    }

    // Now insert new ones for the current URLs
    UINT pos = 0;
    for (ULONG i = 0; i < browsers_.length() && i < 10; i++) {
	MENUITEMINFO info;
        WCHAR menubuf[64];

	if (!browsers_[i].title)
	    continue;
    
	if (urlIsLocal(browsers_[i].url))
	    continue;

	StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Share "));
	StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, browsers_[i].title);
	StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, TEXT("..."));
	StringCchCopy(menubuf + sizeof(menubuf) / sizeof(TCHAR) - 6, 6, TEXT("[...]"));

	memset((void *)&info, 0, sizeof(MENUITEMINFO));
	info.cbSize = sizeof(MENUITEMINFO);

	info.fMask = MIIM_ID | MIIM_DATA | MIIM_STRING;
	info.fType = MFT_STRING;
	info.wID = IDM_SHARE0 + i;
	info.dwTypeData = menubuf;
	    
	InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }

    // Insert a separator if necessary
    if (pos != 0) {
	MENUITEMINFO info;
    
	memset((void *)&info, 0, sizeof(MENUITEMINFO));
	info.cbSize = sizeof(MENUITEMINFO);

	info.fMask = MIIM_ID | MIIM_FTYPE;
	info.fType = MFT_SEPARATOR;
	info.wID = IDM_SHARESEPARATOR;

	InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }

    // Insert the sign in / sign out menu item
    {
	MENUITEMINFO info;
        WCHAR menubuf[64];

	memset((void *)&info, 0, sizeof(MENUITEMINFO));
	info.cbSize = sizeof(MENUITEMINFO);

	info.fMask = MIIM_ID | MIIM_DATA | MIIM_STRING;
	info.fType = MFT_STRING;
	info.wID = IDM_SIGN_IN;

	HippoIM::State state = im_.getState();
	if (state == HippoIM::SIGNED_OUT || state == HippoIM::SIGN_IN_WAIT) {
	    info.wID = IDM_SIGN_IN;
	    StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Sign In..."));
	} else {
	    info.wID = IDM_SIGN_OUT;
	    StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Sign Out"));
	}

	info.dwTypeData = menubuf;
	    
	InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }
}

// Find the pathname for a HTML file, based on the location of the .exe
// We could alternatively use res: URIs and embed the HTML files in the
// executable, but this is probably more flexible
HRESULT
HippoUI::getAppletURL(BSTR appletName, BSTR *result)
{
    HRESULT hr;
    HippoBSTR webServer;
    HippoBSTR url(L"http://");

    if (!url)
	return E_OUTOFMEMORY;

    hr = preferences_.getWebServer(&webServer);
    if (FAILED (hr))
	return hr;

    hr = url.Append(webServer);
    if (FAILED (hr))
	return hr;

    hr = url.Append(L"/");
    if (FAILED (hr))
	return hr;

    hr = url.Append(appletName);
    if (FAILED (hr))
	return hr;

    return url.CopyTo(result);
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
	if (wmId >= IDM_SHARE0 && wmId <= IDM_SHARE9) {
	    UINT i = wmId - IDM_SHARE0;
	    if (i < browsers_.length() && browsers_[i].url)
		showShareWindow(browsers_[i].url);
	    return true;
	}

	switch (wmId)
	{
	case IDM_SIGN_IN:
	    if (im_.signIn())
		showSignInWindow();
	    return true;
	case IDM_SIGN_OUT:
	    im_.signOut();
	    return true;
	case IDM_PREFERENCES:
	    showPreferences();
	    return true;
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
    HippoUI *ui = hippoGetWindowData<HippoUI>(window);
    if (ui) {
	if (ui->processMessage(message, wParam, lParam))
	    return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

INT_PTR CALLBACK 
HippoUI::preferencesProc(HWND   dialog,
      	                 UINT   message,
		         WPARAM wParam,
		         LPARAM lParam)
{
    if (message == WM_INITDIALOG) {
	HippoUI *ui = (HippoUI *)lParam;
	hippoSetWindowData<HippoUI>(dialog, ui);

	return TRUE;
    }

    HippoUI *ui = hippoGetWindowData<HippoUI>(dialog);
    if (!ui)
	return FALSE;

    switch (message) {
    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case IDC_FORGETPASSWORD:
	    ui->im_.forgetAuth();
	    ui->updateForgetPassword();
	    return TRUE;
        case IDOK:
	    {
	    WCHAR messageServer[128];
	    messageServer[0] = '\0';
	    GetDlgItemText(dialog, IDC_MESSAGE_SERVER, 
		           messageServer, sizeof(messageServer) / sizeof(messageServer[0]));
	    ui->preferences_.setMessageServer(HippoBSTR(messageServer));

	    WCHAR webServer[128];
	    webServer[0] = '\0';
	    GetDlgItemText(dialog, IDC_WEB_SERVER, 
		           webServer, sizeof(webServer) / sizeof(webServer[0]));
	    ui->preferences_.setWebServer(HippoBSTR(webServer));

	    EndDialog(dialog, TRUE);
	    }
	    
	    return TRUE;
	case IDCANCEL:
	    EndDialog(dialog, FALSE);
	    return TRUE;
	}
    }

    return FALSE;
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

/* Define a custom main loop source for integrating the Glib main loop with Win32
 * message handling; this isn't very generalized, since we hardcode the handling
 * of a FALSE return from GetMessage() to call g_main_loop_quit() on a particular
 * loop. If we were being more general, we'd probably want a Win32SourceQuitFunc.
 */
struct Win32Source {
    GSource source;
    GPollFD pollFD;
    int result;
    GMainLoop *loop;
};

static gboolean 
win32SourcePrepare(GSource *source,
		   int     *timeout)
{
    MSG msg;

    *timeout = -1;

    return PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);
}

static gboolean
win32SourceCheck(GSource *source)
{
    MSG msg;

    return PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);
}

static gboolean
win32SourceDispatch(GSource     *source,
		    GSourceFunc  callback,
		    gpointer     userData)
{
    MSG msg;

    if (!GetMessage(&msg, NULL, 0, 0)) {
	Win32Source *win32Source = (Win32Source *)(source);

	win32Source->result = (int)msg.wParam;

	g_main_context_remove_poll (NULL, &win32Source->pollFD);
	g_main_loop_quit(win32Source->loop);
	return FALSE;
    }

    TranslateMessage(&msg);
    DispatchMessage(&msg);

    return TRUE;
}

static void
win32SourceFinalize(GSource *source)
{
}

static const GSourceFuncs win32SourceFuncs = {
    win32SourcePrepare,
    win32SourceCheck,
    win32SourceDispatch,
    win32SourceFinalize
};

GSource *
win32SourceNew(GMainLoop *loop)
{
    GSource *source = g_source_new((GSourceFuncs *)&win32SourceFuncs, sizeof(Win32Source));
    Win32Source *win32Source = (Win32Source *)source;

    win32Source->pollFD.fd = G_WIN32_MSG_HANDLE;
    win32Source->pollFD.events = G_IO_IN;
    win32Source->result = 0;
    win32Source->loop = loop;

    g_main_context_add_poll(NULL, &win32Source->pollFD, G_PRIORITY_DEFAULT);

    return source;
}

static bool
initializeWinSock(void)
{
    WSADATA wsData;

    // We can support WinSock 2.2
    int result = WSAStartup(MAKEWORD(2,2), &wsData);
    // Fail to initialize if the system doesn't at least of WinSock 2.0
    // Both of these versions are pretty much arbitrary. No testing across
    // a range of versions has been done.
    if (result || LOBYTE(wsData.wVersion) < 2) {
	if (!result)
	    WSACleanup();
	MessageBox(NULL, L"Couldn't initialize WinSock", NULL, MB_OK);
	return false;
    }

    return true;
}

int APIENTRY 
WinMain(HINSTANCE hInstance,
	HINSTANCE hPrevInstance,
	LPSTR     lpCmdLine,
	int       nCmdShow)
{
    HippoUI *ui;
    GMainLoop *loop;
    GSource *source;
    int result;

    // Initialize COM
    CoInitialize(NULL);

    if (!initializeWinSock())
	return 0;

    ui = new HippoUI();
    if (!ui->create(hInstance))
	return 0;

    loop = g_main_loop_new(NULL, FALSE);

    source = win32SourceNew(loop);
    g_source_attach(source, NULL);

    g_main_loop_run(loop);

    result = ((Win32Source *)source)->result;
    g_source_unref(source);

    ui->destroy();
    ui->Release();

    WSACleanup();
    CoUninitialize();

    return result;
}

