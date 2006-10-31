/* HippoListenerProxy.cpp: track and handle requests for one IHippoUIListener
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include "HippoGSignal.h"
#include "HippoListenerProxy.h"
#include "HippoUtil.h"
#include "HippoUIUtil.h"

#include <vector>

class HippoListenerProxyImpl : public HippoListenerProxy 
{
public:
    HippoListenerProxyImpl(HippoDataCache *dataCache, IHippoUIListener *listener);

    virtual UINT64 getId();
    virtual void unregister();
    virtual UINT64 registerEndpoint();
    virtual void unregisterEndpoint(UINT64 endpoint);

    virtual bool hasEndpoint(UINT64 endpoint);

    virtual void joinChatRoom(UINT64 endpoint, BSTR chatId, BOOL participant);
    virtual void leaveChatRoom(UINT64 endpoint, BSTR chatId);

    HIPPO_DECLARE_REFCOUNTING;

private:
    ~HippoListenerProxyImpl();
    HippoEndpointProxy *getEndpointProxy(UINT64 endpoint);

    void unregisterAllEndpoints();

    void onUserJoin(HippoChatRoom *room, HippoEntity *entity, gboolean participant, HippoEndpointProxy *proxy);
    void onUserLeave(HippoChatRoom *room, HippoEntity *entity, HippoEndpointProxy *proxy);
    void onMessage(HippoChatRoom *room, HippoChatMessage *message, HippoEndpointProxy *proxy);
    void onEntityInfo(HippoEntity *entity, HippoEndpointProxy *proxy);

    void onConnectedChanged(gboolean connected);

    HippoDataCache *dataCache_;
    DWORD refCount_;
    UINT64 id_;
    static UINT64 lastId_;
    HippoPtr<IHippoUIListener> listener_;
    std::vector<HippoEndpointProxy *> endpoints_;
    GConnection1<void, gboolean> connectedChanged_;
};

UINT64 HippoListenerProxyImpl::lastId_ = 0;

HippoListenerProxy *
HippoListenerProxy::createInstance(HippoDataCache *dataCache, IHippoUIListener *listener)
{
    return new HippoListenerProxyImpl(dataCache, listener);
}

HippoListenerProxyImpl::HippoListenerProxyImpl(HippoDataCache *dataCache, IHippoUIListener *listener)
{
    id_ = ++lastId_;
    dataCache_ = dataCache;
    listener_ = listener;

    connectedChanged_.connect(G_OBJECT(hippo_data_cache_get_connection(dataCache_)), "connected-changed", 
                              slot(this, &HippoListenerProxyImpl::onConnectedChanged));
}

HippoListenerProxyImpl::~HippoListenerProxyImpl()
{
    unregister();
}

UINT64 
HippoListenerProxyImpl::getId()
{
    return id_;
}

void 
HippoListenerProxyImpl::unregister()
{
    unregisterAllEndpoints();
    listener_ = NULL;
}

UINT64 
HippoListenerProxyImpl::registerEndpoint()
{
    HippoEndpointProxy *endpointProxy = hippo_endpoint_proxy_new(dataCache_);
    endpoints_.push_back(endpointProxy);

    GConnection3<void, HippoChatRoom *, HippoEntity *, gboolean>::named_connect(
        G_OBJECT(endpointProxy), 
        "hippo-listener-proxy-user-join", "user-join", 
        bind(slot(this, &HippoListenerProxyImpl::onUserJoin), endpointProxy));
    GConnection2<void, HippoChatRoom *, HippoEntity *>::named_connect(
        G_OBJECT(endpointProxy), 
        "hippo-listener-proxy-user-leave", "user-leave", 
        bind(slot(this, &HippoListenerProxyImpl::onUserLeave), endpointProxy));
    GConnection2<void, HippoChatRoom *, HippoChatMessage *>::named_connect(
        G_OBJECT(endpointProxy), "hippo-listener-proxy-message", "message", 
        bind(slot(this, &HippoListenerProxyImpl::onMessage), endpointProxy));
    GConnection1<void, HippoEntity *>::named_connect(
        G_OBJECT(endpointProxy), "hippo-listener-proxy-entity-info", "entity-info", 
        bind(slot(this, &HippoListenerProxyImpl::onEntityInfo), endpointProxy));

    return hippo_endpoint_proxy_get_id(endpointProxy);
}

void 
HippoListenerProxyImpl::unregisterEndpoint(UINT64 endpoint)
{
    for (std::vector<HippoEndpointProxy *>::iterator i = endpoints_.begin();
         i != endpoints_.end();
         i++)
    {
        if (hippo_endpoint_proxy_get_id(*i) == endpoint) {
            hippo_endpoint_proxy_unregister(*i);
            g_object_unref(*i);
            endpoints_.erase(i);

            return;
        }
    }
}

bool 
HippoListenerProxyImpl::hasEndpoint(UINT64 endpoint)
{
    return getEndpointProxy(endpoint) != 0;
}

void 
HippoListenerProxyImpl::joinChatRoom(UINT64 endpoint, BSTR chatId, BOOL participant)
{
    HippoEndpointProxy *endpointProxy = getEndpointProxy(endpoint);
    if (endpointProxy) {
        HippoUStr chatIdU(chatId);

        hippo_endpoint_proxy_join_chat_room(endpointProxy, chatIdU.c_str(), 
                                            participant ? HIPPO_CHAT_STATE_PARTICIPANT : HIPPO_CHAT_STATE_VISITOR);
    }
}

void
HippoListenerProxyImpl::leaveChatRoom(UINT64 endpoint, BSTR chatId)
{
    HippoEndpointProxy *endpointProxy = getEndpointProxy(endpoint);
    if (endpointProxy) {
        HippoUStr chatIdU(chatId);

        hippo_endpoint_proxy_leave_chat_room(endpointProxy, chatIdU.c_str());
    }
}
    
HippoEndpointProxy *
HippoListenerProxyImpl::getEndpointProxy(UINT64 endpoint)
{
    for (std::vector<HippoEndpointProxy *>::iterator i = endpoints_.begin();
         i != endpoints_.end();
         i++)
    {
        if (hippo_endpoint_proxy_get_id(*i) == endpoint)
            return *i;
    }

    return NULL;
}

void 
HippoListenerProxyImpl::unregisterAllEndpoints()
{
    while (!endpoints_.empty())
        unregisterEndpoint(hippo_endpoint_proxy_get_id(endpoints_.front()));
}

void 
HippoListenerProxyImpl::onUserJoin(HippoChatRoom *room, HippoEntity *entity, gboolean participant, HippoEndpointProxy *endpointProxy)
{
    HippoBSTR chatId = HippoBSTR::fromUTF8(hippo_chat_room_get_id(room), -1);
    HippoBSTR entityId = HippoBSTR::fromUTF8(hippo_entity_get_guid(entity), -1);

    if (listener_)
        listener_->OnUserJoin(hippo_endpoint_proxy_get_id(endpointProxy), chatId, entityId,
                              participant ? TRUE : FALSE);
}

void 
HippoListenerProxyImpl::onUserLeave(HippoChatRoom *room, HippoEntity *entity, HippoEndpointProxy *endpointProxy)
{
    HippoBSTR chatId = HippoBSTR::fromUTF8(hippo_chat_room_get_id(room), -1);
    HippoBSTR entityId = HippoBSTR::fromUTF8(hippo_entity_get_guid(entity), -1);

    if (listener_)
        listener_->OnUserLeave(hippo_endpoint_proxy_get_id(endpointProxy), 
                               chatId, entityId);
}

void 
HippoListenerProxyImpl::onMessage(HippoChatRoom *room, HippoChatMessage *message, HippoEndpointProxy *endpointProxy)
{
    HippoPerson *sender = hippo_chat_message_get_person(message);

    HippoBSTR chatId = HippoBSTR::fromUTF8(hippo_chat_room_get_id(room), -1);
    HippoBSTR entityId = HippoBSTR::fromUTF8(hippo_entity_get_guid(HIPPO_ENTITY(sender)), -1);
    HippoBSTR text = HippoBSTR::fromUTF8(hippo_chat_message_get_text(message), -1);

    if (listener_)
        listener_->OnMessage(hippo_endpoint_proxy_get_id(endpointProxy),
                             chatId,
                             entityId,
                             text,
                             hippo_chat_message_get_timestamp(message),
                             hippo_chat_message_get_serial(message));
}

void 
HippoListenerProxyImpl::onEntityInfo(HippoEntity *entity, HippoEndpointProxy *endpointProxy)
{
    HippoPerson *person = HIPPO_IS_PERSON(entity) ? HIPPO_PERSON(entity) : NULL;
    HippoBSTR entityId = HippoBSTR::fromUTF8(hippo_entity_get_guid(entity), -1);
    HippoBSTR name = HippoBSTR::fromUTF8(hippo_entity_get_name(entity), -1);
    HippoBSTR smallPhotoUrl = HippoBSTR::fromUTF8(hippo_entity_get_photo_url(entity), -1);
    HippoBSTR currentSong;
    HippoBSTR currentArtist;
    BOOL musicPlaying = FALSE;

    if (person) {
        currentSong.setUTF8(hippo_person_get_current_song(person));
        currentArtist.setUTF8(hippo_person_get_current_artist(person));
        musicPlaying = hippo_person_get_music_playing(person);
    }

    if (listener_)
        listener_->UserInfo(hippo_endpoint_proxy_get_id(endpointProxy),
                              entityId, name, smallPhotoUrl, currentSong, currentArtist, musicPlaying);
}

void 
HippoListenerProxyImpl::onConnectedChanged(gboolean connected)
{
    if (connected) {
        if (listener_)
            listener_->OnConnect();
    } else {
        unregisterAllEndpoints();
        if (listener_)
            listener_->OnDisconnect();   
    }
}

HIPPO_DEFINE_REFCOUNTING(HippoListenerProxyImpl)
