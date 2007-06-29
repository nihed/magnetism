/* HippoChatWindow.h: Window displaying a ChatWindow for a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <hippo/hippo-basics.h>

class HippoChatManager;

class HippoChatWindow
{
public:
    static HippoChatWindow *createInstance(HippoChatManager *manager);

    virtual bool create() = 0;
    virtual void setForegroundWindow() = 0;
    virtual void setChatId(BSTR chatId) = 0;
    virtual HippoWindowState getWindowState() = 0;
    virtual BSTR getChatId() = 0;

    virtual STDMETHODIMP_(DWORD) AddRef() = 0;
    virtual STDMETHODIMP_(DWORD) Release() = 0;
};
