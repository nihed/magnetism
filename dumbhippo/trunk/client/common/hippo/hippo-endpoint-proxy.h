#ifndef __HIPPO_ENDPOINT_PROXY_H__
#define __HIPPO_ENDPOINT_PROXY_H__

#include <glib-object.h>

G_BEGIN_DECLS

#include "hippo-data-cache.h"

typedef struct _HippoEndpointProxy HippoEndpointProxy;
typedef struct _HippoEndpointProxyClass HippoEndpointProxyClass;

#define HIPPO_TYPE_ENDPOINT_PROXY              (hippo_endpoint_proxy_get_type ())
#define HIPPO_ENDPOINT_PROXY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_ENDPOINT_PROXY, HippoEndpointProxy))
#define HIPPO_ENDPOINT_PROXY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_ENDPOINT_PROXY, HippoEndpointProxyClass))
#define HIPPO_IS_ENDPOINT_PROXY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_ENDPOINT_PROXY))
#define HIPPO_IS_ENDPOINT_PROXY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_ENDPOINT_PROXY))
#define HIPPO_ENDPOINT_PROXY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_ENDPOINT_PROXY, HippoEndpointProxyClass))

GType hippo_endpoint_proxy_get_type (void) G_GNUC_CONST;

HippoEndpointProxy *hippo_endpoint_proxy_new (HippoDataCache *data_cache);

guint64 hippo_endpoint_proxy_get_id          (HippoEndpointProxy *proxy);
void    hippo_endpoint_proxy_unregister      (HippoEndpointProxy *proxy);
void    hippo_endpoint_proxy_join_chat_room  (HippoEndpointProxy *proxy,
					      const char         *chat_id,
					      HippoChatState      state);
void    hippo_endpoint_proxy_leave_chat_room (HippoEndpointProxy *proxy,
					      const char         *chat_id);

G_END_DECLS

#endif /* __HIPPO_ENDPOINT_PROXY_H__ */

