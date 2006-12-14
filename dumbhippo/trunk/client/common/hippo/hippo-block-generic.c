/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-generic.h"
#include "hippo-xml-utils.h"
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

static void set_source                       (HippoBlockGeneric     *block_generic,
                                              HippoEntity           *entity);

struct _HippoBlockGeneric {
    HippoBlock       parent;
    char            *title;
    char            *link;
    char            *description;
    HippoEntity     *source;
};

struct _HippoBlockGenericClass {
    HippoBlockClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_TITLE,
    PROP_LINK,
    PROP_DESCRIPTION,
    PROP_SOURCE
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
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("Title"),
                                                        _("Title of the block"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_LINK,
                                    g_param_spec_string("link",
                                                        _("Link"),
                                                        _("Link when clicking on title of the block"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Description of the block, may be NULL"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));


    g_object_class_install_property(object_class,
                                    PROP_SOURCE,
                                    g_param_spec_object("source",
                                                        _("Source"),
                                                        _("Source displayed in the block"),
                                                        HIPPO_TYPE_ENTITY,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

}

static void
hippo_block_generic_dispose(GObject *object)
{
    HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object);

    set_source(block_generic, NULL);

    G_OBJECT_CLASS(hippo_block_generic_parent_class)->dispose(object);
}

static void
hippo_block_generic_finalize(GObject *object)
{
    HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object);

    g_free(block_generic->title);
    g_free(block_generic->link);
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
    case PROP_SOURCE:
        set_source(block_generic, (HippoEntity*) g_value_get_object(value));
        break;
    case PROP_TITLE:
        g_free(block_generic->title);
        block_generic->title = g_value_dup_string(value);
        break;
    case PROP_DESCRIPTION:
        g_free(block_generic->description);
        block_generic->description = g_value_dup_string(value);
        break;
    case PROP_LINK:
        g_free(block_generic->link);
        block_generic->link = g_value_dup_string(value);
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
    case PROP_SOURCE:
        g_value_set_object(value, (GObject*) block_generic->source);
        break;
    case PROP_TITLE:
        g_value_set_string(value, block_generic->title);
        break;
    case PROP_LINK:
        g_value_set_string(value, block_generic->link);
        break;
    case PROP_DESCRIPTION:
        g_value_set_string(value, block_generic->description);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_block_generic_update_from_xml (HippoBlock           *block,
                                     HippoDataCache       *cache,
                                     LmMessageNode        *node)
{
    /* HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(block); */
    LmMessageNode *title_node;
    LmMessageNode *description_node;
    LmMessageNode *source_node;
    HippoEntity *source;
    const char *description;
    const char *title;
    const char *link;

    if (!HIPPO_BLOCK_CLASS(hippo_block_generic_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    title_node = NULL;
    description_node = NULL;
    source_node = NULL;
    if (!hippo_xml_split(cache, node, NULL,
                         "title", HIPPO_SPLIT_NODE, &title_node,
                         "source", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &source_node,
                         "description", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &description_node,
                         NULL))
        return FALSE;

    title = lm_message_node_get_value(title_node);
    link = lm_message_node_get_attribute(title_node, "link");
    if (title == NULL || link == NULL)
        return FALSE;

    source = NULL;
    description = NULL;

    if (source_node != NULL) {
        if (!hippo_xml_split(cache, source_node, NULL,
                             "source", HIPPO_SPLIT_ENTITY, &source,
                             NULL))
            return FALSE;

    }

    if (description_node != NULL) {
        description = lm_message_node_get_value(description_node);
    }

    g_object_set(G_OBJECT(block),
                 "title", title,
                 "link", link,
                 "source", source,
                 "description", description,
                 NULL);

    return TRUE;
}

static void
set_source(HippoBlockGeneric        *block_generic,
           HippoEntity              *source)
{
    if (source == block_generic->source)
        return;

    if (block_generic->source) {
        g_object_unref(block_generic->source);
        block_generic->source = NULL;
    }

    if (source) {
        g_object_ref(source);
        block_generic->source = source;
    }

    g_object_notify(G_OBJECT(block_generic), "source");
}
