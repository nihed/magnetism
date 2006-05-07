#include "hippo-myspace.h"

/* a little paranoia to help with our C++ skillz */
#define HIPPO_MYSPACE_CONTACT_MAGIC 0x5678efab
#define HIPPO_IS_MYSPACE_CONTACT(x) \
    (((HippoMyspaceContact*)x)->magic == HIPPO_MYSPACE_CONTACT_MAGIC)
#define HIPPO_MYSPACE_BLOG_COMMENT_MAGIC 0xccba9231
#define HIPPO_IS_MYSPACE_BLOG_COMMENT(x) \
    (((HippoMyspaceBlogComment*)x)->magic == HIPPO_MYSPACE_BLOG_COMMENT_MAGIC)

struct HippoMyspaceContact {
    guint32 magic;
    char *name;
    char *friend_id;
};

struct HippoMyspaceBlogComment {
    guint32 magic;
    int comment_id;
    int poster_id;
};

HippoMyspaceContact*
hippo_myspace_contact_new(const char *name,
                          const char *friend_id)
{
    HippoMyspaceContact *contact;
    
    contact = g_new0(HippoMyspaceContact, 1);
    contact->magic = HIPPO_MYSPACE_CONTACT_MAGIC;
    contact->name = g_strdup(name);
    contact->friend_id = g_strdup(friend_id);

    return contact;
}

void
hippo_myspace_contact_free(HippoMyspaceContact *contact)
{
    g_return_if_fail(HIPPO_IS_MYSPACE_CONTACT(contact));
    
    contact->magic = 0xdeadbeef;
    
    g_free(contact->name);
    g_free(contact->friend_id);
    g_free(contact);
}

HippoMyspaceContact*
hippo_myspace_contact_copy(HippoMyspaceContact *contact)
{
    g_return_val_if_fail(HIPPO_IS_MYSPACE_CONTACT(contact), NULL);
    
    return hippo_myspace_contact_new(contact->name, contact->friend_id);
}

const char*
hippo_myspace_contact_get_name(HippoMyspaceContact *contact)
{
    g_return_val_if_fail(HIPPO_IS_MYSPACE_CONTACT(contact), NULL);
    
    return contact->name;
}

const char*
hippo_myspace_contact_get_friend_id(HippoMyspaceContact *contact)
{
    g_return_val_if_fail(HIPPO_IS_MYSPACE_CONTACT(contact), NULL);
    
    return contact->friend_id;
}


HippoMyspaceBlogComment*
hippo_myspace_blog_comment_new(int comment_id,
                               int poster_id)
{
    HippoMyspaceBlogComment *comment;
    
    comment = g_new0(HippoMyspaceBlogComment, 1);
    comment->magic = HIPPO_MYSPACE_BLOG_COMMENT_MAGIC;
    comment->comment_id = comment_id;
    comment->poster_id = poster_id;
    
    return comment;
}

void
hippo_myspace_blog_comment_free(HippoMyspaceBlogComment *comment)
{
    g_return_if_fail(HIPPO_IS_MYSPACE_BLOG_COMMENT(comment));
    
    comment->magic = 0xdeadbeef;    
    
    g_free(comment);
}

HippoMyspaceBlogComment*
hippo_myspace_blog_comment_copy(HippoMyspaceBlogComment *comment)
{
    g_return_val_if_fail(HIPPO_IS_MYSPACE_BLOG_COMMENT(comment), NULL);
    
    return hippo_myspace_blog_comment_new(comment->comment_id, comment->poster_id);
}

int
hippo_myspace_blog_comment_get_comment_id(HippoMyspaceBlogComment *comment)
{
    g_return_val_if_fail(HIPPO_IS_MYSPACE_BLOG_COMMENT(comment), 0);
    
    return comment->comment_id;
}

int
hippo_myspace_blog_comment_get_poster_id(HippoMyspaceBlogComment *comment)
{
    g_return_val_if_fail(HIPPO_IS_MYSPACE_BLOG_COMMENT(comment), 0);
    
    return comment->poster_id;
}

