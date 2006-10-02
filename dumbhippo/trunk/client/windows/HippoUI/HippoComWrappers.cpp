#include "stdafx-hippoui.h"
#include "HippoComWrappers.h"
#include "HippoUI.h"
#include <algorithm>
#include <HippoUtilDispId.h>

static void
utf8ToCom(const char *s,
          BSTR       *bStr)
{
    if (s == NULL)
        *bStr = NULL;
    else
        HippoBSTR::fromUTF8(s, -1).CopyTo(bStr);
}

static void
entityToCom(HippoEntity   *entity,
            IHippoEntity **comEntityReturn)
{
    if (entity == NULL) {
        *comEntityReturn = NULL;
    } else {
        HippoEntityWrapper *wrapper = HippoEntityWrapper::getWrapper(entity);
        wrapper->AddRef();
        *comEntityReturn = wrapper;
    }
}

ITypeInfo *
HippoEntityWrapper::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;

    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoEntity, &typeInfo, NULL);

    return typeInfo;
}

STDMETHODIMP
HippoEntityWrapper::get_Id(BSTR *id)
{
    utf8ToCom(hippo_entity_get_guid(delegate_), id);

    return S_OK;
}

STDMETHODIMP
HippoEntityWrapper::get_Type(int *type)
{
    *type = (int) hippo_entity_get_entity_type(delegate_);

    return S_OK;
}

STDMETHODIMP
HippoEntityWrapper::get_Name(BSTR *name)
{
    utf8ToCom(hippo_entity_get_name(delegate_), name);

    return S_OK;
}

STDMETHODIMP
HippoEntityWrapper::get_SmallPhotoUrl(BSTR *smallPhotoUrl)
{
    utf8ToCom(hippo_entity_get_small_photo_url(delegate_), smallPhotoUrl);

    return S_OK;
}

STDMETHODIMP 
HippoEntityWrapper::get_Ignored(BOOL *ignored)
{
    if (HIPPO_IS_GROUP(delegate_)) {
        *ignored = hippo_group_get_ignored(HIPPO_GROUP(delegate_));    
    } else {
        *ignored = FALSE;
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoEntityWrapper::get_ChatIgnored(BOOL *ignored)
{   
    if (HIPPO_IS_GROUP(delegate_)) {
        HippoChatRoom *room;   
        room = hippo_group_get_chat_room(HIPPO_GROUP(delegate_));
    
        if (room && !hippo_chat_room_get_loading(room)) {
            *ignored = hippo_chat_room_get_ignored(room);
        } else {    
            hippoDebugLogW(L"HippoEntityWrapper::get_ChatIgnored was called when the chat room "
                       L"for the group was null or was still loading");
            *ignored = FALSE;
        }
    } else {
        *ignored = FALSE;
    }
    return S_OK;
}

STDMETHODIMP 
HippoEntityWrapper::get_ChattingUserCount(int *chattingUserCount)
{
    if (HIPPO_IS_GROUP(delegate_)) {
        HippoChatRoom *room = hippo_group_get_chat_room(HIPPO_GROUP(delegate_));

        if (room && !hippo_chat_room_get_loading(room)) {
            *chattingUserCount = hippo_chat_room_get_chatting_user_count(room);
        } else {
            hippoDebugLogW(L"HippoEntityWrapper::get_ChattingUserCount was called when the chat room "
                        L"for the group was null or was still loading");
            *chattingUserCount = 0;
        }
    } else {
        *chattingUserCount = 0;
    }

    return S_OK;
}

void
HippoEntityWrapper::onUserStateChanged(HippoEntity *entity)
{
    // drop cached user information from ChatRoom
    currentChatParticipants_ = NULL;
}

void
HippoEntityWrapper::onCleared()
{
    // drop cached info from ChatRoom (individual user state changes
    // aren't emitted when we clear the room)
    currentChatParticipants_ = NULL;
}

STDMETHODIMP
HippoEntityWrapper::get_ChattingUsers(IHippoEntityCollection **users)
{
    // We keep a pointer to the CurrentChatParticipants object so something like
    // for (i = 0; i < post.CurrentChatParticipants.length; i++) { ... } 
    // doesn't continually create new objects, but we don't try to 
    // incrementally update it, we just clear it and demand create
    // a new one.

    if (!currentChatParticipants_) {
        currentChatParticipants_ = new HippoEntityCollection();
        currentChatParticipants_->Release(); // the smart pointer has got it

        if (HIPPO_IS_GROUP(delegate_)) {
            HippoChatRoom *room = hippo_group_get_chat_room(HIPPO_GROUP(delegate_));
            if (room && !hippo_chat_room_get_loading(room)) {
                GSList *members = hippo_chat_room_get_users(room);
                for (GSList *link = members; link != NULL; link = link->next) {
                    HippoEntity *entity = HIPPO_ENTITY(link->data);
                    currentChatParticipants_->addMember(entity);
                    g_object_unref(entity);
                }
                g_slist_free(members);

                // connect up to clear cache if chat room changes
                userStateChanged_.connect(G_OBJECT(room), "user-state-changed", 
                    slot(this, &HippoEntityWrapper::onUserStateChanged));
                cleared_.connect(G_OBJECT(room), "cleared",
                    slot(this, &HippoEntityWrapper::onCleared));
            }
        }
    }

    currentChatParticipants_->AddRef();
    *users = currentChatParticipants_;

    return S_OK;
}

STDMETHODIMP 
HippoEntityWrapper::get_LastChatMessage(BSTR *message)
{
    *message = NULL;
    
    if (HIPPO_IS_GROUP(delegate_)) {
        HippoChatRoom *room = hippo_group_get_chat_room(HIPPO_GROUP(delegate_));
        if (room) {
            HippoChatMessage *m = hippo_chat_room_get_last_message(room);
            if (m != NULL) {
                utf8ToCom(hippo_chat_message_get_text(m), message);
            }
        }
    }

    return S_OK;
}

STDMETHODIMP
HippoEntityWrapper::get_LastChatSender(IHippoEntity **sender)
{
    *sender = NULL;

    if (HIPPO_IS_GROUP(delegate_)) {
        HippoChatRoom *room = hippo_group_get_chat_room(HIPPO_GROUP(delegate_));
        if (room) {
            HippoChatMessage *m = hippo_chat_room_get_last_message(room);
            if (m != NULL) {
                HippoPerson *person = hippo_chat_message_get_person(m);
                if (person != NULL)
                    entityToCom(HIPPO_ENTITY(person), sender);
            }
        }
    }

    return S_OK;
}

STDMETHODIMP
HippoEntityWrapper::get_HomeUrl(BSTR *homeUrl)
{
    utf8ToCom(hippo_entity_get_home_url(delegate_), homeUrl);

    return S_OK;
}

/////////////////////////////////////////////////////////////////////////

HippoEntityCollection::HippoEntityCollection()
{
}

ITypeInfo *
HippoEntityCollection::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;

    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoEntityCollection, &typeInfo, NULL);

    return typeInfo;
}

void
HippoEntityCollection::addMember(HippoEntityWrapper *entityWrapper)
{
    members_.push_back(entityWrapper);
}

STDMETHODIMP
HippoEntityCollection::get_length(int *length)
{
    *length = (int)members_.size();

    return S_OK;
}

STDMETHODIMP
HippoEntityCollection::item(int index, IHippoEntity **entity)
{
    if (index < 0 || (size_t)index >= members_.size())
        return E_INVALIDARG;

    *entity = members_[index];
    (*entity)->AddRef();

    return S_OK;
}


/////////////////////////////////////////////////////////////////////////

HippoPostWrapper*
HippoPostWrapper::getWrapper(HippoPost *post, HippoDataCache *cache)
{
    HippoPostWrapper *wrapper = getComWrapper(post);
    if (!wrapper->cache_) {
        
        // First wrapping!
        wrapper->cache_ = cache;

        // this has nothing to do with cache_ but 
        // this is a convenient place to put it
        wrapper->changed_.connect(G_OBJECT(post), "changed", 
            slot(wrapper, &HippoPostWrapper::onChanged));

    } else {
        assert(wrapper->cache_ == cache);
    }
    return wrapper;
}

ITypeInfo *
HippoPostWrapper::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;

    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoPost, &typeInfo, NULL);

    return typeInfo;
}

void
HippoPostWrapper::onChanged()
{
    // drop cached EntityCollection objects from Post
    recipients_ = NULL;
    viewers_ = NULL;
}

void
HippoPostWrapper::onUserStateChanged(HippoEntity *entity)
{
    // drop cached user information from ChatRoom
    currentViewers_ = NULL;
}

void
HippoPostWrapper::onCleared()
{
    // drop cached info from ChatRoom (individual user state changes
    // aren't emitted when we clear the room)
    currentViewers_ = NULL;
}

STDMETHODIMP 
HippoPostWrapper::get_Id(BSTR *id)
{
    utf8ToCom(hippo_post_get_guid(delegate_), id);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Sender(IHippoEntity **sender)
{
    const char *sender_id = hippo_post_get_sender(delegate_);
    if (sender_id == NULL) {
        *sender = NULL;
    } else {
        HippoEntity *entity = hippo_data_cache_ensure_bare_entity(cache_, HIPPO_ENTITY_PERSON, sender_id);
        entityToCom(entity, sender);
    }

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Url(BSTR *url)
{
    utf8ToCom(hippo_post_get_url(delegate_), url);
    
    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Title(BSTR *title)
{
    utf8ToCom(hippo_post_get_title(delegate_), title);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Description(BSTR *description)
{
    utf8ToCom(hippo_post_get_description(delegate_), description);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Recipients(IHippoEntityCollection **recipients)
{
    if (!recipients_) {
        // create recipients object
        recipients_ = new HippoEntityCollection();
        recipients_->Release(); // the smart pointer has got it

        // this list isn't copied, no need to free it
        GSList *cRecipients = hippo_post_get_recipients(delegate_);
        
        for (GSList *link = cRecipients; link != NULL; link = link->next) {
            HippoEntity *r = HIPPO_ENTITY(link->data);
            recipients_->addMember(r);
        }
    }

    recipients_->AddRef();
    *recipients = recipients_;

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Viewers(IHippoEntityCollection **viewers)
{
    if (!viewers_) {
        // create viewers object
        viewers_ = new HippoEntityCollection();
        viewers_->Release(); // the smart pointer has got it

        // this list isn't copied, no need to free it
        GSList *cRecipients = hippo_post_get_viewers(delegate_);
        
        for (GSList *link = cRecipients; link != NULL; link = link->next) {
            HippoEntity *r = HIPPO_ENTITY(link->data);
            viewers_->addMember(r);
        }    
    }

    viewers_->AddRef();
    *viewers = viewers_;

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Info(BSTR *info)
{
    utf8ToCom(hippo_post_get_info(delegate_), info);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_PostDate(int *postDate)
{
    *postDate = hippo_post_get_date(delegate_);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Timeout(int *timeout)
{
    *timeout = hippo_post_get_timeout(delegate_);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_ToWorld(BOOL *toWorld)
{
    *toWorld = hippo_post_is_to_world(delegate_);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_Ignored(BOOL *ignored)
{
    *ignored = hippo_post_get_ignored(delegate_);
    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_ViewingUserCount(int *viewingUserCount)
{
    *viewingUserCount = hippo_post_get_viewing_user_count(delegate_);
    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_ChattingUserCount(int *chattingUserCount)
{
    *chattingUserCount = hippo_post_get_chatting_user_count(delegate_);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_TotalViewers(int *totalViewers)
{
    *totalViewers = hippo_post_get_total_viewers(delegate_);

    return S_OK;
}

STDMETHODIMP 
HippoPostWrapper::get_HaveViewed(BOOL *haveViewed)
{
    *haveViewed = hippo_post_get_have_viewed(delegate_) ? TRUE: FALSE;

    return S_OK;
}


STDMETHODIMP 
HippoPostWrapper::get_LastChatMessage(BSTR *message)
{
    *message = NULL;

    HippoChatRoom *room = hippo_post_get_chat_room(delegate_);
    if (room) {
        HippoChatMessage *m = hippo_chat_room_get_last_message(room);
        if (m != NULL) {
            utf8ToCom(hippo_chat_message_get_text(m), message);
        }
    }

    return S_OK;
}

STDMETHODIMP
HippoPostWrapper::get_LastChatSender(IHippoEntity **sender)
{
    *sender = NULL;

    HippoChatRoom *room = hippo_post_get_chat_room(delegate_);
    if (room) {
        HippoChatMessage *m = hippo_chat_room_get_last_message(room);
        if (m != NULL) {
            HippoPerson *person = hippo_chat_message_get_person(m);
            if (person != NULL)
                entityToCom(HIPPO_ENTITY(person), sender);
        }
    }

    return S_OK;
}

STDMETHODIMP
HippoPostWrapper::get_CurrentViewers(IHippoEntityCollection **viewers)
{
    // We keep a pointer to the CurrentViewers object so something like
    // for (i = 0; i < post.CurrentViewers.length; i++) { ... } 
    // doesn't continually create new objects, but we don't try to 
    // incrementally update it, we just clear it and demand create
    // a new one.

    if (!currentViewers_) {
        currentViewers_ = new HippoEntityCollection();
        currentViewers_->Release(); // the smart pointer has got it

        HippoChatRoom *room = hippo_post_get_chat_room(delegate_);
        if (room) {
            GSList *members = hippo_chat_room_get_users(room);
            for (GSList *link = members; link != NULL; link = link->next) {
                HippoEntity *entity = HIPPO_ENTITY(link->data);
                currentViewers_->addMember(entity);
                g_object_unref(entity);
            }
            g_slist_free(members);

            // connect up to clear cache if chat room changes
            userStateChanged_.connect(G_OBJECT(room), "user-state-changed", 
                slot(this, &HippoPostWrapper::onUserStateChanged));
            cleared_.connect(G_OBJECT(room), "cleared",
                slot(this, &HippoPostWrapper::onCleared));
        }
    }

    currentViewers_->AddRef();
    *viewers = currentViewers_;

    return S_OK;
}

/////////////////////////////////////////////////////////////////////////

HippoChatRoomWrapper::HippoChatRoomWrapper(HippoChatRoom *room)
    : refCount_(1), HippoComWrapper<HippoChatRoom,HippoChatRoomWrapper>(room)
{
    connectionPointContainer_.setWrapper(static_cast<IHippoChatRoom *>(this));

    // This could fail with out-of-memory, nothing to do
    connectionPointContainer_.addConnectionPoint(__uuidof(IHippoChatRoomEvents));

    // FIXME do we really need/want one of these per-instance (and thus per post we 
    // have cached?)
    hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoChatRoom, &ifaceTypeInfo_, NULL);

    cleared_.connect(G_OBJECT(room), "cleared", slot(this, &HippoChatRoomWrapper::onCleared));
    userStateChanged_.connect(G_OBJECT(room), "user-state-changed", slot(this, &HippoChatRoomWrapper::onUserStateChanged));
    // connect this directly to notifyMessage
    messageAdded_.connect(G_OBJECT(room), "message-added", slot(this, &HippoChatRoomWrapper::notifyMessage));
}

HippoChatRoomWrapper::~HippoChatRoomWrapper()
{
    removeAllMembers(false);
}

HippoChatRoomWrapper*
HippoChatRoomWrapper::getWrapper(HippoChatRoom *room, HippoDataCache *cache)
{
    HippoChatRoomWrapper *wrapper = getComWrapper(room);
    if (!wrapper->cache_) {
        
        // First wrapping!
        wrapper->cache_ = cache;
    } else {
        assert(wrapper->cache_ == cache);
    }
    return wrapper;
}

void 
HippoChatRoomWrapper::addListener(HippoChatRoomListener *listener)
{
    listeners_.append(listener);
}

void 
HippoChatRoomWrapper::removeListener(HippoChatRoomListener *listener)
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
HippoChatRoomWrapper::onCleared()
{
    removeAllMembers(false);
    notifyReconnect();
}

void
HippoChatRoomWrapper::onUserStateChanged(HippoPerson *person)
{
    HippoChatState state = hippo_chat_room_get_user_state(delegate_, person);
    if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
        addMember(person);
    } else {
        removeMember(person, true);
    }
}

void
HippoChatRoomWrapper::onMemberChanged(HippoPerson *person)
{
    notifyUserChange(person);
}

void
HippoChatRoomWrapper::addMember(HippoPerson *person)
{
    std::set<HippoPerson*>::iterator i = members_.find(person);
    if (i != members_.end())
        return;
    
    g_object_ref(person);
    members_.insert(person);

    GConnection0<void>::named_connect(G_OBJECT(person), "hippo-chat-room-wrapper",
        "changed", bind(slot(this, &HippoChatRoomWrapper::onMemberChanged), person));

    notifyUserJoin(person);
    // FIXME see if the javascript really relies on this, maybe fix it if it does
    notifyUserChange(person);
}

void
HippoChatRoomWrapper::removeMember(HippoPerson *person, bool notify)
{
    std::set<HippoPerson*>::iterator i = members_.find(person);
    if (i != members_.end()) {
        GConnection::named_disconnect(G_OBJECT(person), "hippo-chat-room-wrapper");

        members_.erase(i);
        if (notify)
            notifyUserLeave(person);
        g_object_unref(person);
    }
}

void
HippoChatRoomWrapper::removeAllMembers(bool notify)
{
    std::set<HippoPerson*>::iterator i;
    while ((i = members_.begin()) != members_.end()) {
        removeMember(*i, notify);
    }
}

void
HippoChatRoomWrapper::notifyUserJoin(HippoPerson *user)
{
    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onUserJoin(this, user);

    HippoPtr<IConnectionPoint> point;
    if (FAILED(connectionPointContainer_.FindConnectionPoint(__uuidof(IHippoChatRoomEvents), &point)))
        return;

    HippoPtr<IEnumConnections> e;
    if (FAILED(point->EnumConnections(&e)))
        return;

    HippoChatState state = hippo_chat_room_get_user_state(delegate_, user);
    g_return_if_fail(state != HIPPO_CHAT_STATE_NONMEMBER);

    HippoBSTR name = HippoBSTR::fromUTF8(hippo_entity_get_name(HIPPO_ENTITY(user)), -1);
    HippoBSTR guid = HippoBSTR::fromUTF8(hippo_entity_get_guid(HIPPO_ENTITY(user)), -1);
    HippoBSTR photoUrl = HippoBSTR::fromUTF8(hippo_entity_get_small_photo_url(HIPPO_ENTITY(user)), -1);

    CONNECTDATA data;
    ULONG fetched;
    while (e->Next(1, &data, &fetched) == S_OK) {
        HippoQIPtr<IDispatch> dispatch(data.pUnk);
        if (dispatch) {
            DISPPARAMS dispParams;
            VARIANTARG args[4];

            args[0].vt = VT_BOOL;
            args[0].boolVal = state == HIPPO_CHAT_STATE_PARTICIPANT ? TRUE : FALSE;
            args[1].vt = VT_BSTR;
            args[1].bstrVal = name.m_str;
            args[2].vt = VT_BSTR;
            args[2].bstrVal = photoUrl.m_str;            
            args[3].vt = VT_BSTR;
            args[3].bstrVal = guid.m_str;

            dispParams.rgvarg = args;
            dispParams.cArgs = 4;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONUSERJOIN, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebugDialog(L"OnUserJoin invoke failed %x", hr);
        }
    }
}

void
HippoChatRoomWrapper::notifyUserChange(HippoPerson *person)
{
    // FIXME if user is only viewing, skip this

    // FIXME this is historically only about music changing ... should be modified 
    // to include also the name and photo so those will update

    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onUserChange(this, person);

    HippoPtr<IConnectionPoint> point;
    if (FAILED(connectionPointContainer_.FindConnectionPoint(__uuidof(IHippoChatRoomEvents), &point)))
        return;

    HippoPtr<IEnumConnections> e;
    if (FAILED(point->EnumConnections(&e)))
        return;

    HippoBSTR artist = HippoBSTR::fromUTF8(hippo_person_get_current_artist(person), -1);
    HippoBSTR song = HippoBSTR::fromUTF8(hippo_person_get_current_song(person), -1);
    HippoBSTR guid = HippoBSTR::fromUTF8(hippo_entity_get_guid(HIPPO_ENTITY(person)), -1);

    CONNECTDATA data;
    ULONG fetched;
    while (e->Next(1, &data, &fetched) == S_OK) {
        HippoQIPtr<IDispatch> dispatch(data.pUnk);
        if (dispatch) {
            DISPPARAMS dispParams;
            VARIANTARG args[4];

            // order of these arguments gets reversed, so we need to supply
            // the musicPlaying flag as the first argument, and the userId as the last one
            args[0].vt = VT_BOOL;
            args[0].boolVal = hippo_person_get_music_playing(person);
            args[1].vt = VT_BSTR;
            args[1].bstrVal = artist.m_str;
            args[2].vt = VT_BSTR;
            args[2].bstrVal = song.m_str;
            args[3].vt = VT_BSTR;
            args[3].bstrVal = guid.m_str;

            dispParams.rgvarg = args;
            dispParams.cArgs = 4;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONUSERMUSICCHANGE, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebugDialog(L"OnUserChange invoke failed %x", hr);
        }
    }
}

void
HippoChatRoomWrapper::notifyUserLeave(HippoPerson *user)
{
    for (unsigned long j = 0; j < listeners_.length(); j++)
        listeners_[j]->onUserLeave(this, user);

    HippoPtr<IConnectionPoint> point;
    if (FAILED(connectionPointContainer_.FindConnectionPoint(__uuidof(IHippoChatRoomEvents), &point)))
        return;

    HippoPtr<IEnumConnections> e;
    if (FAILED(point->EnumConnections(&e)))
        return;

    HippoBSTR guid = HippoBSTR::fromUTF8(hippo_entity_get_guid(HIPPO_ENTITY(user)), -1);

    CONNECTDATA data;
    ULONG fetched;
    while (e->Next(1, &data, &fetched) == S_OK) {
        HippoQIPtr<IDispatch> dispatch(data.pUnk);
        if (dispatch) {
            DISPPARAMS dispParams;
            VARIANTARG args[1];

            args[0].vt = VT_BSTR;
            args[0].bstrVal = guid.m_str;

            dispParams.rgvarg = args;
            dispParams.cArgs = 1;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONUSERLEAVE, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebugDialog(L"Invoke failed %x", hr);
        }
    }
}

void
HippoChatRoomWrapper::notifyMessage(HippoChatMessage *message)
{
    HippoPerson *user;

    user = hippo_chat_message_get_person(message);

    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onMessage(this, message);

    HippoPtr<IConnectionPoint> point;
    if (FAILED(connectionPointContainer_.FindConnectionPoint(__uuidof(IHippoChatRoomEvents), &point)))
        return;

    HippoPtr<IEnumConnections> e;
    if (FAILED(point->EnumConnections(&e)))
        return;

    HippoBSTR name = HippoBSTR::fromUTF8(hippo_entity_get_name(HIPPO_ENTITY(user)), -1);
    HippoBSTR guid = HippoBSTR::fromUTF8(hippo_entity_get_guid(HIPPO_ENTITY(user)), -1);
    HippoBSTR photoUrl = HippoBSTR::fromUTF8(hippo_entity_get_small_photo_url(HIPPO_ENTITY(user)), -1);
    HippoBSTR text = HippoBSTR::fromUTF8(hippo_chat_message_get_text(message), -1);

    CONNECTDATA data;
    ULONG fetched;
    while (e->Next(1, &data, &fetched) == S_OK) {
        HippoQIPtr<IDispatch> dispatch(data.pUnk);
        if (dispatch) {
            DISPPARAMS dispParams; 
            VARIANTARG args[6];

            args[0].vt = VT_INT;
            args[0].intVal = hippo_chat_message_get_serial(message);
            args[1].vt = VT_R8;
            args[1].dblVal = (DOUBLE)hippo_chat_message_get_timestamp(message);
            args[2].vt = VT_BSTR;
            args[2].bstrVal = text.m_str;
            args[3].vt = VT_BSTR;
            args[3].bstrVal = name.m_str;
            args[4].vt = VT_BSTR;
            args[4].bstrVal = photoUrl.m_str;
            args[5].vt = VT_BSTR;
            args[5].bstrVal = guid.m_str;

            dispParams.rgvarg = args;
            dispParams.cArgs = 6;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONMESSAGE, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebugDialog(L"OnMessage invoke failed %x", hr);
        }
    }
}

void
HippoChatRoomWrapper::notifyReconnect()
{
    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onReconnect(this);

   HippoPtr<IConnectionPoint> point;
    if (FAILED(connectionPointContainer_.FindConnectionPoint(__uuidof(IHippoChatRoomEvents), &point)))
        return;

    HippoPtr<IEnumConnections> e;
    if (FAILED(point->EnumConnections(&e)))
        return;

    CONNECTDATA data;
    ULONG fetched;
    while (e->Next(1, &data, &fetched) == S_OK) {
        HippoQIPtr<IDispatch> dispatch(data.pUnk);
        if (dispatch) {
            DISPPARAMS dispParams;

            dispParams.rgvarg = NULL;
            dispParams.cArgs = 0;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONRECONNECT, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebugDialog(L"Invoke failed %x", hr);
        }
    }
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoChatRoomWrapper::QueryInterface(const IID &ifaceID, 
                           void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(this);
    else if (IsEqualIID(ifaceID, __uuidof(IHippoChatRoom)))
        *result = static_cast<IHippoChatRoom *>(this);
    else if (IsEqualIID(ifaceID, IID_IConnectionPointContainer)) 
        *result = static_cast<IConnectionPointContainer *>(&connectionPointContainer_);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoChatRoomWrapper)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoChatRoomWrapper::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoChatRoomWrapper::GetTypeInfo(UINT        iTInfo,
                        LCID        lcid,
                        ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoChatRoomWrapper::GetIDsOfNames (REFIID    riid,
                           LPOLESTR *rgszNames,
                           UINT      cNames,
                           LCID      lcid,
                           DISPID   *rgDispId)
 {
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;
    
    return DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
 }
        
STDMETHODIMP
HippoChatRoomWrapper::Invoke (DISPID        member,
                    const IID    &iid,
                    LCID          lcid,              
                    WORD          flags,
                    DISPPARAMS   *dispParams,
                    VARIANT      *result,
                    EXCEPINFO    *excepInfo,  
                    unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    HippoQIPtr<IHippoChatRoom> hippoEmbed(static_cast<IHippoChatRoom *>(this));
    HRESULT hr = DispInvoke(hippoEmbed, ifaceTypeInfo_, member, flags, 
                             dispParams, result, excepInfo, argErr);

#if 0
    hippoDebug(L"Invoke: %#x - result %#x\n", member, hr);
#endif
    
    return hr;
}

/////////////////////// IHippoChatRoom implementation ///////////////////////

STDMETHODIMP 
HippoChatRoomWrapper::Join(BOOL participant)
{
    hippoDebugLogW(L"HippoChatRoomWrapper::Join participant = %d", participant);

    hippo_connection_join_chat_room(hippo_data_cache_get_connection(cache_), delegate_,
        participant ? HIPPO_CHAT_STATE_PARTICIPANT : HIPPO_CHAT_STATE_VISITOR);

    return S_OK;
}

STDMETHODIMP 
HippoChatRoomWrapper::Leave(BOOL participant)
{
    hippoDebugLogW(L"HippoChatRoomWrapper::Leave participant = %d", participant);

    hippo_connection_leave_chat_room(hippo_data_cache_get_connection(cache_), delegate_,
        participant ? HIPPO_CHAT_STATE_PARTICIPANT : HIPPO_CHAT_STATE_VISITOR);

    return S_OK;
}

HRESULT
HippoChatRoomWrapper::SendMessage(BSTR text)
{
    HippoConnection *connection = hippo_data_cache_get_connection(cache_);

    HippoUStr textU(text);

    hippo_connection_send_chat_room_message(connection, delegate_, textU.c_str());

    return S_OK;
}

bool
HippoChatRoomWrapper::idleRescan()
{
    removeAllMembers(false);

    // have to free list and unref each member
    GSList *members = hippo_chat_room_get_users(delegate_);
    
    for (GSList *link = members; link != NULL; link = link->next) {
        HippoPerson *user = HIPPO_PERSON(link->data);
        addMember(user);
        g_object_unref(G_OBJECT(user));
    }
    g_slist_free(members);

    // this list on the other hand is not a copy, but we copy it for ourselves.
    // right now it should be safe not to copy the members since HippoChatRoom
    // never deletes messages once added... but perhaps not the best long-term plan
    GSList *messages = g_slist_copy(hippo_chat_room_get_messages(delegate_));
    for (GSList *link = messages; link != NULL; link = link->next) {
        HippoChatMessage *m = static_cast<HippoChatMessage*>(link->data);
        notifyMessage(m);
    }
    g_slist_free(messages);

    return false;
}

HRESULT
HippoChatRoomWrapper::Rescan()
{
    // If we rescanned immediately, we might get weird reentrancy problems
    // since we're likely notifying the object that called Rescan(). So,
    // push the actually implementation to an idle function.
    
    rescanIdle_.add(slot(this, &HippoChatRoomWrapper::idleRescan));

    return S_OK;
}
