/* HippoToolbarAction.cpp: COM Object called by toolbar button
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"
#include "HippoToolbarAction.h"
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
static const TCHAR *CLASS_NAME = TEXT("HippoToolbarActionClass");

HippoToolbarAction::HippoToolbarAction(void)
{
    refCount_ = 1;
    dllRefCount++;

    window_ = NULL;
    uiWaitCount_ = 0;
}

HippoToolbarAction::~HippoToolbarAction(void)
{
    if (window_)
        DestroyWindow(window_);

    // In case setSite(NULL) wasn't called
    clearSite();
        
    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoToolbarAction::QueryInterface(const IID &ifaceID, 
                                   void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IObjectWithSite *>(this));
    else if (IsEqualIID(ifaceID, IID_IObjectWithSite)) 
        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleCommandTarget)) 
        *result = static_cast<IOleCommandTarget *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoToolbarAction)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoToolbarAction::SetSite(IUnknown *site)
{
    clearSite();
    
    if (site) 
    {
        if (FAILED(site->QueryInterface<IServiceProvider>(&site_)))
            return E_FAIL;

        if (FAILED(site_->QueryService<IWebBrowser2>(SID_SWebBrowserApp, &browser_))) {
            site_ = NULL;
            browser_ = NULL;

            return E_FAIL;
        }
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoToolbarAction::GetSite(const IID &iid, 
                            void     **result)
{
    if (!site_) {
        *result = NULL;
        return E_FAIL;
    }

    return site_->QueryInterface(iid, result);
}

/////////////////// IOleCommandTarget implementation ///////////////////

STDMETHODIMP
HippoToolbarAction::QueryStatus (const GUID *commandGroup,
                                 ULONG       nCommands,
                                 OLECMD     *commands,
                                 OLECMDTEXT *commandText)
{
    // The docs are inconsistent about what the different command IDs are,
    // and don't define the command group. Treat everything the same

    for (ULONG i = 0; i < nCommands; i++) {
        // the commandText parameter queries for extra information; IE always
        // passes NULL, but we provide an implementation anyways
        if (commandText) {
            if ((commandText->cmdtextf & OLECMDTEXTF_NAME) != 0) {
                commandText->cwActual = (ULONG)wcslen(L"Share Link");
                StringCchCopy(commandText->rgwz, commandText->cwBuf, L"Share Link");
            } else if ((commandText->cmdtextf & OLECMDTEXTF_STATUS) != 0) {
                commandText->cwActual = (ULONG)wcslen(L"");
                StringCchCopy(commandText->rgwz, commandText->cwBuf, L"");
            }

            commandText = NULL; // the text result is for the first supported command
        }

        // If we are waiting for the UI to start, we display the icon insensitive
        commands[i].cmdf = OLECMDF_SUPPORTED;
        if (!shareUrl_)
            commands[i].cmdf |= OLECMDF_ENABLED;
    }

    return S_OK;
}

STDMETHODIMP
HippoToolbarAction::Exec (const GUID *commandGroup,
                          DWORD       commandId,
                          DWORD       nCommandExecOptions,
                          VARIANTARG *commandInput,
                          VARIANTARG *commandOutput)
{
    // The docs are inconsistent about what the different command IDs are,
    // and don't define the command group. Treat everything the same

    if (!browser_) {
        hippoDebug(L"No browser is known");
        return E_FAIL;
    }

    HippoBSTR url;
    HippoBSTR title;

    if (!(SUCCEEDED(browser_->get_LocationURL(&url)) &&
          SUCCEEDED(browser_->get_LocationName(&title)) &&
          url && ((WCHAR *)url)[0] && title && ((WCHAR *)title)[0])) 
    {
        hippoDebug(L"There isn't a current web page to share");
        return E_FAIL;
    }

    HippoPtr<IHippoUI> ui;

    if (SUCCEEDED(getUI(&ui))) {
        ui->ShareLink(url, title);
    } else {
        // If the HippoUI client isn't running, we start it. We check periodically
        // in a timeout until it actually starts
        if (!window_) {
            if (!createWindow()) {
                hippoDebug(L"Can't create window to receive timer messages");
                return E_FAIL;
            }
        }

        if (!spawnUI()) {
            hippoDebug(L"Couldn't start DumbHippo client");
            return E_FAIL;
        }

        uiWaitCount_ = UI_WAIT_MAX_TRIES;

        // Stash away the URL and Title to use when the UI shows up
        shareUrl_ = url;
        shareTitle_ = title;

        SetTimer(window_, UI_WAIT, UI_WAIT_INTERVAL, NULL);
        updateCommandEnabled();
    }

    return S_OK;

    
    HippoPtr<IUnknown> unknown;

    return S_OK;
}

/////////////////////////////////////////////////////////////////////

void
HippoToolbarAction::clearSite()
{
    if (site_) {
        site_ = NULL;
        browser_ = NULL;
    }
}

bool 
HippoToolbarAction::registerWindowClass()
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
HippoToolbarAction::createWindow()
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

    hippoSetWindowData<HippoToolbarAction>(window_, this);

    return true;
}

// Check first for a non-debug instance of HippoUI, then for the debug instance
// We favor the non-debug instance to let people use DumbHippo while hacking it.
HRESULT
HippoToolbarAction::getUI(IHippoUI **ui)
{
    HippoPtr<IUnknown> unknown;

    if (SUCCEEDED(GetActiveObject(CLSID_HippoUI, NULL, &unknown)) &&
        SUCCEEDED(unknown->QueryInterface<IHippoUI>(ui)))
            return S_OK;

    unknown = NULL;
    if (SUCCEEDED(GetActiveObject(CLSID_HippoUI_Debug, NULL, &unknown)) &&
        SUCCEEDED(unknown->QueryInterface<IHippoUI>(ui)))
            return S_OK;

    return E_FAIL;
}

// Start up the HippoUI process. We assume it's in the same directory 
// as the HippoExplorer DLL
bool
HippoToolbarAction::spawnUI()
{
    HINSTANCE module = GetModuleHandle(L"HippoExplorer.dll");
    if (!module)
        return false;

    WCHAR fileBuf[MAX_PATH];
    if (!GetModuleFileName(module, fileBuf, sizeof(fileBuf) / sizeof(fileBuf[0])))
        false;

    for (size_t i = wcslen(fileBuf); i > 0; i--)
        if (fileBuf[i - 1] == '\\')
            break;

    if (i == 0)  // No \ in path?
        return false;

    if (FAILED(StringCchCopy(fileBuf + i, MAX_PATH - i, L"HippoUI.exe")))
        return false;

    return _wspawnl(_P_NOWAIT, fileBuf, L"HippoUI", NULL) != -1;
}

// The periodic time called while waiting for the UI to start
void 
HippoToolbarAction::uiWaitTimer()
{
    HippoPtr<IHippoUI> ui;

    if (!SUCCEEDED(getUI(&ui))) {
        uiWaitCount_--;
        if (uiWaitCount_ == 0) {
            stopUIWait();
            hippoDebug(L"Could not start DumbHippo client.");
        }

        return;
    }

    // Stop the timer before invoking the remote shareLink to avoid
    // reentrancy problems
    HippoBSTR url = shareUrl_;
    HippoBSTR title = shareTitle_;
    stopUIWait();

    ui->ShareLink(url, title);

    return;
}

// Tell the container in which we are embedded to recheck command
// sensitivity; the actual new value of sensitivity is conveyed by
// IOleCommandTarget::QueryStatus()
void
HippoToolbarAction::updateCommandEnabled()
{
    HippoQIPtr<IOleCommandTarget> frameTarget(site_);
    if (frameTarget) {
        frameTarget->Exec(NULL, OLECMDID_UPDATECOMMANDS, 0, NULL, NULL);
    }
}

void
HippoToolbarAction::stopUIWait() 
{
    KillTimer(window_, UI_WAIT);
    shareUrl_ = NULL;
    shareTitle_ = NULL;

    updateCommandEnabled();
}

LRESULT CALLBACK 
HippoToolbarAction::windowProc(HWND   window,
                               UINT   message,
                               WPARAM wParam,
                               LPARAM lParam)
{
    HippoToolbarAction *toolbarAction = hippoGetWindowData<HippoToolbarAction>(window);
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
