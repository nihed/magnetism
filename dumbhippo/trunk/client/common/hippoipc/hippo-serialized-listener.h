/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_SERIALIZED_LISTENER_H__
#define __HIPPO_SERIALIZED_LISTENER_H__

#include "hippo-ipc.h"

class HippoSerializedListenerArgs;

/**
 * HippoSerializedListener represents a serialized method call against HippoIpcListener.
 * When you call a method on the HippoSerializedListener object the arguments are saved, 
 * and replayed when you later call invoke() and pass in different HippoIpcListener. 
 *
 * Note that if you call multiple different methods in succession, only the last one is
 * remembered and replayed at the next call to invoke().
 */
class HippoSerializedListener : public HippoIpcListener
{
public:
    HippoSerializedListener();
    ~HippoSerializedListener();

    void invoke(HippoIpcListener *listener);
    virtual void onConnect();
    virtual void onDisconnect();

    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant);
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial);

    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying);

private:
    void clear();

    HippoSerializedListenerArgs *args_;
};

#endif /* __HIPPO_SERIALIZED_LISTENER_H__ */
