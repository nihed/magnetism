#include "stdafx.h"
#include "HippoDataCache.h"

void
HippoDataCache::addEntity(const HippoEntity &entity)
{
    entities_[entity.id] = entity;
}

void 
HippoDataCache::addPost(const HippoPost &share)
{
    posts_[share.postId] = share;
}

bool
HippoDataCache::getPost(BSTR postId, HippoPost *ret)
{
    std::map<HippoBSTR, HippoPost>::const_iterator it = posts_.find(HippoBSTR(postId));
    if (it != posts_.end()) {
        *ret = (*it).second;
        return true;
    }
    return false;
}

bool
HippoDataCache::getEntity(BSTR id, HippoEntity *ret)
{
    std::map<HippoBSTR, HippoEntity>::const_iterator it = entities_.find(HippoBSTR(id));
    if (it != entities_.end()) {
        *ret = (*it).second;
        return true;
    }
    return false;
}

void 
HippoDataCache::getRecentPosts(std::vector<HippoPost> &result)
{
    SYSTEMTIME cursystime;
    FILETIME curfiletime;
    GetSystemTime(&cursystime); // SYSTEMTIME is a broken down format
    SystemTimeToFileTime(&cursystime, &curfiletime); // convert it to epoch windows time (100ns intervals since 1601)
    ULARGE_INTEGER curtime; // Now shove this into the ULARGE_INTEGER union
    curtime.HighPart = curfiletime.dwHighDateTime;
    curtime.LowPart = curfiletime.dwLowDateTime;

    std::map<HippoBSTR, HippoPost>::const_iterator it = posts_.begin();
    while (it != posts_.end()) {
        // Convert from unix time (seconds since jan 1 1970) to windows time
        long postDate = (*it).second.postDate;
        LONGLONG ltime = Int32x32To64(postDate, 10000000) + 116444736000000000;
        LONGLONG diff = curtime.QuadPart - ltime;
        diff /= 10000; // convert to milliseconds
        diff /= 1000; // convert to seconds
        if (diff <= 60 * 60 * 24) {
            result.push_back((*it).second);
        }
        it++;
    }
}
