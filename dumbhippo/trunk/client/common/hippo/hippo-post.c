/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-feed.h"
#include "hippo-group.h"
#include "hippo-person.h"
#include "hippo-post.h"
#include <string.h>

/* === HippoPost implementation === */

static void     hippo_post_finalize             (GObject *object);

struct _HippoPost {
    GObject parent;
    DDMDataResource *resource;
    char *guid;
    HippoEntity *sender;
    char *url;
    char *title;
    char *description;
    GSList *recipients;
    GTime date;

    /* GObject-like freeze count for coelescing notifications */
    int notify_freeze_count;
    gboolean need_notify;
};

struct _HippoPostClass {
    GObjectClass parent;
};

static void hippo_post_update (HippoPost *post);

static void on_post_resource_changed (DDMDataResource *resource,
                                      GSList          *changed_properties,
                                      gpointer         data);

G_DEFINE_TYPE(HippoPost, hippo_post, G_TYPE_OBJECT);

enum {
    CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_post_init(HippoPost *post)
{
}

static void
hippo_post_class_init(HippoPostClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    signals[CHANGED] =
        g_signal_new ("changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
          
    object_class->finalize = hippo_post_finalize;
}

static void
hippo_post_finalize(GObject *object)
{
    HippoPost *post = HIPPO_POST(object);

    ddm_data_resource_disconnect(post->resource, on_post_resource_changed, post);
    ddm_data_resource_unref(post->resource);

    g_free(post->guid);
    if (post->sender)
        g_object_unref(post->sender);
    g_free(post->url);
    g_free(post->title);
    g_free(post->description);
    g_slist_foreach(post->recipients, (GFunc) g_object_unref, NULL);
    g_slist_free(post->recipients);    

    G_OBJECT_CLASS(hippo_post_parent_class)->finalize(object); 
}

static void
hippo_post_freeze_notify(HippoPost *post)
{
    post->notify_freeze_count++;
}

static void
hippo_post_thaw_notify(HippoPost *post)
{
    post->notify_freeze_count--;
    if (post->notify_freeze_count == 0 && post->need_notify) {
        post->need_notify = FALSE;
        g_signal_emit(post, signals[CHANGED], 0);
    }
}

static void 
hippo_post_notify(HippoPost *post)
{
    if (post->notify_freeze_count == 0)
        g_signal_emit(post, signals[CHANGED], 0);
    else
        post->need_notify = TRUE;
}

/* === HippoPost exported API === */

static void
on_post_resource_changed(DDMDataResource *resource,
                         GSList          *changed_properties,
                         gpointer         data)
{
    hippo_post_update(data);
}

HippoPost*
hippo_post_new(DDMDataResource *resource)
{
    const char *resource_id = ddm_data_resource_get_resource_id(resource);
    const char *slash = strrchr(resource_id, '/');
    HippoPost *post;

    if (slash == NULL) {
        g_warning("Cannot extract post GUID from resource ID");
        return NULL;
    }
    
    post = g_object_new(HIPPO_TYPE_POST, NULL);
    post->resource = ddm_data_resource_ref(resource);

    post->guid = g_strdup(slash + 1);

    ddm_data_resource_connect(post->resource, NULL, on_post_resource_changed, post);
    hippo_post_update(post);
    
    return post;
}

static void
hippo_post_update(HippoPost *post)
{
    const char *link, *title, *text;
    DDMDataResource *poster_resource;
    DDMDataResource *feed_resource;
    GSList *user_recipients;
    GSList *group_recipients;
    GSList *recipients = NULL;
    GSList *l;
    gint64 post_date;

    ddm_data_resource_get(post->resource,
                          "link", DDM_DATA_URL, &link,
                          "poster", DDM_DATA_RESOURCE, &poster_resource,
                          "feed", DDM_DATA_RESOURCE, &feed_resource,
                          "title", DDM_DATA_STRING, &title,
                          "text", DDM_DATA_STRING, &text,
                          "date", DDM_DATA_LONG, &post_date,
                          "userRecipients", DDM_DATA_RESOURCE | DDM_DATA_LIST, &user_recipients,
                          "groupRecipients", DDM_DATA_RESOURCE | DDM_DATA_LIST, &group_recipients,
                          NULL);

    hippo_post_freeze_notify(post);

    for (l = user_recipients; l; l = l->next) {
        DDMDataResource *user_resource = l->data;
        recipients = g_slist_prepend(recipients, hippo_person_get_for_resource(user_resource));
    }
    
    for (l = group_recipients; l; l = l->next) {
        DDMDataResource *group_resource = l->data;
        recipients = g_slist_prepend(recipients, hippo_group_get_for_resource(group_resource));
    }
                          
    hippo_post_set_recipients(post, recipients);
    g_slist_foreach(recipients, (GFunc)g_object_unref, NULL);
    g_slist_free(recipients);

    hippo_post_set_url(post, link);
    if (poster_resource) {
        HippoPerson *person = hippo_person_get_for_resource(poster_resource);
        hippo_post_set_sender(post, HIPPO_ENTITY(person));
        g_object_unref(person);
    } else if (feed_resource) {
        HippoFeed *feed = hippo_feed_get_for_resource(feed_resource);
        hippo_post_set_sender(post, HIPPO_ENTITY(feed));
        g_object_unref(feed);
    } else {
        hippo_post_set_sender(post, NULL);
    }
    
    hippo_post_set_date(post, post_date / 1000); /* Convert ms to seconds */
    hippo_post_set_title(post, title);
    hippo_post_set_description(post, text);

    hippo_post_thaw_notify(post);
}

DDMDataResource *
hippo_post_get_resource(HippoPost *post)
{
    return post->resource;
}

const char*
hippo_post_get_guid(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->guid;
}

HippoEntity *
hippo_post_get_sender(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->sender;
}

const char*
hippo_post_get_url(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->url;
}

const char*
hippo_post_get_title(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->title;
}

const char*
hippo_post_get_description(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->description;
}

GSList*
hippo_post_get_recipients(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->recipients;
}

GTime
hippo_post_get_date(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);
    return post->date;
}

static void
set_str(HippoPost *post, char **s_p, const char *val)
{
    if (*s_p == val) /* catches both null, or self-assignment */
        return;
    if (*s_p && val && strcmp(*s_p, val) == 0)
        return;
        
    g_free(*s_p);
    *s_p = g_strdup(val);
    
    hippo_post_notify(post);
}
                   
void
hippo_post_set_sender(HippoPost   *post,
                      HippoEntity *sender)
{
    g_return_if_fail(HIPPO_IS_POST(post));

    if (sender == post->sender)
        return;

    if (post->sender)
        g_object_unref(post->sender);

    post->sender = sender;
    
    if (post->sender)
        g_object_ref(post->sender);
    
    hippo_post_notify(post);
}

void
hippo_post_set_url(HippoPost  *post,
                   const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->url, value);
}

void
hippo_post_set_title(HippoPost  *post,
                   const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->title, value);
}

void
hippo_post_set_description(HippoPost  *post,
                           const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->description, value);                           
}

#if 0
static void
warn_if_entity_list_has_dups(GSList *value)
{
    GHashTable *table;
    
    table = g_hash_table_new(g_str_hash, g_str_equal);
    
    while (value != NULL) {
        HippoEntity *new = HIPPO_ENTITY(value->data);
        HippoEntity *old = g_hash_table_lookup(table, hippo_entity_get_guid(new));
        if (old != NULL) {
            g_warning("Entity list has dup new '%s' %p old '%s' %p", 
                       hippo_entity_get_guid(new), new,
                       hippo_entity_get_guid(old), old);
        }
        g_hash_table_replace(table, (char*) hippo_entity_get_guid(new), new);
    
        value = value->next;
    }
    g_hash_table_destroy(table);
}
#endif

static void
set_entity_list(HippoPost *post, GSList **list_p, GSList *value)
{
    GSList *copy;
    
    /* warn_if_entity_list_has_dups(value); */
    
    copy = g_slist_copy(value);
    g_slist_foreach(copy, (GFunc) g_object_ref, NULL);
    g_slist_foreach(*list_p, (GFunc) g_object_unref, NULL);
    g_slist_free(*list_p);
    *list_p = copy;
    hippo_post_notify(post);
}

void
hippo_post_set_recipients(HippoPost  *post,
                          GSList     *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_entity_list(post, &post->recipients, value);
}

void
hippo_post_set_date(HippoPost  *post,
                    GTime       value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    if (post->date != value) {
        post->date = value;
        hippo_post_notify(post);
    }
}
