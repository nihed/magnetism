/* HippoBubble.h: notification bubble
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include "HippoIE.h"

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
    void show(void);
    void setIdle(bool idle);

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
    STDMETHODIMP DisplaySharedLink(BSTR linkId);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP OpenExternalURL(BSTR url);
    STDMETHODIMP Close();
    STDMETHODIMP SetViewerSpace(DWORD viewerSpace);

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

    bool idle_;
    bool haveMouse_;
    bool effectiveIdle_;
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
