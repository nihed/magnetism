/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_EXTERNAL_ACCOUNT_H__
#define __HIPPO_EXTERNAL_ACCOUNT_H__

#include <hippo/hippo-entity.h>
#include <loudmouth/loudmouth.h>

G_BEGIN_DECLS

typedef struct _HippoExternalAccount      HippoExternalAccount;
typedef struct _HippoExternalAccountClass HippoExternalAccountClass;

#define HIPPO_TYPE_EXTERNAL_ACCOUNT              (hippo_external_account_get_type ())
#define HIPPO_EXTERNAL_ACCOUNT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_EXTERNAL_ACCOUNT, HippoExternalAccount))
#define HIPPO_EXTERNAL_ACCOUNT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_EXTERNAL_ACCOUNT, HippoExternalAccountClass))
#define HIPPO_IS_EXTERNAL_ACCOUNT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_EXTERNAL_ACCOUNT))
#define HIPPO_IS_EXTERNAL_ACCOUNT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_EXTERNAL_ACCOUNT))
#define HIPPO_EXTERNAL_ACCOUNT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_EXTERNAL_ACCOUNT, HippoExternalAccountClass))

GType hippo_external_account_get_type(void) G_GNUC_CONST;

HippoExternalAccount* hippo_external_account_new_from_xml   (HippoDataCache *cache,
                                                             LmMessageNode  *node);

G_END_DECLS

#endif /* __HIPPO_EXTERNAL_ACCOUNT_H__ */
