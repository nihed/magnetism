/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include "hippo-block.h"
#include "hippo-block-generic.h"
#include <string.h>

static void      hippo_block_generic_init                (HippoBlockGeneric       *block_generic);
static void      hippo_block_generic_class_init          (HippoBlockGenericClass  *klass);

static void      hippo_block_generic_dispose             (GObject              *object);
static void      hippo_block_generic_finalize            (GObject              *object);

static void      hippo_block_generic_update              (HippoBlock           *block);

static void hippo_block_generic_set_property (GObject      *object,
                                              guint         prop_id,
                                              const GValue *value,
                                              GParamSpec   *pspec);
static void hippo_block_generic_get_property (GObject      *object,
                                              guint         prop_id,
                                              GValue       *value,
                                              GParamSpec   *pspec);

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_DESCRIPTION
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

    block_class->update = hippo_block_generic_update;

    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Description of the block, may be NULL"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_block_generic_dispose(GObject *object)
{
    /* HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object); */
    
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
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_generic_update (HippoBlock *block)
{
    /* HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(block); */
    const char *description;

    HIPPO_BLOCK_CLASS(hippo_block_generic_parent_class)->update(block);

    ddm_data_resource_get(block->resource,
                          "description", DDM_DATA_STRING, &description,
                          NULL);
    
    g_object_set(G_OBJECT(block),
                 "description", description,
                 NULL);
}
