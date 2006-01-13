/* HippoChatRoom.h: Object representing a chat room about a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoArray.h>

class HippoChatUser
{
public:
    HippoChatUser();
    HippoChatUser(const HippoChatUser &other);
    HippoChatUser(BSTR userId, int version, BSTR name);

    BSTR getUserId() const { return userId_.m_str; }
    const int getVersion() const { return version_; }
    BSTR getName() const { return name_.m_str; };

private:
    HippoBSTR userId_;
    int version_;
    HippoBSTR name_;
};

class HippoChatMessage
{
public:
    HippoChatMessage();
    HippoChatMessage(const HippoChatUser &user, BSTR text);

    const HippoChatUser &getUser() const { return user_; }
    const BSTR getText() const { return text_.m_str; }

private:
    HippoChatUser user_;
    HippoBSTR text_;
};

class HippoChatRoomListener;
class HippoIM;

class HippoChatRoom
{
public:
    HippoChatRoom(HippoIM *im, BSTR postId);
    ~HippoChatRoom();

    void sendMessage(BSTR text);

    BSTR getPostId();

    void setTitle(BSTR title);
    BSTR getTitle();

    void addListener(HippoChatRoomListener *listener);
    void removeListener(HippoChatRoomListener *listener);

    // Called by HippoIM
    void addUser(BSTR userId, int userVersion, BSTR userName);
    void removeUser(BSTR userId);
    void addMessage(BSTR userId, int userVersion, BSTR userName, BSTR text);
    void clear(); // Called on reconnect, since users and messages will be resent

private:
    HippoIM *im_;
    HippoBSTR postId_;
    HippoBSTR title_;

    HippoArray<HippoChatRoomListener*> listeners_;

    HippoArray<HippoChatUser> users_;
    HippoArray<HippoChatMessage> messages_;
};

class HippoChatRoomListener 
{
public:
    virtual void onUserJoin(HippoChatRoom *chatRoom, const HippoChatUser &user) = 0;
    virtual void onUserLeave(HippoChatRoom *chatRoom, const HippoChatUser &user) = 0;
    virtual void onMessage(HippoChatRoom *chatRoom, const HippoChatMessage &message) = 0;
    virtual void onClear(HippoChatRoom *chatRoom) = 0;
    
    virtual ~HippoChatRoomListener() {}
};
