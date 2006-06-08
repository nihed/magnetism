/* HippoBubbleList.h: Window with a list of notification bubbles
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <set>
#include <HippoUtil.h>
#include <hippo/hippo-common.h>
#include "HippoGSignal.h"
#include "HippoAbstractWindow.h"
#include "HippoIE.h"
#include "HippoMySpace.h"

class HippoBubbleList :
    public IHippoBubbleList,
    public IDispatch,
    public HippoAbstractWindow
{
public:
    HippoBubbleList();
    ~HippoBubbleList();

    void addLinkShare(HippoPost *share);
    void addMySpaceCommentNotification(long myId, long blogId, const HippoMySpaceCommentData &comment);
    void clear();

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

    // IHippoBubbleList
    STDMETHODIMP DisplaySharedLink(BSTR linkId, BSTR url);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP Resize(int width, int height);
    STDMETHODIMP ShowChatWindow(BSTR linkId);
    STDMETHODIMP IgnorePost(BSTR linkId);

protected:
    virtual HippoBSTR getURL();
    virtual void initializeWindow();
    virtual void initializeIE();
    virtual void initializeBrowser();

    virtual void onClose(bool fromScript);

private:
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    DWORD refCount_;
    int desiredWidth_;
    int desiredHeight_;

    std::set<HippoPost*> connectedPosts_;

    void onUserJoined(HippoPerson *person, HippoPost *post);
    void onMessageAdded(HippoChatMessage *message, HippoPost *post);
    void onPostChanged(HippoPost *post);
    
    void connectPost(HippoPost *post);
    void disconnectPost(HippoPost *post);
    void disconnectAllPosts();

    void moveResizeWindow();
};
