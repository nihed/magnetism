#include "hippo-data-cache.h"

static void      hippo_data_cache_init                (HippoDataCache       *cache);
static void      hippo_data_cache_class_init          (HippoDataCacheClass  *klass);


struct _HippoDataCache {
    GObject parent;
};

struct _HippoDataCacheClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoDataCache, hippo_data_cache, G_TYPE_OBJECT);
                       
static void
hippo_data_cache_init(HippoDataCache *cache)
{

}

static void
hippo_data_cache_class_init(HippoDataCacheClass *klass)
{


}

HippoDataCache*
hippo_data_cache_new(HippoConnection *connection)
{
    HippoDataCache *cache = g_object_new(HIPPO_TYPE_DATA_CACHE, NULL);

    return cache;
}
