/* HippoChatManager.cpp: Manager chat windows in a separate thread
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx-hippoui.h"
#include <vector>

#include "HippoChatManager.h"
#include "HippoChatWindow.h"
#include "HippoJScriptEngine.h"
#include <HippoUtil.h>

class HippoChatManagerGCTask : public HippoThreadTask
{
public:
    void call();
    void cancel();
};

void
HippoChatManagerGCTask::call()
{
    // This is fairly horrible; we need to force garbage collection in
    // the executor thread after we've closed the HippoChatWindow
    // (See comments in HippoChatWindow::onWindowDestroy.) The only
    // way I know do that is to have a pointer to an IActiveScript
    // object. So, we create one, force the garbage collection, and
    // then immediately destroy it.

    HippoPtr<HippoJScriptEngine> engine;
    HippoJScriptEngine::createInstance(&engine);
    // Creating and destroying the engine is actually enough to force
    // a garbage collection; but do it explicitly rather than relying
    // on a side effect.
    engine->forceGC();
    engine->close();

    delete this;
}

void
HippoChatManagerGCTask::cancel()
{
    delete this;
}

class HippoChatManagerImpl : public HippoChatManager, public HippoThreadExecutorHelper
{
public:
    HippoChatManagerImpl(IHippoUI *ui);
    virtual ~HippoChatManagerImpl();
    virtual void showChatWindow(BSTR chatId);
    virtual void registerMessageHook(HWND window, HippoMessageHook *hook);
    virtual void unregisterMessageHook(HWND window);
    virtual HippoPtr<IHippoUI> getThreadUI();
    virtual void gcWhenIdle();
    virtual HippoThreadExecutor *getExecutor();

    virtual void init();
    virtual void shutdown();

    void doShowChatWindow(BSTR chatId);
    void onChatWindowClosed(HippoChatWindow *chatWindow);

private:
    HippoThreadExecutor *executor_;
    std::vector<HippoChatWindow *> chatWindows_;
    HippoPtr<IHippoUI> threadUI;

    void setUI(IStream *marshalledPointer);
};

class HippoShowChatWindowTask : public HippoThreadTask
{
public:
    HippoShowChatWindowTask(HippoChatManagerImpl *impl, BSTR chatId) 
        : impl(impl), chatId_(chatId) {
    }
    void call() { impl->doShowChatWindow(chatId_); delete this; }
    void cancel() { delete this; }
private:
    HippoChatManagerImpl *impl;
    HippoBSTR chatId_;
};

HippoChatManager *
HippoChatManager::createInstance(IHippoUI *ui)
{
    return new HippoChatManagerImpl(ui);
}

HippoChatManagerImpl::HippoChatManagerImpl(IHippoUI *ui)
{
    executor_ = HippoThreadExecutor::createInstance(this);

    // The executor thread needs a reference to the UI as a marshaled COM 
    // pointer, since the C++ object is not remotely thread safe
    IStream *stream;
    HRESULT hr = CoMarshalInterThreadInterfaceInStream(IID_IHippoUI, ui, &stream);
    if (SUCCEEDED(hr)) {
        executor_->callSync(this, &HippoChatManagerImpl::setUI, stream);
        stream->Release();
    } else {
        hippoDebugLogW(L"Couldn't create marshaled IHippoUI pointer to pass to executor thread");
    }
}

HippoChatManagerImpl::~HippoChatManagerImpl()
{
    delete executor_;
}

void
HippoChatManagerImpl::showChatWindow(BSTR chatId)
{
    executor_->doAsync(new HippoShowChatWindowTask(this, chatId));
}

void 
HippoChatManagerImpl::registerMessageHook(HWND window, HippoMessageHook *hook)
{
    executor_->registerMessageHook(window, hook);
}

void
HippoChatManagerImpl::unregisterMessageHook(HWND window)
{
    executor_->unregisterMessageHook(window);
}

HippoPtr<IHippoUI> 
HippoChatManagerImpl::getThreadUI()
{
    return threadUI;
}

void 
HippoChatManagerImpl::init()
{
    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
}

void
HippoChatManagerImpl::shutdown()
{
    for (std::vector<HippoChatWindow *>::iterator i = chatWindows_.begin();
         i != chatWindows_.end();
         i++)
    {
        delete *i;
    }
    chatWindows_.clear();

    CoUninitialize();
}

void
HippoChatManagerImpl::doShowChatWindow(BSTR chatId)
{
    // If a chat window already exists for the post, just raise it
    for (std::vector<HippoChatWindow *>::iterator i = chatWindows_.begin();
         i != chatWindows_.end();
         i++)
    {
        if (wcscmp((*i)->getChatId(), chatId) == 0) {
            (*i)->setForegroundWindow();
            return;
        }
    }

    HippoChatWindow *window = HippoChatWindow::createInstance(this);
    window->setChatId(chatId);

    chatWindows_.push_back(window);

    window->create();
    window->setForegroundWindow();
}

void 
HippoChatManagerImpl::onChatWindowClosed(HippoChatWindow *chatWindow)
{
    for (std::vector<HippoChatWindow *>::iterator i = chatWindows_.begin();
         i != chatWindows_.end();
         i++)
    {
        if (*i == chatWindow) {
            chatWindows_.erase(i);
            chatWindow->Release(); // should be safe, called from WM_CLOSE only

            return;
        }
    }

    assert(false);
}

void 
HippoChatManagerImpl::setUI(IStream *marshalledPointer)
{
    CoUnmarshalInterface(marshalledPointer, IID_IHippoUI, (void **)&threadUI);
}

void
HippoChatManagerImpl::gcWhenIdle()
{
    executor_->doAsync(new HippoChatManagerGCTask());
}

HippoThreadExecutor *
HippoChatManagerImpl::getExecutor()
{
    return executor_;
}
