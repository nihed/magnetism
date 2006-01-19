/* HippoBubble.h: notification bubble
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include "HippoIE.h"
#include "HippoMySpace.h"

class HippoUI;
struct HippoLinkShare;

class HippoBubble :
    public IHippoBubble,
    public IDispatch
{
public:
    HippoBubble();
    ~HippoBubble();

    void setUI(HippoUI *ui);

    void setLinkNotification(HippoLinkShare &share);
    void addMySpaceCommentNotification(long myId, long blogId, HippoMySpaceBlogComment &comment);
    void show(void);
    void setIdle(bool idle);
    void setScreenSaverRunning(bool screenSaverRunning);
    void showMissedBubbles();

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

private:
    HINSTANCE instance_;
    HWND window_;

    class HippoBubbleIECallback : public HippoIECallback
    {
    public:
        HippoBubbleIECallback(HippoBubble *bubble) {
            bubble_ = bubble;
        }
        HippoBubble *bubble_;
        void onDocumentComplete();
        void onError(WCHAR *text);
        void onClose() {}
    };
    HippoBubbleIECallback *ieCallback_;

    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    HippoUI* ui_;

    HippoBSTR currentLink_;
    HippoBSTR currentLinkId_;
    HippoBSTR currentSenderUrl_;

    bool shown_;
    bool idle_;
    bool haveMouse_;
    bool effectiveIdle_;
    bool screenSaverRunning_;
    DWORD viewerSpace_;

    bool embedIE(void);
    bool appendTransform(BSTR src, BSTR style, ...);
    bool invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...);
    bool create(void);
    bool createWindow(void);
    void moveResizeWindow(void);
    bool registerClass();
    void checkMouse();
    void updateIdle();
    void doSetIdle();
    void doShow();
    void doClose();

    HippoPtr<ITypeInfo> ifaceTypeInfo_;

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);
    DWORD refCount_;

	// private so they aren't used
	HippoBubble(const HippoBubble &other);
	HippoBubble& operator=(const HippoBubble &other);
};
