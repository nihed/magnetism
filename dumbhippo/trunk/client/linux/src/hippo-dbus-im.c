/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>

#include <ddm/ddm.h>

#include "hippo-dbus-im.h"
#include "main.h"

#define BUDDY_CLASS "online-desktop:/p/o/buddy"

typedef struct {
    GHashTable *buddies;
} HippoDBusIm;

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
} HippoDBusImBuddy;

static void 
hippo_dbus_im_buddy_destroy(HippoDBusImBuddy *buddy)
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
hippo_dbus_im_destroy(HippoDBusIm *im)
{
    g_hash_table_destroy(im->buddies);
    g_free(im);
}

static HippoDBusIm *
hippo_dbus_im_get(HippoDataCache *cache)
{
    HippoDBusIm *im = g_object_get_data(G_OBJECT(cache), "hippo-dbus-im");
    if (im == NULL) {
        im = g_new0(HippoDBusIm, 1);
        im->buddies = g_hash_table_new_full(g_str_hash, g_str_equal,
                                            NULL, (GDestroyNotify)hippo_dbus_im_buddy_destroy);
        g_object_set_data_full(G_OBJECT(cache), "hippo-dbus-im", im, (GDestroyNotify)hippo_dbus_im_destroy);
    }

    return im;
}

void
hippo_dbus_init_im(void)
{
}

static DDMDataResource *
get_system_resource(DDMDataModel *model)
{
    return ddm_data_model_ensure_local_resource(model, DDM_GLOBAL_RESOURCE, DDM_GLOBAL_RESOURCE_CLASS);
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
hippo_dbus_im_update_buddy_icon (const char           *buddy_id,
                                 const char           *icon_hash,
                                 const char           *icon_content_type,
                                 const char           *icon_binary_data,
                                 int                   icon_data_len)
{
    DDMDataValue value;
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    HippoDBusImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);
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
hippo_dbus_im_has_icon_hash (const char           *buddy_id,
                             const char           *icon_hash)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);

    HippoDBusImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);

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

void
hippo_dbus_im_update_buddy(const char           *buddy_id,
                           const char           *protocol,
                           const char           *name,
                           const char           *alias,
                           gboolean              is_online,
                           const char           *status,
                           const char           *webdav_url)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    gboolean new_buddy = FALSE;
    gboolean online_changed;
    DDMDataResource *buddy_resource;
    DDMDataValue value;
    gboolean buddy_changed = FALSE;
    HippoDBusImBuddy *buddy;

    g_return_if_fail(buddy_id != NULL);
    g_return_if_fail(protocol != NULL);
    g_return_if_fail(name != NULL);
    /* other stuff is allowed to be NULL */

    buddy = g_hash_table_lookup(im->buddies, buddy_id);    
    if (buddy == NULL) {
        buddy = g_new0(HippoDBusImBuddy, 1);
        buddy->resource_id = g_strdup(buddy_id);
        g_hash_table_insert(im->buddies, buddy->resource_id, buddy);
        new_buddy = TRUE;
    }

    buddy_resource = ddm_data_model_ensure_local_resource(model, buddy_id, BUDDY_CLASS);

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

    if (new_buddy || !compare_strings(name, buddy->name)) {
        g_free(buddy->name);
        buddy->name = g_strdup(name);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->name;
        
        ddm_data_resource_update_property(buddy_resource,
                                          ddm_qname_get(BUDDY_CLASS, "name"),
                                          DDM_DATA_UPDATE_REPLACE,
                                          DDM_DATA_CARDINALITY_1,
                                          TRUE, NULL,
                                          &value);
        
        buddy_changed = !new_buddy;
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

    if (online_changed || (new_buddy && buddy->is_online)) {
        DDMDataResource *system_resource = get_system_resource(model);
        DDMDataValue value;

        value.type = DDM_DATA_RESOURCE;
        value.u.resource = buddy_resource;
        
        ddm_data_resource_update_property(system_resource,
                                          ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "onlineBuddies"),
                                          buddy->is_online ? DDM_DATA_UPDATE_ADD : DDM_DATA_UPDATE_DELETE,
                                          DDM_DATA_CARDINALITY_N,
                                          FALSE, NULL,
                                          &value);
        
    }
}

void 
hippo_dbus_im_remove_buddy(const char         *buddy_id)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    DDMDataResource *system_resource = get_system_resource(model);

    HippoDBusImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);

    if (buddy == NULL)
        return;

    if (buddy->is_online) {
        DDMDataResource *buddy_resource = ddm_data_model_lookup_resource(model, buddy_id);
        DDMDataValue value;

        value.type = DDM_DATA_RESOURCE;
        value.u.resource = buddy_resource;
        
        ddm_data_resource_update_property(system_resource,
                                          ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "onlineBuddies"),
                                          DDM_DATA_UPDATE_DELETE,
                                          DDM_DATA_CARDINALITY_N,
                                          FALSE, NULL,
                                          &value);
    }

    g_hash_table_remove(im->buddies, buddy_id);
}
