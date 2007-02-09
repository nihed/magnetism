/* hippo-com-ipc-hub.cpp: Process wide singleton to manage connections to clients
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoipc.h"
#include "HippoUtil.h"
#include "HippoThreadLock.h"
#include "hippo-com-ipc-hub.h"
#include "hippo-com-ipc-locator.h"
#include <vector>

class HippoListenerInfo 
{
public:
    HippoListenerInfo(HippoComIpcListener *listener) {
        listener_ = listener;
        refCount_ = 1;
    }

    HippoComIpcListener *getListener() {
        return listener_;
    }

    void clearListener() {
        listener_ = NULL;
    }

    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

private:
    HippoComIpcListener *listener_;
    DWORD refCount_;
};

HIPPO_DEFINE_REFCOUNTING(HippoListenerInfo)

class HippoComIpcHubImpl;

typedef void (HippoComIpcHubImpl::*HippoListenerFunc)(HippoComIpcListener *listener);

class HippoListenerTask : public HippoThreadTask
{
public:
    HippoListenerTask(HippoComIpcHubImpl *hub, HippoListenerInfo *info, HippoListenerFunc func) {
        info_ = info;
        info_->AddRef();
        func_ = func;
    }

    ~HippoListenerTask() {
        info_->Release();
    }

    virtual void call();
    virtual void cancel();

private:
    HippoComIpcHubImpl *hub_;
    HippoListenerInfo *info_;
    HippoListenerFunc func_;
};

void 
HippoListenerTask::call() {
    (hub_->*func_)(info_->getListener());
    delete(this);
}

void HippoListenerTask::cancel() {
    delete(this);
}

class HippoComIpcHubImpl : public HippoComIpcHub, public HippoThreadExecutorHelper
{
public:
    HippoComIpcHubImpl();
    ~HippoComIpcHubImpl();
    
    virtual HippoIpcLocator *getLocator();

    virtual HippoThreadExecutor *getExecutor();
    virtual DWORD getThreadId();
    virtual void doSync(HippoThreadTask *task);
    virtual void doAsync(HippoThreadTask *task);

    virtual void addListener(HippoComIpcListener *listener);
    virtual void removeListener(HippoComIpcListener *listener);

    virtual HRESULT getUI(HippoUIInstance instance, IHippoUI **ui);
    virtual HRESULT getUI(const char *serverName, IHippoUI **ui);

    // HippoThreadExecutorHelper methods
    virtual void init();
    virtual void shutdown();

private:
    HippoThreadExecutor *executor_;
    DWORD threadId_;
    HippoThreadLock lock_;
    std::vector<HippoListenerInfo *> listeners_;

    // This window is used for receiving notifications when a new UI instance is started
    HWND window_;
    UINT uiStartedMessage_;

    HippoComIpcLocator *locator_;

    bool createWindow();
    bool registerWindowClass();

    void doNotifyUIStarted(HippoComIpcListener *listener);

    void onUIStarted();

    HRESULT checkUIInstance(HippoUIInstance instance, BSTR serverName, IHippoUI **ui);

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);
};

// Window class for our notification window
static const TCHAR *CLASS_NAME = TEXT("HippoComIpcHub");
static HINSTANCE dllInstance;
static CRITICAL_SECTION globalCritical;
static HippoComIpcHubImpl *globalInstance;

void
HippoComIpcHub::startup(HINSTANCE instance)
{
    InitializeCriticalSection(&globalCritical);
    dllInstance = instance;
}

void
HippoComIpcHub::shutdown()
{
    if (globalInstance)
        delete globalInstance;
    globalInstance = NULL;

    DeleteCriticalSection(&globalCritical);
    dllInstance = NULL;
}

HippoComIpcHub *
HippoComIpcHub::getInstance()
{
    EnterCriticalSection(&globalCritical);

    if (!globalInstance)
        globalInstance = new HippoComIpcHubImpl();

    LeaveCriticalSection(&globalCritical);

    return globalInstance;
}

HippoComIpcHubImpl::HippoComIpcHubImpl()
{
    locator_ = NULL;

    uiStartedMessage_ = RegisterWindowMessage(TEXT("HippoUIStarted"));

    executor_ = HippoThreadExecutor::createInstance(this);
}

HippoComIpcHubImpl::~HippoComIpcHubImpl()
{
    delete executor_;
}

HippoIpcLocator *
HippoComIpcHubImpl::getLocator()
{
    HIPPO_WITH_LOCK(&lock_);

    if (locator_ == NULL)
        locator_ = HippoComIpcLocator::createInstance();

    return locator_;
}

HippoThreadExecutor *
HippoComIpcHubImpl::getExecutor()
{
    return executor_;
}

DWORD 
HippoComIpcHubImpl::getThreadId()
{
    return threadId_;
}

void 
HippoComIpcHubImpl::doSync(HippoThreadTask *task)
{
    executor_->doSync(task);
}

void 
HippoComIpcHubImpl::doAsync(HippoThreadTask *task)
{
    executor_->doAsync(task);
}

void 
HippoComIpcHubImpl::addListener(HippoComIpcListener *listener)
{
    HIPPO_WITH_LOCK(&lock_);

    listeners_.push_back(new HippoListenerInfo(listener));
}

void 
HippoComIpcHubImpl::removeListener(HippoComIpcListener *listener)
{
    HIPPO_WITH_LOCK(&lock_);

    for (std::vector<HippoListenerInfo *>::iterator i = listeners_.begin();
         i != listeners_.end();
         i++) 
    {
        if ((*i)->getListener() == listener) {
            (*i)->clearListener();
            (*i)->Release();
            listeners_.erase(i);
            break;
        }
    }
}

static const CLSID &
classIdForInstance(HippoUIInstance instance)
{
    switch (instance) {
        case HIPPO_UI_INSTANCE_PRODUCTION:
        default:
            return CLSID_HippoUI;
        case HIPPO_UI_INSTANCE_DEBUG:
            return CLSID_HippoUI_Debug;
        case HIPPO_UI_INSTANCE_DOGFOOD:
            return CLSID_HippoUI_Dogfood;
    }
}

HRESULT 
HippoComIpcHubImpl::getUI(HippoUIInstance instance, IHippoUI **ui)
{
    const CLSID &classId = classIdForInstance(instance);

    // See if there is an instance for this class ID
    HippoPtr<IUnknown> unknown;

    HRESULT hr = GetActiveObject(classId, NULL, &unknown);
    if (FAILED(hr))
        return hr;

    hr = unknown->QueryInterface<IHippoUI>(ui);
    if (FAILED(hr))
        return hr;

    return S_OK;
}

HRESULT 
HippoComIpcHubImpl::checkUIInstance(HippoUIInstance instance, BSTR serverName, IHippoUI **ui)
{
    HRESULT hr;

    HippoPtr<IHippoUI> instanceUI;
    hr = getUI(instance, &instanceUI);
    if (FAILED(hr))
        return hr;

    HippoBSTR instanceName;
    hr = instanceUI->GetServerName(&instanceName);
    if (FAILED(hr))
        return hr;

    if (!instanceName || !serverName || wcscmp(instanceName.m_str, serverName) != 0)
        return E_FAIL;
    
    instanceUI->AddRef();
    *ui = instanceUI;
   
    return S_OK;
}

HRESULT 
HippoComIpcHubImpl::getUI(const char *serverName, IHippoUI **ui)
{
    HippoBSTR serverNameW = HippoBSTR::fromUTF8(serverName);

    if (SUCCEEDED(checkUIInstance(HIPPO_UI_INSTANCE_PRODUCTION, serverNameW, ui)) ||
        SUCCEEDED(checkUIInstance(HIPPO_UI_INSTANCE_DOGFOOD, serverNameW, ui)) ||
        SUCCEEDED(checkUIInstance(HIPPO_UI_INSTANCE_DEBUG, serverNameW, ui)))
        return S_OK;
    else
        return E_FAIL;
}

bool 
HippoComIpcHubImpl::registerWindowClass()
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

    ATOM cls = RegisterClass(&windowClass);
    
    if (cls == 0)
        hippoDebugLastErr(L"Failed to register window class");

    return cls != 0;
}

bool
HippoComIpcHubImpl::createWindow()
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
    if (!window_) {
        hippoDebugLastErr(L"Failed to create window");
        return false;
    }

    hippoSetWindowData<HippoComIpcHubImpl>(window_, this);

    return true;
}

void 
HippoComIpcHubImpl::doNotifyUIStarted(HippoComIpcListener *listener)
{
    listener->uiStarted();
}

void
HippoComIpcHubImpl::onUIStarted(void)
{
    HIPPO_WITH_LOCK(&lock_);

    for (std::vector<HippoListenerInfo *>::iterator i = listeners_.begin();
         i != listeners_.end();
         i++) 
    {
        doAsync(new HippoListenerTask(this, *i, &HippoComIpcHubImpl::doNotifyUIStarted));
    }
}

void
HippoComIpcHubImpl::init()
{
    // Create the window where we'll receive notifications of UI instances starting
    createWindow();

    // Run our communications thread as a single-threaded apartment; this means that
    // incoming calls (from other processes, other threads) are run in our main loop
    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);

    threadId_ = GetCurrentThreadId();
}

void
HippoComIpcHubImpl::shutdown()
{
    CoUninitialize();

    DestroyWindow(window_);
    window_ = NULL;

    return;
}

LRESULT CALLBACK 
HippoComIpcHubImpl::windowProc(HWND   window,
                               UINT   message,
                               WPARAM wParam,
                               LPARAM lParam)
{
    HippoComIpcHubImpl *hub = hippoGetWindowData<HippoComIpcHubImpl>(window);
    if (hub) {
        if (message == hub->uiStartedMessage_) {
            hub->onUIStarted();
            return 0;
        }
    }

    return DefWindowProc(window, message, wParam, lParam);
}
 