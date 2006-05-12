/* Repo of bad hacks to make gtk cut-and-pastage build outside of gtk tree. 
 * This can all die when new gtk comes out.
 */
#ifndef GTK_SIMULATED_H
#define GTK_SIMULATED_H

#include <gtk/gtk.h>

#define GTK_PARAM_READABLE G_PARAM_READABLE|G_PARAM_STATIC_NAME|G_PARAM_STATIC_NICK|G_PARAM_STATIC_BLURB
#define GTK_PARAM_WRITABLE G_PARAM_WRITABLE|G_PARAM_STATIC_NAME|G_PARAM_STATIC_NICK|G_PARAM_STATIC_BLURB
#define GTK_PARAM_READWRITE G_PARAM_READWRITE|G_PARAM_STATIC_NAME|G_PARAM_STATIC_NICK|G_PARAM_STATIC_BLURB

#define GETTEXT_PACKAGE "gtk20"

#include <glib/gi18n-lib.h>

#ifdef ENABLE_NLS
#define P_(String) dgettext(GETTEXT_PACKAGE "-properties",String)
#else
#define P_(String) (String)
#endif

/* not really I18N-related, but also a string marker macro */
#define I_(string) g_intern_static_string (string)

#endif /* GTK_SIMULATED_H */
