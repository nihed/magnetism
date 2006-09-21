/* HippoAbstractIEWindow.h: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005, 2006
 **/
#pragma once

#include <HippoIE.h>
#include <HippoMessageHook.h>
#include "HippoAbstractWindow.h"

class HippoUI;

class HippoAbstractIEWindow : public HippoAbstractWindow
{
public:
    HippoAbstractIEWindow();

    /**
     * Provide an (optional) object that will be available as window.external.application
     * Must be called before create() if called at all.
     * @param application pointer to the COM object to make available to javascript
     */
    void setApplication(IDispatch *application);

    /**
     * Actually go ahead and create the window by chaining up, then embed IE in it. This must be called
     * before any methods that manipulate the embedded IE or window.
     *
     * FIXME: We possibly should do this behind the scenes
     */
    virtual bool create();

    /**
     * Get the HippoIE object that wraps the embedded web browser control
     * @return the HippoIE object
     */
    HippoIE *getIE();

    ////////////////////////////////////////////////////////////

    virtual bool hookMessage(MSG *msg);

protected:

    virtual void destroy();

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
     * Callback when the document finishes loading 
     **/
    virtual void onDocumentComplete();

    virtual void onWindowDestroyed();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    ~HippoAbstractIEWindow() {}

private:

    HippoBSTR url_;
    HippoPtr<IDispatch> application_;

    class HippoAbstractWindowIECallback : public HippoIECallback
    {
    public:
        HippoAbstractWindowIECallback(HippoAbstractIEWindow *chatWindow) {
            abstractWindow_ = chatWindow;
        }
        HippoAbstractIEWindow *abstractWindow_;
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

    // private so they aren't used
    HippoAbstractIEWindow(const HippoAbstractIEWindow &other);
    HippoAbstractIEWindow& operator=(const HippoAbstractIEWindow &other);
};
