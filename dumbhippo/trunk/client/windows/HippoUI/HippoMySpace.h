#pragma once

#include "stdafx.h"
#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoHTTP.h"

class HippoUI;
struct HippoBrowserInfo;

class HippoMySpaceBlogComment
{
public:
    long commentId;
    long posterId;
    HippoBSTR posterName;
    HippoBSTR content;
    HippoBSTR posterImgUrl;

    HippoMySpaceBlogComment() {
        commentId = -1;
        posterId = -1;
    }
};

class HippoMySpaceContact
{
public:
    const HippoBSTR &getName() { return name_; }
    const HippoBSTR &getFriendId() { return friendId_; }

    HippoMySpaceContact(WCHAR *name, WCHAR *friendId) {
        name_ = name;
        friendId_ = friendId;
    }
private:
    HippoBSTR name_;
    HippoBSTR friendId_;
};

class HippoMySpaceContactPost
{
public:
    HippoMySpaceContactPost(HippoMySpaceContact *contact, IWebBrowser2 *browser) {
        contact_ = contact;
        browser_ = browser;
    }
    HippoMySpaceContact *getContact() { return contact_; }
    IWebBrowser2 *getBrowser() { return browser_; }
private:
    HippoMySpaceContact *contact_;
    HippoPtr<IWebBrowser2> browser_;
};

class HippoMySpace
{
public:
    HippoMySpace(BSTR name, HippoUI *ui);
    ~HippoMySpace(void);

    typedef enum {
        IDLE,
        RETRIEVING_SAVED_COMMENTS,
        RETRIEVING_CONTACTS,
        FINDING_FRIENDID,
        INITIAL_COMMENT_SCAN,
        COMMENT_CHANGE_POLL,
        REFRESHING_CONTACTS
    } State;
    
    State state_;

    void setSeenComments(HippoArray<HippoMySpaceBlogComment*> *comments);
    void setContacts(HippoArray<HippoMySpaceContact *> &contacts);

    void browserChanged(HippoBrowserInfo &browser);
    void onReceivingMySpaceContactPost();

private:
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

    void sanitizeCommentHTML(BSTR html, HippoBSTR &ret);

    void setState(HippoMySpace::State newState);
    void getSeenComments(); 
    static UINT idleGetFriendId(void * data);
    void getFriendId();
    void refreshComments();
    static UINT idleRefreshComments(void *data);
    void addBlogComment(HippoMySpaceBlogComment &comment);


    static UINT idleRefreshContacts(void *data);
    void refreshContacts();

    bool handlePostStart(HippoBrowserInfo &browser);

    HippoUI *ui_;

    HippoArray<HippoMySpaceBlogComment *> comments_;

    HippoArray<HippoMySpaceContact *> contacts_;

    HippoArray<HippoMySpaceContactPost *> activeContactPosts_;

    const char *blogUrlPrefix_;

    HippoBSTR blogPostPrefix_;

    int mySpaceIdSize_;

    HippoBSTR name_;
    long friendId_;
    long blogId_;

    int idleGetFriendIdId_;
    int idlePollMySpaceId_;
    int idleRefreshContactsId_;

    long lastUpdateTime_;
};
