/* HippoChatRoom.h: Object representing a chat room about a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoArray.h>
#include <HippoConnectionPointContainer.h>

class HippoChatUser
{
public:
    HippoChatUser();
    HippoChatUser(const HippoChatUser &other);
    HippoChatUser(BSTR userId, int version, BSTR name, bool participant);

    BSTR getUserId() const { return userId_.m_str; }
    const int getVersion() const { return version_; }
    BSTR getName() const { return name_.m_str; };
    bool isParticipant() const { return participant_; };

private:
    HippoBSTR userId_;
    int version_;
    HippoBSTR name_;
    bool participant_;
};

class HippoChatMessage
{
public:
    HippoChatMessage();
    HippoChatMessage(const HippoChatUser &user, BSTR text, INT64 timestamp, int serial);

    const HippoChatUser &getUser() const { return user_; }
    const BSTR getText() const { return text_.m_str; }
    const INT64 getTimestamp() const { return timestamp_; }
    const int getSerial() const { return serial_; }

private:
    HippoChatUser user_;
    HippoBSTR text_;
    INT64 timestamp_;
    int serial_;
};

class HippoChatRoomListener;
class HippoIM;

class HippoChatRoom : public IHippoChatRoom
{
public:
    enum State {
        NONMEMBER,
        VISITOR,
        PARTICIPANT
    };

    HippoChatRoom(HippoIM *im, BSTR postId);
    ~HippoChatRoom();

    BSTR getPostId();

    State getState();

    void setTitle(BSTR title);
    BSTR getTitle();

    void addListener(HippoChatRoomListener *listener);
    void removeListener(HippoChatRoomListener *listener);

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IDispatch methods
    STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
    STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);           
    STDMETHODIMP GetTypeInfoCount (unsigned int *);
    STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                         VARIANT *, EXCEPINFO *, unsigned int *);

    // IHippoChatRoom methods
    STDMETHODIMP Join(BOOL participant);
    STDMETHODIMP Leave(BOOL participant);
    STDMETHODIMP SendMessage(BSTR text);
    STDMETHODIMP Rescan();

    // Called by HippoIM
    void addUser(BSTR userId, int userVersion, BSTR userName, bool participant);
    void removeUser(BSTR userId);
    void addMessage(BSTR userId, int userVersion, BSTR userName, BSTR text, INT64 timestamp, int serial);
    void updateMusicForUser(const BSTR userId, const BSTR arrangementName, const BSTR artist);
    void clear(); // Called on reconnect, since users and messages will be resent

private:
    void setState(State state);

    void notifyUserJoin(const HippoChatUser &user);
    void notifyUserLeave(const HippoChatUser &user);
    void notifyMessage(const HippoChatMessage &message);
    void notifyReconnect();
    void notifyUserMusicChange(const BSTR userId, const BSTR arrangementName, const BSTR artist);

    void doRescanIdle();
    static int rescanIdleCallback(void *data);

    DWORD refCount_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;

    HippoIM *im_;
    HippoBSTR postId_;
    HippoBSTR title_;

    int memberCount_;
    int participantCount_;
    State state_;

    HippoConnectionPointContainer connectionPointContainer_;
    HippoArray<HippoChatRoomListener*> listeners_;

    HippoArray<HippoChatUser> users_;
    HippoArray<HippoChatMessage> messages_;

    unsigned int rescanIdle_;
};

class HippoChatRoomListener 
{
public:
    virtual void onUserJoin(HippoChatRoom *chatRoom, const HippoChatUser &user) = 0;
    virtual void onUserLeave(HippoChatRoom *chatRoom, const HippoChatUser &user) = 0;
    virtual void onMessage(HippoChatRoom *chatRoom, const HippoChatMessage &message) = 0;
    virtual void onReconnect(HippoChatRoom *chatRoom) = 0;
    virtual void onUserMusicChange(HippoChatRoom *chatRoom, const BSTR userId, const BSTR arrangementName, const BSTR artist) = 0;
    
    virtual ~HippoChatRoomListener() {}
};
