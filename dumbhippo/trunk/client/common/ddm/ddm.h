/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_H__
#define __DDM_H__

#ifndef DDM_COMPILATION
#ifndef DDM_I_KNOW_THIS_IS_UNSTABLE
#error "This library is in development and does not yet have a stable API or ABI. Use at your own risk."
#endif /* DDM_I_KNOW_THIS_IS_UNSTABLE */
#endif /* DDM_COMPILATION */

#include <glib-object.h>

G_BEGIN_DECLS

#define DDM_INSIDE_DDM_H 1

#include <ddm/ddm-client.h>
#include <ddm/ddm-data-fetch.h>
#include <ddm/ddm-data-model.h>
#include <ddm/ddm-data-model-backend.h>
#include <ddm/ddm-data-query.h>
#include <ddm/ddm-data-resource.h>
#include <ddm/ddm-notification-set.h>
#include <ddm/ddm-qname.h>

#undef DDM_INSIDE_DDM_H

G_END_DECLS

#endif /* __DDM_H__ */
