/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BASICS_H__
#define __HIPPO_BASICS_H__

#include <config.h>
#include <glib-object.h>

G_BEGIN_DECLS

/* Having a single error enum for everything is pretty crazy */
typedef enum {
    HIPPO_ERROR_ALREADY_RUNNING, /* Client already running for this server */
    HIPPO_ERROR_FAILED
} HippoError;

#define HIPPO_ERROR hippo_error_quark()
GQuark hippo_error_quark(void);

typedef enum {
    HIPPO_INSTANCE_NORMAL,
    HIPPO_INSTANCE_DOGFOOD,
    HIPPO_INSTANCE_DEBUG
} HippoInstanceType;

typedef enum {
    HIPPO_CHAT_STATE_NONMEMBER,
    HIPPO_CHAT_STATE_VISITOR,
    HIPPO_CHAT_STATE_PARTICIPANT
} HippoChatState;

typedef enum {
    HIPPO_CHAT_KIND_UNKNOWN,
    HIPPO_CHAT_KIND_POST,
    HIPPO_CHAT_KIND_GROUP,
    HIPPO_CHAT_KIND_MUSIC,
    HIPPO_CHAT_KIND_BLOCK,
    HIPPO_CHAT_KIND_BROKEN
} HippoChatKind;

typedef enum {
    HIPPO_MEMBERSHIP_STATUS_NONMEMBER,
    HIPPO_MEMBERSHIP_STATUS_INVITED_TO_FOLLOW,
    HIPPO_MEMBERSHIP_STATUS_FOLLOWER,
    HIPPO_MEMBERSHIP_STATUS_REMOVED,
    HIPPO_MEMBERSHIP_STATUS_INVITED,
    HIPPO_MEMBERSHIP_STATUS_ACTIVE
} HippoMembershipStatus;

typedef enum {
    HIPPO_SENTIMENT_INDIFFERENT,
    HIPPO_SENTIMENT_LOVE,
    HIPPO_SENTIMENT_HATE
} HippoSentiment;

gboolean hippo_verify_guid           (const char      *possible_guid);
gboolean hippo_verify_guid_wide      (const gunichar2 *possible_guid);

/* same strings used in URIs, the xmpp protocol */
HippoChatKind hippo_parse_chat_kind        (const char   *str);
const char*   hippo_chat_kind_as_string    (HippoChatKind kind);

gboolean hippo_parse_sentiment(const char     *str,
                               HippoSentiment *sentiment);
const char *hippo_sentiment_as_string(HippoSentiment sentiment);

gint64   hippo_current_time_ms             (void);
char*    hippo_format_time_ago             (GTime       now,
                                            GTime       then);

gboolean hippo_membership_status_from_string (const char            *s,
                                              HippoMembershipStatus *result);

char*    hippo_size_photo_url              (const char *base_url,
                                            int         size);

G_END_DECLS

#endif /* __HIPPO_BASICS_H__ */
