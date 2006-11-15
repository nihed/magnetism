/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_SERIALIZED_CONTROLLER_H__
#define __HIPPO_SERIALIZED_CONTROLLER_H__

#include "hippo-ipc.h"

class HippoSerializedControllerArgs;

/**
 * HippoSerializedController represents a serialized method call against HippoIpcController.
 * When you call a method on the HippoSerializedController object the arguments are saved, 
 * and replayed when you later call invoke() and pass in different HippoIpcController. In the 
 * case of registerEndpoint, which has a return value, the return value can be retrieved after 
 * calling invoke() by calling getResultEndpointId();
 *
 * Note that if you call multiple different methods in succession, only the last one is
 * remembered and replayed at the next call to invoke().
 */
class HippoSerializedController : public HippoIpcController
{
public:
    HippoSerializedController();
    ~HippoSerializedController();

    void invoke(HippoIpcController *controller);
    HippoEndpointId getResultEndpointId();

    virtual void addListener(HippoIpcListener *listener);
    virtual void removeListener(HippoIpcListener *listener);

    virtual HippoEndpointId registerEndpoint(HippoIpcListener *listener);
    virtual void unregisterEndpoint(HippoEndpointId endpoint);
    
    virtual void joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoEndpointId endpoint, const char *chatId);
    
    virtual void sendChatMessage(const char *chatId, const char *text);
    virtual void showChatWindow(const char *chatId);

private:
    void clear();
    HippoSerializedControllerArgs *args_;
};

#endif /* __HIPPO_SERIALIZED_CONTROLLER_H__ */
