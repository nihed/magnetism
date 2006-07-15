#ifndef __HIPPO_COMMON_INTERNAL_H__
#define __HIPPO_COMMON_INTERNAL_H__

#include <glib-object.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

/* we don't do i18n on Windows yet */
#ifndef G_OS_WIN32
#include <config.h>
#endif
#ifdef GETTEXT_PACKAGE
#include <glib/gi18n-lib.h>
#else
#define _(x) x
#endif

G_END_DECLS

#endif /* __HIPPO_COMMON_INTERNAL_H__ */
