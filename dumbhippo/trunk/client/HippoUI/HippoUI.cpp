/* HippoUI.cpp: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include "HippoUI.h"
#include <stdio.h>
#include <strsafe.h>
#include <HippoUtil_i.c>

// GUID definition
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include "Guid.h"
#pragma data_seg()

// Global Variables:
#define MAX_LOADSTRING 100

HINSTANCE hInst;					// current instance
TCHAR szTitle[MAX_LOADSTRING];				// The title bar text
TCHAR szWindowClass[MAX_LOADSTRING];			// the main window class name
HICON bigIcon;
HICON smallIcon;
HWND hWnd;
UINT notifyMessage;
HMENU notifyMenu;

ATOM		 MyRegisterClass(HINSTANCE hInstance);
BOOL		 InitInstance(HINSTANCE, int);
LRESULT CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
void             RemoveNotificationIcon(void);
void             NotifyBalloonURL(LPTSTR str);
void             ShowNotificationMenu(UINT buttonFlag);

CHippoUI::CHippoUI()
{
    objRefCount_ = 1;

    uiTypeInfo_ = NULL;

    ITypeLib *pTypeLib;
    if (SUCCEEDED (LoadRegTypeLib(LIBID_HippoUtil, 1, 1, 0, &pTypeLib))) {
	pTypeLib->GetTypeInfoOfGuid(IID_IHippoUI, &uiTypeInfo_);
	pTypeLib->Release();
    }
}


CHippoUI::~CHippoUI()
{
    if (uiTypeInfo_) {
	uiTypeInfo_->Release();
	uiTypeInfo_ = NULL;
    }
}

/* IUnknown Implementation */

STDMETHODIMP 
CHippoUI::QueryInterface(REFIID riid, LPVOID *ppReturn)
{
    *ppReturn = NULL;

    //IUnknown
    if(IsEqualIID(riid, IID_IUnknown))
    {
        *ppReturn = this;
    }
    
    //IHippoUI
    else if(IsEqualIID(riid, IID_IHippoUI))
    {
        *ppReturn = (IHippoUI*)this;
    }   

    if(*ppReturn)
    {
        (*(LPUNKNOWN*)ppReturn)->AddRef();
        return S_OK;
    }
    
    return E_NOINTERFACE;
}                                             

STDMETHODIMP_(DWORD) 
CHippoUI::AddRef()
{
    return ++objRefCount_;
}


STDMETHODIMP_(DWORD) 
CHippoUI::Release()
{
    if(--objRefCount_ == 0)
    {
        delete this;
        return 0;
    }
   
    return objRefCount_;
}

// IDispatch implementations. We just delegate IDispatch to the 
// standard Typelib-based version.

STDMETHODIMP
CHippoUI::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
	return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
CHippoUI::GetTypeInfo(UINT        iTInfo,
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
CHippoUI::GetIDsOfNames (REFIID    riid,
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
CHippoUI::Invoke (DISPID      dispIdMember,
		  REFIID      riid,
		  LCID        lcid,
		  WORD        wFlags,
		  DISPPARAMS *pDispParams,
                  VARIANT    *pVarResult,
                  EXCEPINFO  *pExcepInfo,
                  UINT       *puArgErr)
{
    HRESULT hr;
    IHippoUI *pHippoUI;

    if (!uiTypeInfo_) 
	 return E_OUTOFMEMORY;

    this->QueryInterface(IID_IHippoUI, (LPVOID *)&pHippoUI);

    hr = DispInvoke(pHippoUI, uiTypeInfo_, dispIdMember, wFlags, 
	            pDispParams, pVarResult, pExcepInfo, puArgErr);

    pHippoUI->Release();

    return hr;
}

// IHippoTracker implementations

STDMETHODIMP 
CHippoUI::Log(BSTR message)
{
    NotifyBalloonURL(message);
    return S_OK;
}

int APIENTRY 
WinMain(HINSTANCE hInstance,
	HINSTANCE hPrevInstance,
	LPSTR     lpCmdLine,
	int       nCmdShow)
{
    MSG msg;
    CHippoUI *ui;

    // Load global resources
    LoadString(hInstance, IDS_APP_TITLE, szTitle, MAX_LOADSTRING);
    LoadString(hInstance, IDC_HIPPOUI, szWindowClass, MAX_LOADSTRING);

    smallIcon = (HICON)LoadImage(hInstance, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    bigIcon = (HICON)LoadImage(hInstance, MAKEINTRESOURCE(IDI_DUMBHIPPO),
	IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);
    notifyMenu = LoadMenu(hInstance, MAKEINTRESOURCE(IDR_NOTIFY));
    notifyMessage = RegisterWindowMessage(TEXT("HippoNotifyMessage"));

    // Register window class
    MyRegisterClass(hInstance);

    // Initialize COM
    CoInitialize(NULL);

    // Create the Singleton HippoUI object and try to register it
    ui = new CHippoUI();
    if (!ui->registerActive()) {
	ui->Release();
	return 0;
    }

    // Perform application initialization:
    if (!InitInstance (hInstance, nCmdShow)) 
	return 0;

    // Main message loop:
    while (GetMessage(&msg, NULL, 0, 0)) 
    {
	TranslateMessage(&msg);
	DispatchMessage(&msg);
    }

    RemoveNotificationIcon();

    ui->revokeActive();
    ui->Release();

    return (int) msg.wParam;
}

// Tries to register as the singleton CHippoUI, returns true on success
bool 
CHippoUI::registerActive()
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
CHippoUI::revokeActive()
{
    RevokeActiveObject(registerHandle_, NULL);
}

ATOM 
MyRegisterClass(HINSTANCE hInstance)
{
    WNDCLASSEX wcex;

    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style		= CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc	= (WNDPROC)WndProc;
    wcex.cbClsExtra	= 0;
    wcex.cbWndExtra	= 0;
    wcex.hInstance	= hInstance;
    wcex.hIcon		= bigIcon;
    wcex.hCursor	= LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground	= (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName	= NULL;
    wcex.lpszClassName	= szWindowClass;
    wcex.hIconSm	= smallIcon;

    return RegisterClassEx(&wcex);
}

BOOL 
InitInstance(HINSTANCE hInstance, int nCmdShow)
{
   NOTIFYICONDATA notifyIconData = { 0 };

   hInst = hInstance; // Store instance handle in our global variable

   hWnd = CreateWindow(szWindowClass, szTitle, WS_OVERLAPPEDWINDOW,
      CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, NULL, NULL, hInstance, NULL);

   if (!hWnd)
      return FALSE;
   
   // We don't actually show the window; it's just there for communication
   // with our Notification icon

   notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
   notifyIconData.hWnd = hWnd;
   notifyIconData.uID = 0;
   notifyIconData.uFlags = NIF_ICON | NIF_MESSAGE;
   notifyIconData.uCallbackMessage = notifyMessage;
   notifyIconData.hIcon = smallIcon;

   Shell_NotifyIcon(NIM_ADD, &notifyIconData);

   return TRUE;
}

void
RemoveNotificationIcon(void)
{
   NOTIFYICONDATA notifyIconData = { 0 };

   notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
   notifyIconData.hWnd = hWnd;
   notifyIconData.uID = 0;
   
   Shell_NotifyIcon(NIM_DELETE, &notifyIconData);
}

// XP SP2 addition
#ifndef NIIF_USER
#define NIIF_USER 4
#endif

void
NotifyBalloonURL(LPTSTR str)
{
    TCHAR menubuf[64];

    StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Share "));
    StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, str);
    StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, TEXT("..."));
    StringCchCopy(menubuf + sizeof(menubuf) / sizeof(TCHAR) - 6, 6, TEXT("[...]"));

    ModifyMenu(notifyMenu, IDM_SHARE, MF_BYCOMMAND | MF_STRING, 
	       IDM_SHARE, menubuf);

    NOTIFYICONDATA notifyIconData = { 0 };

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = hWnd;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_INFO;
    StringCchCopy(notifyIconData.szInfo, sizeof(notifyIconData.szInfo) / sizeof(TCHAR), str);
    StringCchCat(notifyIconData.szInfo, sizeof(notifyIconData.szInfo) / sizeof(TCHAR),
                 TEXT("\n(click to view)"));
    notifyIconData.szInfo[sizeof(notifyIconData.szInfo) - 1] = '\0';
    notifyIconData.uTimeout = 10 * 1000; // 10 seconds
    StringCchCopy(notifyIconData.szInfoTitle, sizeof(notifyIconData.szInfoTitle) / sizeof(TCHAR), TEXT("New Link"));
    notifyIconData.dwInfoFlags = NIIF_USER;
   
    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}

void
ShowNotificationMenu(UINT buttonFlag)
{
    POINT pt;
    HMENU popupMenu;

    // We:
    //  - Set the foreground window to our (non-shown) window so that clicking
    //    away elsewhere works
    //  - Send the dummy event to force a context switch to our app
    // See Microsoft knowledgebase Q135788

    GetCursorPos(&pt);
    popupMenu = GetSubMenu(notifyMenu, 0);

    SetForegroundWindow(hWnd);
    TrackPopupMenu(popupMenu, buttonFlag, pt.x, pt.y, 0, hWnd, NULL);

    PostMessage(hWnd, WM_NULL, 0, 0);
}

LRESULT CALLBACK 
WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    int wmId, wmEvent;

    // Messages sent from the notification icon
    if (message == notifyMessage) 
    {
	switch (lParam) {
	    case WM_LBUTTONDOWN:
	    case NIN_SELECT:
	        ShowNotificationMenu(TPM_LEFTBUTTON);
	        break;
	    case WM_RBUTTONDOWN:
	    case WM_CONTEXTMENU:
	        ShowNotificationMenu(TPM_RIGHTBUTTON);
	        break;
	    case NIN_BALLOONUSERCLICK:
	        MessageBox(NULL, TEXT("View it, baby!"), TEXT("View It!"), MB_OK);
	        break;
	}

	return 0;
    }

    switch (message) 
    {
    case WM_COMMAND:
	wmId    = LOWORD(wParam); 
	wmEvent = HIWORD(wParam);
	// Parse the menu selections:
	switch (wmId)
	{
	case IDM_EXIT:
	    DestroyWindow(hWnd);
	    break;
	default:
	    return DefWindowProc(hWnd, message, wParam, lParam);
	}
	break;
    case WM_DESTROY:
	PostQuitMessage(0);
	break;
    default:
	return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}
