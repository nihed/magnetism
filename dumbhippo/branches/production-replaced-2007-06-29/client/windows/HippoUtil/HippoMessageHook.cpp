/* HippoMessageHook.cpp: Manage functions to be run on messages for a particular window
 *
 * Copyright Red Hat, Inc. 2006
 */
#include "stdafx-hippoutil.h"
#include <vector>
#include "HippoMessageHook.h"

class HippoMessageHookInfo
{
public:
    HippoMessageHookInfo(HWND window, HippoMessageHook *hook) : 
      window_(window), hook_(hook), inInvoke_(false) {}
    HWND getWindow() { return window_; }
    HippoMessageHook *getHook() { return hook_; }

    // We track whether we are currently invoking a hook in the
    // list using a boolean variable.
    bool getInInvoke() { return inInvoke_; }
    void setInInvoke(bool inInvoke) { inInvoke_ = inInvoke; }

    void clear() {
        hook_ = 0;
    }
    bool isCleared() {
        return hook_ == 0;
    }

private:
    HWND window_;
    HippoMessageHook *hook_;
    bool inInvoke_;
};

class HippoMessageHookListImpl
{
public:
    void registerMessageHook(HWND window, HippoMessageHook *hook);
    void unregisterMessageHook(HWND window);
    bool processMessage(MSG *message);

private:
    std::vector<HippoMessageHookInfo> hooks_;
};

void
HippoMessageHookListImpl::registerMessageHook(HWND window, HippoMessageHook *hook)
{
    hooks_.push_back(HippoMessageHookInfo(window, hook));
}

void
HippoMessageHookListImpl::unregisterMessageHook(HWND window)
{
    for (std::vector<HippoMessageHookInfo>::iterator i = hooks_.begin();
         i != hooks_.end();
         i++)
    {
        if (i->getWindow() == window) {
            if (i->getInInvoke())
                i->clear();
            else
                hooks_.erase(i);
            return;
        }
    }
}

bool
HippoMessageHookListImpl::processMessage(MSG *message)
{
    for (std::vector<HippoMessageHookInfo>::iterator i = hooks_.begin();
         i != hooks_.end();)
    {
        if (IsChild(i->getWindow(), message->hwnd)) {
            i->setInInvoke(true);
            bool result = i->getHook()->hookMessage(message);
            i->setInInvoke(false);
            if (i->isCleared()) {
                i = hooks_.erase(i);
                if (result)
                    return true;
                else
                    continue;
            } else {
                if (result)
                    return true;
            }
        }
        i++;
    }

    return false;
 }

HippoMessageHookList::HippoMessageHookList()
{
    impl_ = new HippoMessageHookListImpl();
}

HippoMessageHookList::~HippoMessageHookList()
{
//    delete impl_;
}

void
HippoMessageHookList::registerMessageHook(HWND window, HippoMessageHook *hook)
{
    impl_->registerMessageHook(window, hook);
}

void
HippoMessageHookList::unregisterMessageHook(HWND window)
{
    impl_->unregisterMessageHook(window);
}

bool
HippoMessageHookList::processMessage(MSG *message)
{
    return impl_->processMessage(message);
}
