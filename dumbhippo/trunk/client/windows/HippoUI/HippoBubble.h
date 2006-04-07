/* HippoBubble.h: notification bubble
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include "HippoDataCache.h"
#include "HippoAbstractWindow.h"
#include "HippoIE.h"
#include "HippoMySpace.h"
#include "HippoDispatchableObject.h"

class HippoPost;

class HippoBubble :
    public HippoDispatchableObject<IHippoBubble, HippoBubble>,
    public HippoAbstractWindow
{
public:
    HippoBubble();
    ~HippoBubble();

    static ITypeInfo *getTypeInfo();

    void setLinkNotification(bool isRedisplay, HippoPost *share);
    void addMySpaceCommentNotification(long myId, long blogId, HippoMySpaceBlogComment &comment);
    void setIdle(bool idle);
    void setScreenSaverRunning(bool screenSaverRunning);
    void showMissedBubbles();

    void onViewerJoin(HippoPost *post);
    void onChatRoomMessage(HippoPost *post);
    void updatePost(HippoPost *post);

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

protected:
    virtual HippoBSTR getURL();
    virtual void initializeWindow();
    virtual void initializeIE();
    virtual void initializeBrowser();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onClose(bool fromScript);

private:
    HippoBSTR currentLink_;
    HippoBSTR currentLinkId_;
    HippoBSTR currentSenderUrl_;

    HippoArray<HippoBSTR> activeChatShares_;

    bool shown_;
    bool idle_;
    bool haveMouse_;
    bool effectiveIdle_;
    bool screenSaverRunning_;
    int desiredWidth_;
    int desiredHeight_;
    HDC layerDC_;
    HBITMAP oldBitmap_;

    void setShown();
    void moveResizeWindow(void);
    void checkMouse();
    void updateIdle();
    void doSetIdle();
    void doShow();
    void doClose();
};
