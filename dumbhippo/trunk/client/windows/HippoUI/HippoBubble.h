/* HippoBubble.h: notification bubble
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include "HippoAbstractWindow.h"
#include "HippoIE.h"
#include "HippoMySpace.h"

struct HippoLinkShare;

class HippoBubble :
    public IHippoBubble,
    public IDispatch,
    public HippoAbstractWindow
{
public:
    HippoBubble();
    ~HippoBubble();

    void setLinkNotification(HippoLinkShare &share);
    void addMySpaceCommentNotification(long myId, long blogId, HippoMySpaceBlogComment &comment);
    void setIdle(bool idle);
    void setScreenSaverRunning(bool screenSaverRunning);
    void showMissedBubbles();

    void setShareHasChatActive(const WCHAR *postId, bool isActive);

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

    // IHippoBubble
    STDMETHODIMP DebugLog(BSTR str);
    STDMETHODIMP DisplaySharedLink(BSTR linkId, BSTR url);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP OpenExternalURL(BSTR url);
    STDMETHODIMP Close();
    STDMETHODIMP SetViewerSpace(DWORD viewerSpace);
    STDMETHODIMP SetHaveMissedBubbles(BOOL haveMissed);

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
    DWORD viewerSpace_;

    void setShown();
    void moveResizeWindow(void);
    void checkMouse();
    void updateIdle();
    void doSetIdle();
    void doShow();
    void doClose();

    HippoPtr<ITypeInfo> ifaceTypeInfo_;

    DWORD refCount_;
};
