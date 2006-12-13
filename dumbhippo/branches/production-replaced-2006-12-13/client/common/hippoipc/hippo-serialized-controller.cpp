/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-serialized-controller.h"

#include <string>

class HippoSerializedControllerArgs
{
public:
    virtual ~HippoSerializedControllerArgs() {};

    virtual void invoke(HippoIpcController *controller) = 0;
    virtual HippoEndpointId getResultEndpointId();
};

HippoEndpointId 
HippoSerializedControllerArgs::getResultEndpointId()
{
    return 0;
}

class HippoSerializedControllerAddListener : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerAddListener(HippoIpcListener *listener) {
        listener_ = listener;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    HippoIpcListener *listener_;
};

void
HippoSerializedControllerAddListener::invoke(HippoIpcController *controller)
{
    controller->addListener(listener_);
}

class HippoSerializedControllerRemoveListener : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerRemoveListener(HippoIpcListener *listener) {
        listener_ = listener;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    HippoIpcListener *listener_;
};

void
HippoSerializedControllerRemoveListener::invoke(HippoIpcController *controller)
{
    controller->removeListener(listener_);
}

class HippoSerializedControllerRegisterEndpoint : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerRegisterEndpoint(HippoIpcListener *listener) {
        listener_ = listener;
        result_ = 0;
    }

    virtual void invoke(HippoIpcController *controller);
    virtual HippoEndpointId getResultEndpointId();

private:
    HippoIpcListener *listener_;
    HippoEndpointId result_;
};

void
HippoSerializedControllerRegisterEndpoint::invoke(HippoIpcController *controller)
{
    result_ = controller->registerEndpoint(listener_);
}

HippoEndpointId 
HippoSerializedControllerRegisterEndpoint::getResultEndpointId()
{
    return result_;
}

class HippoSerializedControllerUnregisterEndpoint : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerUnregisterEndpoint(HippoEndpointId endpoint) {
        endpoint_ = endpoint;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    HippoEndpointId endpoint_;
};

void
HippoSerializedControllerUnregisterEndpoint::invoke(HippoIpcController *controller)
{
    controller->unregisterEndpoint(endpoint_);
}

class HippoSerializedControllerJoinChatRoom : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerJoinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant) {
        endpoint_ = endpoint;
        chatId_ = chatId;
        participant_ = participant;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    HippoEndpointId endpoint_;
    std::string chatId_;
    bool participant_;
};

void
HippoSerializedControllerJoinChatRoom::invoke(HippoIpcController *controller)
{
    controller->joinChatRoom(endpoint_, chatId_.c_str(), participant_);
}

class HippoSerializedControllerLeaveChatRoom : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerLeaveChatRoom(HippoEndpointId endpoint, const char *chatId) {
        endpoint_ = endpoint;
        chatId_ = chatId;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    HippoEndpointId endpoint_;
    std::string chatId_;
};

void
HippoSerializedControllerLeaveChatRoom::invoke(HippoIpcController *controller)
{
    controller->leaveChatRoom(endpoint_, chatId_.c_str());
}

class HippoSerializedControllerSendChatMessage : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerSendChatMessage(const char *chatId, const char *text) {
        chatId_ = chatId;
        text_ = text;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    std::string chatId_;
    std::string text_;
};

void
HippoSerializedControllerSendChatMessage::invoke(HippoIpcController *controller)
{
    controller->sendChatMessage(chatId_.c_str(), text_.c_str());
}

class HippoSerializedControllerShowChatWindow : public HippoSerializedControllerArgs
{
public:
    HippoSerializedControllerShowChatWindow(const char *chatId) {
        chatId_ = chatId;
    }

    virtual void invoke(HippoIpcController *controller);

private:
    std::string chatId_;
};

void
HippoSerializedControllerShowChatWindow::invoke(HippoIpcController *controller)
{
    controller->showChatWindow(chatId_.c_str());
}

HippoSerializedController::HippoSerializedController()
{
    args_ = 0;
}

HippoSerializedController::~HippoSerializedController()
{
    clear();
}

void 
HippoSerializedController::invoke(HippoIpcController *controller)
{
    if (args_)
        args_->invoke(controller);
}


HippoEndpointId 
HippoSerializedController::getResultEndpointId()
{
    if (args_)
        return args_->getResultEndpointId();
    else
        return 0;
}

void
HippoSerializedController::addListener(HippoIpcListener *listener)
{
    clear();
    args_ = new HippoSerializedControllerAddListener(listener);
}

void 
HippoSerializedController::removeListener(HippoIpcListener *listener)
{
    clear();
    args_ = new HippoSerializedControllerRemoveListener(listener);
}


HippoEndpointId 
HippoSerializedController::registerEndpoint(HippoIpcListener *listener)
{
    clear();
    args_ = new HippoSerializedControllerRegisterEndpoint(listener);

    return 0; // Dummy value
}

void 
HippoSerializedController::unregisterEndpoint(HippoEndpointId endpoint)
{
    clear();
    args_ = new HippoSerializedControllerUnregisterEndpoint(endpoint);
}

void 
HippoSerializedController::joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant)
{
    clear();
    args_ = new HippoSerializedControllerJoinChatRoom(endpoint, chatId, participant);
}

void 
HippoSerializedController::leaveChatRoom(HippoEndpointId endpoint, const char *chatId)
{
    clear();
    args_ = new HippoSerializedControllerLeaveChatRoom(endpoint, chatId);
}

void
HippoSerializedController::sendChatMessage(const char *chatId, const char *text)
{
    clear();
    args_ = new HippoSerializedControllerSendChatMessage(chatId, text);
}

void 
HippoSerializedController::showChatWindow(const char *chatId)
{
    clear();
    args_ = new HippoSerializedControllerShowChatWindow(chatId);
}

void 
HippoSerializedController::clear()
{
    if (args_) {
        delete args_;
        args_ = NULL;
    }
}
