/* HippoTrackerUpdater.h: Asynchronous notification of changes to a browser's title
 *
 * Copyright Red Hat, Inc. 2006
 */

#include <shlobj.h>
#include <HippoUtil.h>

class HippoTrackerUpdater
{
public:
    HippoTrackerUpdater(IHippoTracker *tracker, IWebBrowser2 *browser);
    ~HippoTrackerUpdater();

    // Pass a new url and name to any UI's that we are updating
    void setInfo(const HippoBSTR &url, const HippoBSTR &name);

private:
    IHippoTracker *tracker_;
    HippoPtr<IWebBrowser2> browser_;

    HANDLE thread_; // The worker thread
    HANDLE updateSemaphore_; // Signalled when needUpdate has been set

    CRITICAL_SECTION criticalSection_; // Protects the following variable
    bool needUpdate_; // Indicates that either the name/url has changed or shouldExit_ is set
    bool shouldExit_; // Done, worker thread should unregister and exit
    HippoBSTR name_; // Current name
    HippoBSTR url_; // Current url

    // Information about the UI's we are currently talking to
    HippoPtr<IHippoUI> ui_;
    DWORD registerCookie_;
    bool registered_;

    HippoPtr<IHippoUI> dogfoodUi_;
    DWORD dogfoodRegisterCookie_;
    bool dogfoodRegistered_;

    HippoPtr<IHippoUI> debugUi_;
    DWORD debugRegisterCookie_;
    bool debugRegistered_;

    // This window is used for receiving notifications when a new UI instance is started
    HWND window_;
    UINT uiStartedMessage_;

    bool createWindow();
    bool registerWindowClass();

    void clearUI();
    void onUIStarted();
    void registerBrowser();
    void unregisterBrowser();

    void setNeedUpdate();
    bool processUpdate();

    void run();

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);
    static DWORD WINAPI threadProc(void *data);
};
