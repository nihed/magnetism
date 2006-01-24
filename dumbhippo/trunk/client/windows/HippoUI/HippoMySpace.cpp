#include "stdafx.h"

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include "HippoMySpace.h"
#include "HippoHttp.h"
#include "HippoUI.h"

#define HIPPO_MYSPACE_POLL_START_INTERVAL_SECS (7*60)

HippoMySpace::HippoMySpace(BSTR name, HippoUI *ui)
{
    ui_ = ui;
    name_ = name;
    lastUpdateTime_ = 0;
    friendId_ = 0;

    mySpaceIdSize_ = 14;

    blogUrlPrefix_ = "http://blog.myspace.com/index.cfm?fuseaction=blog.view";

    state_ = HippoMySpace::State::RETRIEVING_SAVED_COMMENTS;
    ui_->getSeenMySpaceComments(); // Start retriving seen comments
}

HippoMySpace::~HippoMySpace(void)
{
}

static void
handleHtmlTag(struct taginfo *tag, void *data)
{
    HippoBSTR *str = (HippoBSTR *) data;
}

void
HippoMySpace::SanitizeCommentHTML(BSTR html, HippoBSTR &ret)
{
    // To hopefully be replaced by a somewhat more real parser later.
    UINT len = ::SysStringLen(html);
    for (UINT i = 0; i < len; i++) {
        if (html[i] == '<') {
            while (i < len && html[i] != '>') {
                i++;
            }
            if (i < len)
                i++;
        } else if (html[i] == '&') {
            while (i < len && html[i] != ';' && !iswspace(html[i])) {
                i++;
            }
            if (i < len)
                i++;
        } else if (iswalnum(html[i]) || iswspace(html[i])) {
            ret.Append(html[i]);
        }
    }
    return;
}

void
HippoMySpace::GetFriendId()
{
    state_ = HippoMySpace::State::FINDING_FRIENDID;
    HippoMySpace::HippoMySpaceFriendIdHandler *handler = new HippoMySpace::HippoMySpaceFriendIdHandler(this);
    HippoHTTP *http = new HippoHTTP();
    HippoBSTR url(L"http://www.myspace.com/");
    url.Append(name_);
    http->doGet(url.m_str, false, handler);
}

void 
HippoMySpace::setSeenComments(HippoArray<HippoMySpaceBlogComment*> *comments)
{
    ui_->debugLogU("got %d MySpace comments seen", comments->length());
    for (UINT i = 0; i < comments->length(); i++) {
        comments_.append((*comments)[i]);
    }
    GetFriendId(); // Now poll the web page
}

void
HippoMySpace::HippoMySpaceFriendIdHandler::handleError(HRESULT res) 
{
    myspace_->ui_->logError(L"got error while retriving MySpace friend id", res);
    delete this;
}

void
HippoMySpace::HippoMySpaceFriendIdHandler::handleContentType(WCHAR *mime, WCHAR *charset)
{
    myspace_->ui_->debugLogW(L"got mime type %s, charset %s", mime, charset);
}

void 
HippoMySpace::HippoMySpaceFriendIdHandler::handleComplete(void *responseData, long responseBytes)
{
    char *response = (char*)responseData;
    char *ptr = strstr(response, myspace_->blogUrlPrefix_);
    if (ptr) {
        const char *friendIDParam = "&friendID=";
        char *friendIdStart = ptr + strlen(myspace_->blogUrlPrefix_) + strlen(friendIDParam);
        const char *blogIdParam = "&blogID=";
        char *next = strstr(friendIdStart, blogIdParam);
        if (next && ((next - friendIdStart) <= myspace_->mySpaceIdSize_)) {
            char *blogIdStr = next + strlen(blogIdParam);
            char *end = strchr(blogIdStr, '"');
            if (end && (end - blogIdStr) <= myspace_->mySpaceIdSize_) {
                myspace_->friendId_ = strtol(friendIdStart, NULL, 10);
                myspace_->blogId_ = strtol(blogIdStr, NULL, 10);
                myspace_->ui_->debugLogU("got myspace friend id: %d, blogid: %d", myspace_->friendId_, myspace_->blogId_);
                myspace_->RefreshComments();
            }
            return;
        }
    }
    myspace_->ui_->debugLogU("failed to find myspace friend id");
    delete this;
}

void
HippoMySpace::RefreshComments()
{
    if (state_ == HippoMySpace::State::FINDING_FRIENDID)
        state_ = HippoMySpace::State::INITIAL_COMMENT_SCAN;
    else
        state_ = HippoMySpace::State::COMMENT_CHANGE_POLL;
    HippoMySpace::HippoMySpaceCommentHandler *handler = new HippoMySpace::HippoMySpaceCommentHandler(this);
    HippoHTTP *http = new HippoHTTP();
    HippoBSTR url;
    url.setUTF8(blogUrlPrefix_);
    url.Append(L"&friendID=");
    WCHAR buf[40];
    StringCchPrintfW(buf, sizeof(buf)/sizeof(buf[0]), L"%d", friendId_);
    url.Append(buf);
    url.Append(L"&blogID=");
    StringCchPrintfW(buf, sizeof(buf)/sizeof(buf[0]), L"%d", blogId_);
    url.Append(buf);
    http->doGet(url.m_str, false, handler);
}

UINT
HippoMySpace::idleRefreshComments(void *data)
{
    HippoMySpace *mySpace = (HippoMySpace*)data;
    mySpace->RefreshComments();
    return FALSE;
}

void
HippoMySpace::HippoMySpaceCommentHandler::handleError(HRESULT res) 
{
    myspace_->ui_->logError(L"error while retriving MySpace blog feed", res);
}

void 
HippoMySpace::HippoMySpaceCommentHandler::handleComplete(void *responseData, long responseBytes)
{
    myspace_->ui_->debugLogU("got myspace blog feed");
    const char *profileSectionElt = "<td class=\"blogCommentsProfile\">";
    const char *response = (const char*)responseData;
    const char *profileSectionStart;

    g_timeout_add(HIPPO_MYSPACE_POLL_START_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshComments, this->myspace_);

    myspace_->state_ = HippoMySpace::State::IDLE;
    if (!(profileSectionStart = strstr(response, profileSectionElt))) {
        myspace_->ui_->debugLogU("failed to find blog comment profile");
        return;
    }

    const char *profile = profileSectionStart;
    while ((profile = strstr(profile, profileSectionElt)) != NULL) {
        const char *imglinkStr = "<img src=\"";
        const char *imglink = strstr(profile, imglinkStr);
        if (!imglink)
            break;
        const char *imglinkStart = imglink + strlen(imglinkStr);
        const char *imglinkEnd = strchr(imglinkStart, '"');
        if (!imglinkEnd)
            break;

        const char *commentSectionElt = "<td class=\"blogComments\">";
        const char *commentSectionStart;
        if (!(commentSectionStart = strstr(profile, commentSectionElt)))
            break;

        const char *commentStartStr = "<p class=\"blogCommentsContent\">";
        const char *comment = strstr(commentSectionStart, commentStartStr);
        if (!comment)
            break;
        const char *commentStart = comment + strlen(commentStartStr);
        const char *commentEnd = strstr(comment, "</p>");
        if (!commentEnd)
            break;
        const char *friendLink = "Posted by <a href=\"http://profile.myspace.com/index.cfm?fuseaction=user.viewprofile&friendID=";
        const char *postedByStart = strstr(commentEnd, friendLink);
        if (!postedByStart)
            break;
        const char *postedByEnd = strstr(postedByStart, "</p>");
        if (!postedByEnd)
            break;
        const char *friendIdStart = postedByStart + strlen(friendLink);
        const char *friendIdEnd = strchr(friendIdStart, '"');
        if (!friendIdEnd)
            break;
        if (friendIdEnd - friendIdStart > myspace_->mySpaceIdSize_)
            break;
        const char *friendNameStr = "\"><b>";
        if (strncmp(friendIdEnd, friendNameStr, strlen(friendNameStr)) != 0)
            break;
        const char *friendNameStart = friendIdEnd + strlen(friendNameStr);
        const char *friendNameEnd = strstr(friendNameStart, "</b>");
        if (!friendNameEnd)
            break;
        const char *commentIdStr = "&journalDetailID=";
        const char *commentIdLink = strstr(friendIdEnd, commentIdStr);
        if (!commentIdLink)
            break;
        const char *commentIdStart = commentIdLink + strlen(commentIdStr);
        const char *commentIdEnd = strchr(commentIdStart, '&');
        if (!commentIdEnd)
            break;
        if (commentIdEnd - commentIdStart > myspace_->mySpaceIdSize_)
            break;
        HippoMySpaceBlogComment commentData;
        commentData.commentId = strtol(commentIdStart, NULL, 10);
        commentData.posterId = strtol(friendIdStart, NULL, 10);
        commentData.posterName.setUTF8(friendNameStart, (UINT) (friendNameEnd - friendNameStart));
        commentData.posterImgUrl.setUTF8(imglinkStart, imglinkEnd - imglinkStart);
        HippoBSTR rawHtml;
        rawHtml.setUTF8(commentStart, (UINT) (commentEnd - commentStart));
        myspace_->SanitizeCommentHTML(rawHtml, commentData.content);
        myspace_->addBlogComment(commentData);
        profile += strlen(profileSectionElt);
    }
}

void
HippoMySpace::addBlogComment(HippoMySpaceBlogComment &comment)
{
    for (UINT i = 0; i < comments_.length(); i++) {
        if (comments_[i]->commentId == comment.commentId) {
            ui_->debugLogU("already seen myspace comment %d", comment.commentId);
            return;
        }
    }
    ui_->debugLogU("appending myspace comment %d", comment.commentId);
    comments_.append(new HippoMySpaceBlogComment(comment));
    // We only display them if this is the polling state, otherwise just record
    // them as seen - we don't want to display all the comments the first time
    // they log in
    bool doDisplay = (state_ == HippoMySpace::State::COMMENT_CHANGE_POLL);
    ui_->onNewMySpaceComment(friendId_, blogId_, comment, doDisplay);
}