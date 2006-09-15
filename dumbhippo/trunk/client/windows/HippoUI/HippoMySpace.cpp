#include "stdafx-hippoui.h"

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include "HippoMySpace.h"
#include "HippoHttp.h"
#include "HippoUI.h"

#define HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS (30)
#define HIPPO_MYSPACE_RETRY_FRIENDID_SECS (20 * 60)
#define HIPPO_MYSPACE_POLL_START_INTERVAL_SECS (7*60)
#define HIPPO_MYSPACE_REFRESH_CONTACT_INTERVAL_SECS (30*60)

#define HIPPO_MYSPACE_ID_SIZE 14

HippoMySpace::HippoMySpace()
: ui_(NULL)
{

}

void
HippoMySpace::setUI(HippoUI *ui)
{
    ui_ = ui;

    blogUrlPrefix_ = "http://blog.myspace.com/index.cfm?fuseaction=blog.view";
    blogPostPrefix_ = L"http://blog.myspace.com/index.cfm?fuseaction=blog.comment";

    // Most initialization should be on connecting, not in here.

    setState(NO_MYSPACE_NAME);

    connectedChanged_.connect(G_OBJECT(ui->getConnection()), "connected-changed",
        slot(this, &HippoMySpace::onConnectedChanged));
    myspaceChanged_.connect(G_OBJECT(ui->getConnection()), "myspace-changed",
        slot(this, &HippoMySpace::onMyspaceChanged));

    nameChanged_.connect(G_OBJECT(ui->getDataCache()), "myspace-name-changed", 
        slot(this, &HippoMySpace::onNameChanged));
    commentsChanged_.connect(G_OBJECT(ui->getDataCache()), "myspace-comments-changed", 
        slot(this, &HippoMySpace::onCommentsChanged));
    contactsChanged_.connect(G_OBJECT(ui->getDataCache()), "myspace-contacts-changed", 
        slot(this, &HippoMySpace::onContactsChanged));

    // init with current name, possibly NULL
    onNameChanged(hippo_data_cache_get_myspace_name(ui->getDataCache()));
}

HippoMySpace::~HippoMySpace(void)
{
    freeContacts();
    freeComments();
}

void
HippoMySpace::setState(HippoMySpace::State newState)
{
    ui_->debugLogU("HippoMySpace changing to state: %d", newState);
    state_ = newState;
}

void
HippoMySpace::freeContacts()
{
    std::vector<HippoMyspaceContact*>::iterator i = contacts_.begin();
    while (i != contacts_.end()) {
        hippo_myspace_contact_free(*i);
        ++i;
    }
    contacts_.clear();
}

// should only do this on disconnect, since we operate on a 
// "merge new stuff in" model.
void
HippoMySpace::freeComments()
{
    comments_.clear();
}

void
HippoMySpace::onConnectedChanged(gboolean connected)
{
    if (connected) {
        onNameChanged(NULL);
        // ask for name, which will get an XMPP back and invoke onNameChanged
        // again if it gets one
        hippo_connection_request_myspace_name(ui_->getConnection());
    } else {
        setState(NO_MYSPACE_NAME);
    }
}

void
HippoMySpace::onNameChanged(const char *nameU)
{
    if (nameU != NULL) {
        name_ = HippoBSTR::fromUTF8(nameU, -1);
        setState(RETRIEVING_SAVED_COMMENTS);
        // Start retrieving seen comments
        hippo_connection_request_myspace_blog_comments(ui_->getConnection());
    } else {
        // we need to start over - our myspace name may have changed,
        // everything could be different.
        setState(NO_MYSPACE_NAME);
        scrapeFriendIdTimeout_.remove();
        scrapeCommentsTimeout_.remove();
        refreshContactsTimeout_.remove();
        freeContacts();
        freeComments();
        name_ = NULL;
        friendId_ = 0;
    }
}

void
HippoMySpace::onCommentsChanged()
{
    // Merge already-seen comments into the comments we know about
    GSList *comments = hippo_data_cache_get_myspace_blog_comments(ui_->getDataCache());
    for (GSList *link = comments; link != NULL; link = link->next) {
        HippoMyspaceBlogComment *comment = static_cast<HippoMyspaceBlogComment*>(link->data);
        mergeBlogComment(comment);
    }

    refreshContacts(); // transitions the state
}

void
HippoMySpace::onContactsChanged()
{
    GSList *contacts;

    freeContacts();

    contacts = hippo_data_cache_get_myspace_contacts(ui_->getDataCache());

    for (GSList *link = contacts; link != NULL; link = link->next) {
        HippoMyspaceContact *contact = static_cast<HippoMyspaceContact*>(link->data);
        contacts_.push_back(hippo_myspace_contact_copy(contact));
    }

    if (state_ == RETRIEVING_CONTACTS) {
        // we just got contacts for the first time; now 
        // scrape the web page for our friend and blog ID
        // given our myspace name
        scrapeFriendId();
    } else if (state_ == REFRESHING_CONTACTS) {
        // We were just reloading contacts in a timeout; we can go back to the 
        // IDLE state, after queuing a future timeout.
        setState(IDLE);
        refreshContactsTimeout_.add(HIPPO_MYSPACE_REFRESH_CONTACT_INTERVAL_SECS * 1000,
            slot(this, &HippoMySpace::timeoutRefreshContacts));
    } else {
        hippoDebugLogW(L"In unexpected state in HippoMySpace::onContactsChanged");
        return;
    }
}

void
HippoMySpace::onMyspaceChanged()
{
    ui_->debugLogU("got contact post notification, re-polling immediately");
    scrapeComments();
}

static void
handleHtmlTag(struct taginfo *tag, void *data)
{
    HippoBSTR *str = (HippoBSTR *) data;
}

void
HippoMySpace::sanitizeCommentHTML(BSTR html, BSTR *sanitizedReturn)
{
    HippoBSTR sanitized(L"");

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
        } 
        // This isn't an exhaustive list but it's reasonable and safe
        if (iswalnum(html[i]) || isspace(html[i]) || html[i] == '!' || html[i] == ',' || html[i] == '.' || html[i] == '?') {
            sanitized.Append(html[i]);
        }
    }

    sanitized.CopyTo(sanitizedReturn);
}

bool
HippoMySpace::timeoutScrapeFriendId()
{

    return false; // remove
}

void
HippoMySpace::scrapeFriendId()
{
    setState(FINDING_FRIENDID);
    HippoMySpace::HippoMySpaceFriendIdHandler *handler = new HippoMySpace::HippoMySpaceFriendIdHandler(this);
    HippoHTTP *http = new HippoHTTP();
    HippoBSTR url(L"http://www.myspace.com/");
    url.Append(name_);
    http->doGet(url.m_str, false, handler);
}

void
HippoMySpace::refreshContacts()
{
    // FIXME this assumes that request_myspace_contacts() always results
    // in onContactsChanged, but it doesn't result in that if they 
    // haven't actually changed

    if (state_ == RETRIEVING_SAVED_COMMENTS) {
        // we just downloaded known comments, now download known contacts
        setState(RETRIEVING_CONTACTS);
        hippo_connection_request_myspace_contacts(ui_->getConnection());
    } else if (state_ == IDLE) {
        // invoked from a timeout to see if contacts have changed
        setState(REFRESHING_CONTACTS);
        hippo_connection_request_myspace_contacts(ui_->getConnection());
    } else {
        ui_->debugLogU("unknown HippoMySpace state %d for refreshContacts()", state_);
    }
}

bool
HippoMySpace::timeoutRefreshContacts()
{
    if (state_ != IDLE) { 
        // If we're busy, requeue this
        ui_->debugLogU("HippoMySpace currently busy, requeueing comment refresh");
        refreshContactsTimeout_.add(HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS * 1000, 
            slot(this, &HippoMySpace::timeoutRefreshContacts));
    } else {
        refreshContacts();
    }
    return false;
}

void
HippoMySpace::HippoMySpaceFriendIdHandler::handleError(HRESULT res) 
{
    myspace_->ui_->logHresult(L"got error while retriving MySpace friend id, queuing retry", res);
    myspace_->scrapeFriendIdTimeout_.add(HIPPO_MYSPACE_RETRY_FRIENDID_SECS * 1000, 
        slot(myspace_, &HippoMySpace::timeoutScrapeFriendId));
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
        if (next && ((next - friendIdStart) <= HIPPO_MYSPACE_ID_SIZE)) {
            char *blogIdStr = next + strlen(blogIdParam);
            char *endHref = strchr(blogIdStr, '"');
            char *endParam = strchr(blogIdStr, '&');
            char *end;
            if (endHref < endParam)
                end = endHref;
            else
                end = endParam;
            if (end && (end - blogIdStr) <= HIPPO_MYSPACE_ID_SIZE) {
                myspace_->friendId_ = strtol(friendIdStart, NULL, 10);
                myspace_->blogId_ = strtol(blogIdStr, NULL, 10);
                myspace_->ui_->debugLogU("got myspace friend id: %d, blogid: %d", myspace_->friendId_, myspace_->blogId_);

                // We can now download our comments since we have our friend ID
                myspace_->scrapeComments();
            } else {
                myspace_->ui_->logErrorU("failed to parse MySpace friend it");
            }
            return;
        }
    }
    myspace_->ui_->debugLogU("failed to find myspace friend id");
    delete this;
}

void
HippoMySpace::scrapeComments()
{
    if (state_ == FINDING_FRIENDID) {
        // Just scraped our friend ID, so now scraping comments
        setState(INITIAL_COMMENT_SCAN);
    } else if (state_ == IDLE) {
        // Re-scraping comments in a timeout
        setState(COMMENT_CHANGE_POLL);
    } else {
        hippoDebugLogW(L"Unexpected state %d in scrapeComments()", state_);
        return;
    }
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

bool
HippoMySpace::timeoutScrapeComments()
{
    if (state_ != IDLE) {
        // If we're busy, requeue this
        ui_->debugLogU("HippoMySpace not idle, requeuing comment refresh");
        scrapeCommentsTimeout_.add(HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS * 1000, 
            slot(this, &HippoMySpace::timeoutScrapeComments));
        return false;
    }
    scrapeComments();
    return false;
}

void
HippoMySpace::HippoMySpaceCommentHandler::handleError(HRESULT res) 
{
    myspace_->ui_->logHresult(L"error while retrieving MySpace blog feed", res);
    // Try again later
    myspace_->scrapeCommentsTimeout_.add(HIPPO_MYSPACE_REQUEUE_INTERVAL_SECS * 1000, 
            slot(myspace_, &HippoMySpace::timeoutScrapeComments));

    delete this;
}

void 
HippoMySpace::HippoMySpaceCommentHandler::handleComplete(void *responseData, long responseBytes)
{
    myspace_->ui_->debugLogU("got myspace blog feed");

    const char *profileSectionElt = "<td class=\"blogCommentsProfile\">";
    const char *response = (const char*)responseData;
    const char *profileSectionStart;

    myspace_->scrapeCommentsTimeout_.add(HIPPO_MYSPACE_POLL_START_INTERVAL_SECS * 1000, 
            slot(myspace_, &HippoMySpace::timeoutScrapeComments));

    if (!(profileSectionStart = strstr(response, profileSectionElt))) {
        myspace_->setState(IDLE);
        myspace_->ui_->debugLogU("failed to find blog comment profile (possibly no comments)");

        delete this;
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
        if (friendIdEnd - friendIdStart > HIPPO_MYSPACE_ID_SIZE)
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
        if (commentIdEnd - commentIdStart > HIPPO_MYSPACE_ID_SIZE)
            break;
        HippoMySpaceCommentData commentData;
        commentData.commentId = strtol(commentIdStart, NULL, 10);
        commentData.posterId = strtol(friendIdStart, NULL, 10);
        commentData.posterName.setUTF8(friendNameStart, (UINT) (friendNameEnd - friendNameStart));
        commentData.posterImgUrl.setUTF8(imglinkStart, (int) (imglinkEnd - imglinkStart));
        HippoBSTR rawHtml;
        rawHtml.setUTF8(commentStart, (UINT) (commentEnd - commentStart));
        myspace_->sanitizeCommentHTML(rawHtml, &commentData.content);
        myspace_->mergeBlogComment(commentData);
        profile += strlen(profileSectionElt);
    }

    if (myspace_->state_ == INITIAL_COMMENT_SCAN) {
        // The first time we successfully scrape, we queue another refresh of 
        // our contacts list from the XMPP server in a while
        myspace_->ui_->debugLogU("queueing timeout contact refresh");
        myspace_->refreshContactsTimeout_.add(HIPPO_MYSPACE_REFRESH_CONTACT_INTERVAL_SECS * 1000, 
            slot(myspace_, &HippoMySpace::timeoutRefreshContacts));
    } else if (myspace_->state_ == COMMENT_CHANGE_POLL) {
        // Expected, do nothing
        ;
    } else {
        hippoDebugLogW(L"Unexpected state in comment scraper: %d", myspace_->state_);
    }

    myspace_->setState(IDLE);

    delete this;
}

void
HippoMySpace::mergeBlogComment(const HippoMySpaceCommentData &newComment)
{
    std::vector<HippoMySpaceCommentData>::iterator i = comments_.begin();
    while (i != comments_.end()) {
        HippoMySpaceCommentData & comment = *i;
        if (comment.commentId == newComment.commentId) {
            ui_->debugLogU("already seen myspace comment %d", comment.commentId);
            return;
        }
        ++i;            
    }

    ui_->debugLogU("appending myspace comment %d", newComment.commentId);
    comments_.push_back(newComment);

    // We only display them if this is the polling state, otherwise just record
    // them as seen - we don't want to display all the comments the first time
    // they log in
    bool doDisplay = (state_ == COMMENT_CHANGE_POLL);
#if 0
    // disabled since HippoUI no longer has bubbles
    if (doDisplay)
        ui_->bubbleNewMySpaceComment(friendId_, blogId_, newComment);
#endif
}

void
HippoMySpace::mergeBlogComment(HippoMyspaceBlogComment *comment)
{
    HippoMySpaceCommentData data;

    data.commentId = hippo_myspace_blog_comment_get_comment_id(comment);
    data.posterId = hippo_myspace_blog_comment_get_poster_id(comment);

    mergeBlogComment(data);
}

bool
HippoMySpace::handlePostStart(const HippoBrowserInfo &browser)
{
    const WCHAR *friendIdParam = L"&friendID=";
    const WCHAR *friendId = wcsstr(browser.url.m_str, friendIdParam);
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
    HippoBSTR friendIdStr((int) (friendIdEnd - friendIdStart), friendIdStart);
    for (std::vector<HippoMySpaceContactPost>::iterator i = activeContactPosts_.begin();
        i != activeContactPosts_.end();
        ++i) {

        if (i->getFriendId() == friendIdStr)
            return true;
    }
    HippoUStr friendIdUStr(friendIdStr);
    ui_->debugLogU("posting on myspace blog for contact, friendId=%S", friendIdStr.m_str);
    
    for (std::vector<HippoMyspaceContact*>::iterator i = contacts_.begin();
         i != contacts_.end();
         ++i) {
        HippoMyspaceContact *contact = *i;
        if (strcmp(friendIdUStr.c_str(), hippo_myspace_contact_get_friend_id(contact)) != 0)
            continue;
        HippoMySpaceContactPost post(contact, browser.getBrowser());
        activeContactPosts_.push_back(post);
        return true;
    }
    return false;
}

void
HippoMySpace::browserChanged(const HippoBrowserInfo &browser)
{
    if (!ui_ || !name_)
        return; // not active right now

    // Check if this is a post
    if (wcsncmp(browser.url.m_str, blogPostPrefix_, blogPostPrefix_.Length()) == 0) {
        if (handlePostStart(browser))
            return;
    }
    for (std::vector<HippoMySpaceContactPost>::iterator i = activeContactPosts_.begin();
        i != activeContactPosts_.end();
        ++i) {
        if (i->getBrowser() == browser.browser) {
            hippo_connection_notify_myspace_contact_post(ui_->getConnection(),
                    HippoUStr(i->getName()).c_str());
            activeContactPosts_.erase(i);
            return;
        }
    }
}
