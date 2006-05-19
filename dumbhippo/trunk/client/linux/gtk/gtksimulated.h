/* Repo of bad hacks to make gtk cut-and-pastage build outside of gtk tree. 
 * This can all die when new gtk comes out.
 */
#ifndef GTK_SIMULATED_H
#define GTK_SIMULATED_H

/* try to avoid gtk/gtk.h since it's likely to conflict with us */
#include <gtk/gtkwidget.h>
#include <gtk/gtktooltips.h>
#include <gtk/gtkiconfactory.h>
#include <config.h>
#include <hippo/hippo-gtk-marshal.h>
/* put this after the system gtk headers to avoid conflicts */
#include "hippo-gtk-renamings.h"

#define _gtk_marshal_VOID__UINT_UINT hippo_gtk_marshal_VOID__UINT_UINT
#define _gtk_marshal_BOOLEAN__INT hippo_gtk_marshal_BOOLEAN__INT

#if (GTK_MINOR_VERSION < 8)
#define G_PARAM_STATIC_NICK 0
#define G_PARAM_STATIC_BLURB 0
#define G_PARAM_STATIC_NAME 0
#define I_(string) string
#define g_object_ref_sink(o) do { gtk_object_ref(GTK_OBJECT(o)); gtk_object_sink(GTK_OBJECT(o)); } while(0)
#else
/* not really I18N-related, but also a string marker macro */
#define I_(string) g_intern_static_string (string)
#endif

#ifndef GTK_PARAM_READABLE
#define GTK_PARAM_READABLE G_PARAM_READABLE|G_PARAM_STATIC_NAME|G_PARAM_STATIC_NICK|G_PARAM_STATIC_BLURB
#define GTK_PARAM_WRITABLE G_PARAM_WRITABLE|G_PARAM_STATIC_NAME|G_PARAM_STATIC_NICK|G_PARAM_STATIC_BLURB
#define GTK_PARAM_READWRITE G_PARAM_READWRITE|G_PARAM_STATIC_NAME|G_PARAM_STATIC_NICK|G_PARAM_STATIC_BLURB
#endif

#include <glib/gi18n-lib.h>

#ifndef P_
#ifdef ENABLE_NLS
#define P_(String) dgettext(GETTEXT_PACKAGE "-properties",String)
#else
#define P_(String) (String)
#endif
#endif

#endif /* GTK_SIMULATED_H */
