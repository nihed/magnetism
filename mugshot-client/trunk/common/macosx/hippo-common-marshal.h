
#ifndef __hippo_common_marshal_MARSHAL_H__
#define __hippo_common_marshal_MARSHAL_H__

#include	<glib-object.h>

G_BEGIN_DECLS

/* VOID:OBJECT,OBJECT,STRING (../hippo/hippo-common-marshal.list:1) */
extern void hippo_common_marshal_VOID__OBJECT_OBJECT_STRING (GClosure     *closure,
                                                             GValue       *return_value,
                                                             guint         n_param_values,
                                                             const GValue *param_values,
                                                             gpointer      invocation_hint,
                                                             gpointer      marshal_data);

G_END_DECLS

#endif /* __hippo_common_marshal_MARSHAL_H__ */

