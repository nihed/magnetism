/* HippoComWrappers.h: COM wrappers around some of our data model objects
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include "stdafx.h"
#include <vector>
#include <set>
#include <HippoUtil.h>
#include <HippoDispatchableObject.h>
#include <HippoConnectionPointContainer.h>
#include <hippo/hippo-common.h>
#include "HippoUIUtil.h"
#include "HippoGSignal.h"

template <class OriginalType, class WrapperType>
class HippoComWrapper
{
protected:
    HippoGObjectPtr<OriginalType> delegate_;

    explicit HippoComWrapper(OriginalType *object)
        : delegate_(object)
    {
        
    }
   
    virtual ~HippoComWrapper() {
        GObject *obj = delegate_;
        if (obj)
            g_object_set_data(obj, "hippo-com-wrapper", NULL);
    }

    static WrapperType* getComWrapper(OriginalType *object)
    {
        WrapperType *wrapper = (WrapperType*) g_object_get_data(G_OBJECT(object), "hippo-com-wrapper");
        if (wrapper == NULL) {
            wrapper = new WrapperType(object);
            g_object_set_data(G_OBJECT(object), "hippo-com-wrapper", wrapper);
        }
        return wrapper;
    };
};

template <class OriginalType, class ComType, class WrapperType>
class HippoComWrapperDispatchable
    : public HippoDispatchableObject<ComType,WrapperType>, public HippoComWrapper<OriginalType, WrapperType>
{
protected:

    explicit HippoComWrapperDispatchable(OriginalType *object)
        : HippoComWrapper<OriginalType,WrapperType>(object)
    {
    }

    virtual ~HippoComWrapperDispatchable() 
    {
    }
};

class HippoEntityCollection;

class HippoEntityWrapper
    : public HippoComWrapperDispatchable<HippoEntity, IHippoEntity, HippoEntityWrapper>
{
public:

    explicit HippoEntityWrapper(HippoEntity *entity) 
        : HippoComWrapperDispatchable<HippoEntity,IHippoEntity,HippoEntityWrapper>(entity) 
    {
    }

    static HippoEntityWrapper* getWrapper(HippoEntity *entity) {
        return getComWrapper(entity);
    }

    static HippoEntityWrapper* getWrapper(HippoPerson *person) {
        return getComWrapper(HIPPO_ENTITY(person));
    }

    static ITypeInfo *getTypeInfo();

    // IHippoEntity
    STDMETHODIMP get_Type(int *type);
    STDMETHODIMP get_Id(BSTR *id);
    STDMETHODIMP get_Name(BSTR *name);
    STDMETHODIMP get_SmallPhotoUrl(BSTR *smallPhotoUrl);
    STDMETHODIMP get_ChattingUserCount(int *chattingUserCount);
    STDMETHODIMP get_ChattingUsers(IHippoEntityCollection **users);
    STDMETHODIMP get_Ignored(BOOL *ignored);
    STDMETHODIMP get_ChatIgnored(BOOL *ignored);
    STDMETHODIMP get_HomeUrl(BSTR *homeUrl);

    STDMETHODIMP get_LastChatMessage(BSTR *message);
    STDMETHODIMP get_LastChatSender(IHippoEntity **sender);

private:    
    HippoPtr<HippoEntityCollection> currentChatParticipants_;

    GConnection1<void,HippoEntity*> userStateChanged_;
    GConnection0<void> cleared_;

    void onUserStateChanged(HippoEntity *entity);
    void onCleared();

    HippoEntityWrapper(const HippoEntityWrapper &other);
    const HippoEntityWrapper &operator=(const HippoEntityWrapper &other);
};

class HippoEntityCollection 
    : public HippoDispatchableObject<IHippoEntityCollection,HippoEntityCollection>
{
public:
    HippoEntityCollection();
    static ITypeInfo *getTypeInfo();

    void addMember(HippoEntity *entity) {
        addMember(HippoEntityWrapper::getWrapper(entity));
    }
    void addMember(HippoEntityWrapper *entityWrapper);

    // IHippoEntityCollection
    STDMETHODIMP get_length(int *length);
    STDMETHODIMP item(int index, IHippoEntity **entity);

private:
    std::vector<HippoPtr<HippoEntityWrapper> > members_;

    HippoEntityCollection(const HippoEntityCollection &other);
    const HippoEntityCollection & operator=(const HippoEntityCollection &other);
};

class HippoPostWrapper
    : public HippoComWrapperDispatchable<HippoPost,IHippoPost,HippoPostWrapper>
{
public:
    explicit HippoPostWrapper(HippoPost *post)
        : HippoComWrapperDispatchable<HippoPost,IHippoPost,HippoPostWrapper>(post)
    {
    }

    // FIXME it'd be cleaner if hippo_post_get_sender returned an entity 
    // and we didn't have to pass in the cache here just to convert from guid
    // to entity for the sender. Or if we stored the cache in the GObject, 
    // alternatively, though be careful to avoid a cyclical refcount in that case
    static HippoPostWrapper* getWrapper(HippoPost *post, HippoDataCache *cache);

    static ITypeInfo *getTypeInfo();

    // IHippoPost
    STDMETHODIMP get_Id(BSTR *id);
    STDMETHODIMP get_Sender(IHippoEntity **sender);
    STDMETHODIMP get_Url(BSTR *url);
    STDMETHODIMP get_Title(BSTR *title);
    STDMETHODIMP get_Description(BSTR *description);
    STDMETHODIMP get_Recipients(IHippoEntityCollection **recipients);
    STDMETHODIMP get_Viewers(IHippoEntityCollection **viewers);
    STDMETHODIMP get_Info(BSTR *info);
    STDMETHODIMP get_PostDate(int *postDate);
    STDMETHODIMP get_Timeout(int *timeout);
    STDMETHODIMP get_ToWorld(BOOL *toWorld);
    STDMETHODIMP get_ViewingUserCount(int *viewingUserCount);
    STDMETHODIMP get_ChattingUserCount(int *chattingUserCount);
    STDMETHODIMP get_TotalViewers(int *totalViewers);
    STDMETHODIMP get_HaveViewed(BOOL *haveViewed);
    STDMETHODIMP get_Ignored(BOOL *ignored);

    STDMETHODIMP get_LastChatMessage(BSTR *message);
    STDMETHODIMP get_LastChatSender(IHippoEntity **sender);
    STDMETHODIMP get_CurrentViewers(IHippoEntityCollection **currentViewers);

private:
    HippoGObjectPtr<HippoDataCache> cache_;
    HippoPtr<HippoEntityCollection> recipients_;
    HippoPtr<HippoEntityCollection> viewers_;
    HippoPtr<HippoEntityCollection> currentViewers_;

    GConnection0<void> changed_;
    GConnection1<void,HippoEntity*> userStateChanged_;
    GConnection0<void> cleared_;

    void onChanged();
    void onUserStateChanged(HippoEntity *entity);
    void onCleared();

    HippoPostWrapper(const HippoPostWrapper &other);
    const HippoPostWrapper &operator=(const HippoPostWrapper &other);
};

class HippoChatRoomListener;

class HippoChatRoomWrapper
    : public IHippoChatRoom, public HippoComWrapper<HippoChatRoom,HippoChatRoomWrapper>
{
public:

    explicit HippoChatRoomWrapper(HippoChatRoom *room);

    virtual ~HippoChatRoomWrapper();

    // FIXME it'd be cleaner if we stored the cache in the GObject, 
    // though be careful to avoid a cyclical refcount in that case
    static HippoChatRoomWrapper* getWrapper(HippoChatRoom *room, HippoDataCache *cache);

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

private:
    DWORD refCount_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoGObjectPtr<HippoDataCache> cache_;
    HippoConnectionPointContainer connectionPointContainer_;
    HippoArray<HippoChatRoomListener*> listeners_;

    GIdle rescanIdle_;
    bool idleRescan();

    GConnection0<void> cleared_;
    GConnection1<void,HippoPerson*> userStateChanged_;   // HippoChatRoom::user-state-changed
    GConnection1<void,HippoChatMessage*> messageAdded_; // HippoChatRoom::message-added

    void onUserStateChanged(HippoPerson *person);
    void onCleared();

    std::set<HippoPerson*> members_;

    void onMemberChanged(HippoPerson *person);
    void addMember(HippoPerson *person);
    void removeMember(HippoPerson *person, bool notify);
    void removeAllMembers(bool notify);

    void notifyUserJoin(HippoPerson *person);
    void notifyUserLeave(HippoPerson *person);
    void notifyMessage(HippoChatMessage *message);
    void notifyReconnect();
    void notifyUserChange(HippoPerson *person);
};

/* FIXME this listener stuff isn't actually used as of writing this comment, delete? 
 * People can use the signals on the HippoChatRoom GObject 
 */
class HippoChatRoomListener 
{
public:
    virtual void onUserJoin(HippoChatRoomWrapper *chatRoom, HippoPerson *person) = 0;
    virtual void onUserLeave(HippoChatRoomWrapper *chatRoom, HippoPerson *person) = 0;
    virtual void onMessage(HippoChatRoomWrapper *chatRoom, HippoChatMessage *message) = 0;
    virtual void onReconnect(HippoChatRoomWrapper *chatRoom) = 0;
    virtual void onUserChange(HippoChatRoomWrapper *chatRoom, HippoPerson *person) = 0;
    
    virtual ~HippoChatRoomListener() {}
};
