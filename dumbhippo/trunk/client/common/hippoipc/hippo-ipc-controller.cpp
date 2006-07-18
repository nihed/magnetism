#include "hippo-ipc.h"
#include <vector>

class HippoIpcControllerSource {
public:
    HippoIpcControllerSource(HippoIpcListener *listener, HippoIpcId id) {
	listener_ = listener;
	id_ = id;
    }

    HippoIpcListener *getListener() { return listener_; }
    HippoIpcId getId() { return id_; }

private:
    HippoIpcListener *listener_;
    HippoIpcId id_;
};

class HippoIpcControllerImpl : public HippoIpcController, HippoIpcListener {
public:
    HippoIpcControllerImpl(HippoIpcProvider *provider);
    virtual ~HippoIpcControllerImpl();

    virtual void disconnect(HippoIpcId source);
    
    virtual void joinChatRoom(HippoIpcId source, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoIpcId source,const char *chatId);
    virtual void showChatWindow(const char *chatId);
    virtual void sendChatMessage(const char *chatId, const char *text);
    
    virtual void addListener(HippoIpcListener *listener);
    virtual void removeListener(HippoIpcListener *listener);
    virtual HippoIpcId connect(HippoIpcListener *listener);
    
    // HippoIpcListener methods
    virtual void onUserJoin(HippoIpcId dest, const char *chatId, const char *userId);
    virtual void onUserLeave(HippoIpcId dest, const char *chatId, const char *userId);
    virtual void onMessage(HippoIpcId dest, const char *chatId, const char *userId, const char *message,double timestamp,long serial);
    virtual void onReconnect(HippoIpcId dest, const char *chatId);

    virtual void userInfo(HippoIpcId dest, const char *userId, const char *name, const char *smallPhotoUrl, const char *arrangementName, const char *artistName, bool musicPlaying);
    
private:
    HippoIpcProvider *provider_;
    
    std::vector<HippoIpcControllerSource> sources_;
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

HippoIpcId 
HippoIpcControllerImpl::connect(HippoIpcListener *listener)
{
    HippoIpcId id = provider_->connect();

    sources_.push_back(HippoIpcControllerSource(listener, id));

    return id;
}

void
HippoIpcControllerImpl::disconnect(HippoIpcId source)
{
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getId() == source) {
	    sources_.erase(i);
	    provider_->disconnect(source);
	    break;
	}
    }
}

void 
HippoIpcControllerImpl::joinChatRoom(HippoIpcId source, const char *chatId, bool participant)
{
    provider_->joinChatRoom(source, chatId, participant);
}

void 
HippoIpcControllerImpl::leaveChatRoom(HippoIpcId source,const char *chatId)
{
    provider_->leaveChatRoom(source, chatId);
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
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getListener() == listener) {
	    sources_.erase(i);
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
HippoIpcControllerImpl::onUserJoin(HippoIpcId dest, const char *chatId, const char *userId)
{
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getId() == dest) {
	    i->getListener()->onUserJoin(dest, chatId, userId);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onUserLeave(HippoIpcId dest, const char *chatId, const char *userId)
{
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getId() == dest) {
	    i->getListener()->onUserLeave(dest, chatId, userId);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onMessage(HippoIpcId dest, const char *chatId, const char *userId, const char *message, double timestamp, long serial)
{
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getId() == dest) {
	    i->getListener()->onMessage(dest, chatId, userId, message, timestamp, serial);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::onReconnect(HippoIpcId dest, const char *chatId)
{
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getId() == dest) {
	    i->getListener()->onReconnect(dest, chatId);
	    break;
	}
    }
}

void
HippoIpcControllerImpl::userInfo(HippoIpcId dest, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    for (std::vector<HippoIpcControllerSource>::iterator i = sources_.begin(); i != sources_.end(); i++) {
	if (i->getId() == dest) {
	    i->getListener()->userInfo(dest, userId, name, smallPhotoUrl, currentSong, currentArtist, musicPlaying);
	    break;
	}
    }
}
