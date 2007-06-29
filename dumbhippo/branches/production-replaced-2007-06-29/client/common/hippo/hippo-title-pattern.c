/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include "hippo-title-pattern.h"
#ifdef HAVE_PCRE
#  include <pcre.h>
#  include <string.h>
#endif 

struct HippoTitlePattern {
    char *app_id;
    char *pattern;
#ifdef HAVE_PCRE
    pcre *compiled;
    gboolean compile_error;
#endif
};

HippoTitlePattern *
hippo_title_pattern_new(const char *app_id,
                        const char *pattern_string)
{
    HippoTitlePattern *pattern;

    g_return_val_if_fail(app_id != NULL, NULL);
    g_return_val_if_fail(pattern_string != NULL, NULL);

    pattern = g_new0(HippoTitlePattern, 1);
    pattern->app_id = g_strdup(app_id);
    pattern->pattern = g_strdup(pattern_string);

    return pattern;
}

void
hippo_title_pattern_free(HippoTitlePattern *pattern)
{
    g_return_if_fail(pattern != NULL);

    g_free(pattern->app_id);
    g_free(pattern->pattern);

#ifdef HAVE_PCRE
    if (pattern->compiled)
        pcre_free(pattern->compiled);
#endif

    g_free(pattern);
}

gboolean
hippo_title_pattern_matches(HippoTitlePattern *pattern,
                            const char        *title)
{
#ifdef HAVE_PCRE
    /* We actually only need a two-element output vector, but using a bigger
     * one is a performance win if someone uses backreferences in a title pattern...
     * (right, that's going to happen...)
     */
    int ovector[30];
    int title_len;
#endif
    
    g_return_val_if_fail(pattern != NULL, FALSE);
    g_return_val_if_fail(title != NULL, FALSE);

#ifdef HAVE_PCRE
    if (pattern->compile_error)
        return FALSE;
    
    if (!pattern->compiled) {
        const char *err;
        int err_offset;
        pattern->compiled = pcre_compile(pattern->pattern, 0, &err, &err_offset, NULL);
        if (!pattern->compiled) {
            g_warning("Problem compiling title pattern '%s' at position %d:\n   %s\n",
                      pattern->pattern, err_offset, err);
            pattern->compile_error = TRUE;
            return FALSE;
        }
    }

    title_len = strlen(title);
    if (pcre_exec(pattern->compiled, NULL,
                  title, title_len, 0,
                  PCRE_ANCHORED,
                  ovector, G_N_ELEMENTS(ovector)) >= 0) {
        if (ovector[0] == 0 && ovector[1] == title_len)
            return TRUE;
    }
    
    return FALSE;
#else
    g_warning("hippo_title_pattern_matches(), but Mugshot wasn't compiled with PCRE support");
    return FALSE;
#endif
}

const char *
hippo_title_pattern_get_app_id (HippoTitlePattern *pattern)
{
    return pattern->app_id;
}
