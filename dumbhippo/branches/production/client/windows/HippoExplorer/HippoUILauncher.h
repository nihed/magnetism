/* HippoUILauncher.h: COM object called by toolbar button
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <HippoUtil.h>

class HippoUILaunchListener;

class HippoUILauncher
{
public:
    HippoUILauncher();
    ~HippoUILauncher(void);

    void setListener(HippoUILaunchListener *listener);

    /**
     * Find an existing HippoUI instance.
     * @param ui location to store the HippoUI pointer if found (with a new refcount)
     * @param user (optional) if set, prefer an instance with this user; this
     *    feature is to allow disambiguating in devel environments where there
     *    are multiple copies of HippoUI running.
     * @return S_OK on success, otherwise a failure code
     **/
    HRESULT getUI(IHippoUI **ui, BSTR user = NULL);

    /**
     * Launch a new HippoUI instance; either the onLauncherSuccess()
     * or the onLaunchFailure() method will be called on the listener
     * set with setListener().
     * @return S_OK on success, otherwise a failure code
     **/
    HRESULT launchUI();

private:
    // Helper for getUI()
    void checkOneInstance(const CLSID &classId, BSTR user, IHippoUI **exact, IHippoUI **approximate);

    bool spawnUI();
    bool createWindow();
    bool registerWindowClass();

    void uiWaitTimer();
    void stopUIWait();

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

    HWND window_;

    unsigned uiWaitCount_;
    HippoUILaunchListener *listener_;
};

class HippoUILaunchListener
{
public:
    virtual void onLaunchSuccess(HippoUILauncher *launcher, IHippoUI *ui) = 0;
    virtual void onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason) = 0;

    virtual ~HippoUILaunchListener() {}
};
