#ifndef __HIPPO_DATA_CACHE_H__
#define __HIPPO_DATA_CACHE_H__

#include <hippo/hippo-connection.h>
#include <hippo/hippo-person.h>
#include <hippo/hippo-post.h>
#include <hippo/hippo-chat-room.h>

G_BEGIN_DECLS

/* HippoDataCache forward-declared in hippo-connection.h */

#define HIPPO_TYPE_DATA_CACHE              (hippo_data_cache_get_type ())
#define HIPPO_DATA_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DATA_CACHE, HippoDataCache))
#define HIPPO_DATA_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DATA_CACHE, HippoDataCacheClass))
#define HIPPO_IS_DATA_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DATA_CACHE))
#define HIPPO_IS_DATA_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DATA_CACHE))
#define HIPPO_DATA_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DATA_CACHE, HippoDataCacheClass))

GType        	 hippo_data_cache_get_type               (void) G_GNUC_CONST;

HippoDataCache*  hippo_data_cache_new                    (HippoConnection *connection);

HippoPost*       hippo_data_cache_lookup_post            (HippoDataCache  *cache,
                                                          const char      *guid);
HippoEntity*     hippo_data_cache_lookup_entity          (HippoDataCache  *cache,
                                                          const char      *guid);
/* A convenience method like ensure_post() doesn't work well because we want to be 
 * able to init the properties of a post before adding it and thus emitting the 
 * post-added signal
 */
void             hippo_data_cache_add_post               (HippoDataCache *cache,
                                                          HippoPost      *post);
void             hippo_data_cache_add_entity             (HippoDataCache *cache,
                                                          HippoEntity    *entity);
/* but sometimes we want an entity with no properties anyhow */
HippoEntity*     hippo_data_cache_ensure_bare_entity     (HippoDataCache *cache,
                                                          HippoEntityType type,
                                                          const char     *guid);
                                                          
/* must free list and unref each post in it */
GSList*          hippo_data_cache_get_recent_posts       (HippoDataCache  *cache);

HippoChatRoom*   hippo_data_cache_lookup_chat_room       (HippoDataCache  *cache,
                                                          const char      *chat_id,
                                                          HippoChatKind   *kind_p);
HippoChatRoom*   hippo_data_cache_ensure_chat_room       (HippoDataCache  *cache,
                                                          const char      *chat_id,
                                                          HippoChatKind    kind);

G_END_DECLS

#endif /* __HIPPO_DATA_CACHE_H__ */
