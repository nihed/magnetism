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

struct _HippoBlockGeneric {
    HippoBlock       parent;
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
    PROP_0
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
    G_OBJECT_CLASS(hippo_block_generic_parent_class)->finalize(object);
}

static void
hippo_block_generic_set_property(GObject         *object,
                                 guint            prop_id,
                                 const GValue    *value,
                                 GParamSpec      *pspec)
{
    /* HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object); */

    switch (prop_id) {

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
    /* HippoBlockGeneric *block_generic = HIPPO_BLOCK_GENERIC(object); */

    switch (prop_id) {

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

    if (!HIPPO_BLOCK_CLASS(hippo_block_generic_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    return TRUE;
}
