#pragma once

#include "stdafx.h"
#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoHTTP.h"

class HippoUI;

class HippoMySpaceBlogComment
{
public:
    long commentId;
    long posterId;
    HippoBSTR content;

    HippoMySpaceBlogComment() {
        commentId = -1;
        posterId = -1;
    }

    HippoMySpaceBlogComment(const HippoMySpaceBlogComment &other) {
        commentId = other.commentId;
        posterId = other.posterId;
        if (other.content.m_str != NULL)
            content = other.content;
    }

    HippoMySpaceBlogComment & operator=(const HippoMySpaceBlogComment &other) {
		if (this != &other) {
            commentId = other.commentId;
            posterId = other.posterId;
            if (other.content.m_str != NULL)
                content = other.content.m_str;
            else
                content = NULL;
		}

         return *this;
    }

    bool operator==(const HippoMySpaceBlogComment& other) const {
        if (&other == this)
            return true;
        return other.commentId == commentId;
    }
};

class HippoMySpace
{
public:
    HippoMySpace(BSTR name, HippoUI *ui);
    ~HippoMySpace(void);

    typedef enum {
        INITIAL,
        FINDING_FRIENDID,
        RETRIEVING_BLOG,
        SCANNING_COMMENTS
    } State;
    
    State state_;

    void setSeenComments(HippoArray<HippoMySpaceBlogComment> &comments);

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

    const char * blogUrlPrefix_;

    int mySpaceIdSize_;

    HippoBSTR name_;
    long friendId_;
    long blogId_;

    long lastUpdateTime_;
};
