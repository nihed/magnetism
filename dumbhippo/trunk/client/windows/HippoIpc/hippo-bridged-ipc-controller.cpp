/* hippo-bridged-ipc-controller.cpp: Thread-safe wrapper for HippoIpcController
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoipc.h"
#include "hippo-ipc.h"
#include "hippo-com-ipc-hub.h"
#include "hippo-com-ipc-provider.h"
#include "hippo-bridged-ipc-controller.h"
#include "hippo-bridged-ipc-listener.h"
#include "hippo-serialized-controller.h"
#include "hippo-serialized-listener.h"
#include "HippoThreadLock.h"

#include <vector>

class HippoComIpcControllerTask : public HippoThreadTask
{
public:
    HippoComIpcControllerTask(HippoIpcController *controller, HippoSerializedController *serialized) {
        controller_ = controller;
        serialized_ = serialized;
    }

    virtual void call();
    virtual void cancel();

private:
    HippoIpcController *controller_;
    HippoSerializedController *serialized_;
};

void
HippoComIpcControllerTask::call() 
{
    serialized_->invoke(controller_);
}

void
HippoComIpcControllerTask::cancel() 
{
}

class HippoBridgedIpcControllerImpl : public HippoBridgedIpcController
{
public:
    HippoBridgedIpcControllerImpl(HippoIpcProvider *provider);
    ~HippoBridgedIpcControllerImpl();

    virtual void addListener(HippoIpcListener *listener);
    virtual void removeListener(HippoIpcListener *listener);

    virtual HippoEndpointId registerEndpoint(HippoIpcListener *listener);
    virtual void unregisterEndpoint(HippoEndpointId endpoint);
    
    virtual void joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoEndpointId endpoint, const char *chatId);
    
    virtual void sendChatMessage(const char *chatId, const char *text, int sentiment);
    virtual void showChatWindow(const char *chatId);

private:
    HippoThreadLock lock_;
    HippoComIpcHub *hub_;
    HippoIpcController *inner_;
    std::vector<HippoBridgedIpcListener *> listeners_;
};

HippoBridgedIpcController *
HippoBridgedIpcController::createInstance(HippoIpcProvider *provider)
{
    return new HippoBridgedIpcControllerImpl(provider);
}

HippoBridgedIpcControllerImpl::HippoBridgedIpcControllerImpl(HippoIpcProvider *provider)
{
    hub_ = HippoComIpcHub::getInstance();
    inner_ = HippoIpcController::createInstance(provider);
}

HippoBridgedIpcControllerImpl::~HippoBridgedIpcControllerImpl()
{
    delete inner_;
}

void 
HippoBridgedIpcControllerImpl::addListener(HippoIpcListener *listener)
{
    HippoBridgedIpcListener *bridged = HippoBridgedIpcListener::createInstance(listener);
    {
        HIPPO_WITH_LOCK(&lock_);
        listeners_.push_back(bridged);
    }

    HippoSerializedController serialized;
    serialized.addListener(bridged);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));
}

void 
HippoBridgedIpcControllerImpl::removeListener(HippoIpcListener *listener)
{
    HippoBridgedIpcListener *bridged;

    {
        HIPPO_WITH_LOCK(&lock_);
        for (std::vector<HippoBridgedIpcListener *>::iterator i = listeners_.begin(); i != listeners_.end(); i++) {
            if ((*i)->getInner() == listener) {
                bridged = *i;
                listeners_.erase(i);
                break;
            }
        }
    }

    HippoSerializedController serialized;
    serialized.removeListener(bridged);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));

    delete bridged;
}

HippoEndpointId
HippoBridgedIpcControllerImpl::registerEndpoint(HippoIpcListener *listener)
{
    HippoBridgedIpcListener *bridged;

    {
        HIPPO_WITH_LOCK(&lock_);
        for (std::vector<HippoBridgedIpcListener *>::iterator i = listeners_.begin(); i != listeners_.end(); i++) {
            if ((*i)->getInner() == listener) {
                bridged = *i;
                break;
            }
        }
    }

    HippoSerializedController serialized;
    serialized.registerEndpoint(bridged);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));

    return serialized.getResultEndpointId();
}

void
HippoBridgedIpcControllerImpl::unregisterEndpoint(HippoEndpointId endpoint)
{
    HippoSerializedController serialized;
    serialized.unregisterEndpoint(endpoint);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));
}

void
HippoBridgedIpcControllerImpl::joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant)
{
    HippoSerializedController serialized;
    serialized.joinChatRoom(endpoint, chatId, participant);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));
}

void
HippoBridgedIpcControllerImpl::leaveChatRoom(HippoEndpointId endpoint, const char *chatId)
{
    HippoSerializedController serialized;
    serialized.leaveChatRoom(endpoint, chatId);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));
}

void
HippoBridgedIpcControllerImpl::sendChatMessage(const char *chatId, const char *text, int sentiment)
{
    HippoSerializedController serialized;
    serialized.sendChatMessage(chatId, text, sentiment);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));
}

void
HippoBridgedIpcControllerImpl::showChatWindow(const char *chatId)
{
    HippoSerializedController serialized;
    serialized.showChatWindow(chatId);

    hub_->doSync(&HippoComIpcControllerTask(inner_, &serialized));
}
