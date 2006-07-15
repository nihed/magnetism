/* HippoBubble.h: notification bubble
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <hippo/hippo-common.h>
#include "HippoAbstractWindow.h"
#include "HippoIE.h"
#include "HippoMySpace.h"
#include "HippoDispatchableObject.h"
#include <HippoArray.h>
#include <set>

class HippoBubbleImages;

class HippoBubble :
    public HippoDispatchableObject<IHippoBubble, HippoBubble>,
    public HippoAbstractWindow
{
public:
    HippoBubble();
    virtual ~HippoBubble();

    static ITypeInfo *getTypeInfo();

    void addMySpaceCommentNotification(long myId, long blogId, const HippoMySpaceCommentData &comment);
    void setIdle(bool idle);
    void setScreenSaverRunning(bool screenSaverRunning);
    void showMissedBubbles();

    void setShareHasChatActive(const WCHAR *postId, bool isActive);

    // IHippoBubble
    STDMETHODIMP DebugLog(BSTR str);
    STDMETHODIMP DisplaySharedLink(BSTR linkId, BSTR url);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP OpenExternalURL(BSTR url);
    STDMETHODIMP Close();
    STDMETHODIMP Resize(int width, int height);
    STDMETHODIMP SetHaveMissedBubbles(BOOL haveMissed);
    STDMETHODIMP UpdateDisplay();
    STDMETHODIMP ShowChatWindow(BSTR postId);
    STDMETHODIMP IgnorePost(BSTR postId);
    STDMETHODIMP IgnoreEntity(BSTR entityId);
    STDMETHODIMP IgnoreChat(BSTR chatId);
    STDMETHODIMP DoGroupInvite(BSTR groupId, BSTR userId);

protected:
    virtual HippoBSTR getURL();
    virtual void initializeWindow();
    virtual void initializeIE();
    virtual void initializeBrowser();
    virtual void initializeUI();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onClose(bool fromScript);

private:
    HippoBubbleImages *images_;

    HippoBSTR currentLink_;
    HippoBSTR currentLinkId_;
    HippoBSTR currentSenderUrl_;

    bool shown_;
    bool idle_;
    bool haveMouse_;
    bool effectiveIdle_;
    bool screenSaverRunning_;
    int desiredWidth_;
    int desiredHeight_;
    HDC layerDC_;
    HBITMAP oldBitmap_;

    std::set<HippoPost*> connectedPosts_;

    GConnection1<void,HippoPost*> postAdded_;           // HippoDataCache::post-added
    GConnection1<void,HippoChatRoom*> chatRoomLoaded_;  // HippoDataCache::chat-room-loaded
    GConnection3<void,HippoEntity*, HippoEntity*, const char*> groupMembershipChanged_;  // HippoConnection::group-membership-changed

    void onUserJoinedGroupChatRoom(HippoPerson *person, HippoEntity *group);
    void onGroupChatRoomMessageAdded(HippoChatMessage *message, HippoEntity *group);
    void onGroupMembershipChanged(HippoEntity *group, HippoEntity *user, const char *membershipStatus);
    void onUserJoined(HippoPerson *person, HippoPost *post);
    void onMessageAdded(HippoChatMessage *message, HippoPost *post);
    void onPostChanged(HippoPost *post);
    void onPostAdded(HippoPost *post);
    void onChatRoomLoaded(HippoChatRoom *room);
    
    void connectPost(HippoPost *post);
    void disconnectPost(HippoPost *post);
    void disconnectAllPosts();

    void setLinkNotification(HippoPost *share);

    void setShown();
    void moveResizeWindow(void);
    void checkMouse();
    void updateIdle();
    void doSetIdle();
    void doShow();
    void doClose();
};
