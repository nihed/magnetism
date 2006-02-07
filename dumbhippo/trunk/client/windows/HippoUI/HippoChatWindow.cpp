/* HippoChatWindow.cpp: Window displaying a chat room for a post
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include "HippoChatWindow.h"
#include "HippoUI.h"

static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

HippoChatWindow::HippoChatWindow(void)
{
    setClassName(L"HippoChatWindowClass");
    setTitle(L"Hippo Chat");
}

HippoChatWindow::~HippoChatWindow(void)
{
}

void 
HippoChatWindow::setPostId(BSTR postId)
{
    postId_ = postId;

    HippoBSTR srcURL;
    ui_->getRemoteURL(L"chatwindow?postId=", &srcURL);
    srcURL.Append(postId_);

    setURL(srcURL);
}

BSTR
HippoChatWindow::getPostId()
{
    return postId_.m_str;
}

void
HippoChatWindow::onClose(bool fromScript)
{
    ui_->onChatWindowClosed(this);
}

void
HippoChatWindow::onDocumentComplete()
{
}
