/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_QNAME_H__
#define __HIPPO_QNAME_H__

#include <glib.h>

G_BEGIN_DECLS

typedef struct _HippoQName HippoQName;

struct _HippoQName {
    const char *uri;
    const char *name;
};

HippoQName *hippo_qname_get(const char *uri,
                            const char *name);

G_END_DECLS

#endif /* __HIPPO_QNAME_H__ */
