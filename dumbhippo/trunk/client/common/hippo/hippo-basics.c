/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-basics.h"

#include <errno.h>
#include <math.h>
#include <string.h>
#include <stdlib.h>
#include "hippo-common-internal.h"

GQuark
hippo_error_quark(void)
{
    return g_quark_from_static_string("hippo-error-quark");
}

#define VALID_GUID_CHAR(c)                    \
             (((c) >= '0' && (c) <= '9') ||   \
              ((c) >= 'A' && (c) <= 'Z') ||   \
              ((c) >= 'a' && (c) <= 'z'))
#define GUID_LEN 14

/* keep in sync with below */
gboolean
hippo_verify_guid(const char *possible_guid)
{
    const char *p;
    p = possible_guid;
    while (*p) {
        if (!VALID_GUID_CHAR(*p))
              return FALSE;
            
        ++p;
    }
    if ((p - possible_guid) != GUID_LEN)
        return FALSE;
        
    return TRUE;
}

/* keep in sync with above */
gboolean
hippo_verify_guid_wide(const gunichar2 *possible_guid)
{
    const gunichar2 *p;
    
    p = possible_guid;
    while (*p) {
        if (!VALID_GUID_CHAR(*p))
              return FALSE;
            
        ++p;
    }
    if ((p - possible_guid) != (sizeof(gunichar2) * GUID_LEN))
        return FALSE;
        
    return TRUE;
}

HippoChatKind
hippo_parse_chat_kind(const char *str)
{
    if (strcmp(str, "post") == 0)
        return HIPPO_CHAT_KIND_POST;
    else if (strcmp(str, "group") == 0)
        return HIPPO_CHAT_KIND_GROUP;
    else if (strcmp(str, "music") == 0)
        return HIPPO_CHAT_KIND_MUSIC;
    else if (strcmp(str, "block") == 0)
        return HIPPO_CHAT_KIND_BLOCK;
    else if (strcmp(str, "unknown") == 0)
        return HIPPO_CHAT_KIND_UNKNOWN;
    else
        return HIPPO_CHAT_KIND_BROKEN;
}

const char*
hippo_chat_kind_as_string(HippoChatKind kind)
{
    switch (kind) {
    case HIPPO_CHAT_KIND_POST:
        return "post";
    case HIPPO_CHAT_KIND_GROUP:
        return "group";
    case HIPPO_CHAT_KIND_MUSIC:
        return "music";
    case HIPPO_CHAT_KIND_BLOCK:
        return "block";
    case HIPPO_CHAT_KIND_UNKNOWN:
        return "unknown";
    case HIPPO_CHAT_KIND_BROKEN:
        return "broken";
    }
    
    g_warning("Invalid HippoChatKind value %d", kind);
    return NULL;
}

gboolean 
hippo_parse_sentiment(const char     *str,
                      HippoSentiment *sentiment)
{
    if (strcmp(str, "INDIFFERENT") == 0) {
        *sentiment = HIPPO_SENTIMENT_INDIFFERENT;
        return TRUE;
    } else if (strcmp(str, "LOVE") == 0) {
        *sentiment = HIPPO_SENTIMENT_LOVE;
        return TRUE;
    } else if (strcmp(str, "HATE") == 0) {
        *sentiment = HIPPO_SENTIMENT_HATE;
        return TRUE;
    }

    return FALSE;
}

const char *
hippo_sentiment_as_string(HippoSentiment sentiment)
{
    switch (sentiment) {
    case HIPPO_SENTIMENT_INDIFFERENT:
        return "INDIFFERENT";
    case HIPPO_SENTIMENT_LOVE:
        return "LOVE";
    case HIPPO_SENTIMENT_HATE:
        return "HATE";
    }

    g_warning("Invalid HippoSentiment value %d", sentiment);
    return NULL;
}

/* rint doesn't exist on Windows */
static double 
hippo_rint(double n)
{
    double ci, fl;
    ci = ceil(n);
    fl = floor(n);
    return (((ci-n) >= (n-fl)) ? fl :ci);
}

/* improvements to this should probably go in the javascript version too */
char*
hippo_format_time_ago(GTime now,
                      GTime then)
{
    GTime delta = now - then;
    double delta_hours;
    double delta_weeks;
    double delta_years;

    if (then <= 0)
        return g_strdup("");
    
    if (delta < 0)
        return g_strdup("the future");

    if (delta < 120)
        return g_strdup("a minute ago");

    if (delta < 60*60) {
        int delta_minutes = delta / 60;
        if (delta_minutes > 5)
            delta_minutes = delta_minutes - (delta_minutes % 5);
        return g_strdup_printf("%d minutes ago", delta_minutes);
    }

    delta_hours = delta / (60.0 * 60.0);

    if (delta_hours < 1.55)
        return g_strdup("1 hr. ago");

    if (delta_hours < 24) {
        return g_strdup_printf("%.0f hrs. ago", hippo_rint(delta_hours));
    }

    if (delta_hours < 48) {
        return g_strdup("Yesterday");
    }
    
    if (delta_hours < 24*15) {
        return g_strdup_printf("%.0f days ago", hippo_rint(delta_hours / 24));
    }

    delta_weeks = delta_hours / (24.0 * 7.0);

    if (delta_weeks < 6) {
        return g_strdup_printf("%.0f weeks ago", hippo_rint(delta_weeks));
    }

    if (delta_weeks < 50) {
        return g_strdup_printf("%.0f months ago", hippo_rint(delta_weeks / 4));
    }

    delta_years = delta_weeks / 52;

    if (delta_years < 1.55)
        return g_strdup_printf("1 year ago");

    return g_strdup_printf("%.0f years ago", hippo_rint(delta_years));
}


char*
hippo_size_photo_url(const char *base_url,
                     int         size)
{
    if (strchr(base_url, '?') != 0)
        return g_strdup_printf("%s&size=%d", base_url, size);
    else
        return g_strdup_printf("%s?size=%d", base_url, size);
}

gint64
hippo_current_time_ms(void)
{
    GTimeVal now;

    g_get_current_time(&now);
    return (gint64)now.tv_sec * 1000 + now.tv_usec / 1000;
}

gboolean
hippo_membership_status_from_string(const char            *s,
                                    HippoMembershipStatus *result)
{
    static const struct { const char *name; HippoMembershipStatus status; } statuses[] = {
        { "NONMEMBER", HIPPO_MEMBERSHIP_STATUS_NONMEMBER },
        { "INVITED_TO_FOLLOW", HIPPO_MEMBERSHIP_STATUS_INVITED_TO_FOLLOW },
        { "FOLLOWER", HIPPO_MEMBERSHIP_STATUS_FOLLOWER },
        { "REMOVED", HIPPO_MEMBERSHIP_STATUS_REMOVED },
        { "INVITED", HIPPO_MEMBERSHIP_STATUS_INVITED },
        { "ACTIVE", HIPPO_MEMBERSHIP_STATUS_ACTIVE }
    };
    unsigned int i;
    for (i = 0; i < G_N_ELEMENTS(statuses); ++i) {
        if (strcmp(s, statuses[i].name) == 0) {
            *result = statuses[i].status;
            return TRUE;
        }
    }
    g_warning("Unknown membership status '%s'", s);
    return FALSE;
}
