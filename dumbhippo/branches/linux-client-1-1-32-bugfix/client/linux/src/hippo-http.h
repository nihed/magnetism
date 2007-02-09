/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_HTTP_H__
#define __HIPPO_HTTP_H__

#include <glib-object.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

/* This does no caching or anything clever */
void hippo_http_get(const char   *url,
                    HippoHttpFunc func,
                    void         *data);

G_END_DECLS

#endif /* __HIPPO_HTTP_H__ */
