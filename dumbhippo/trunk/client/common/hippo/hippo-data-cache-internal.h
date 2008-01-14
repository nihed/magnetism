/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_CACHE_INTERNAL_H__
#define __HIPPO_DATA_CACHE_INTERNAL_H__

/*
 *
 * Methods here are for updating the data cache, used by HippoConnection.
 * This avoids a bunch of extra signals on HippoConnection that would
 * only be used by the data cache to see new info.
 *
 * The public API to HippoDataCache is generally "read only" since changes
 * are made asynchronously by sending them to the server, then being notified
 * that the change has taken effect.
 */

#include <hippo/hippo-data-cache.h>

G_BEGIN_DECLS

HippoChatRoom*   hippo_data_cache_ensure_chat_room       (HippoDataCache  *cache,
                                                          const char      *chat_id,
                                                          HippoChatKind    kind);

void             hippo_data_cache_set_music_sharing_enabled (HippoDataCache  *cache,
                                                             gboolean         enabled);
void             hippo_data_cache_set_music_sharing_primed  (HippoDataCache  *cache,
                                                             gboolean         primed);
void             hippo_data_cache_set_application_usage_enabled (HippoDataCache *cache,
                                                                 gboolean        enabled);

void             hippo_data_cache_set_client_info           (HippoDataCache        *cache,
                                                             const HippoClientInfo *info);

/* this takes ownership of the list members and the list itself */
void             hippo_data_cache_set_title_patterns        (HippoDataCache *cache,
                                                             GSList         *title_patterns);

G_END_DECLS

#endif /* __HIPPO_DATA_CACHE_INTERNAL_H__ */
