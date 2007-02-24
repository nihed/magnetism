/* hippo-com-ipc-provider.h: HippoIpcProvider implementation via COM for Windows
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoipc.h"
#include "hippo-com-ipc-provider.h"
#include "hippo-com-ipc-hub.h"
#include "HippoUtil.h"
#include "HippoDispatchableObject.h"
#include "hippo-serialized-listener.h"

#include <string>

class HippoComIpcProviderImpl : 
    public HippoComIpcProvider,
    public HippoDispatchableObject<IHippoUIListener, HippoComIpcProviderImpl>,
    public HippoComIpcListener
{
public:
    HippoComIpcProviderImpl(const char *serverName);
    virtual ~HippoComIpcProviderImpl();

    // HippoIpcProvider methods
    virtual HippoEndpointId registerEndpoint();
    virtual void setListener(HippoIpcListener *listener);

    virtual void unregisterEndpoint(HippoEndpointId endpoint);

    virtual void setWindowId(HippoEndpointId endpoint, HippoWindowId windowId);

    virtual void joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoEndpointId endpoint, const char *chatId);
    
    virtual void sendChatMessage(const char *chatId, const char *text, int sentiment);
    virtual void showChatWindow(const char *chatId);
    virtual void ref();
    virtual void unref();

    // IHippoUIListener methods
    STDMETHODIMP OnConnect(void);
    STDMETHODIMP OnDisconnect(void);
    STDMETHODIMP OnUserJoin(UINT64 endpointId, BSTR chatId, BSTR userId, BOOL participant);
    STDMETHODIMP OnUserLeave(UINT64 endpointId, BSTR chatId, BSTR userId);
    STDMETHODIMP OnMessage(UINT64 endpointId, BSTR chatId, BSTR userId, BSTR message, int sentiment, double timestamp, int serial);
    STDMETHODIMP UserInfo(UINT64 endpointId, BSTR userId, BSTR name, BSTR smallPhotoUrl, 
                          BSTR currentSong, BSTR currentArtist, BOOL musicPlaying);

    // HippoComIpcListener methods
    virtual void uiStarted();

    static ITypeInfo *getTypeInfo();

    friend class HippoComIpcAsyncListenerTask;

private:
    HippoIpcListener *createAsyncNotification();
    bool pingUI();
    void checkForUI();

    std::string serverName_;
    HippoComIpcHub *hub_;
    HippoIpcListener *listener_;
    UINT64 listenerId_;
    HippoPtr<IHippoUI> ui_;
};

class HippoComIpcAsyncListenerTask : public HippoThreadTask, public HippoSerializedListener
{
public:
    HippoComIpcAsyncListenerTask(HippoComIpcProviderImpl *provider) {
        provider_ = provider;
        provider_->ref();
    }
    virtual HippoComIpcAsyncListenerTask::~HippoComIpcAsyncListenerTask();

    virtual void call();
    virtual void cancel();

private:
    HippoComIpcProviderImpl *provider_;
};

HippoComIpcAsyncListenerTask::~HippoComIpcAsyncListenerTask()
{
    provider_->unref();
}

void 
HippoComIpcAsyncListenerTask::call() {
    if (provider_->listener_) {
        invoke(provider_->listener_);
    }
    delete(this);
}
 

void HippoComIpcAsyncListenerTask::cancel() {
    delete(this);
}

HippoComIpcProvider *
HippoComIpcProvider::createInstance(const char *serverName)
{
    return new HippoComIpcProviderImpl(serverName);
}

HippoComIpcProviderImpl::HippoComIpcProviderImpl(const char *serverName)
{
    hippoDebugLogU("Created provider for %s", serverName);

    serverName_ = serverName;

    hub_ = HippoComIpcHub::getInstance();
    listener_ = NULL;
    listenerId_ = 0;

    // Get notifications of future UI starts
    hub_->addListener(this);

   // See if there is a UI instance currently running
    checkForUI();
}

HippoComIpcProviderImpl::~HippoComIpcProviderImpl()
{
    hippoDebugLogU("Destroying provider for %s", serverName_.c_str());
    hub_->removeListener(this);

    if (ui_)
        ui_->UnregisterListener(listenerId_);
}

ITypeInfo *
HippoComIpcProviderImpl::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;
    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", __uuidof(IHippoUIListener), &typeInfo, NULL);

    return typeInfo;
}

HippoEndpointId 
HippoComIpcProviderImpl::registerEndpoint()
{
    if (ui_) {
        UINT64 endpointId;
        HRESULT hr = ui_->RegisterEndpoint(listenerId_, &endpointId);
        if (SUCCEEDED(hr)) {
            return endpointId;
        } else {
            return endpointId;
        }
    }

    return 0;
}

void 
HippoComIpcProviderImpl::setListener(HippoIpcListener *listener)
{
    listener_ = listener;
}

void 
HippoComIpcProviderImpl::unregisterEndpoint(HippoEndpointId endpoint)
{
    if (ui_)
        ui_->UnregisterEndpoint(endpoint);
}

void 
HippoComIpcProviderImpl::setWindowId(HippoEndpointId endpoint, HippoWindowId windowId)
{
    // Not implemented for Windows
}

void 
HippoComIpcProviderImpl::joinChatRoom(HippoEndpointId endpoint, const char *chatIdU, bool participant)
{
    if (ui_)
        ui_->JoinChatRoom(endpoint, HippoBSTR::fromUTF8(chatIdU, -1), participant);
}

void 
HippoComIpcProviderImpl::leaveChatRoom(HippoEndpointId endpoint, const char *chatId)
{
    if (ui_)
        ui_->LeaveChatRoom(endpoint, HippoBSTR::fromUTF8(chatId, -1));
}

void 
HippoComIpcProviderImpl::sendChatMessage(const char *chatId, const char *text, int sentiment)
{
    if (ui_)
        ui_->SendChatMessage(HippoBSTR::fromUTF8(chatId, -1), HippoBSTR::fromUTF8(text, -1), sentiment);
}

void 
HippoComIpcProviderImpl::showChatWindow(const char *chatId)
{
    if (ui_)
        ui_->ShowChatWindow(HippoBSTR::fromUTF8(chatId, -1));
}

void 
HippoComIpcProviderImpl::ref()
{
    AddRef();
}

void
HippoComIpcProviderImpl::unref()
{
    Release();
}

STDMETHODIMP
HippoComIpcProviderImpl::OnConnect(void) 
{
    createAsyncNotification()->onConnect();

    return S_OK;
}

STDMETHODIMP
HippoComIpcProviderImpl::OnDisconnect(void)
{
    createAsyncNotification()->onDisconnect();

    return S_OK;
}

STDMETHODIMP
HippoComIpcProviderImpl::OnUserJoin(UINT64 endpointId, BSTR chatId, BSTR userId, BOOL participant)
{
    HippoUStr chatIdU(chatId);
    HippoUStr userIdU(userId);

    createAsyncNotification()->onUserJoin(endpointId, chatIdU.c_str(), userIdU.c_str(), participant ? true : false);

    return S_OK;
}

STDMETHODIMP 
HippoComIpcProviderImpl::OnUserLeave(UINT64 endpointId, BSTR chatId, BSTR userId)
{
    HippoUStr chatIdU(chatId);
    HippoUStr userIdU(userId);
    
    createAsyncNotification()->onUserLeave(endpointId, chatIdU.c_str(), userIdU.c_str());

    return S_OK;
}

STDMETHODIMP
HippoComIpcProviderImpl::OnMessage(UINT64 endpointId, BSTR chatId, BSTR userId, BSTR message, int sentiment, double timestamp, int serial)
{
    HippoUStr chatIdU(chatId);
    HippoUStr userIdU(userId);
    HippoUStr messageU(message);

    createAsyncNotification()->onMessage(endpointId, chatIdU.c_str(), userIdU.c_str(), messageU.c_str(), sentiment, timestamp, serial);

    return S_OK;
}

STDMETHODIMP 
HippoComIpcProviderImpl::UserInfo(UINT64 endpointId, BSTR userId, BSTR name, BSTR smallPhotoUrl, 
                                  BSTR currentSong, BSTR currentArtist, BOOL musicPlaying)
{
    if (!userId || !name || !smallPhotoUrl)
        return E_INVALIDARG;

    HippoUStr userIdU(userId);
    HippoUStr nameU(name);
    HippoUStr smallPhotoUrlU(smallPhotoUrl);
    HippoUStr currentSongU(currentSong ? currentSong : HippoBSTR(L""));
    HippoUStr currentArtistU(currentArtist ? currentArtist : HippoBSTR(L""));

    createAsyncNotification()->userInfo(endpointId, userIdU.c_str(), nameU.c_str(), smallPhotoUrlU.c_str(),
                                        currentSongU.c_str(), currentArtistU.c_str(), musicPlaying ? true : false);

    return S_OK;
}

void 
HippoComIpcProviderImpl::uiStarted()
{
    if (ui_) {
        if (pingUI())
        // If the old UI is still there, nothing to do
            return;

        ui_ = NULL;
        listenerId_ = 0;

        // Notify that the old UI has gone away
        createAsyncNotification()->onDisconnect();
    }

    checkForUI();

    if (ui_) {
        // Notify that we have a new UI
        createAsyncNotification()->onConnect();
    }
}

void 
HippoComIpcProviderImpl::checkForUI()
{
    hub_->getUI(serverName_.c_str(), &ui_);
    if (ui_) {
        HRESULT hr = ui_->RegisterListener(this, &listenerId_);
        if (FAILED(hr) && listenerId_ == 0) {
            ui_ = NULL;
            listenerId_ = 0;
        }
    }
}

bool
HippoComIpcProviderImpl::pingUI()
{
    HippoBSTR currentName;
    HRESULT hr = ui_->GetServerName(&currentName);
    if (SUCCEEDED(hr) && currentName.m_str) {
        HippoUStr currentNameU(currentName);
        if (strcmp(currentNameU.c_str(), serverName_.c_str()) == 0)
            return true;
    }

    return false;
}

HippoIpcListener *
HippoComIpcProviderImpl::createAsyncNotification()
{
    HippoComIpcAsyncListenerTask *task = new HippoComIpcAsyncListenerTask(this);
    hub_->doAsync(task);

    return task;
 }
