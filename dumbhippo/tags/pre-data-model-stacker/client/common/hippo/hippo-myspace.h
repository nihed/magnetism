/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_MYSPACE_H__
#define __HIPPO_MYSPACE_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct HippoMyspaceContact HippoMyspaceContact;
typedef struct HippoMyspaceBlogComment HippoMyspaceBlogComment;


HippoMyspaceContact*     hippo_myspace_contact_new                 (const char              *name,
                                                                    const char              *friend_id);
void                     hippo_myspace_contact_free                (HippoMyspaceContact     *contact);
HippoMyspaceContact*     hippo_myspace_contact_copy                (HippoMyspaceContact     *contact);
const char*              hippo_myspace_contact_get_name            (HippoMyspaceContact     *contact);
const char*              hippo_myspace_contact_get_friend_id       (HippoMyspaceContact     *contact);


HippoMyspaceBlogComment* hippo_myspace_blog_comment_new            (int                      comment_id,
                                                                    int                      poster_id);
void                     hippo_myspace_blog_comment_free           (HippoMyspaceBlogComment *comment);
HippoMyspaceBlogComment* hippo_myspace_blog_comment_copy           (HippoMyspaceBlogComment *comment);
int                      hippo_myspace_blog_comment_get_comment_id (HippoMyspaceBlogComment *comment);
int                      hippo_myspace_blog_comment_get_poster_id  (HippoMyspaceBlogComment *comment);

G_END_DECLS

#endif /* __HIPPO_MYSPACE_H__ */

