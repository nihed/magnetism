/* HippoChatRoom.h: Window displaying a chatroom for a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include "HippoIE.h"

class HippoUI;
struct HippoLinkShare;

class HippoChatRoom :
    public IHippoChatRoom,
    public IDispatch
{
public:
    HippoChatRoom();
    ~HippoChatRoom();

    void setUI(HippoUI *ui);
    bool create(void);
    void show(void);

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

    // IHippoChatRoom
    STDMETHODIMP SendMessage(BSTR message);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP OpenExternalURL(BSTR url);

private:
    HINSTANCE instance_;
    HWND window_;

    class HippoChatRoomIECallback : public HippoIECallback
    {
    public:
        HippoChatRoomIECallback(HippoChatRoom *chatRoom) {
            chatRoom_ = chatRoom;
        }
        HippoChatRoom *chatRoom_;
        void onDocumentComplete();
        void onError(WCHAR *text);
        void onClose() {}
    };
    HippoChatRoomIECallback *ieCallback_;

    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    HippoUI* ui_;

    bool embedIE(void);
    bool appendTransform(BSTR src, BSTR style, ...);
    bool invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...);
    bool createWindow(void);
    bool registerClass();
    void close();

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
    HippoChatRoom(const HippoChatRoom &other);
    HippoChatRoom& operator=(const HippoChatRoom &other);
};
