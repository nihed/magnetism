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
        COMMENT_CHANGE_POLL
    } State;
    
    State state_;

    void setSeenComments(HippoArray<HippoMySpaceBlogComment*> *comments);
    void setContacts(HippoArray<HippoMySpaceContact *> &contacts);

    void browserChanged(HippoBrowserInfo &browser);

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

    void SanitizeCommentHTML(BSTR html, HippoBSTR &ret);

    void GetSeenComments();
    void GetFriendId();
    void RefreshComments();
    static UINT idleRefreshComments(void *data);
    void addBlogComment(HippoMySpaceBlogComment &comment);

    HippoUI *ui_;

    HippoArray<HippoMySpaceBlogComment *> comments_;

    HippoArray<HippoMySpaceContact *> contacts_;

    const char *blogUrlPrefix_;

    HippoBSTR blogPostPrefix_;

    int mySpaceIdSize_;

    HippoBSTR name_;
    long friendId_;
    long blogId_;

    long lastUpdateTime_;
};
