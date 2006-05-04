#ifndef __HIPPO_DATA_CACHE_H__
#define __HIPPO_DATA_CACHE_H__

#include <hippo/hippo-connection.h>

G_BEGIN_DECLS

typedef struct _HippoDataCache      HippoDataCache;
typedef struct _HippoDataCacheClass HippoDataCacheClass;

#define HIPPO_TYPE_DATA_CACHE              (hippo_data_cache_get_type ())
#define HIPPO_DATA_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DATA_CACHE, HippoDataCache))
#define HIPPO_DATA_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DATA_CACHE, HippoDataCacheClass))
#define HIPPO_IS_DATA_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DATA_CACHE))
#define HIPPO_IS_DATA_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DATA_CACHE))
#define HIPPO_DATA_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DATA_CACHE, HippoDataCacheClass))

GType        	 hippo_data_cache_get_type               (void) G_GNUC_CONST;

HippoDataCache*  hippo_data_cache_new                    (HippoConnection *connection);

G_END_DECLS

#endif /* __HIPPO_DATA_CACHE_H__ */
