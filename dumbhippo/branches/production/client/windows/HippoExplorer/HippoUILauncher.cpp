/* HippoUILauncher.cpp: COM Object called by toolbar button
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoexplorer.h"
#include "HippoUILauncher.h"
#include "Guid.h"
#include "Globals.h"
#include <process.h>
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

// If we have to start the client, we need to wait in a timeout
static const UINT_PTR UI_WAIT = 1;        // timeout identifier (unique only for window)
static const UINT UI_WAIT_INTERVAL = 250; // timeout interval: 0.25s
static const int UI_WAIT_MAX_TRIES = 100; // How many times to check

// Window class for the window we use to receive the above timeout
static const TCHAR *CLASS_NAME = TEXT("HippoUILauncherClass");

HippoUILauncher::HippoUILauncher()
{
    window_ = NULL;
    uiWaitCount_ = 0;
}

HippoUILauncher::~HippoUILauncher(void)
{
    if (window_)
        DestroyWindow(window_);
}

void
HippoUILauncher::setListener(HippoUILaunchListener *listener)
{
    listener_ = listener;
}

bool 
HippoUILauncher::registerWindowClass()
{
    WNDCLASS windowClass;

    if (GetClassInfo(dllInstance, CLASS_NAME, &windowClass))
        return true;  // Already registered

    windowClass.style = 0;
    windowClass.lpfnWndProc = windowProc;
    windowClass.cbClsExtra = 0;
    windowClass.cbWndExtra = 0;
    windowClass.hInstance = dllInstance;
    windowClass.hIcon = NULL;
    windowClass.hCursor = NULL;
    windowClass.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    windowClass.lpszMenuName = NULL;
    windowClass.lpszClassName = CLASS_NAME;    

    return RegisterClass(&windowClass) != 0;
}

bool
HippoUILauncher::createWindow()
{
    if (!registerWindowClass())
        return false;

    window_ = CreateWindow(CLASS_NAME, 
                           NULL, // No title
                           0,    // Window style doesn't matter
                           0, 0, 10, 10,
                           NULL, // No parent
                           NULL, // No menu
                           dllInstance,
                           NULL); // lpParam
    if (!window_)
        return false;

    hippoSetWindowData<HippoUILauncher>(window_, this);

    return true;
}

// Check first for a non-debug instance of HippoUI, then a "dogfood" (testing)
// instance, then the debug instance. We favor the non-debug instance to let 
// people use DumbHippo while hacking it.
HRESULT
HippoUILauncher::getUI(IHippoUI **ui, BSTR user)
{
    IHippoUI *exact = NULL;
    IHippoUI *approximate = NULL;

    checkOneInstance(CLSID_HippoUI, user, &exact, &approximate);
    checkOneInstance(CLSID_HippoUI_Dogfood, user, &exact, &approximate);
    checkOneInstance(CLSID_HippoUI_Debug, user, &exact, &approximate);

    if (exact) {
        if (approximate)
            approximate->Release();
        *ui = exact;
        return S_OK;
    } else if (approximate) {
        *ui = approximate;
        return S_OK;
    } else {
        return E_FAIL;
    }
}

void
HippoUILauncher::checkOneInstance(const CLSID &classId, BSTR user, IHippoUI **exact, IHippoUI **approximate)
{
    // If we already have an exact match, nothing to do
    if (*exact)
        return;

    // See if there is an instance for this class ID
    HippoPtr<IUnknown> unknown;
    HippoPtr<IHippoUI> ui;
    if (FAILED(GetActiveObject(classId, NULL, &unknown)) ||
        FAILED(unknown->QueryInterface<IHippoUI>(&ui)))
        return;

    // Any instance counts as an approximate match
    if (!*approximate) {
        ui->AddRef();
        *approximate = ui;
    }

    // If user is null, any instance counts as an exact match as well
    if (!user) {
        ui->AddRef();
        *exact = ui;
        return;
    }

    // Otherwise check the user ID for the found match
    HippoBSTR loginId;
    if (FAILED(ui->GetLoginId(&loginId)) || !loginId)
        return;

    if (wcscmp(loginId, user) == 0) {
        ui->AddRef();
        *exact = ui;
    }
}

HRESULT
HippoUILauncher::launchUI()
{
    if (!window_) {
       if (!createWindow()) {
            hippoDebugDialog(L"Can't create window to receive timer messages");
            return E_FAIL;
        }
    }

    if (!spawnUI()) {
        hippoDebugDialog(L"Could not start Mugshot client");
        return E_FAIL;
    }

    uiWaitCount_ = UI_WAIT_MAX_TRIES;
    SetTimer(window_, UI_WAIT, UI_WAIT_INTERVAL, NULL);

    return S_OK;
}

// Start up the HippoUI process. We assume it's in the same directory 
// as the HippoExplorer DLL
bool
HippoUILauncher::spawnUI()
{
    HINSTANCE module = GetModuleHandle(L"HippoExplorer.dll");
    if (!module)
        module = GetModuleHandle(L"HIPPOE~1.DLL");
    if (!module)
        return false;

    WCHAR fileBuf[MAX_PATH];
    if (!GetModuleFileName(module, fileBuf, sizeof(fileBuf) / sizeof(fileBuf[0])))
        return false;

    size_t i;
    for (i = wcslen(fileBuf); i > 0; i--)
        if (fileBuf[i - 1] == '\\')
            break;

    if (i == 0)  // No \ in path?
        return false;

    if (FAILED(StringCchCopy(fileBuf + i, MAX_PATH - i, L"Mugshot.exe")))
        return false;

    return _wspawnl(_P_NOWAIT, fileBuf, L"HippoUI", NULL) != -1;
}

// The periodic time called while waiting for the UI to start
void 
HippoUILauncher::uiWaitTimer()
{
    HippoPtr<IHippoUI> ui;

    if (SUCCEEDED(getUI(&ui))) {
        stopUIWait();
        listener_->onLaunchSuccess(this, ui);
    } else {
        uiWaitCount_--;
        if (uiWaitCount_ == 0) {
            stopUIWait();
            listener_->onLaunchFailure(this, L"Could not start Mugshot client.");
        }
    }
}

void
HippoUILauncher::stopUIWait() 
{
    KillTimer(window_, UI_WAIT);
}

LRESULT CALLBACK 
HippoUILauncher::windowProc(HWND   window,
                               UINT   message,
                               WPARAM wParam,
                               LPARAM lParam)
{
    HippoUILauncher *toolbarAction = hippoGetWindowData<HippoUILauncher>(window);
    if (toolbarAction) {
        if (message == WM_TIMER && wParam == UI_WAIT) {
            toolbarAction->uiWaitTimer();
            return true;
        } else {
            return false;
        }
    }

    return DefWindowProc(window, message, wParam, lParam);
}
