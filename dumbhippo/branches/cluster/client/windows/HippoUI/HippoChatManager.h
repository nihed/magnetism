/* HippoChatManager.h: Manager chat windows in a separate thread
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <HippoMessageHook.h>
#include <HippoThreadExecutor.h>
#include <HippoUtil.h>

class HippoChatWindow;

class HippoChatManager
{
public:
    static HippoChatManager *createInstance(IHippoUI *ui);

    virtual ~HippoChatManager() {}
    virtual void showChatWindow(BSTR chatId) = 0;
    virtual void onChatWindowClosed(HippoChatWindow *chatWindow) = 0;
    virtual void registerMessageHook(HWND window, HippoMessageHook *hook) = 0;
    virtual void unregisterMessageHook(HWND window) = 0;

    /**
     * Returns a pointer to the UI COM object (not C++ object) that is properly
     * marshalled for use within the executor thread
     */
    virtual HippoPtr<IHippoUI> getThreadUI() = 0;
    
    /**
     * Force JScript garbage collection once the executor thread is idle
     */
    virtual void gcWhenIdle() = 0;

    /**
     * Gets the executor object; useful for queing async tasks
     */
    virtual HippoThreadExecutor *getExecutor() = 0;
};
