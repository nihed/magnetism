/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>

#include <ddm/ddm.h>

#include "hippo-im.h"
#include "main.h"

#define BUDDY_CLASS "online-desktop:/p/o/buddy"

typedef struct {
    GHashTable *buddies;
} HippoIm;

typedef struct {
    char *resource_id;
    char *protocol;
    char *name;
    char *alias; /* Human visible name */
    gboolean is_online;
    char *status;
    char *webdav_url;
    char *icon_hash;
    char *icon_data_url;
} HippoImBuddy;

static void 
hippo_im_buddy_destroy(HippoImBuddy *buddy)
{
    g_free(buddy->resource_id);
    g_free(buddy->protocol);
    g_free(buddy->name);
    g_free(buddy->alias);
    g_free(buddy->status);
    g_free(buddy->webdav_url);
    g_free(buddy->icon_hash);
    g_free(buddy->icon_data_url);
    g_free(buddy);
}

static void
hippo_im_destroy(HippoIm *im)
{
    g_hash_table_destroy(im->buddies);
    g_free(im);
}

static HippoIm *
hippo_im_get(HippoDataCache *cache)
{
    HippoIm *im = g_object_get_data(G_OBJECT(cache), "hippo-dbus-im");
    if (im == NULL) {
        im = g_new0(HippoIm, 1);
        im->buddies = g_hash_table_new_full(g_str_hash, g_str_equal,
                                            NULL, (GDestroyNotify)hippo_im_buddy_destroy);
        g_object_set_data_full(G_OBJECT(cache), "hippo-dbus-im", im, (GDestroyNotify)hippo_im_destroy);
    }

    return im;
}

void
hippo_im_init(void)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    DDMDataModel *model = hippo_data_cache_get_model(cache);

    ddm_data_model_add_rule(model,
                            "online-desktop:/p/o/buddy",
                            "online-desktop:/p/o/buddy#user",
                            "http://mugshot.org/p/o/user",
                            DDM_DATA_CARDINALITY_01, FALSE, NULL,
                            "(source.aim = target.name and target.protocol = 'aim') or "
                            "(source.xmpp = target.name and target.protocol = 'xmpp') or "
                            "(source = target.name and target.protocol = 'mugshot-local')");
    
    ddm_data_model_add_rule(model,
                            "http://mugshot.org/p/o/user",
                            "online-desktop:/p/o/buddy/reverse#aimBuddy",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_01, FALSE, NULL,
                            "target.aim = source.name and source.protocol = 'aim' and not source.deleted");
    
    ddm_data_model_add_rule(model,
                            "http://mugshot.org/p/o/user",
                            "online-desktop:/p/o/buddy/reverse#xmppBuddy",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_01, FALSE, NULL,
                            "target.xmpp = source.name and source.protocol = 'xmpp' and not source.deleted");
    
    ddm_data_model_add_rule(model,
                            "http://mugshot.org/p/o/user",
                            "online-desktop:/p/o/buddy/reverse#mugshotLocalBuddy",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_01, FALSE, NULL,
                            "target = source.name and source.protocol = 'mugshot-local' and not source.deleted");
    
    ddm_data_model_add_rule(model,
                            "online-desktop:/p/o/global",
                            "online-desktop:/p/o/global#aimBuddies",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_N, FALSE, NULL,
                            "source.protocol = 'aim' and not source.deleted");
    
    ddm_data_model_add_rule(model,
                            "online-desktop:/p/o/global",
                            "online-desktop:/p/o/global#xmppBuddies",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_N, FALSE, NULL,
                            "source.protocol = 'xmpp' and not source.deleted");
    
    ddm_data_model_add_rule(model,
                            "online-desktop:/p/o/global",
                            "online-desktop:/p/o/global#mugshotLocalBuddies",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_N, FALSE, NULL,
                            "source.protocol = 'mugshot-local' and not source.deleted");
}

static gboolean
compare_strings(const char *a, const char *b)
{
    if (a == b)
        return TRUE;

    if (a == NULL || b == NULL)
        return FALSE;

    return strcmp(a, b) == 0;
}

static char*
build_data_url(const char           *icon_content_type,
               const char           *icon_binary_data,
               int                   icon_data_len)
{
    char *base64;
    char *url;
    
    base64 = g_base64_encode((unsigned char*) icon_binary_data, icon_data_len);
    
    url = g_strdup_printf("data:%s;base64,%s", icon_content_type, base64);

    g_free(base64);

    return url;
}

void
hippo_im_update_buddy_icon (const char           *buddy_id,
                            const char           *icon_hash,
                            const char           *icon_content_type,
                            const char           *icon_binary_data,
                            int                   icon_data_len)
{
    DDMDataValue value;
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoIm *im = hippo_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    HippoImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);
    DDMDataResource *buddy_resource;

    g_debug("Updating buddy icon %s %s", buddy_id, icon_hash);
    
    /* This should only happen if we already removed a buddy before its icon data
     * arrives. Since we get the basics on a buddy before its icon data, we
     * would expect the buddy to exist otherwise.
     */
    if (buddy == NULL)
        return;

    if (buddy->icon_hash && strcmp(buddy->icon_hash, icon_hash) == 0)
        return;

    g_free(buddy->icon_hash);
    g_free(buddy->icon_data_url);

    buddy->icon_hash = g_strdup(icon_hash);
    buddy->icon_data_url = build_data_url(icon_content_type,
                                          icon_binary_data,
                                          icon_data_len);

    buddy_resource = ddm_data_model_ensure_local_resource(model, buddy_id, BUDDY_CLASS);
    
    value.type = DDM_DATA_URL;
    value.u.string = buddy->icon_data_url;
    
    ddm_data_resource_update_property(buddy_resource,
                                      ddm_qname_get(BUDDY_CLASS, "icon"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_01,
                                      FALSE, NULL,
                                      &value);

    g_debug("  (updated)");
}

gboolean
hippo_im_has_icon_hash (const char           *buddy_id,
                        const char           *icon_hash)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoIm *im = hippo_im_get(cache);

    HippoImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);

    if (buddy == NULL) {
        return FALSE;
    } else if (buddy->icon_hash == NULL) {
        return icon_hash == NULL;
    } else if (icon_hash == NULL) {
        return FALSE;
    } else {
        return strcmp(buddy->icon_hash, icon_hash) == 0;
    }
}

static char *
canonicalize_aim_name(const char *name)
{
    /* This roughly matches the canonicalization we do on the server
     * ... actually the server lower-cases non-ASCII characters as well,
     * but there are no examples of such AIM names in a sample of 1000+
     * AIM names currently registered.
     */
    
    GString *result = g_string_new(NULL);
    const char *p = name;
    for (p = name; *p; p++) {
        if (g_ascii_isupper(*p))
            g_string_append_c(result, g_ascii_tolower(*p));
        else if (*p != ' ')
            g_string_append_c(result, *p);
    }

    return g_string_free(result, FALSE);
}

void
hippo_im_update_buddy(const char           *buddy_id,
                      const char           *protocol,
                      const char           *name,
                      const char           *alias,
                      gboolean              is_online,
                      const char           *status,
                      const char           *webdav_url)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoIm *im = hippo_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    gboolean new_buddy = FALSE;
    gboolean online_changed;
    DDMDataResource *buddy_resource;
    DDMDataValue value;
    gboolean buddy_changed = FALSE;
    HippoImBuddy *buddy;
    char *canonical_name;

    g_return_if_fail(buddy_id != NULL);
    g_return_if_fail(protocol != NULL);
    g_return_if_fail(name != NULL);
    /* other stuff is allowed to be NULL */

    buddy = g_hash_table_lookup(im->buddies, buddy_id);    
    if (buddy == NULL) {
        buddy = g_new0(HippoImBuddy, 1);
        buddy->resource_id = g_strdup(buddy_id);
        g_hash_table_insert(im->buddies, buddy->resource_id, buddy);
        new_buddy = TRUE;
    }

    buddy_resource = ddm_data_model_ensure_local_resource(model, buddy_id, BUDDY_CLASS);

    if (new_buddy) {
        value.type = DDM_DATA_BOOLEAN;
        value.u.boolean = FALSE;
    
        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "deleted"),
                                          DDM_DATA_UPDATE_REPLACE,
                                          DDM_DATA_CARDINALITY_1,
                                          FALSE, NULL,
                                          &value);

    }

    if (new_buddy || !compare_strings(protocol, buddy->protocol)) {
        g_free(buddy->protocol);
        buddy->protocol = g_strdup(protocol);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->protocol;
        
        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "protocol"),
                                          DDM_DATA_UPDATE_REPLACE,
                                          DDM_DATA_CARDINALITY_1,
                                          TRUE, NULL,
                                          &value);

        buddy_changed = !new_buddy;
    }

    if (protocol != NULL && strcmp(protocol, "aim") == 0)
        canonical_name = canonicalize_aim_name(name);
    else
        canonical_name = g_strdup(name);

    /* FIXME: We should really canonicalize XMPP names as well */

    if (new_buddy || !compare_strings(name, canonical_name)) {
        g_free(buddy->name);
        buddy->name = canonical_name;

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->name;

        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "name"),
                                          DDM_DATA_UPDATE_REPLACE,
                                          DDM_DATA_CARDINALITY_1,
                                          TRUE, NULL,
                                          &value);
        
        buddy_changed = !new_buddy;
    } else {
        g_free(canonical_name);
    }

    if (new_buddy || !compare_strings(alias, buddy->alias)) {
        g_free(buddy->alias);
        buddy->alias = g_strdup(alias);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->alias;

        if (alias == NULL) {
            if (!new_buddy)
                ddm_data_resource_update_property(buddy_resource,
                                                  ddm_qname_get(BUDDY_CLASS, "alias"),
                                                  DDM_DATA_UPDATE_CLEAR,
                                                  DDM_DATA_CARDINALITY_1,
                                                  TRUE, NULL,
                                                  &value);
        } else {
            ddm_data_resource_update_property(buddy_resource,
                                              ddm_qname_get(BUDDY_CLASS, "alias"),
                                              DDM_DATA_UPDATE_REPLACE,
                                              DDM_DATA_CARDINALITY_1,
                                              TRUE, NULL,
                                              &value);
        }
        
        buddy_changed = !new_buddy;
    }

    online_changed= !new_buddy && is_online != buddy->is_online;
    
    if (new_buddy || is_online != buddy->is_online) {
        buddy->is_online = is_online;

        value.type = DDM_DATA_BOOLEAN;
        value.u.boolean = buddy->is_online;
        
        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "isOnline"),
                                          DDM_DATA_UPDATE_REPLACE,
                                          DDM_DATA_CARDINALITY_1,
                                          TRUE, NULL,
                                          &value);
        
        buddy_changed = !new_buddy;
    }

    if (new_buddy || !compare_strings(status, buddy->status)) {
        g_free(buddy->status);
        buddy->status = g_strdup(status);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->status;

        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "status"),
                                          buddy->status ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_CLEAR,
                                          DDM_DATA_CARDINALITY_01,
                                          TRUE, NULL,
                                          &value);
        
        buddy_changed = !new_buddy;
    }

    if (new_buddy || !compare_strings(webdav_url, buddy->webdav_url)) {
        g_free(buddy->webdav_url);
        buddy->webdav_url = g_strdup(webdav_url);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->webdav_url;

        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "webdavUrl"),
                                          buddy->webdav_url ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_CLEAR,
                                          DDM_DATA_CARDINALITY_01,
                                          TRUE, NULL,
                                          &value);
        
        buddy_changed = !new_buddy;
    }
}

void 
hippo_im_remove_buddy(const char         *buddy_id)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoIm *im = hippo_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    DDMDataResource *buddy_resource;
    DDMDataValue value;

    HippoImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);

    if (buddy == NULL)
        return;

    buddy_resource = ddm_data_model_ensure_local_resource(model, buddy_id, BUDDY_CLASS);

    value.type = DDM_DATA_BOOLEAN;
    value.u.boolean = TRUE;
    
    ddm_data_resource_update_property(buddy_resource,
                                      ddm_qname_get(BUDDY_CLASS, "deleted"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);

    g_hash_table_remove(im->buddies, buddy_id);
}
