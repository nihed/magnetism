/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CONNECTION_H__
#define __HIPPO_CONNECTION_H__

#include <hippo/hippo-platform.h>
#include <hippo/hippo-chat-room.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_STATE_SIGNED_OUT,     // User hasn't asked to connect
    HIPPO_STATE_SIGN_IN_WAIT,   // Waiting for the user to sign in
    HIPPO_STATE_CONNECTING,     // Waiting for connecting to server
    HIPPO_STATE_RETRYING,       // Connection to server failed, retrying
    HIPPO_STATE_AUTHENTICATING, // Waiting for authentication
    HIPPO_STATE_AUTH_WAIT,      // Authentication failed, waiting for new creds
    HIPPO_STATE_AWAITING_CLIENT_INFO, // Authenticated, but waiting for the client info exchange
    HIPPO_STATE_AUTHENTICATED   // Ready to go
} HippoState;

typedef struct {
    char **keys;
    char **values;
} HippoSong;

#define HIPPO_TYPE_CONNECTION              (hippo_connection_get_type ())
#define HIPPO_CONNECTION(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CONNECTION, HippoConnection))
#define HIPPO_CONNECTION_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CONNECTION, HippoConnectionClass))
#define HIPPO_IS_CONNECTION(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CONNECTION))
#define HIPPO_IS_CONNECTION_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CONNECTION))
#define HIPPO_CONNECTION_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CONNECTION, HippoConnectionClass))

GType            hippo_connection_get_type                  (void) G_GNUC_CONST;
HippoConnection *hippo_connection_new                       (HippoPlatform    *platform);

HippoPlatform*   hippo_connection_get_platform              (HippoConnection  *connection);

int              hippo_connection_get_generation            (HippoConnection  *connection);

gboolean         hippo_connection_get_too_old               (HippoConnection  *connection);
gboolean         hippo_connection_get_upgrade_available     (HippoConnection  *connection);
const char*      hippo_connection_get_download_url          (HippoConnection  *connection);

void             hippo_connection_set_cache                 (HippoConnection  *connection,
                                                             HippoDataCache   *cache);

gboolean         hippo_connection_get_has_auth              (HippoConnection  *connection);
void             hippo_connection_forget_auth               (HippoConnection  *connection);
HippoBrowserKind hippo_connection_get_auth_browser          (HippoConnection  *connection);

/* CAN RETURN NULL if we don't have auth information right now */
const char*      hippo_connection_get_self_guid             (HippoConnection  *connection);

HippoState       hippo_connection_get_state                 (HippoConnection  *connection);
/* are we in a state such that the main app should go about its normal server interactions?
 * (right now this means state == AUTHENTICATED) 
 */
gboolean         hippo_connection_get_connected             (HippoConnection  *connection);
/* signin returns TRUE if we're waiting on the user to set the login cookie, FALSE if we already have it */
gboolean         hippo_connection_signin                    (HippoConnection  *connection);
void             hippo_connection_signout                   (HippoConnection  *connection);
void             hippo_connection_notify_post_clicked       (HippoConnection  *connection,
                                                             const char       *post_id);
void             hippo_connection_set_post_ignored          (HippoConnection  *connection,
                                                             const char       *post_id);
void             hippo_connection_do_invite_to_group        (HippoConnection  *connection,
                                                             const char       *group_id,
                                                             const char       *person_id);                                                             
void             hippo_connection_notify_music_changed      (HippoConnection  *connection,
                                                             gboolean          currently_playing,
                                                             const HippoSong  *song);
void             hippo_connection_provide_priming_music     (HippoConnection  *connection,
                                                             const HippoSong  *songs,
                                                             int               n_songs);
                                                            
/*
 * chat_room join/leave act "reference counted", that is, you need to call them in matched
 * pairs. If the join(PARTICIPANT) count is nonzero, we'll be a participant, else if 
 * join(VISITOR) count is nonzero we'll just be a viewer, else if both are zero we'll leave the
 * room.
 */
void             hippo_connection_join_chat_room            (HippoConnection *connection,
                                                             HippoChatRoom   *room,
                                                             HippoChatState   desiredState);
void             hippo_connection_leave_chat_room           (HippoConnection *connection,
                                                             HippoChatRoom   *room,
                                                             HippoChatState   stateJoinedWith);
/* called on every chat room when reconnecting after disconnect */
void             hippo_connection_rejoin_chat_room          (HippoConnection *connection,
                                                             HippoChatRoom   *room);

void             hippo_connection_send_chat_room_message    (HippoConnection *connection,
                                                             HippoChatRoom   *room,
                                                             const char      *text);

void             hippo_connection_request_chat_room_details (HippoConnection *connection,
                                                             HippoChatRoom   *room);

void     hippo_connection_request_prefs             (HippoConnection *connection);
void     hippo_connection_request_recent_posts      (HippoConnection *connection);
void     hippo_connection_request_post              (HippoConnection *connection,
                                                     const char      *post_id);
void     hippo_connection_request_hotness           (HippoConnection *connection);
void     hippo_connection_request_blocks            (HippoConnection *connection,
                                                     gint64           last_timestamp);

void hippo_connection_request_myspace_name          (HippoConnection *connection);
void hippo_connection_request_myspace_blog_comments (HippoConnection *connection);
void hippo_connection_request_myspace_contacts      (HippoConnection *connection);
void hippo_connection_add_myspace_comment           (HippoConnection *connection,
                                                     int              comment_id,
                                                     int              poster_id);
void hippo_connection_notify_myspace_contact_post   (HippoConnection *connection,
                                                     const char      *myspace_name);


const char*      hippo_connection_get_tooltip       (HippoConnection *connection);

/* return string form of enum values */
const char*      hippo_state_debug_string(HippoState state);


/* Convenience wrappers around open_url that create part of the url for you */
char* hippo_connection_make_absolute_url (HippoConnection *connection,
                                          const char      *relative_url);
void  hippo_connection_open_relative_url (HippoConnection *connection,
                                          const char      *relative_url);
void  hippo_connection_visit_post        (HippoConnection *connection,
                                          HippoPost       *post);
void  hippo_connection_visit_post_id     (HippoConnection *connection,
                                          const char      *guid);
void  hippo_connection_visit_entity      (HippoConnection *connection,
                                          HippoEntity     *entity);



G_END_DECLS

#endif /* __HIPPO_CONNECTION_H__ */
