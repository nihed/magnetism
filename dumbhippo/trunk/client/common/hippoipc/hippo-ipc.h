/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
typedef unsigned long long HippoIpcId;

class HippoIpcListener {
public:
    virtual void onUserJoin(HippoIpcId dest, const char *chatId, const char *userId) = 0;
    virtual void onUserLeave(HippoIpcId dest, const char *chatId, const char *userId) = 0;
    virtual void onMessage(HippoIpcId dest, const char *chatId, const char *userId, const char *message, double timestamp, long serial) = 0;
    virtual void onReconnect(HippoIpcId dest, const char *chatId) = 0;

    virtual void userInfo(HippoIpcId dest, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying) = 0;
    
protected:
    virtual ~HippoIpcListener() {}
};

class HippoIpcMethods {
public:
    virtual void disconnect(HippoIpcId source) = 0;
    
    virtual void joinChatRoom(HippoIpcId source, const char *chatId, bool participant) = 0;
    virtual void leaveChatRoom(HippoIpcId source, const char *chatId) = 0;
    
    virtual void sendChatMessage(const char *chatId, const char *text) = 0;
    virtual void showChatWindow(const char *chatId) = 0;
    
protected:
    virtual ~HippoIpcMethods() {};
};

class HippoIpcProvider : public HippoIpcMethods {
public:
    virtual HippoIpcId connect() = 0;
    virtual void setListener(HippoIpcListener *listener) = 0;
};

class HippoIpcController : public HippoIpcMethods {
public:
    static HippoIpcController *createInstance(HippoIpcProvider *provider);

    virtual HippoIpcId connect(HippoIpcListener *listener) = 0;
    virtual void addListener(HippoIpcListener *listener) = 0;
    virtual void removeListener(HippoIpcListener *listener) = 0;
};

class HippoIpcLocator {
    virtual HippoIpcController *getController(const char *url);
    
private:
    virtual ~HippoIpcLocator();
};
