/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include "hippo-data-cache.h"
#include "hippo-external-account.h"
#include "hippo-xml-utils.h"

static void     hippo_external_account_finalize             (GObject *object);

static void hippo_external_account_set_property (GObject      *object,
                                      guint         prop_id,
                                      const GValue *value,
                                      GParamSpec   *pspec);
static void hippo_external_account_get_property (GObject      *object,
                                      guint         prop_id,
                                      GValue       *value,
                                      GParamSpec   *pspec);

struct _HippoExternalAccount {
    GObject parent;
    
    char *name;
    char *icon_url;
};

struct _HippoExternalAccountClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoExternalAccount, hippo_external_account, G_TYPE_OBJECT);

/*
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
*/

enum {
    PROP_0,
    PROP_NAME,
    PROP_ICON_URL
};

static void
hippo_external_account_init(HippoExternalAccount *track)
{
}

static void
hippo_external_account_class_init(HippoExternalAccountClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
          
    object_class->finalize = hippo_external_account_finalize;

    object_class->set_property = hippo_external_account_set_property;
    object_class->get_property = hippo_external_account_get_property;

    g_object_class_install_property(object_class,
                                    PROP_NAME,
                                    g_param_spec_string("name",
                                                        _("Name"),
                                                        _("Name of account"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_ICON_URL,
                                    g_param_spec_string("icon-url",
                                                        _("Icon Url"),
                                                        _("Possibly-remote URL of favicon"),
                                                        NULL,
                                                        G_PARAM_READABLE));
}

static void
hippo_external_account_finalize(GObject *object)
{
    HippoExternalAccount *track = HIPPO_EXTERNAL_ACCOUNT(object);
    
    g_free(track->name);
    g_free(track->icon_url);
  
    G_OBJECT_CLASS(hippo_external_account_parent_class)->finalize(object); 
}

static void
hippo_external_account_set_property(GObject         *object,
                                    guint            prop_id,
                                    const GValue    *value,
                                    GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_external_account_get_property(GObject         *object,
                                    guint            prop_id,
                                    GValue          *value,
                                    GParamSpec      *pspec)
{
    HippoExternalAccount *track;

    track = HIPPO_EXTERNAL_ACCOUNT(object);

    switch (prop_id) {
    case PROP_NAME:
        g_value_set_string(value, track->name);
        break;
    case PROP_ICON_URL:
        g_value_set_string(value, track->icon_url);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/* === HippoExternalAccount exported API === */

HippoExternalAccount *
hippo_external_account_new_from_xml(HippoDataCache *cache,
                                    LmMessageNode  *node)
{
    HippoExternalAccount *acct;
    const char *name;
    const char *icon_url;

    acct = g_object_new(HIPPO_TYPE_EXTERNAL_ACCOUNT, NULL);

    if (!hippo_xml_split(cache, node, NULL,
                         "type", HIPPO_SPLIT_STRING, &name,
                         "icon", HIPPO_SPLIT_STRING, &icon_url,
                         NULL))
        return NULL;
    acct->name = g_strdup(name);
    acct->icon_url = g_strdup(icon_url);

    return acct;
}
