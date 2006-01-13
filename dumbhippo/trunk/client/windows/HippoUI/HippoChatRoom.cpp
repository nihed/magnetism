/* HippoChatRoom.cpp: Object representing a chat room about a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include "stdafx.h"
#include "HippoChatRoom.h"
#include "HippoIM.h"

HippoChatUser::HippoChatUser()
{
}

HippoChatUser::HippoChatUser(BSTR userId, int version, BSTR name) 
{
    userId_ = userId;
    version_ = version;
    name_ = name;
}

HippoChatUser::HippoChatUser(const HippoChatUser &other)
{
    userId_ = other.userId_;
    version_ = other.version_;
    name_ = other.name_;
}

HippoChatMessage::HippoChatMessage()
{
}

HippoChatMessage::HippoChatMessage(const HippoChatUser &user, BSTR text)
    : user_(user)
{
    text_ = text;
}

HippoChatRoom::HippoChatRoom(HippoIM *im, BSTR postId)
{
    im_ = im;
    postId_ = postId;
}

HippoChatRoom::~HippoChatRoom()
{
}

void 
HippoChatRoom::sendMessage(BSTR text)
{
    im_->sendChatRoomMessage(this, text);
}

BSTR
HippoChatRoom::getPostId()
{
    return postId_;
}

void 
HippoChatRoom::setTitle(BSTR title)
{
    title_ = title;
}

BSTR 
HippoChatRoom::getTitle()
{
    return title_;
}

void 
HippoChatRoom::addListener(HippoChatRoomListener *listener)
{
	listeners_.append(listener);
}

void 
HippoChatRoom::removeListener(HippoChatRoomListener *listener)
{
    for (unsigned long i = listeners_.length(); i > 0; --i) {
        if (listeners_[i - 1] == listener) {
	    listeners_.remove(i - 1);
	    return;
	}
    }

    assert(false);
}

void 
HippoChatRoom::addUser(BSTR userId, int userVersion, BSTR userName)
{
    // Treat adding an existing user as adding then removing; this allows
    // for version/name changes.
    removeUser(userId);

    HippoChatUser user(userId, userVersion, userName);
    users_.append(user);

    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onUserJoin(this, user);
}

void 
HippoChatRoom::removeUser(BSTR userId)
{
    for (unsigned long i = 0; i < users_.length(); i++) {
        HippoChatUser &user = users_[i];
        if (wcscmp(user.getUserId(), userId) == 0) {
            for (unsigned long j = 0; j < listeners_.length(); j++)
                listeners_[j]->onUserLeave(this, user);
        
            users_.remove(i);
            return;
        }
    }
}

void 
HippoChatRoom::addMessage(BSTR userId, int userVersion, BSTR userName, BSTR text)
{
    HippoChatUser user(userId, userVersion, userName);
    HippoChatMessage message(user, text);

    messages_.append(message);

    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onMessage(this, message);
}

void
HippoChatRoom::clear() 
{
    for (unsigned long i = users_.length(); i > 0; --i)
        users_.remove(i);

    for (unsigned long i = messages_.length(); i > 0; --i)
        messages_.remove(i);

    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onClear(this);
}