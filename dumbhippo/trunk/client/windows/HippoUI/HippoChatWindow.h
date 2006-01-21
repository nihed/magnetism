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

class HippoChatWindow
{
public:
    HippoChatWindow();
    ~HippoChatWindow();

    void setUI(HippoUI *ui);
    bool create();
    void show();
    void setForegroundWindow();

    void setPostId(BSTR postId);
    BSTR getPostId();

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
    HippoBSTR postId_;

    bool embedIE(void);
    bool createWindow(void);
    bool registerClass();

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

    // private so they aren't used
    HippoChatWindow(const HippoChatWindow &other);
    HippoChatWindow& operator=(const HippoChatWindow &other);
};
