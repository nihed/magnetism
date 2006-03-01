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

struct HippoEntity
{
    enum EntityType {
        RESOURCE,
        PERSON,
        GROUP
    };

    EntityType type;
    HippoBSTR id;
    HippoBSTR name;
    HippoBSTR smallPhotoUrl;
};

struct HippoPost
{
    HippoBSTR postId;
    HippoBSTR senderId;
    HippoBSTR url;
    HippoBSTR title;
    HippoBSTR description;
    std::vector<HippoBSTR> recipients;
    std::vector<HippoBSTR> viewers;
    HippoBSTR info;
    long postDate;
    int chattingUserCount;
    int viewingUserCount;
    int totalViewers;
    int timeout;
  
    bool haveViewed;

    HippoPost() {
        postDate = 0;
        chattingUserCount = 0;
        viewingUserCount = 0;
        totalViewers = 0;
        timeout = 0;
        haveViewed = false;
    }
};

class HippoDataCache
{
public:
    void addPost(const HippoPost &share);
    void addEntity(const HippoEntity &person);
    bool getPost(BSTR postId, HippoPost *post);
    bool getEntity(BSTR id, HippoEntity *entity);
    const HippoEntity &getEntity(BSTR entityId);
    void getRecentPosts(std::vector<HippoPost> &result);

private:
    std::map<HippoBSTR, HippoEntity> entities_;
    std::map<HippoBSTR, HippoPost> posts_;
};