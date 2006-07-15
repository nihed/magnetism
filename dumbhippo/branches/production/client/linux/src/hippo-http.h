#ifndef __HIPPO_HTTP_H__
#define __HIPPO_HTTP_H__

#include <glib-object.h>

G_BEGIN_DECLS

/* if content_type == NULL then we failed to get any content */
typedef void (* HippoHttpFunc) (const char *content_type, GString *content_or_error, void *data);

/* This does no caching or anything clever */
void hippo_http_get(const char   *url,
                    HippoHttpFunc func,
                    void         *data);

G_END_DECLS

#endif /* __HIPPO_HTTP_H__ */
