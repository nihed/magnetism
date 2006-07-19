/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-ipc.h"
#include <vector>

class HippoIpcControllerEndpoint {
public:
    HippoIpcControllerEndpoint(HippoIpcListener *listener, HippoEndpointId id) {
	listener_ = listener;
	id_ = id;
    }

    HippoIpcListener *getListener() { return listener_; }
    HippoEndpointId getId() { return id_; }

private:
    HippoIpcListener *listener_;
    HippoEndpointId id_;
};

class HippoIpcControllerImpl : public HippoIpcController, HippoIpcListener {
public:
    HippoIpcControllerImpl(HippoIpcProvider *provider);
    virtual ~HippoIpcControllerImpl();

    virtual void disconnect(HippoEndpointId endpoint);
    
    virtual void joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoEndpointId endpoint,const char *chatId);
    virtual void showChatWindow(const char *chatId);
    virtual void sendChatMessage(const char *chatId, const char *text);
    
    virtual void addListener(HippoIpcListener *listener);
    virtual void removeListener(HippoIpcListener *listener);
    virtual HippoEndpointId connect(HippoIpcListener *listener);
    
    // HippoIpcListener methods
    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message,double timestamp,long serial);
    virtual void onReconnect(HippoEndpointId endpoint, const char *chatId);

    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *arrangementName, const char *artistName, bool musicPlaying);
    
private:
    HippoIpcProvider *provider_;
    
    std::vector<HippoIpcControllerEndpoint> endpoints_;
    std::vector<HippoIpcListener *> listeners_;
};

HippoIpcController *
HippoIpcController::createInstance(HippoIpcProvider *provider)
{
    return new HippoIpcControllerImpl(provider);
}

HippoIpcControllerImpl::HippoIpcControllerImpl(HippoIpcProvider *provider)
{
    provider_ = provider;
    provider_->setListener(this);
}

HippoIpcControllerImpl::~HippoIpcControllerImpl()
{
    provider_->setListener(NULL);
}

HippoEndpointId 
HippoIpcControllerImpl::connect(HippoIpcListener *listener)
{
    HippoEndpointId id = provider_->connect();

    endpoints_.push_back(HippoIpcControllerEndpoint(listener, id));

    return id;
}

void
HippoIpcControllerImpl::disconnect(HippoEndpointId endpoint)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getId() == endpoint) {
	    endpoints_.erase(i);
	    provider_->disconnect(endpoint);
	    break;
	}
    }
}

void 
HippoIpcControllerImpl::joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant)
{
    provider_->joinChatRoom(endpoint, chatId, participant);
}

void 
HippoIpcControllerImpl::leaveChatRoom(HippoEndpointId endpoint,const char *chatId)
{
    provider_->leaveChatRoom(endpoint, chatId);
}

void 
HippoIpcControllerImpl::showChatWindow(const char *chatId)
{
    provider_->showChatWindow(chatId);
}

void 
HippoIpcControllerImpl::sendChatMessage(const char *chatId, const char *text)
{
    provider_->sendChatMessage(chatId, text);
}

void 
HippoIpcControllerImpl::addListener(HippoIpcListener *listener)
{
    listeners_.push_back(listener);
}

void 
HippoIpcControllerImpl::removeListener(HippoIpcListener *listener)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getListener() == listener) {
	    endpoints_.erase(i);
	}
    }

    for (std::vector<HippoIpcListener *>::iterator i = listeners_.begin(); i != listeners_.end(); i++) {
	if (*i == listener) {
	    listeners_.erase(i);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getId() == endpoint) {
	    i->getListener()->onUserJoin(endpoint, chatId, userId);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getId() == endpoint) {
	    i->getListener()->onUserLeave(endpoint, chatId, userId);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getId() == endpoint) {
	    i->getListener()->onMessage(endpoint, chatId, userId, message, timestamp, serial);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onReconnect(HippoEndpointId endpoint, const char *chatId)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getId() == endpoint) {
	    i->getListener()->onReconnect(endpoint, chatId);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    for (std::vector<HippoIpcControllerEndpoint>::iterator i = endpoints_.begin(); i != endpoints_.end(); i++) {
	if (i->getId() == endpoint) {
	    i->getListener()->userInfo(endpoint, userId, name, smallPhotoUrl, currentSong, currentArtist, musicPlaying);
	    break;
	}
    }
}
