#pragma once

#include <HippoUtil.h>
#include "HippoIE.h"

class HippoUI;

class HippoIEWindowCallback
{
public:
    virtual void onDocumentComplete() = 0;
    // Return TRUE to allow close, FALSE to disallow
    virtual bool onClose(HWND window) {
        return TRUE;
    }
};

class HippoIEWindow : public HippoMessageHook
{
public:
    HippoIEWindow(HippoUI *ui, WCHAR *title, int width, int height, WCHAR *src, IDispatch *external, HippoIEWindowCallback *cb);
    ~HippoIEWindow(void);

    HippoIE *getIE();

    void show();
    void hide();

    bool hookMessage(MSG *msg);

private:
    class HippoIEWindowIECallback : public HippoIECallback
    {
    public:
        HippoIEWindow *win_;
        HippoIEWindowIECallback(HippoIEWindow *win) {
            win_ = win;
        }
        void onDocumentComplete(void) {
            if (win_->cb_)
                win_->cb_->onDocumentComplete();
        }
        void onError(WCHAR *errText);
    };

    HippoIEWindowIECallback *ieCb_;
    HINSTANCE instance_;
    HWND window_;
    HippoIEWindowCallback *cb_;
    HippoUI *ui_;
    HippoIE *ie_;
    bool created_;

    bool processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam);
    static LRESULT CALLBACK windowProc(HWND   window,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);
    bool registerClass();
};
