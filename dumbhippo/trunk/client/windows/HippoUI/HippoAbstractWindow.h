/* HippoAbstractWindow.h: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include "HippoIE.h"

class HippoUI;

class HippoAbstractWindow : public HippoMessageHook
{
public:
    HippoAbstractWindow();
    virtual ~HippoAbstractWindow();

    /**
     * Provide a pointer to the main UI object for the application. Mandatory to call
     * before create().
     * @param ui the main UI object
     */
    void setUI(HippoUI *ui);

    /**
     * Provide an (optional) object that will be available as window.external.application
     * Must be called before create() if called at all.
     * @param application pointer to the COM object to make available to javascript
     */
    void setApplication(IDispatch *application);

    /**
     * Set the title of the window. Note that this currently is always used, and any title
     * set by the page that is loaded is ignored. Must be called before create() to have
     * any effect.
     * @param title the title to set
     */
    void setTitle(const HippoBSTR &title);

    /**
     * Actually go ahead and create the window and embed IE in it. This must be called
     * before any methods that manipulate the embedded IE or window.
     *
     * FIXME: We possibly should do this behind the scenes
     */
    bool create();

    /**
     * Make the window visible onscreen.
     */
    void show();

    /**
     * Hide the window 
     */
    void hide();

    /**
     * Set the window as the "foreground window". This will either bring the window to
     * the front, or flash it in the user's task list if Windows doesn't want to
     * steal focus from the user.
     */
    void setForegroundWindow();

    /**
     * Move and resize the window to the given size and position
     * @param x X position. CW_DEAULT means center horizontally
     * @param y Y position. CW_DEAULT means center vertically
     * @param width the new width, including window decorations
     * @param height the new height, including window decorations
     */
    void moveResize(int x, int y, int width, int height);

    /**
     * Get the HippoIE object that wraps the embedded web browser control
     * @return the HippoIE object
     */
    HippoIE *getIE();

    ////////////////////////////////////////////////////////////

    bool hookMessage(MSG *msg);

protected:
    /**
     * Set whether showing and hiding of the window will be done with fade-in
     * and fade-out
     * @param animate true if animation should be done
     */
    void setAnimate(bool animate);

    /**
     * Set the window style flags that will be passed to CreateWindow()
     * @param windowStyle the window style flags. (See CreateWindow() docs)
     */
    void setWindowStyle(DWORD windowStyle);

    /**
     * Set the class name that will be used when registering the class for
     * this type of window. Mandatory to call before create().
     * @param className the class name string
     */
    void setClassName(const HippoBSTR &className);

    /**
     * Set the URL that will be loaded into the window. Mandatory to
     * call before create().
     @param URL the URL to load
     */
    void setURL(const HippoBSTR &url);

    /**
     * Callback when the document finishes loading 
     **/
    virtual void onDocumentComplete() = 0;

    /**
     * Callback when the window is closed either by the user
     * or by a script. There is no default implementation, so
     * close attempts will have no effect other than anything
     * you do here.
     * @param fromScript true if a script invoked as window.close()
     **/
    virtual void onClose(bool fromScript) = 0;

    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;
    HippoUI* ui_;

private:
    bool animate_;
    DWORD windowStyle_;
    HippoBSTR className_;
    HippoBSTR url_;
    HippoBSTR title_;

    HINSTANCE instance_;
    HippoPtr<IDispatch> application_;
    HWND window_;

    class HippoAbstractWindowIECallback : public HippoIECallback
    {
    public:
        HippoAbstractWindowIECallback(HippoAbstractWindow *chatWindow) {
            abstractWindow_ = chatWindow;
        }
        HippoAbstractWindow *abstractWindow_;
        void onDocumentComplete();
        void onError(WCHAR *text);
        void onClose();
    };
    HippoAbstractWindowIECallback *ieCallback_;

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
    HippoAbstractWindow(const HippoAbstractWindow &other);
    HippoAbstractWindow& operator=(const HippoAbstractWindow &other);
};
