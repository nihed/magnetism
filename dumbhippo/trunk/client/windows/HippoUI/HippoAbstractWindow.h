/* HippoAbstractWindow.h: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoIE.h>
#include <HippoMessageHook.h>

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
     * Destroy the window and shutdown the underlying Internet Explorer instance
     */
    void destroy();

    /**
     * Make the window visible onscreen.
     */
    void show(BOOL activate = true);

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
     * Whether to set the parent window of the window to ui_->window.
     * This is needed for transient popups, but causes problems for
     * more "windowy" windows. (Defaults to false
     * @param useParent true if we should set the parent window.
     **/
    void setUseParent(bool useParent);

    /**
     * Set whether showing and hiding of the window will be done with fade-in
     * and fade-out
     * @param animate true if animation should be done
     */
    void setAnimate(bool animate);

    /**
     * Set whether to force an update after showing the window. This is a 
     * bug workaround for undiagnosed problems where HippoBubble can end up 
     * shown without the embedded IE being redrawn.
     * @param updateOnShow true if we should force an update after showing the window.
     */
    void setUpdateOnShow(bool updateOnShow);

    /**
     * Set the class style passed to RegisterClassEx. The default value is
     * CS_HREDRAW | CS_VREDRAE.
     * @param classStyle the new class style
     **/
    void setClassStyle(UINT classStyle);

    /**
     * Set the window style flags that will be passed to CreateWindow()
     * @param windowStyle the window style flags. (See CreateWindow() docs)
     */
    void setWindowStyle(DWORD windowStyle);

    /**
     * Set the extended window style flags that will be passed to CreateWindowEx()
     * @param extendedStyle the extended window style flags. (See CreateWindowEx() docs)
     */
    void setExtendedStyle(DWORD extendedStyle);

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

    /********************************************************/

    /**
     * Get the URL that will be passed to the HippoIE object. The default
     * implementation just returns the result of setURL(), but this 
     * is virtual so that it can be determined dynamically, depending
     * on, for example, the HippoUI object passed to setUI().
     **/
    virtual HippoBSTR getURL();

    /**
     * Do any necessary post-creation initialization of our window
     **/
    virtual void initializeWindow();

    /**
     * Do any initializion of the HippoIE needed before calling ie_->create(),
     * for example, call ie_->setXsltTransform()
     **/
    virtual void initializeIE();

    /**
     * Do any necessary initialization of the browser control 
     * post-creation.
     **/
    virtual void initializeBrowser();

    /** 
     * Do any initialization after the UI is set
     */
    virtual void initializeUI();

    /**
     * Called on incoming messages sent to the window. If you
     * don't handle the event yourself, you should chain up to
     * to the parent class's implementation.
     * @param message the message type
     * @wParam WPARAM from the window procedure
     * @lParam LPARAM from the window procedure
     * @return true if the event was handled, otherwise false (default
     *   handle will be done)
     */
    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    /**
     * Callback when the document finishes loading 
     **/
    virtual void onDocumentComplete();

    /**
     * Callback when the window is closed either by the user
     * or by a script. There is no default implementation, so
     * close attempts will have no effect other than anything
     * you do here.
     * @param fromScript true if a script invoked as window.close()
     **/
    virtual void onClose(bool fromScript);

    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;
    HippoUI* ui_;
    HWND window_;
    DWORD windowStyle_;
    DWORD extendedStyle_;

private:
    bool useParent_;
    bool animate_;
    bool updateOnShow_;
    UINT classStyle_;
    HippoBSTR className_;
    HippoBSTR url_;
    HippoBSTR title_;

    HINSTANCE instance_;
    HippoPtr<IDispatch> application_;

    class HippoAbstractWindowIECallback : public HippoIECallback
    {
    public:
        HippoAbstractWindowIECallback(HippoAbstractWindow *chatWindow) {
            abstractWindow_ = chatWindow;
        }
        HippoAbstractWindow *abstractWindow_;
        void onDocumentComplete();
        void onClose();
        void launchBrowser(const HippoBSTR &url);
        bool isOurServer(const HippoBSTR &host);
        HRESULT getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser);

    };
    HippoAbstractWindowIECallback *ieCallback_;

    bool embedIE(void);
    bool createWindow(void);
    bool registerClass();
    void onWindowDestroyed();

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

    // private so they aren't used
    HippoAbstractWindow(const HippoAbstractWindow &other);
    HippoAbstractWindow& operator=(const HippoAbstractWindow &other);
};
