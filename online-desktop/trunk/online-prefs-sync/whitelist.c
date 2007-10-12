/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include "whitelist.h"

typedef struct
{
    char *key;
    KeyScope scope;
    /* whitelist only the exact key, or anything with the key as prefix */
    guint exact_match_only : 1;

} WhitelistEntry;

static WhitelistEntry*
entry_new(const char *key,
          KeyScope    scope,
          gboolean    exact_match_only)
{
    WhitelistEntry *entry;

    entry = g_new0(WhitelistEntry, 1);
    entry->key = g_strdup(key);
    entry->scope = scope;
    entry->exact_match_only = exact_match_only != FALSE;

    return entry;
}

static void
entry_free(WhitelistEntry *entry)
{

    g_free(entry->key);
    g_free(entry);
}

static GSList *entries = NULL;

static void
scan_directory(const char *dirname)
{
    GDir *dir;
    GError *error;
    const char *filename;
    
    error = NULL;
    dir = g_dir_open(dirname, 0, &error);
    if (dir == NULL) {
        g_debug("Failed to open '%s': %s", dirname, error->message);
        g_error_free(error);
        return;
    }

    while ((filename = g_dir_read_name(dir))) {
        
        /* FIXME */
        
    }
    
    g_dir_close(dir);
}

static void
scan_all_directories(void)
{
    if (entries == NULL) {
        static const char *hardcoded_test_entries[] = {
            "/apps/metacity",
            "/desktop/gnome/applications",
            "/desktop/gnome/background",
            "/desktop/gnome/interface",
            "/desktop/gnome/url-handlers"
        };
        int i;
        for (i = 0; i < (int) G_N_ELEMENTS(hardcoded_test_entries); ++i) {
            entries = g_slist_prepend(entries,
                                      entry_new(hardcoded_test_entries[i],
                                                KEY_SCOPE_SAVED_PER_USER,
                                                FALSE));
        }
    }
    
    /* FIXME scan XDG_DATA_DIRS */
    
    /* scan_directory(CONFIG_FILES_DIR); */
}

static WhitelistEntry*
find_entry_for_key(const char *gconf_key)
{
    GSList *tmp;
    
    scan_all_directories();
    
    for (tmp = entries; tmp != NULL; tmp = tmp->next) {
        WhitelistEntry *entry = tmp->data;

        if (entry->exact_match_only && strcmp(gconf_key, entry->key) == 0)
            return entry;
        else if (g_str_has_prefix(gconf_key, entry->key))
            return entry;
    }

    return NULL;
}

KeyScope
whitelist_get_key_scope(const char *gconf_key)
{
    WhitelistEntry *entry;

    entry = find_entry_for_key(gconf_key);
    if (entry != NULL)
        return entry->scope;
    else
        return KEY_SCOPE_NOT_SAVED_REMOTELY;
}
