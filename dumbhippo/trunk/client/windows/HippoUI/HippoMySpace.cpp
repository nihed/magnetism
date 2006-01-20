#include "stdafx.h"

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include "HippoMySpace.h"
#include "HippoHttp.h"
#include "HippoUI.h"
extern "C" {
#include <html-parse.h>
}

#define HIPPO_MYSPACE_POLL_START_INTERVAL_SECS (7*60)

HippoMySpace::HippoMySpace(BSTR name, HippoUI *ui)
{
    ui_ = ui;
    state_ = HippoMySpace::State::INITIAL;
    name_ = name;
    lastUpdateTime_ = 0;
    friendId_ = 0;

    mySpaceIdSize_ = 14;

    blogUrlPrefix_ = "http://blog.myspace.com/index.cfm?fuseaction=blog.view";
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
    // Punt on the below for now
    ret = html;
    return;

    HippoPtr<IHTMLDocument2> doc;
    CoCreateInstance(CLSID_HTMLDocument,
                     NULL,
                     CLSCTX_INPROC_SERVER,
                     IID_IHTMLDocument2,
                     (void **) &doc);
    if (!doc) {
        ui_->debugLogU("failed to create IID_IHTMLDocument2");
        return;
    }

    HippoQIPtr<IPersistStreamInit> persist(doc);
    persist->InitNew();
    persist->Release();

    HippoQIPtr<IMarkupServices> markup(doc);
    if (!markup) {
        ui_->debugLogU("failed to cast to IMarkupServices");
        return;
    }
    
    HippoPtr<IMarkupContainer> container;
    IMarkupPointer *markupStart = NULL, *markupEnd = NULL;
    markup->CreateMarkupPointer(&markupStart);
    markup->CreateMarkupPointer(&markupEnd);
    markup->ParseString(html, 0, &container, markupStart, markupEnd);
    if (!container) {
        ui_->logLastError(L"failed parse MySpace comment html to IMarkupContainer");
        return;
    }
    HippoPtr<IHTMLElementCollection> allElts;
    doc->get_all(&allElts);
    long len = 0;
    allElts->get_length(&len);
    for (long i = 0; i < len; i++) {
        variant_t vMissing(VT_NULL);
        variant_t vIndex(i);
        HippoPtr<IDispatch> eltDisp;
        allElts->item(vMissing, vIndex, &eltDisp);
        HippoQIPtr<IHTMLElement> elt(eltDisp);
        BSTR innerHtml;
        elt->get_innerHTML(&innerHtml);
    }
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
HippoMySpace::setSeenComments(HippoArray<HippoMySpaceBlogComment> &comments)
{
    for (UINT i = 0; i < comments.length(); i++) {
        comments_.append(new HippoMySpaceBlogComment(comments[i]));
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
    state_ = HippoMySpace::State::RETRIEVING_BLOG;
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
    return TRUE;
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
    const char *commentSectionElt = "<td class=\"blogComments\">";
    const char *response = (const char*)responseData;
    const char *commentSectionStart;

    g_timeout_add(HIPPO_MYSPACE_POLL_START_INTERVAL_SECS * 1000, (GSourceFunc) idleRefreshComments, this);

    if (!(commentSectionStart = strstr(response, commentSectionElt))) {
        myspace_->ui_->debugLogU("failed to find blog comments");
        return;
    }
    const char *commentStartStr = "<p class=\"blogCommentsContent\">";
    const char *comment = commentSectionStart;
    while ((comment = strstr(comment, commentStartStr)) != NULL) {
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
        HippoBSTR rawHtml;
        rawHtml.setUTF8(commentStart, (UINT) (commentEnd - commentStart));
        myspace_->SanitizeCommentHTML(rawHtml, commentData.content);
        myspace_->addBlogComment(commentData);
        comment = commentStart; 
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
    ui_->onNewMySpaceComment(friendId_, blogId_, comment);
}