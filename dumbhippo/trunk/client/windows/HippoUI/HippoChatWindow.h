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
    HippoBSTR getURL();
    HippoBSTR getClassName();
    HippoBSTR getTitle();

    void onClose(bool fromScript);
    void onDocumentComplete();

private:
    HippoBSTR postId_;
};
