/* HippoAbstractWindow.h: Base class for Windows windows.
 *
 * Copyright Red Hat, Inc. 2005, 2006
 **/
#pragma once

#include <HippoMessageHook.h>
#include <HippoUtil.h>
#include <hippo/hippo-basics.h>
#include <hippo/hippo-graphics.h>

class HippoUI;

class HippoAbstractWindow : public HippoMessageHook
{
public:
    HIPPO_DECLARE_REFCOUNTING;

    HippoAbstractWindow();

    /**
     * Provide a pointer to the main UI object for the application. Mandatory to call
     * before create().
     * @param ui the main UI object
     */
    void setUI(HippoUI *ui);

    /**
     * Set the title of the window. Note that this currently is always used, even if 
     * e.g. the window contains a browser control that loads a document with a title.
     * Must be called before create() to have
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
    virtual bool create();

    /**
     * Destroy the window and shutdown any stuff in it.
     */
    void destroy();

    /**
     * Make the window visible onscreen.
     */
    virtual void show(BOOL activate = true);

    /**
     * Hide the window 
     */
    virtual void hide();

    /**
     * Set the window as the "foreground window". This will either bring the window to
     * the front, or flash it in the user's task list if Windows doesn't want to
     * steal focus from the user.
     */
    void setForegroundWindow();

    void invalidate(int x, int y, int width, int height);

    void getClientArea(HippoRectangle *rect);

    int getX();

    int getY();

    int getWidth();

    int getHeight();

    bool isCreated() { return created_; }

    bool isShowing() { return showing_; }

    void setDefaultSize(int width, int height);
    void setDefaultPosition(int x, int y);
    void setPosition(int x, int y);
    void setSize(int width, int height);

    const HippoBSTR& getClassName() const { return className_; }

    ////////////////////////////////////////////////////////////

    virtual bool hookMessage(MSG *msg);

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
     * Sets the parent window to create the control with
     */
    void setCreateWithParent(HippoAbstractWindow* parent);
        
    /********************************************************/

    /**
     * Do any necessary post-creation initialization of our window
     **/
    virtual void initializeWindow();

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
     * Callback when the window is closed either by the user
     * or by a script. There is no default implementation, so
     * close attempts will have no effect other than anything
     * you do here.
     * @param fromScript true if a script invoked as window.close()
     **/
    virtual void onClose(bool fromScript);

    virtual void onWindowDestroyed();

    // called for WM_SIZE / WM_MOVE, but *not* for move resizes in general
    virtual void onMoveResizeMessage(const HippoRectangle *newClientArea);

    DWORD getWindowStyle() const { return windowStyle_; }
    DWORD getExtendedStyle() const { return extendedStyle_; }

    HippoUI* ui_;
    HWND window_;

    // protected since we're refcounted; subclasses
    // ideally override destroy() instead
    virtual ~HippoAbstractWindow();

    void moveResizeWindow(int x, int y, int width, int height);

private:
    bool useParent_;
    HippoAbstractWindow *createWithParent_;
    bool animate_;
    bool updateOnShow_;
    UINT classStyle_;
    HippoBSTR className_;
    HippoBSTR title_;
    DWORD windowStyle_;
    DWORD extendedStyle_;

    HINSTANCE instance_;

    DWORD refCount_;

    int x_;
    int y_;
    int width_;
    int height_;

    unsigned int defaultPositionSet_ : 1;

    unsigned int created_ : 1;
    unsigned int showing_ : 1;
    unsigned int destroyed_ : 1;

    bool createWindow(void);
    bool registerClass();

    void convertClientRectToWindowRect(HippoRectangle *rect);
    void queryCurrentClientRect(HippoRectangle *rect);

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

    // private so they aren't used
    HippoAbstractWindow(const HippoAbstractWindow &other);
    HippoAbstractWindow& operator=(const HippoAbstractWindow &other);
};
