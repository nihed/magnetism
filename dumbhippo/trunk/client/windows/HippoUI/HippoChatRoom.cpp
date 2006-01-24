/* HippoChatRoom.cpp: Object representing a chat room about a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include "stdafx.h"
#include "HippoChatRoom.h"
#include "HippoIM.h"
#include "HippoLogWindow.h"
#include <HippoUtilDispId.h>

HippoChatUser::HippoChatUser()
{
}

HippoChatUser::HippoChatUser(BSTR userId, int version, BSTR name, bool participant) 
{
    userId_ = userId;
    version_ = version;
    name_ = name;
    participant_ = participant;
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

HippoChatMessage::HippoChatMessage(const HippoChatUser &user, BSTR text, INT64 timestamp, int serial)
    : user_(user)
{
    text_ = text;
    timestamp_ = timestamp;
    serial_ = serial;
}

HippoChatRoom::HippoChatRoom(HippoIM *im, BSTR postId)
{
    refCount_ = 1;

    im_ = im;
    postId_ = postId;
    rescanIdle_ = 0;
    participantCount_ = 0;
    memberCount_ = 0;
    state_ = NONMEMBER;

    connectionPointContainer_.setWrapper(static_cast<IHippoChatRoom *>(this));

    // This could fail with out-of-memory, nothing to do
    connectionPointContainer_.addConnectionPoint(__uuidof(IHippoChatRoomEvents));

    hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoChatRoom, &ifaceTypeInfo_, NULL);
}

HippoChatRoom::~HippoChatRoom()
{
    if (rescanIdle_)
        g_source_remove(rescanIdle_);

    im_->removeChatRoom(this);
}

BSTR
HippoChatRoom::getPostId()
{
    return postId_;
}

HippoChatRoom::State
HippoChatRoom::getState()
{
    return state_;
}

void 
HippoChatRoom::setState(State state)
{
    if (state == state_)
        return;
    
    State oldState = state_;
    state_ = state;

    im_->onChatRoomStateChange(this, oldState);
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
HippoChatRoom::addUser(BSTR userId, int userVersion, BSTR userName, bool participant)
{
    // Treat adding an existing user as adding then removing; this allows
    // for version/name changes.
    removeUser(userId);

    HippoChatUser user(userId, userVersion, userName, participant);
    users_.append(user);

    notifyUserJoin(user);
}

void
HippoChatRoom::notifyUserJoin(const HippoChatUser &user)
{
    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onUserJoin(this, user);

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
            VARIANTARG args[4];

            args[0].vt = VT_BOOL;
            args[0].boolVal = user.isParticipant() ? TRUE : FALSE;
            args[1].vt = VT_BSTR;
            args[1].bstrVal = user.getName();
            args[2].vt = VT_INT;
            args[2].intVal = user.getVersion();
            args[3].vt = VT_BSTR;
            args[3].bstrVal = user.getUserId();

            dispParams.rgvarg = args;
            dispParams.cArgs = 4;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONUSERJOIN, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebug(L"OnUserJoin invoke failed %x", hr);
        }
    }
}

void 
HippoChatRoom::removeUser(BSTR userId)
{
    for (unsigned long i = 0; i < users_.length(); i++) {
        HippoChatUser &user = users_[i];
        if (wcscmp(user.getUserId(), userId) == 0) {
            notifyUserLeave(user);
            users_.remove(i);
            return;
        }
    }
}

void
HippoChatRoom::notifyUserLeave(const HippoChatUser &user)
{
    for (unsigned long j = 0; j < listeners_.length(); j++)
        listeners_[j]->onUserLeave(this, user);

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
            VARIANTARG args[1];

            args[0].vt = VT_BSTR;
            args[0].bstrVal = user.getUserId();

            dispParams.rgvarg = args;
            dispParams.cArgs = 1;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONUSERLEAVE, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebug(L"Invoke failed %x", hr);
        }
    }
}

void 
HippoChatRoom::addMessage(BSTR userId, int userVersion, BSTR userName, BSTR text, INT64 timestamp, int serial)
{
    HippoChatUser user(userId, userVersion, userName, true);
    HippoChatMessage message(user, text, timestamp, serial);

    messages_.append(message);
    notifyMessage(message);
}

void
HippoChatRoom::notifyMessage(const HippoChatMessage &message)
{
    for (unsigned long i = 0; i < listeners_.length(); i++)
        listeners_[i]->onMessage(this, message);

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
            VARIANTARG args[6];

            args[0].vt = VT_INT;
            args[0].intVal = message.getSerial();
            args[1].vt = VT_R8;
            args[1].dblVal = (DOUBLE)message.getTimestamp();
            args[2].vt = VT_BSTR;
            args[2].bstrVal = message.getText();
            args[3].vt = VT_BSTR;
            args[3].bstrVal = message.getUser().getName();
            args[4].vt = VT_INT;
            args[4].intVal = message.getUser().getVersion();
            args[5].vt = VT_BSTR;
            args[5].bstrVal = message.getUser().getUserId();

            dispParams.rgvarg = args;
            dispParams.cArgs = 6;
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_ONMESSAGE, IID_NULL, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebug(L"OnMessage invoke failed %x", hr);
        }
    }
}

void
HippoChatRoom::clear() 
{
    for (unsigned long i = users_.length(); i > 0; --i)
        users_.remove(i);

    for (unsigned long i = messages_.length(); i > 0; --i)
        messages_.remove(i);
}

void
HippoChatRoom::notifyReconnect()
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
                hippoDebug(L"Invoke failed %x", hr);
        }
    }
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoChatRoom::QueryInterface(const IID &ifaceID, 
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

HIPPO_DEFINE_REFCOUNTING(HippoChatRoom)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoChatRoom::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoChatRoom::GetTypeInfo(UINT        iTInfo,
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
HippoChatRoom::GetIDsOfNames (REFIID    riid,
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
HippoChatRoom::Invoke (DISPID        member,
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
HippoChatRoom::Join(BOOL participant)
{
    hippoDebugLogW(L"HippoChatRoom: Join %ls (%d) %d/%d", postId_.m_str, participant, memberCount_, participantCount_);

    // We multiplex all calls to Join() either as participant or guest
    // into a single membership in the system copy of the chatroom, as
    // participant if we have any participant calls to Join(), otherwise
    // as a guest.

    memberCount_++;
    if (participant)
        participantCount_++;

    if (memberCount_ == 1) {
        setState(participant ? PARTICIPANT : VISITOR);
    } else if (participant && participantCount_ == 1) {
        // Was a guest, become a participant
        setState(PARTICIPANT);
    }

    return S_OK;
}

STDMETHODIMP 
HippoChatRoom::Leave(BOOL participant)
{
    hippoDebugLogW(L"HippoChatRoom: Leave %ls (%d) %d/%d", postId_.m_str, participant, memberCount_, participantCount_);

    if (memberCount_ == 0 || (participant && participantCount_ == 0))
        return E_INVALIDARG;

    memberCount_--;
    if (participant)
        participantCount_--;

    if (memberCount_ == 0) {
        hippoDebugLogW(L"Leaving room!");
        setState(NONMEMBER);
    } else if (participant && participantCount_ == 0) {
        // Was a participant, become a guest
        setState(VISITOR);
    }

    return S_OK;
}

HRESULT
HippoChatRoom::SendMessage(BSTR text)
{
    im_->sendChatRoomMessage(this, text);

    return S_OK;
}

void
HippoChatRoom::doRescanIdle()
{
    // This is not completely safe against reentrancy, we could
    // miss a notification if a user gets deleted while we
    // are notifying them. Additions will mostly be safe.
    // (Perhaps we should make a copy)

    for (unsigned long i = 0; i < users_.length(); i++)
        notifyUserJoin(users_[i]);

    for (unsigned long i = 0; i < messages_.length(); i++)
        notifyMessage(messages_[i]);

    rescanIdle_ = 0;
}

int
HippoChatRoom::rescanIdleCallback(void *data)
{
    HippoChatRoom *chatRoom = (HippoChatRoom *)data;
    chatRoom->doRescanIdle();

    return FALSE;
}

HRESULT
HippoChatRoom::Rescan()
{
    // If we rescanned immediately, we might get weird reentrancy problems
    // since we're likely notifying the object that called Rescan(). So,
    // push the actually implementation to an idle function.

    if (!rescanIdle_)
        rescanIdle_ = g_idle_add(rescanIdleCallback, this);

    return S_OK;
}
