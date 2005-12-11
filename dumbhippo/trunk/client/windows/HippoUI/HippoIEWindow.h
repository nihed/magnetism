/* HippoIEWindow.h: Toplevel window with an embedded IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
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
    HippoIEWindow(HippoUI *ui, WCHAR *title, WCHAR *src, IDispatch *external, HippoIEWindowCallback *cb);
    ~HippoIEWindow(void);

    HippoIE *getIE();

    /***
     * Move and resize the window to the given size and position
     * @param x X position. CW_DEAULT means center horizontally
     * @param y Y position. CW_DEAULT means center vertically
     * @param width the new width, including window decorations
     * @param height the new height, including window decorations
     **/
    void moveResize(int x, int y, int width, int height);
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
