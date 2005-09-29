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

    username_ = NULL;
    password_ = NULL;
    rememberPassword_ = FALSE;
}

HippoUI::~HippoUI()
{
    if (username_)
	g_free(username_);
    if (password_)
	g_free(password_);
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

    loadUserInfo();

    if (!username_ || !password_) {
	bool result = DialogBoxParam(instance_, MAKEINTRESOURCE(IDD_LOGIN), 
    		                     window_, loginProc, (::LONG_PTR)this) != false;
    
	if (!result) // user cancelled
	    return false;
    }

    lmConnection_ = lm_connection_new("dumbhippo.com");
    LmMessageHandler *handler = lm_message_handler_new(onMessage, (gpointer)this, NULL);
    lm_connection_register_message_handler(lmConnection_, handler, 
	                                   LM_MESSAGE_TYPE_MESSAGE, 
	                                   LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    GError *error = NULL;
    if (!lm_connection_open(lmConnection_, 
	                    onConnectionOpen, (gpointer)this, NULL, 
			    &error)) {
	hippoDebug(L"Couldn't open connection to dumbhippo.com: %s",
		    error->message); // XXX encoding needs fixage

	lm_connection_close(lmConnection_, NULL);
	lm_connection_unref(lmConnection_);
	lmConnection_ = NULL;

	return false;
    }

    return true;
}

void
HippoUI::destroy()
{
    lm_connection_close(lmConnection_, NULL);
    lm_connection_unref(lmConnection_);
    lmConnection_ = NULL;

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

// Loads username, password from saved cookies
void
HippoUI::loadUserInfo()
{
    WCHAR staticBuffer[1024];
    WCHAR *allocBuffer = NULL;
    WCHAR *cookieBuffer = staticBuffer;
    DWORD cookieSize = sizeof(staticBuffer) / sizeof(staticBuffer[0]);
    char *cookie = NULL;

retry:
    if (!InternetGetCookieEx(L"http://messages.dumbhippo.com", 
			     L"userinfo",
	                     cookieBuffer, &cookieSize,
			     0,
			     NULL)) 
    {
	if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
	    cookieBuffer = allocBuffer = new WCHAR[cookieSize];
	    if (!cookieBuffer)
		goto out;
	    goto retry;
	}
    }

    cookie = g_utf16_to_utf8(cookieBuffer, cookieSize, NULL, NULL, NULL);
    if (!cookie)
	goto out;

    if (!strncmp(cookie, "username=", 9))
	goto out;

    for (char *p = cookie + 9; *p;) {
	char *next = strchr(p, '&');
	if (!next)
	    next = p + strlen(p);
	if (strncmp(p, "name=", 5) == 0)
	    username_ = g_strndup(p + 5, next - (p + 5));
	else if (strncmp(p, "password=", 9) == 0)
	    password_ = g_strndup(p + 9, next - (p + 9));

	p = next;
	if (*p) // Skip &
	    p++;
    }

out:
    g_free (cookie);
    delete[] allocBuffer;
}

void
HippoUI::saveUserInfo()
{
    if (!rememberPassword_ || !username_ || !password_)
	return;
	
    char *cookieString = g_strdup_printf("userinfo=name=%s&password=%s;"
	                                 "expires=Sun 01-Jan-2006 00:00:00 GMT;"
                                         "domain=.dumbhippo.com",
				         username_, password_);
    WCHAR *cookieStringW = g_utf8_to_utf16(cookieString, -1, NULL, NULL, NULL);

    InternetSetCookie(L"http://dumbhippo.com", NULL, cookieStringW);

    g_free (cookieStringW);
    g_free(cookieString);
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

void 
HippoUI::onPasswordDialogLogin(const WCHAR *username,
	                       const WCHAR *password,
			       bool         rememberPassword)
{
    username_ = g_utf16_to_utf8(username, -1, NULL, NULL, NULL);
    password_ = g_utf16_to_utf8(password, -1, NULL, NULL, NULL);
    rememberPassword_ = rememberPassword;
}

void 
HippoUI::onConnectionOpen (LmConnection *connection,
			   gboolean      success,
			   gpointer      userData)
{
    HippoUI *ui = (HippoUI *)userData;

    if (success) {
	if (ui->username_ && ui->password_) {
	    GError *error = NULL;
	    if (!lm_connection_authenticate(connection, 
	                                    ui->username_, ui->password_, "DumbHippo",
					    onConnectionAuthenticate, userData, NULL, &error)) {
		hippoDebug(L"Failed to authenticate: %s", error->message);
		g_error_free(error);
	    }
	}
    } else {
	hippoDebug(L"Failed to connect to server");
    }
}

void 
HippoUI::onConnectionAuthenticate (LmConnection *connection,
			           gboolean      success,
	    			   gpointer      userData)
{
    HippoUI *ui = (HippoUI *)userData;

    if (success) {
	ui->saveUserInfo();

	LmMessage *message;
	message = lm_message_new_with_sub_type(NULL, 
	                                       LM_MESSAGE_TYPE_PRESENCE, 
					       LM_MESSAGE_SUB_TYPE_AVAILABLE);

	GError *error = NULL;
	lm_connection_send(connection, message, &error);
	if (error) {
		hippoDebug(L"Failed to send presence: %s", error->message);
		g_error_free(error);
	}
	lm_message_unref(message);
    } else {
	hippoDebug(L"Couldn't authenticate");
    }
}

LmHandlerResult 
HippoUI::onMessage (LmMessageHandler *handler,
                    LmConnection     *connection,
	            LmMessage        *message,
	            gpointer          userData)
{
    HippoUI *ui = (HippoUI *)userData;

    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_CHAT) {
	LmMessageNode *node = lm_message_node_find_child(message->node, "body");
	if (node && node->value) {
	    WCHAR *ls = g_utf8_to_utf16(node->value, -1, NULL, NULL, NULL);
	    if (ls)
		ui->Log(HippoBSTR(ls));
	    g_free (ls);
	}
    }

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
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

INT_PTR CALLBACK 
HippoUI::loginProc(HWND   dialog,
      	           UINT   message,
		   WPARAM wParam,
		   LPARAM lParam)
{
    if (message == WM_INITDIALOG) {
	HippoUI *ui = (HippoUI *)lParam;
	SetWindowLongPtr(dialog, GWLP_USERDATA, (::LONG_PTR)ui);

	return TRUE;
    }

    HippoUI *ui = (HippoUI *)GetWindowLongPtr(dialog, GWLP_USERDATA);
    if (!ui)
	return FALSE;

    switch (message) {
    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case IDOK:
	    {
	    WCHAR username[128];
	    username[0] = '\0';
	    GetDlgItemText(dialog, IDC_USERNAME, username, sizeof(username) / sizeof(username[0]));

	    WCHAR password[128];
	    password[0] = '\0';
	    GetDlgItemText(dialog, IDC_PASSWORD, password, sizeof(password) / sizeof(password[0]));

	    bool rememberPass = (IsDlgButtonChecked(dialog, IDC_REMEMBERPASS) == BST_CHECKED);

	    ui->onPasswordDialogLogin(username, password, rememberPass);

	    EndDialog(dialog, TRUE);
	    return TRUE;
	    }
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

