/* HippoDataCache.h: Cache of server-computed data such as posts
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include "stdafx.h"
#include <vector>
#include <map>
#include <HippoUtil.h>
#include <HippoArray.h>
#include <HippoDispatchableObject.h>
#include "HippoChatRoom.h"

class HippoEntity : public HippoDispatchableObject<IHippoEntity,HippoEntity>
{
public:
    enum EntityType {
        RESOURCE = 0,
        PERSON = 1,
        GROUP = 2
    };

    HippoEntity(const HippoBSTR &id, EntityType type);
    static ITypeInfo *getTypeInfo();

    void setName(const HippoBSTR &name) { name_ = name; }
    void setSmallPhotoUrl(const HippoBSTR &smallPhotoUrl) { smallPhotoUrl_ = smallPhotoUrl; }

    // More convenient for use within the client than get_Id()
    const HippoBSTR &getId() { return id_; }

    // IHippoEntity
    STDMETHODIMP get_Type(int *type);
    STDMETHODIMP get_Id(BSTR *id);
    STDMETHODIMP get_Name(BSTR *name);
    STDMETHODIMP get_SmallPhotoUrl(BSTR *smallPhotoUrl);

private:
    EntityType type_;
    HippoBSTR id_;
    HippoBSTR name_;
    HippoBSTR smallPhotoUrl_;

    HippoEntity(const HippoEntity &other) {}
    const HippoEntity &operator=(const HippoEntity &other) {}
};

class HippoEntityCollection : public HippoDispatchableObject<IHippoEntityCollection,HippoEntityCollection>
{
public:
    HippoEntityCollection();
    static ITypeInfo *getTypeInfo();

    void addMember(HippoEntity *entity);

    // IHippoEntityCollection
    STDMETHODIMP get_length(int *length);
    STDMETHODIMP item(int index, IHippoEntity **entity);

private:
    std::vector<HippoPtr<HippoEntity> > members_;

    HippoEntityCollection(const HippoEntityCollection &other) {}
    const HippoEntityCollection & operator=(const HippoEntityCollection &other) {}
};

class HippoDataCache;

class HippoPost : public HippoDispatchableObject<IHippoPost,HippoPost>
{
public:
    HippoPost(HippoDataCache *cache_, const HippoBSTR &id);
    static ITypeInfo *getTypeInfo();

    void setSender(HippoEntity *sender) { sender_ = sender; }
    void setUrl(const HippoBSTR &url) { url_ = url; }
    void setTitle(const HippoBSTR &title) { title_ = title; }
    void setDescription(const HippoBSTR &description) { description_ = description; }
    void setRecipients(HippoEntityCollection *recipients) { recipients_ = recipients; }
    void setViewers(HippoEntityCollection *viewers) { viewers_ = viewers; }
    void setInfo(const HippoBSTR &info) { info_ = info; }
    void setPostDate(int postDate) { postDate_ = postDate; }
    void setTimeout(int timeout) { timeout_ = timeout; }
    void setChattingUserCount(int chattingUserCount) { chattingUserCount_ = chattingUserCount; }
    void setViewingUserCount(int viewingUserCount) { viewingUserCount_ = viewingUserCount; }
    void setTotalViewers(int totalViewers) { totalViewers_ = totalViewers; }
    void setHaveViewed(bool haveViewed) { haveViewed_ = haveViewed; }

    void setChatRoom(HippoChatRoom *chatRoom) { chatRoom_ = chatRoom; }
    HippoChatRoom *getChatRoom() { return chatRoom_; }

    // Call this when the set of current viewers changes, clears the cached object
    void resetCurrentViewers();

    const HippoBSTR &getId() const { return postId_; }

    // IHippoPost
    STDMETHODIMP get_Id(BSTR *id);
    STDMETHODIMP get_Sender(IHippoEntity **sender);
    STDMETHODIMP get_Url(BSTR *url);
    STDMETHODIMP get_Title(BSTR *title);
    STDMETHODIMP get_Description(BSTR *description);
    STDMETHODIMP get_Recipients(IHippoEntityCollection **recipients);
    STDMETHODIMP get_Viewers(IHippoEntityCollection **viewers);
    STDMETHODIMP get_Info(BSTR *info);
    STDMETHODIMP get_PostDate(int *postDate);
    STDMETHODIMP get_Timeout(int *timeout);
    STDMETHODIMP get_ViewingUserCount(int *viewingUserCount);
    STDMETHODIMP get_ChattingUserCount(int *chattingUserCount);
    STDMETHODIMP get_TotalViewers(int *totalViewers);
    STDMETHODIMP get_HaveViewed(BOOL *haveViewed);

    STDMETHODIMP get_LastChatMessage(BSTR *message);
    STDMETHODIMP get_LastChatSender(IHippoEntity **sender);
    STDMETHODIMP get_CurrentViewers(IHippoEntityCollection **currentViewers);

private:
    HippoDataCache *cache_;
    HippoBSTR postId_;
    HippoPtr<IHippoEntity> sender_;
    HippoBSTR url_;
    HippoBSTR title_;
    HippoBSTR description_;
    HippoPtr<HippoEntityCollection> recipients_;
    HippoPtr<HippoEntityCollection> viewers_;
    HippoBSTR info_;
    long postDate_;
    int chattingUserCount_;
    int viewingUserCount_;
    int totalViewers_;
    int timeout_;
  
    bool haveViewed_;

    HippoPtr<HippoChatRoom> chatRoom_;
    HippoPtr<HippoEntityCollection> currentViewers_;

    HippoPost(const HippoPost &other) {}
    const HippoPost &operator=(const HippoPost &other) {}
};

class HippoDataCache
{
public:
    HippoDataCache();

    // Look up objects in the cache. These *do not* return a new reference to the object
    HippoPost *getPost(const HippoBSTR &id);
    HippoEntity *getEntity(const HippoBSTR &id);

    void addPost(HippoPost *post);
    void addEntity(HippoEntity *entity);

    // Allocate new data cache objects. You should use these instead of 'new HippoEntity()'
    // because work better with HippoPtr<>. Once you've created an HippoEntity or HippoPost
    // you need to add it to the cache with addPost() or addEntity()
    void createEntity(const HippoBSTR &id, HippoEntity::EntityType type, HippoEntity **entity);
    void createEntityCollection(HippoEntityCollection **collection);
    void createPost(const HippoBSTR &id, HippoPost **post);

    // Return a list of posts within the last 24 hours, sorted with the most recent
    // post last. The reference counts of the rteurned objects are not increased
    void getRecentPosts(std::vector<HippoPost *> &result);

    // Get a HippoEntity for a HippoChatUser, adding a new entry to the cache
    // based on the HippoChatUser if necessary
    HippoEntity *getEntityForChatUser(const HippoChatUser &user);

private:
    std::map<HippoBSTR, HippoPtr<HippoEntity> > entities_;
    std::map<HippoBSTR, HippoPtr<HippoPost> > posts_;

    HippoDataCache(const HippoDataCache &other) {}
    HippoDataCache &operator=(const HippoDataCache &other);
};