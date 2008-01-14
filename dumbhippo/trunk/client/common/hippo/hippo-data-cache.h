/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_CACHE_H__
#define __HIPPO_DATA_CACHE_H__

#include <loudmouth/loudmouth.h>
#include <hippo/hippo-connection.h>
#include <ddm/ddm.h>
#include <hippo/hippo-person.h>
#include <hippo/hippo-chat-room.h>

G_BEGIN_DECLS

/* HippoDataCache forward-declared in hippo-basics.h */

typedef struct
{
    char *minimum;
    char *current;
    char *download;
    char *ddm_protocol_version;
} HippoClientInfo;

#define HIPPO_TYPE_DATA_CACHE              (hippo_data_cache_get_type ())
#define HIPPO_DATA_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DATA_CACHE, HippoDataCache))
#define HIPPO_DATA_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DATA_CACHE, HippoDataCacheClass))
#define HIPPO_IS_DATA_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DATA_CACHE))
#define HIPPO_IS_DATA_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DATA_CACHE))
#define HIPPO_DATA_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DATA_CACHE, HippoDataCacheClass))

GType            hippo_data_cache_get_type               (void) G_GNUC_CONST;

HippoDataCache*  hippo_data_cache_new                    (HippoConnection *connection);

HippoConnection* hippo_data_cache_get_connection         (HippoDataCache  *cache);

HippoChatRoom*   hippo_data_cache_lookup_chat_room       (HippoDataCache  *cache,
                                                          const char      *chat_id,
                                                          HippoChatKind   *kind_p);
HippoChatRoom*   hippo_data_cache_ensure_chat_room       (HippoDataCache  *cache,
                                                          const char      *chat_id,
                                                          HippoChatKind    kind);

gboolean         hippo_data_cache_get_music_sharing_enabled (HippoDataCache   *cache);
gboolean         hippo_data_cache_get_application_usage_enabled (HippoDataCache *cache);
gboolean         hippo_data_cache_get_need_priming_music    (HippoDataCache   *cache);

/* CAN RETURN NULL if we aren't connected */
HippoPerson*     hippo_data_cache_get_self                  (HippoDataCache   *cache);

const HippoClientInfo* hippo_data_cache_get_client_info     (HippoDataCache   *cache);

const char *     hippo_data_cache_match_application_title   (HippoDataCache   *cache,
                                                             const char       *title);

DDMDataModel*    hippo_data_cache_get_model                 (HippoDataCache   *cache);
HippoDataCache*  hippo_data_model_get_data_cache            (DDMDataModel     *model);

G_END_DECLS

#endif /* __HIPPO_DATA_CACHE_H__ */
