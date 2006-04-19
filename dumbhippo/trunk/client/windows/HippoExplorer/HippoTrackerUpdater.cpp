/* HippoTrackerUpdater.cpp: Asynchronous notification of changes to a browser's url/title
 *
 * Copyright Red Hat, Inc. 2006
 */

#include "stdafx.h"
#include "Globals.h"
#include "Guid.h"
#include "HippoTrackerUpdater.h"

// Window class for our notification window
static const TCHAR *CLASS_NAME = TEXT("HippoTrackerClass");

HippoTrackerUpdater::HippoTrackerUpdater(IWebBrowser2 *browser)
{
    browser_ = browser;

    InitializeCriticalSection(&criticalSection_);
    updateSemaphore_ = CreateSemaphore(NULL, 0, MAXLONG, NULL);
    needUpdate_ = false;
    shouldExit_ = false;

    registered_ = false;
    dogfoodRegistered_ = false;
    debugRegistered_ = false;

    uiStartedMessage_ = RegisterWindowMessage(TEXT("HippoUIStarted"));

    thread_ = CreateThread(NULL, 0, threadProc, this, 0, NULL);
}

HippoTrackerUpdater::~HippoTrackerUpdater()
{
    EnterCriticalSection(&criticalSection_);

    shouldExit_ = true;
    setNeedUpdate();

    LeaveCriticalSection(&criticalSection_);

    // It's important not to hang indefinitely, but killing our thread
    // is impolite and will leak a small amount of memory. So we wait
    // 250 milliseconds and if that fails, kill it explicitly
    LONG result = WaitForSingleObject(thread_, 250);
    if (result == WAIT_TIMEOUT || result == WAIT_FAILED) {
        TerminateThread(thread_, 0);
    }

    CloseHandle(thread_);
    CloseHandle(updateSemaphore_);
}

void
HippoTrackerUpdater::setInfo(const HippoBSTR &url, const HippoBSTR &name)
{
    EnterCriticalSection(&criticalSection_);

    if (!(url == url_) || !(name == name_)) {
        url_ = url;
        name_ = name;

        setNeedUpdate();
    }

    LeaveCriticalSection(&criticalSection_);
}

bool 
HippoTrackerUpdater::registerWindowClass()
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
HippoTrackerUpdater::createWindow()
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

    hippoSetWindowData<HippoTrackerUpdater>(window_, this);

    return true;
}

void
HippoTrackerUpdater::clearUI()
{
    unregisterBrowser();
 
    if (ui_)
        ui_ = NULL;
    if (dogfoodUi_)
        dogfoodUi_ = NULL;
    if (debugUi_)
        debugUi_ = NULL;
}

void
HippoTrackerUpdater::onUIStarted(void)
{
    clearUI();

    HippoPtr<IUnknown> unknown;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&ui_);

    unknown = NULL;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI_Dogfood, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&dogfoodUi_);

    unknown = NULL;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI_Debug, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&debugUi_);

    registerBrowser();

    // We need to now notify any found UIs of our current state

    EnterCriticalSection(&criticalSection_);
    setNeedUpdate();
    LeaveCriticalSection(&criticalSection_);
}

void
HippoTrackerUpdater::registerBrowser()
{
    if (!registered_ && ui_) {
        registered_ = true;
        HRESULT hr = ui_->RegisterBrowser(browser_, &registerCookie_); // may reenter
        if (FAILED (hr))
            registered_ = false;
    }

    if (!dogfoodRegistered_ && dogfoodUi_) {
        dogfoodRegistered_ = true;
        HRESULT hr = dogfoodUi_->RegisterBrowser(browser_, &dogfoodRegisterCookie_); // may reenter
        if (FAILED (hr))
            dogfoodRegistered_ = false;
    }

    if (!debugRegistered_ && debugUi_) {
        debugRegistered_ = true;
        HRESULT hr = debugUi_->RegisterBrowser(browser_, &debugRegisterCookie_); // may reenter
        if (FAILED (hr))
            debugRegistered_ = false;
    }
}

void
HippoTrackerUpdater::unregisterBrowser()
{
    if (registered_) {
        registered_ = false;
        ui_->UnregisterBrowser(registerCookie_);
    }

    if (dogfoodRegistered_) {
        dogfoodRegistered_ = false;
        dogfoodUi_->UnregisterBrowser(dogfoodRegisterCookie_);
    }

    if (debugRegistered_) {
        debugRegistered_ = false;
        debugUi_->UnregisterBrowser(debugRegisterCookie_);
    }
}

void
HippoTrackerUpdater::setNeedUpdate()
{
    if (!needUpdate_) {
        needUpdate_ = true;
        // Releasing the semaphore wakes up the updater thread
        ReleaseSemaphore(updateSemaphore_, 1, NULL);
    }
}

bool
HippoTrackerUpdater::processUpdate()
{
    EnterCriticalSection(&criticalSection_);

    needUpdate_ = false;

    HippoBSTR &url = url_;
    HippoBSTR &name = name_;
    bool shouldExit = shouldExit_;

    LeaveCriticalSection(&criticalSection_);

    if (shouldExit)
        return true;

    if (url && name) {
        if (registered_)
            ui_->UpdateBrowser(registerCookie_, url, name);
        if (dogfoodRegistered_)
            dogfoodUi_->UpdateBrowser(dogfoodRegisterCookie_, url, name);
        if (debugRegistered_)
            debugUi_->UpdateBrowser(debugRegisterCookie_, url, name);
    }

    return false;
 }
 
void
HippoTrackerUpdater::run()
{
    // Create the window where we'll receive notifications of UI instances starting
    if (!createWindow()) {
        hippoDebugLogW(L"Failed to create HippoTrackerUpdater window");
        return;
    }

    // Passing COINIT_MULTITHREADED should avoid reentrancy to the message loop
    // during our outgoing calls, which would be a good thing; the threading
    // model doesn't matter a lot, however, since we aren't a server
    CoInitializeEx(NULL, COINIT_MULTITHREADED);

    // Check for any UI instances already running
    onUIStarted();

    // Now loop processing updates and notifications
    while (true) {
        DWORD result = MsgWaitForMultipleObjects(1, &updateSemaphore_, FALSE, INFINITE, QS_ALLINPUT);
        if (result == WAIT_OBJECT_0) { // Our semaphore was triggered
            if (processUpdate())
                break;
        } else if (result == WAIT_OBJECT_0 + 1) { // Received a message
            MSG message;
            if (PeekMessage(&message, NULL, 0, 0, TRUE)) {
                TranslateMessage(&message);
                DispatchMessage(&message);
            }
        } else {
            hippoDebugLogW(L"Unexpected result from MsgWaitForMultipleObjects");
            break;
        }
    }

    // The HippoTrackerUpdater is being destroyed, clean up and exit the thread

    unregisterBrowser();

    CoUninitialize();

    DestroyWindow(window_);
    window_ = NULL;

    return;
}

DWORD WINAPI
HippoTrackerUpdater::threadProc(void *data) 
{
    HippoTrackerUpdater *updater = (HippoTrackerUpdater *)data;
    updater->run();

    return 0;
}


LRESULT CALLBACK 
HippoTrackerUpdater::windowProc(HWND   window,
                                UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    HippoTrackerUpdater *updater = hippoGetWindowData<HippoTrackerUpdater>(window);
    if (updater) {
        if (message == updater->uiStartedMessage_) {
            updater->onUIStarted();
            return 0;
        }
    }

    return DefWindowProc(window, message, wParam, lParam);
}
