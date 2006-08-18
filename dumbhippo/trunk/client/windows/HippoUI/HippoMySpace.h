#pragma once

#include <vector>
#include <HippoUtil.h>
#include "HippoHTTP.h"
#include "HippoGSignal.h"
#include <hippo/hippo-common.h>

/* FIXME
 * 
 * This got totally broken in moving to the HippoCommon library... the core issue is that
 * the state machine approach is not compatible with the signal-based setup on HippoDataCache.
 * 
 * The state transitions won't happen reliably since they rely on getting notified 
 * of replies to XMPP requests, but now HippoDataCache only emits a signal if the 
 * info has _changed_, not just if we get a reply.
 * 
 * I don't think the state machine makes that much sense anyway; it is several 
 * state machines packed into one enum and just pretty confusing and fragile.
 *
 * One approach would be to split apart the distinct state machines for each 
 * asynchronous operation, another is to just e.g. key off whether we have
 * the particular bit of data.
 * 
 * Once this is working again, uncomment the setUI() for it in HippoUI.cpp
 * For now, it compiles, but that's it.
 * 
 */

class HippoUI;
struct HippoBrowserInfo;

class HippoMySpaceCommentData
{
    // USING DEFAULT COPY CONSTRUCTOR
public:
    long commentId;
    long posterId;
    HippoBSTR posterName;
    HippoBSTR content;
    HippoBSTR posterImgUrl;

    HippoMySpaceCommentData() {
        commentId = -1;
        posterId = -1;
    }
};

class HippoMySpaceContactPost
{
    // USING DEFAULT COPY CONSTRUCTOR
public:
    HippoMySpaceContactPost(HippoMyspaceContact *contact, IWebBrowser2 *browser) {
        const char *myspaceNameU = hippo_myspace_contact_get_name(contact);
        if (myspaceNameU)
            name_ = HippoBSTR::fromUTF8(myspaceNameU, -1);
        else
            hippoDebugLogW(L"Missing myspace name in myspace contact, should not happen, bad");
        const char *friendIdU = hippo_myspace_contact_get_friend_id(contact);
        if (friendIdU)
            friendId_ = HippoBSTR::fromUTF8(friendIdU, -1);
        else
            hippoDebugLogW(L"Missing myspace friend ID from contact, badness");
        browser_ = browser;
    }
    HippoBSTR getFriendId() const { return friendId_; }
    HippoBSTR getName() const { return name_; }
    IWebBrowser2 *getBrowser() { return browser_; }
private:
    HippoBSTR name_;
    HippoBSTR friendId_;
    HippoPtr<IWebBrowser2> browser_;
};

class HippoMySpace
{
public:
    HippoMySpace();
    ~HippoMySpace();

    void setUI(HippoUI *ui);
    
    void browserChanged(const HippoBrowserInfo &browser);
    void onReceivingMySpaceContactPost();

private:
    enum State {
        NO_MYSPACE_NAME,
        IDLE,
        RETRIEVING_SAVED_COMMENTS,
        RETRIEVING_CONTACTS,
        FINDING_FRIENDID,
        INITIAL_COMMENT_SCAN,
        COMMENT_CHANGE_POLL,
        REFRESHING_CONTACTS
    };

    class HippoMySpaceFriendIdHandler : public HippoHTTPAsyncHandler
    {
    public:
        HippoMySpaceFriendIdHandler(HippoMySpace *myspace) {
            myspace_ = myspace;
        }
        virtual void handleError(HRESULT result);
        virtual void handleContentType(WCHAR *mimetype, WCHAR *charset);
        virtual void handleComplete(void *responseData, long responseBytes);
    protected:
        HippoMySpace *myspace_;
    };
    class HippoMySpaceCommentHandler : public HippoHTTPAsyncHandler
    {
    public:
        HippoMySpaceCommentHandler(HippoMySpace *myspace) {
            myspace_ = myspace;
        }
        virtual void handleError(HRESULT result);
        virtual void handleComplete(void *responseData, long responseBytes);
    protected:
        HippoMySpace *myspace_;
    };

    HippoUI *ui_;

    State state_;

    std::vector<HippoMySpaceCommentData> comments_;
    std::vector<HippoMyspaceContact *> contacts_;
    std::vector<HippoMySpaceContactPost> activeContactPosts_;

    const char *blogUrlPrefix_;

    HippoBSTR blogPostPrefix_;

    HippoBSTR name_;
    long friendId_;
    long blogId_;

    GTimeout scrapeFriendIdTimeout_;
    GTimeout refreshContactsTimeout_;
    GTimeout scrapeCommentsTimeout_;

    bool timeoutScrapeFriendId();
    bool timeoutRefreshContacts();
    bool timeoutScrapeComments();

    GConnection1<void,gboolean> connectedChanged_;
    GConnection1<void,const char*> nameChanged_;
    GConnection0<void> commentsChanged_; // saved comments changed
    GConnection0<void> contactsChanged_;
    GConnection0<void> myspaceChanged_; // need to re-scrape, someone else posted

    void onConnectedChanged(gboolean connected);
    void onNameChanged(const char *name);
    void onCommentsChanged();
    void onContactsChanged();
    void onMyspaceChanged();

    void freeContacts();
    void freeComments();

    void scrapeFriendId();
    void scrapeComments();
    void refreshContacts(); // reload friends from xmpp server
    
    void sanitizeCommentHTML(BSTR html, BSTR *sanitizedReturn);
    void setState(HippoMySpace::State newState);

    void mergeBlogComment(const HippoMySpaceCommentData &comment);
    void mergeBlogComment(HippoMyspaceBlogComment *comment);

    bool handlePostStart(const HippoBrowserInfo &browser);
};
