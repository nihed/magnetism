/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-serialized-listener.h"

#include <string>

class HippoSerializedListenerArgs
{
public:
    virtual ~HippoSerializedListenerArgs() {};
    virtual void invoke(HippoIpcListener *listener) = 0;
};

class HippoSerializedListenerOnConnect : public HippoSerializedListenerArgs
{
public:
    void invoke(HippoIpcListener *listener);
};

void
HippoSerializedListenerOnConnect::invoke(HippoIpcListener *listener)
{
    listener->onConnect();
}

class HippoSerializedListenerOnDisconnect : public HippoSerializedListenerArgs
{
public:
    HippoSerializedListenerOnDisconnect() {
    }

    void invoke(HippoIpcListener *listener);
};

void
HippoSerializedListenerOnDisconnect::invoke(HippoIpcListener *listener)
{
    listener->onDisconnect();
}

class HippoSerializedListenerOnUserJoin : public HippoSerializedListenerArgs
{
public:
    HippoSerializedListenerOnUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant) {
        endpoint_ = endpoint;
        chatId_ = chatId;
        userId_ = userId;
        participant_ = participant;
    }

    void invoke(HippoIpcListener *listener);

private:
    HippoEndpointId endpoint_;
    std::string chatId_;
    std::string userId_;
    bool participant_;

};

void
HippoSerializedListenerOnUserJoin::invoke(HippoIpcListener *listener)
{
    listener->onUserJoin(endpoint_, chatId_.c_str(), userId_.c_str(), participant_);
}

class HippoSerializedListenerOnUserLeave : public HippoSerializedListenerArgs
{
public:
    HippoSerializedListenerOnUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId) {
        endpoint_ = endpoint;
        chatId_ = chatId;
        userId_ = userId;
    }

    void invoke(HippoIpcListener *listener);

private:
    HippoEndpointId endpoint_;
    std::string chatId_;
    std::string userId_;

};

void
HippoSerializedListenerOnUserLeave::invoke(HippoIpcListener *listener)
{
    listener->onUserLeave(endpoint_, chatId_.c_str(), userId_.c_str());
}

class HippoSerializedListenerOnMessage : public HippoSerializedListenerArgs
{
public:
    HippoSerializedListenerOnMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial) {
        endpoint_ = endpoint;
        chatId_ = chatId;
        userId_ = userId;
        message_ = message;
        timestamp_ = timestamp;
        serial_ = serial;
    }

    void invoke(HippoIpcListener *listener);

private:
    HippoEndpointId endpoint_;
    std::string chatId_;
    std::string userId_;
    std::string message_;
    double timestamp_;
    long serial_;
};

void
HippoSerializedListenerOnMessage::invoke(HippoIpcListener *listener)
{
    listener->onMessage(endpoint_, chatId_.c_str(), userId_.c_str(), message_.c_str(), timestamp_, serial_);
}

class HippoSerializedListenerUserInfo : public HippoSerializedListenerArgs
{
public:
    HippoSerializedListenerUserInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying) {
        endpoint_ = endpoint;
        userId_ = userId;
        name_ = name;
        smallPhotoUrl_ = smallPhotoUrl;
        currentSong_ = currentSong;
        currentArtist_ = currentArtist;
        musicPlaying_ = musicPlaying;
    }

    void invoke(HippoIpcListener *listener);

private:
    HippoEndpointId endpoint_;
    std::string userId_;
    std::string name_;
    std::string smallPhotoUrl_;
    std::string currentSong_;
    std::string currentArtist_;
    bool musicPlaying_;
};

void
HippoSerializedListenerUserInfo::invoke(HippoIpcListener *listener)
{
    listener->userInfo(endpoint_, userId_.c_str(), name_.c_str(), smallPhotoUrl_.c_str(), 
                       currentSong_.c_str(), currentArtist_.c_str(), musicPlaying_);
}

HippoSerializedListener::HippoSerializedListener()
{
    args_ = 0;
}

HippoSerializedListener::~HippoSerializedListener()
{
    clear();
}

void
HippoSerializedListener::invoke(HippoIpcListener *listener)
{
    if (args_)
        args_->invoke(listener);
}

void 
HippoSerializedListener::onConnect() 
{
    clear();
    args_ = new HippoSerializedListenerOnConnect();
}

void 
HippoSerializedListener::onDisconnect() 
{
    clear();
    args_ = new HippoSerializedListenerOnDisconnect();
}

void
HippoSerializedListener::onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant)
{
    clear();
    args_ = new HippoSerializedListenerOnUserJoin(endpoint, chatId, userId, participant);
}

void 
HippoSerializedListener::onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    clear();
    args_ = new HippoSerializedListenerOnUserLeave(endpoint, chatId, userId);
}

void 
HippoSerializedListener::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial)
{
    clear();
    args_ = new HippoSerializedListenerOnMessage(endpoint, chatId, userId, message, timestamp, serial);
}

void 
HippoSerializedListener::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    clear();
    args_ = new HippoSerializedListenerUserInfo(endpoint, userId, name, smallPhotoUrl, currentSong, currentArtist, musicPlaying);
}

void
HippoSerializedListener::clear()
{
    if (args_) {
        delete args_;
        args_ = 0;
    }
}