#include "stdafx.h"

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include "HippoMySpace.h"
#include "HippoHttp.h"
#include "HippoUI.h"

#define HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS (30)
#define HIPPO_MYSPACE_RETRY_FRIENDID_SECS (20 * 60)
#define HIPPO_MYSPACE_POLL_START_INTERVAL_SECS (7*60)
#define HIPPO_MYSPACE_REFRESH_CONTACT_INTERVAL_SECS (30*60)

HippoMySpace::HippoMySpace(BSTR name, HippoUI *ui)
{
    ui_ = ui;
    name_ = name;
    lastUpdateTime_ = 0;
    friendId_ = 0;

    mySpaceIdSize_ = 14;

    idleGetFriendIdId_ = 0;
    idlePollMySpaceId_ = 0;
    idleRefreshContactsId_ = 0;

    blogUrlPrefix_ = "http://blog.myspace.com/index.cfm?fuseaction=blog.view";
    blogPostPrefix_ = L"http://blog.myspace.com/index.cfm?fuseaction=blog.comment";

    setState(HippoMySpace::State::RETRIEVING_SAVED_COMMENTS);
    ui_->getSeenMySpaceComments(); // Start retriving seen comments
}

HippoMySpace::~HippoMySpace(void)
{
    if (idleGetFriendIdId_ > 0)
        g_source_remove(idleGetFriendIdId_);
    if (idlePollMySpaceId_ > 0)
        g_source_remove(idlePollMySpaceId_);
    if (idleRefreshContactsId_ > 0)
        g_source_remove(idleRefreshContactsId_);
}

static void
handleHtmlTag(struct taginfo *tag, void *data)
{
    HippoBSTR *str = (HippoBSTR *) data;
}

void
HippoMySpace::sanitizeCommentHTML(BSTR html, HippoBSTR &ret)
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

UINT
HippoMySpace::idleGetFriendId(void * data)
{
    HippoMySpace *mySpace = (HippoMySpace *) data;
    mySpace->getFriendId();
    return FALSE;
}

void
HippoMySpace::getFriendId()
{
    setState(HippoMySpace::State::FINDING_FRIENDID);
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
    refreshContacts();
}

void
HippoMySpace::refreshContacts()
{
    if (state_ == HippoMySpace::State::RETRIEVING_SAVED_COMMENTS)
        setState(HippoMySpace::State::RETRIEVING_CONTACTS);
    else if (state_ == HippoMySpace::State::IDLE)
        setState(HippoMySpace::State::REFRESHING_CONTACTS);
    else
        ui_->debugLogU("unknown HippoMySpace state %d for refreshContacts()", state_);
    ui_->getMySpaceContacts();
}

void
HippoMySpace::setContacts(HippoArray<HippoMySpaceContact *> &contacts)
{
    for (UINT i = 0; i < contacts.length(); i++) {
        contacts_.append(contacts[i]);
    }
    if (state_ == HippoMySpace::State::RETRIEVING_CONTACTS)
        getFriendId(); // Now poll the web page
    else {
        assert(state_ == HippoMySpace::State::REFRESHING_CONTACTS);
        setState(HippoMySpace::State::IDLE);
        idleRefreshContactsId_ = g_timeout_add(HIPPO_MYSPACE_REFRESH_CONTACT_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshContacts, this);
    }
}

UINT
HippoMySpace::idleRefreshContacts(void *data)
{
    HippoMySpace *mySpace = (HippoMySpace*)data;
    if (mySpace->state_ != HippoMySpace::State::IDLE) { // If we're busy, requeue this
        mySpace->ui_->debugLogU("HippoMySpace currently busy, requeueing comment refresh");
        mySpace->idleRefreshContactsId_ = g_timeout_add(HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshContacts, mySpace);
        return FALSE;
    }
    mySpace->refreshContacts();
    return FALSE;
}

void
HippoMySpace::HippoMySpaceFriendIdHandler::handleError(HRESULT res) 
{
    myspace_->ui_->logHresult(L"got error while retriving MySpace friend id, queuing retry", res);
    myspace_->idleGetFriendIdId_ = g_timeout_add(HIPPO_MYSPACE_RETRY_FRIENDID_SECS * 1000, (GSourceFunc) HippoMySpace::idleGetFriendId, myspace_);
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
                myspace_->refreshComments();
            }
            return;
        }
    }
    myspace_->ui_->debugLogU("failed to find myspace friend id");
    delete this;
}

void
HippoMySpace::refreshComments()
{
    if (state_ == HippoMySpace::State::FINDING_FRIENDID)
        setState(HippoMySpace::State::INITIAL_COMMENT_SCAN);
    else
        setState(HippoMySpace::State::COMMENT_CHANGE_POLL);
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
    if (mySpace->state_ != HippoMySpace::State::IDLE) { // If we're busy, requeue this
        mySpace->ui_->debugLogU("HippoMySpace not idle, requeuing comment refresh");
        mySpace->idlePollMySpaceId_ = g_timeout_add(HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshComments, mySpace);
        return FALSE;
    }
    mySpace->refreshComments();
    return FALSE;
}

void
HippoMySpace::HippoMySpaceCommentHandler::handleError(HRESULT res) 
{
    myspace_->ui_->logHresult(L"error while retriving MySpace blog feed", res);
}

void
HippoMySpace::setState(HippoMySpace::State newState)
{
    ui_->debugLogU("HippoMySpace changing to state: %d", newState);
    state_ = newState;
}

void 
HippoMySpace::HippoMySpaceCommentHandler::handleComplete(void *responseData, long responseBytes)
{
    myspace_->ui_->debugLogU("got myspace blog feed");
    const char *profileSectionElt = "<td class=\"blogCommentsProfile\">";
    const char *response = (const char*)responseData;
    const char *profileSectionStart;

    myspace_->idlePollMySpaceId_ = g_timeout_add(HIPPO_MYSPACE_POLL_START_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshComments, myspace_);
    if (!(profileSectionStart = strstr(response, profileSectionElt))) {
        myspace_->setState(HippoMySpace::State::IDLE);
        myspace_->ui_->debugLogU("failed to find blog comment profile (possibly no comments)");
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
        myspace_->sanitizeCommentHTML(rawHtml, commentData.content);
        myspace_->addBlogComment(commentData);
        profile += strlen(profileSectionElt);
    }
    if (myspace_->state_ == HippoMySpace::State::INITIAL_COMMENT_SCAN) {
        // Queue the initial contact refresh now
        myspace_->ui_->debugLogU("queueing idle contact refresh");
        myspace_->idleRefreshContactsId_ = g_timeout_add(HIPPO_MYSPACE_REFRESH_CONTACT_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshContacts, myspace_);
    }
    myspace_->setState(HippoMySpace::State::IDLE);
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

bool
HippoMySpace::handlePostStart(HippoBrowserInfo &browser)
{
    const WCHAR *friendIdParam = L"&friendID=";
    const WCHAR *friendId = wcsstr(browser.url, friendIdParam);
    if (!friendId) {
        ui_->debugLogU("failed to find friendId parameter");
        return false;
    }
    const WCHAR *friendIdStart = friendId + wcslen(friendIdParam);
    const WCHAR *friendIdEnd = wcschr(friendIdStart, '&');
    if (!friendIdEnd) {
        ui_->debugLogU("failed to find end of friendId parameter");
        return false;
    }
    HippoBSTR friendIdStr(friendIdEnd - friendIdStart, friendIdStart);
    for (UINT i = 0; i < activeContactPosts_.length(); i++) {
        if (wcscmp(activeContactPosts_[i]->getContact()->getFriendId().m_str, friendIdStr) == 0)
            return true;
    }
    ui_->debugLogU("posting on myspace blog for contact, friendId=%S", friendIdStr.m_str);
    for (UINT i = 0; i < contacts_.length(); i++) {
        if (wcscmp(contacts_[i]->getFriendId().m_str, friendIdStr) != 0)
            continue;
        HippoMySpaceContactPost *post = new HippoMySpaceContactPost(contacts_[i], browser.browser);
        activeContactPosts_.append(post);
        return true;
    }
    return false;
}

void
HippoMySpace::browserChanged(HippoBrowserInfo &browser)
{
    // Check if this is a post
    if (wcsncmp(browser.url, blogPostPrefix_, blogPostPrefix_.Length()) == 0) {
        if (handlePostStart(browser))
            return;
    }
    for (UINT i = 0; i < activeContactPosts_.length(); i++) {
        if (activeContactPosts_[i]->getBrowser() == browser.browser) {
            ui_->onCreatingMySpaceContactPost(activeContactPosts_[i]->getContact());
            delete activeContactPosts_[i];
            activeContactPosts_.remove(i);
            return;
        }
    }

}

void
HippoMySpace::onReceivingMySpaceContactPost()
{
    ui_->debugLogU("got contact post, re-polling");
    if (idlePollMySpaceId_ > 0)
        g_source_remove(idlePollMySpaceId_);
    refreshComments();
}