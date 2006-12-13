/* hippo-bridged-ipc-controller.cpp: Wrapper for HippoIpcListener that proxies across threads
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoipc.h"
#include "HippoThreadBridge.h"
#include "HippoUtil.h"
#include "hippo-ipc.h"
#include "hippo-bridged-ipc-listener.h"
#include "hippo-serialized-listener.h"

class HippoBridgedIpcListenerImpl : public HippoBridgedIpcListener, private HippoThreadBridge
{
public:
    HippoBridgedIpcListenerImpl(HippoIpcListener *inner);

    virtual void onConnect();
    virtual void onDisconnect();

    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant);
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial);

    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying);

    virtual HippoIpcListener *getInner();

protected:
    virtual void *call(void *param);
    virtual void cancel(void *param);

    HippoIpcListener *inner_;
};

HippoBridgedIpcListener *
HippoBridgedIpcListener::createInstance(HippoIpcListener *inner)
{
    return new HippoBridgedIpcListenerImpl(inner);
}

HippoBridgedIpcListenerImpl::HippoBridgedIpcListenerImpl(HippoIpcListener *inner)
{
    inner_ = inner;
}

void
HippoBridgedIpcListenerImpl::onConnect()
{
    HippoSerializedListener *serialized = new HippoSerializedListener();
    serialized->onConnect();

    invokeAsync((void *)serialized);
}

void 
HippoBridgedIpcListenerImpl::onDisconnect()
{
    HippoSerializedListener *serialized = new HippoSerializedListener();
    serialized->onDisconnect();

    invokeAsync((void *)serialized);
}

void
HippoBridgedIpcListenerImpl::onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant)
{
    HippoSerializedListener *serialized = new HippoSerializedListener();
    serialized->onUserJoin(endpoint, chatId, userId, participant);

    invokeAsync((void *)serialized);
}

void 
HippoBridgedIpcListenerImpl::onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    HippoSerializedListener *serialized = new HippoSerializedListener();
    serialized->onUserLeave(endpoint, chatId, userId);

    invokeAsync((void *)serialized);
}

void 
HippoBridgedIpcListenerImpl::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial)
{
    HippoSerializedListener *serialized = new HippoSerializedListener();
    serialized->onMessage(endpoint, chatId, userId, message, timestamp, serial);

    invokeAsync((void *)serialized);
}

void 
HippoBridgedIpcListenerImpl::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    HippoSerializedListener *serialized = new HippoSerializedListener();
    serialized->userInfo(endpoint, userId, name, smallPhotoUrl, currentSong, currentArtist, musicPlaying);

    invokeAsync((void *)serialized);
}

void *
HippoBridgedIpcListenerImpl::call(void *param)
{
    HippoSerializedListener *serialized = (HippoSerializedListener *)param;
    serialized->invoke(inner_); 

    delete serialized;

    return NULL;
}

void
HippoBridgedIpcListenerImpl::cancel(void *param)
{
    HippoSerializedListener *serialized = (HippoSerializedListener *)param;

    delete serialized;
}

HippoIpcListener *
HippoBridgedIpcListenerImpl::getInner()
{
    return inner_;
}
