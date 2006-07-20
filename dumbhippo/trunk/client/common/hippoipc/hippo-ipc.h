/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IPC_H__
#define __HIPPO_DBUS_IPC_H__

typedef unsigned long long HippoEndpointId;

class HippoIpcListener {
public:
    virtual void onConnect() = 0;
    virtual void onDisconnect() = 0;

    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId) = 0;
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId) = 0;
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial) = 0;

    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying) = 0;
    
protected:
    virtual ~HippoIpcListener() {}
};

class HippoIpcMethods {
public:
    virtual void unregisterEndpoint(HippoEndpointId endpoint) = 0;
    
    virtual void joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant) = 0;
    virtual void leaveChatRoom(HippoEndpointId endpoint, const char *chatId) = 0;
    
    virtual void sendChatMessage(const char *chatId, const char *text) = 0;
    virtual void showChatWindow(const char *chatId) = 0;
    
protected:
    virtual ~HippoIpcMethods() {};
};

class HippoIpcProvider : public HippoIpcMethods {
public:
    virtual HippoEndpointId registerEndpoint() = 0;
    virtual void setListener(HippoIpcListener *listener) = 0;
};

class HippoIpcController : public HippoIpcMethods {
public:
    static HippoIpcController *createInstance(HippoIpcProvider *provider);

    virtual HippoEndpointId registerEndpoint(HippoIpcListener *listener) = 0;
    virtual void addListener(HippoIpcListener *listener) = 0;
    virtual void removeListener(HippoIpcListener *listener) = 0;
};

class HippoIpcLocatorMap;

class HippoIpcLocator {
public:    
    HippoIpcController *getController(const char *url);
    void releaseController(HippoIpcController *controller);

protected:
    HippoIpcLocator();
    virtual ~HippoIpcLocator();

    virtual HippoIpcProvider *createProvider(const char *url) = 0;
        
private:
    HippoIpcLocatorMap *map_;
};

#endif /* __HIPPO_DBUS_IPC_H__ */
