/* HippoChatWindow.h: Window displaying a ChatWindow for a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include "HippoAbstractWindow.h"

class HippoChatWindow : public HippoAbstractWindow
{
public:
    HippoChatWindow();
    ~HippoChatWindow();

    void setPostId(BSTR postId);
    BSTR getPostId();

protected:
    void onClose(bool fromScript);

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

private:
    HippoBSTR postId_;
};
