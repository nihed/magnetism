/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_QNAME_H__
#define __DDM_QNAME_H__

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#include <glib.h>

G_BEGIN_DECLS

typedef struct _DDMQName DDMQName;

struct _DDMQName {
    const char *uri;
    const char *name;
};

DDMQName *ddm_qname_get(const char *uri,
                        const char *name);

/* Create a QName from a representation in <uri>#<name> form */
DDMQName *ddm_qname_from_uri(const char *full_uri);

G_END_DECLS

#endif /* __DDM_QNAME_H__ */
