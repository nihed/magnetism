#include "stdafx.h"
#include "HippoDataCache.h"
#include "HippoUI.h"
#include <algorithm>

HippoEntity::HippoEntity(const HippoBSTR &id, EntityType type)
{
    id_ = id;
    type_ = type;
}

ITypeInfo *
HippoEntity::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;

    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoEntity, &typeInfo, NULL);

    return typeInfo;
}

STDMETHODIMP
HippoEntity::get_Id(BSTR *id)
{
    id_.CopyTo(id);

    return S_OK;
}

STDMETHODIMP
HippoEntity::get_Type(int *type)
{
    *type = (int)type_;

    return S_OK;
}

STDMETHODIMP
HippoEntity::get_Name(BSTR *name)
{
    name_.CopyTo(name);

    return S_OK;
}

STDMETHODIMP
HippoEntity::get_SmallPhotoUrl(BSTR *smallPhotoUrl)
{
    smallPhotoUrl_.CopyTo(smallPhotoUrl);

    return S_OK;
}

/////////////////////////////////////////////////////////////////////////

HippoEntityCollection::HippoEntityCollection()
{
}

ITypeInfo *
HippoEntityCollection::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;

    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoEntityCollection, &typeInfo, NULL);

    return typeInfo;
}

void
HippoEntityCollection::addMember(HippoEntity *entity)
{
    members_.push_back(entity);
}

STDMETHODIMP
HippoEntityCollection::get_length(int *length)
{
    *length = (int)members_.size();

    return S_OK;
}

STDMETHODIMP
HippoEntityCollection::item(int index, IHippoEntity **entity)
{
    if (index < 0 || (size_t)index >= members_.size())
        return E_INVALIDARG;

    *entity = members_[index];
    (*entity)->AddRef();

    return S_OK;
}


/////////////////////////////////////////////////////////////////////////


HippoPost::HippoPost(HippoDataCache *cache, const HippoBSTR &id) {
    cache_ = cache;
    postId_ = id;
    postDate_ = 0;
    chattingUserCount_ = 0;
    viewingUserCount_ = 0;
    totalViewers_ = 0;
    timeout_ = 0;
    haveViewed_ = false;
}

ITypeInfo *
HippoPost::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;

    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoPost, &typeInfo, NULL);

    return typeInfo;
}

void 
HippoPost::resetCurrentViewers()
{
    currentViewers_ = NULL;
}

STDMETHODIMP 
HippoPost::get_Id(BSTR *id)
{
    postId_.CopyTo(id);    

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Sender(IHippoEntity **sender)
{
    if (sender_)
        sender_->AddRef();
    *sender = sender_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Url(BSTR *url)
{
    url_.CopyTo(url);

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Title(BSTR *title)
{
    title_.CopyTo(title);

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Description(BSTR *description)
{
    description_.CopyTo(description);

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Recipients(IHippoEntityCollection **recipients)
{
    if (!recipients_)
        cache_->createEntityCollection(&recipients_);

    recipients_->AddRef();
    *recipients = recipients_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Viewers(IHippoEntityCollection **viewers)
{
    if (!viewers_)
        cache_->createEntityCollection(&viewers_);

    viewers_->AddRef();
    *viewers = viewers_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Info(BSTR *info)
{
    info_.CopyTo(info);

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_PostDate(int *postDate)
{
    *postDate = postDate_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_Timeout(int *timeout)
{
    *timeout = timeout_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_ViewingUserCount(int *viewingUserCount)
{
    // When we first get a Post, the data in the post is more accurate -
    // waiting until we are filled to use the chatroom count avoids
    // the user seeing a count-up as the chatroom fills
    if (chatRoom_ && !chatRoom_->getFilling())
        *viewingUserCount = chatRoom_->getViewingUserCount();
    else
        *viewingUserCount = viewingUserCount_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_ChattingUserCount(int *chattingUserCount)
{
    if (chatRoom_ && !chatRoom_->getFilling())
        *chattingUserCount = chatRoom_->getChattingUserCount();
    else
        *chattingUserCount = chattingUserCount_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_TotalViewers(int *totalViewers)
{
    *totalViewers = totalViewers_;

    return S_OK;
}

STDMETHODIMP 
HippoPost::get_HaveViewed(BOOL *haveViewed)
{
    *haveViewed = haveViewed_ ? TRUE: FALSE;

    return S_OK;
}


STDMETHODIMP 
HippoPost::get_LastChatMessage(BSTR *message)
{
    *message = NULL;

    if (chatRoom_) {
        const HippoChatMessage *chatMessage = chatRoom_->getLastMessage();
        if (chatMessage)
            HippoBSTR(chatMessage->getText()).CopyTo(message);
    }

    return S_OK;
}

STDMETHODIMP
HippoPost::get_LastChatSender(IHippoEntity **sender)
{
    *sender = NULL;

    if (chatRoom_) {
        const HippoChatMessage *message = chatRoom_->getLastMessage();
        if (message) {
            HippoEntity *entity = cache_->getEntityForChatUser(message->getUser());
            entity->AddRef();
            *sender = entity;
        }
    }

    return S_OK;
}

STDMETHODIMP
HippoPost::get_CurrentViewers(IHippoEntityCollection **viewers)
{
    // We keep a pointer to the CurrentViewers object so something like
    // for (i = 0; i < post.CurrentViewers.length; i++) { ... } 
    // doesn't continually create new objects, but we don't try to 
    // incrementally update it, we just clear it and demand create
    // a new one.

    if (!currentViewers_) {
        cache_->createEntityCollection(&currentViewers_);

        if (chatRoom_) {
            std::vector<const HippoChatUser *> users = chatRoom_->getUsers();
            std::vector<const HippoChatUser *>::const_iterator i;
            for (i = users.begin(); i != users.end(); i++) {
                const HippoChatUser *user = *i;
                HippoEntity *entity = cache_->getEntityForChatUser(*user);
                currentViewers_->addMember(entity);
            }
        }
    }

    currentViewers_->AddRef();
    *viewers = currentViewers_;

    return S_OK;
}

/////////////////////////////////////////////////////////////////////////

HippoDataCache::HippoDataCache()
{
}

void
HippoDataCache::addEntity(HippoEntity *entity)
{
    entities_[entity->getId()] = entity;
}

void 
HippoDataCache::addPost(HippoPost *post)
{
    posts_[post->getId()] = post;
}

HippoPost *
HippoDataCache::getPost(const HippoBSTR &id)
{
    std::map<HippoBSTR, HippoPtr<HippoPost> >::iterator it = posts_.find(id);
    if (it != posts_.end())
        return (*it).second;
    else
        return NULL;
}

HippoEntity *
HippoDataCache::getEntity(const HippoBSTR &id)
{
    std::map<HippoBSTR, HippoPtr<HippoEntity> >::iterator it = entities_.find(HippoBSTR(id));
    if (it != entities_.end())
        return (*it).second;
    else
        return NULL;
}

void 
HippoDataCache::createEntity(const HippoBSTR &id, HippoEntity::EntityType type, HippoEntity **entity)
{
    *entity = new HippoEntity(id, type);
}


void 
HippoDataCache::createEntityCollection(HippoEntityCollection **collection)
{
    *collection = new HippoEntityCollection();
}

void 
HippoDataCache::createPost(const HippoBSTR &id, HippoPost **post)
{
    *post = new HippoPost(this, id);
}

static bool 
postDateCompare(HippoPost *a, HippoPost *b) {
    int dateA, dateB;
    a->get_PostDate(&dateA);
    b->get_PostDate(&dateB);

    return dateA < dateB;
}

void 
HippoDataCache::getRecentPosts(std::vector<HippoPost *> &result)
{
    SYSTEMTIME cursystime;
    FILETIME curfiletime;
    GetSystemTime(&cursystime); // SYSTEMTIME is a broken down format
    SystemTimeToFileTime(&cursystime, &curfiletime); // convert it to epoch windows time (100ns intervals since 1601)
    ULARGE_INTEGER curtime; // Now shove this into the ULARGE_INTEGER union
    curtime.HighPart = curfiletime.dwHighDateTime;
    curtime.LowPart = curfiletime.dwLowDateTime;

    std::map<HippoBSTR, HippoPtr<HippoPost> >::iterator it = posts_.begin();
    while (it != posts_.end()) {
        // Convert from unix time (seconds since jan 1 1970) to windows time
        HippoPost *post = ((*it).second);
        int postDate;
        post->get_PostDate(&postDate);
        LONGLONG ltime = Int32x32To64(postDate, 10000000) + 116444736000000000;
        LONGLONG diff = curtime.QuadPart - ltime;
        diff /= 10000; // convert to milliseconds
        diff /= 1000; // convert to seconds
        if (diff <= 60 * 60 * 24) {
            result.push_back(post);
        }
        it++;
    }

    return std::sort(result.begin(), result.end(), postDateCompare);
}

HippoEntity *
HippoDataCache::getEntityForChatUser(const HippoChatUser &user)
{
    HippoEntity *existing = getEntity(user.getUserId());
    if (existing) 
        return existing;

    HippoPtr<HippoEntity> newEntity;
    createEntity(user.getUserId(), HippoEntity::PERSON, &newEntity);
    addEntity(newEntity);

    newEntity->setName(user.getName());

    HippoBSTR photoUrl(L"/files/headshots/48/");
    photoUrl.Append(user.getUserId());

    photoUrl.Append(L"?v=");
    WCHAR buffer[32];
    StringCchPrintfW(buffer, sizeof(buffer) / sizeof(buffer[0]), L"%d", user.getVersion());
    photoUrl.Append(buffer);
    
    newEntity->setSmallPhotoUrl(photoUrl);

    return newEntity;
}