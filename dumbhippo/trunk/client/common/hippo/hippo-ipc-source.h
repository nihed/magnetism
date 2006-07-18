#ifndef __HIPPO_IPC_SOURCE_H__
#define __HIPPO_IPC_SOURCE_H__

#include <glib-object.h>

G_BEGIN_DECLS

#include "hippo-data-cache.h"

typedef struct _HippoIpcSource HippoIpcSource;
typedef struct _HippoIpcSourceClass HippoIpcSourceClass;

#define HIPPO_TYPE_IPC_SOURCE              (hippo_ipc_source_get_type ())
#define HIPPO_IPC_SOURCE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_IPC_SOURCE, HippoIpcSource))
#define HIPPO_IPC_SOURCE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_IPC_SOURCE, HippoIpcSourceClass))
#define HIPPO_IS_IPC_SOURCE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_IPC_SOURCE))
#define HIPPO_IS_IPC_SOURCE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_IPC_SOURCE))
#define HIPPO_IPC_SOURCE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_IPC_SOURCE, HippoIpcSourceClass))

GType hippo_ipc_source_get_type (void) G_GNUC_CONST;

HippoIpcSource *hippo_ipc_source_new (HippoDataCache *data_cache);

guint64 hippo_ipc_source_get_id            (HippoIpcSource *source);
void    hippo_ipc_source_connect           (HippoIpcSource *source);
void    hippo_ipc_source_disconnect        (HippoIpcSource *source);
void    hippo_ipc_source_join_chat_room    (HippoIpcSource *source,
					    const char     *chat_id,
					    HippoChatState  state);
void    hippo_ipc_source_leave_chat_room   (HippoIpcSource *source,
					    const char     *chat_id);

G_END_DECLS

#endif /* __HIPPO_IPC_SOURCE_H__ */

