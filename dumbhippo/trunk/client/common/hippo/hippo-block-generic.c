/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-generic.h"
#include "hippo-xml-utils.h"
#include "hippo-thumbnails.h"
#include <string.h>

static void      hippo_block_generic_init                (HippoBlockGeneric       *block_generic);
static void      hippo_block_generic_class_init          (HippoBlockGenericClass  *klass);

static void      hippo_block_generic_dispose             (GObject              *object);
static void      hippo_block_generic_finalize            (GObject              *object);

static gboolean  hippo_block_generic_update_from_xml     (HippoBlock           *block,
                                                          HippoDataCache       *cache,
                                                          LmMessageNode        *node);

static void hippo_block_generic_set_property (GObject      *object,
                                              guint         prop_id,
                                              const GValue *value,
                                              GParamSpec   *pspec);
static void hippo_block_generic_get_property (GObject      *object,
                                              guint         prop_id,
                                              GValue       *value,
                                              GParamSpec   *pspec);

static void set_thumbnails (HippoBlockGeneric      *block_generic,
                            HippoThumbnails        *thumbnails);

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_DESCRIPTION,
    PROP_THUMBNAILS
};

G_DEFINE_TYPE(HippoBlockGeneric, hippo_block_generic, HIPPO_TYPE_BLOCK);

static void
hippo_block_generic_init(HippoBlockGeneric *block_generic)
{
}

static void
hippo_block_generic_class_init(HippoBlockGenericClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_generic_set_property;
    object_class->get_property = hippo_block_generic_get_property;

    object_class->dispose = hippo_block_generic_dispose;
    object_class->finalize = hippo_block_generic_finalize;

    block_class->update_from_xml = hippo_block_generic_update_from_xml;

    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Description of the block, may be NULL"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_THUMBNAILS,
                                    g_param_spec_object("thumbnails",
                                                        _("Thumbnails"),
                                                        _("The thumbnails or NULL if none"),
                                                        HIPPO_TYPE_THUMBNAILS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));    
}

static void
hippo_block_generic_dispose(GObject *object)
{
    HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object);
    
    set_thumbnails(block_generic, NULL);
    
    G_OBJECT_CLASS(hippo_block_generic_parent_class)->dispose(object);
}

static void
hippo_block_generic_finalize(GObject *object)
{
    HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object);

    g_free(block_generic->description);

    G_OBJECT_CLASS(hippo_block_generic_parent_class)->finalize(object);
}

static void
hippo_block_generic_set_property(GObject         *object,
                                 guint            prop_id,
                                 const GValue    *value,
                                 GParamSpec      *pspec)
{
    HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object);

    switch (prop_id) {
    case PROP_DESCRIPTION:
        g_free(block_generic->description);
        block_generic->description = g_value_dup_string(value);
        break;
    case PROP_THUMBNAILS:
        set_thumbnails(block_generic, (HippoThumbnails*) g_value_get_object(value));
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_generic_get_property(GObject         *object,
                                 guint            prop_id,
                                 GValue          *value,
                                 GParamSpec      *pspec)
{
    HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object);

    switch (prop_id) {
    case PROP_DESCRIPTION:
        g_value_set_string(value, block_generic->description);
        break;
    case PROP_THUMBNAILS:
        g_value_set_object(value, (GObject*) block_generic->thumbnails);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_thumbnails (HippoBlockGeneric      *block_generic,
                HippoThumbnails        *thumbnails)
{
    if (block_generic->thumbnails == thumbnails)
        return;

    if (block_generic->thumbnails) {
        g_object_unref(block_generic->thumbnails);

        block_generic->thumbnails = NULL;
    }

    if (thumbnails) {
        g_object_ref(thumbnails);
        block_generic->thumbnails = thumbnails;
    }

    g_object_notify(G_OBJECT(block_generic), "thumbnails");
}

static gboolean
hippo_block_generic_update_from_xml (HippoBlock           *block,
                                     HippoDataCache       *cache,
                                     LmMessageNode        *node)
{
    /* HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(block); */
    LmMessageNode *description_node;
    LmMessageNode *thumbnails_node;
    const char *description;
    HippoThumbnails *thumbnails;

    if (!HIPPO_BLOCK_CLASS(hippo_block_generic_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    description_node = NULL;
    thumbnails_node = NULL;
    if (!hippo_xml_split(cache, node, NULL,
                         "description", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &description_node,
                         "thumbnails", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &thumbnails_node,
                         NULL))
        return FALSE;

    description = NULL;

    if (description_node != NULL) {
        description = lm_message_node_get_value(description_node);
    }

    thumbnails = NULL;
    
    if (thumbnails_node != NULL) {
        thumbnails = hippo_thumbnails_new_from_xml(cache, thumbnails_node);
        if (thumbnails == NULL)
            g_warning("Failed to parse <thumbnails> node");
        else
            g_debug("Parsed %d thumbnails", hippo_thumbnails_get_count(thumbnails));
    }
    
    g_object_set(G_OBJECT(block),
                 "description", description,
                 "thumbnails", thumbnails,
                 NULL);

    if (thumbnails != NULL)
        g_object_unref(thumbnails);
    
    return TRUE;
}
