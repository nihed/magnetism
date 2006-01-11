/* HippoChatWindow.h: Window displaying a ChatWindow for a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include "HippoIE.h"
#include "HippoChatRoom.h"

class HippoUI;

class HippoChatWindow :
    public IHippoChatWindow,
    public IDispatch,
    public HippoChatRoomListener
{
public:
    HippoChatWindow();
    ~HippoChatWindow();

    void setUI(HippoUI *ui);
    bool create();
    void show();
    void setForegroundWindow();

    // The chat room that the window should track; argument can be NULL
    void setChatRoom(HippoChatRoom *chatRoom);
    HippoChatRoom *getChatRoom();

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

    // IHippoChatWindow
    STDMETHODIMP SendMessage(BSTR message);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP OpenExternalURL(BSTR url);

    // HippoChatRoomListener
    void onUserJoin(HippoChatRoom *chatRoom, const HippoChatUser &user);
    void onUserLeave(HippoChatRoom *chatRoom, const HippoChatUser &user);
    void onMessage(HippoChatRoom *chatRoom, const HippoChatMessage &message);

private:
    HINSTANCE instance_;
    HWND window_;

    class HippoChatWindowIECallback : public HippoIECallback
    {
    public:
        HippoChatWindowIECallback(HippoChatWindow *chatWindow) {
            chatWindow_ = chatWindow;
        }
        HippoChatWindow *chatWindow_;
        void onDocumentComplete();
        void onError(WCHAR *text);
        void onClose() {}
    };
    HippoChatWindowIECallback *ieCallback_;

    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    HippoUI* ui_;
    HippoChatRoom *chatRoom_;

    bool embedIE(void);
    bool appendTransform(BSTR src, BSTR style, ...);
    bool invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...);
    bool createWindow(void);
    bool registerClass();

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
    HippoChatWindow(const HippoChatWindow &other);
    HippoChatWindow& operator=(const HippoChatWindow &other);
};
