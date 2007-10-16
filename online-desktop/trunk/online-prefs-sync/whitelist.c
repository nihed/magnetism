/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include "whitelist.h"
#include "parser.h"

static GHashTable *exact_match_entries;
static GHashTable *any_subkey_entries;

static void
scan_directory(const char *dirname)
{
    GDir *dir;
    GError *error;
    const char *filename;

    g_debug("Scanning directory '%s'", dirname);
    
    error = NULL;
    dir = g_dir_open(dirname, 0, &error);
    if (dir == NULL) {
        g_debug("Failed to open '%s': %s", dirname, error->message);
        g_error_free(error);
        return;
    }

    while ((filename = g_dir_read_name(dir))) {
        ParsedEntry **parsed;
        char *full_path;
        int n_parsed;
        int i;
        
        if (!g_str_has_suffix(filename, ".synclist")) {
            g_debug("Ignoring wrong-suffixed file '%s'", filename);
            continue;
        }

        full_path = g_build_filename(dirname, filename, NULL);
        
        if (!parse_entries(full_path, &parsed, &n_parsed)) {
            g_free(full_path);
            continue;
        }
        g_free(full_path);

        for (i = 0; i < n_parsed; ++i) {
            ParsedEntry *existing;
            GHashTable *table;
            
            if (parsed[i]->exact_match_only)
                table = exact_match_entries;
            else
                table = any_subkey_entries;
            
            existing = g_hash_table_lookup(table, parsed[i]->key);

            if (existing) {
                /* note >=, i.e. same priority but later in file overrides */
                if (parsed[i]->priority >= existing->priority) {
                    existing->scope = parsed[i]->scope;
                    existing->priority = parsed[i]->priority;
                }
                parsed_entry_free(parsed[i]);
            } else {
                g_hash_table_replace(table, parsed[i]->key, parsed[i]);
            }
        }

        g_free(parsed);
    }
    
    g_dir_close(dir);
}

static void
scan_all_directories(void)
{
    const char* const * data_dirs;
    int i;
    
    if (exact_match_entries != NULL)
        return;

    exact_match_entries = g_hash_table_new(g_str_hash, g_str_equal);
    any_subkey_entries = g_hash_table_new(g_str_hash, g_str_equal);

    /* first try hardcoded location */
    scan_directory(SYNCLIST_FILES_DIR);

    /* override it with anything in XDG_DATA_DIRS (if higher or same priority) */
    data_dirs = g_get_system_data_dirs();
    for (i = 0; data_dirs[i] != NULL; ++i) {
        char *dirname;
        dirname = g_build_filename(data_dirs[i], "online-prefs-sync", NULL);
        scan_directory(dirname);
        g_free(dirname);
    }
}

/* from g_path_get_dirname() */
static char*
parent_gconf_key (const gchar *key)
{
     char *base;
     int len;    
  
     base = strrchr (key, '/');
     
     if (!base) {
         return NULL;
     }
     
     while (base > key && *base == '/')
         base--;
     
     len = (1 + base - key);
     
     base = g_new (char, len + 1);
     g_memmove (base, key, len);
     base[len] = 0;
     
     return base;
}

static ParsedEntry*
find_entry_for_key(const char *gconf_key)
{
    ParsedEntry *entry;
    
    scan_all_directories();

    /* exact matches override the wildcard matches */
    entry = g_hash_table_lookup(exact_match_entries, gconf_key);
    /* it would be nicer to just drop all "not saved" after parsing all config */
    if (entry && entry->scope == KEY_SCOPE_NOT_SAVED_REMOTELY)
        entry = NULL;
    
    /* Now look for each parent in the wildcard list; note that more-specific
     * (deeper) matches "win" since we start from the bottom.
     */
    if (entry == NULL) {        
        char *parent;
        
        parent = g_strdup(gconf_key); /* dup to avoid special-case */
        
        while (parent != NULL) {
            entry = g_hash_table_lookup(any_subkey_entries, parent);
            if (entry && entry->scope == KEY_SCOPE_NOT_SAVED_REMOTELY)
                entry = NULL;            
            
            if (entry != NULL) {
                g_free(parent);
                break;
            } else {
                char *old;
                old = parent;
                parent = parent_gconf_key(old);
                g_free(old);
            }
        }
    }

    return entry;
}

KeyScope
whitelist_get_key_scope(const char *gconf_key)
{
    ParsedEntry *entry;

    entry = find_entry_for_key(gconf_key);
    if (entry != NULL)
        return entry->scope;
    else
        return KEY_SCOPE_NOT_SAVED_REMOTELY;
}

static GSList*
read_entries(GConfClient    *client,
             const char     *key,
             gboolean        exact_match_only)
{    
    if (exact_match_only) {
        GConfEntry *gconf_entry;
        
        gconf_entry = gconf_client_get_entry(client, key, NULL, FALSE /* don't want default */, NULL);
        if (gconf_entry) {
            if (!gconf_entry_get_is_default(gconf_entry) && gconf_entry->value) {
                return g_slist_prepend(NULL, gconf_entry);
            } else {
                gconf_entry_unref(gconf_entry);
                return NULL;
            }
        } else {
            return NULL;
        }
    } else {
        GSList *result;
        GSList *gconf_entries;
        GSList *gconf_subdirs;
        GSList *l;

        result = NULL;
        gconf_entries = gconf_client_all_entries(client, key, NULL);
        gconf_subdirs = gconf_client_all_dirs(client, key, NULL);
        
        for (l = gconf_entries; l != NULL; l = l->next) {
            GConfEntry *gconf_entry = l->data;
            if (!gconf_entry_get_is_default(gconf_entry) && gconf_entry->value) {
                result = g_slist_prepend(result, gconf_entry);
            } else {
                gconf_entry_unref(gconf_entry);
            }
        }
        g_slist_free(gconf_entries);

        for (l = gconf_subdirs; l != NULL; l = l->next) {
            char *full_gconf_key = l->data;
            GSList *subdir_results;
            
            subdir_results = read_entries(client, full_gconf_key, FALSE);
            result = g_slist_concat(result, subdir_results);
            
            g_free(full_gconf_key);
        }
        g_slist_free(gconf_subdirs);

        return result;
    }
}            

typedef struct {
    GConfClient *client;
    GSList *result;
} ReadEntriesData;

static void
read_entries_foreach(void *key,
                     void *value,
                     void *data)
{
    ReadEntriesData *red = data;
    ParsedEntry *entry = value;
    GSList *gconf_entries;

    /* would be cleaner to drop these "not saved" entries from the hash after loading config */
    if (entry->scope == KEY_SCOPE_NOT_SAVED_REMOTELY)
        return;
    
    gconf_entries = read_entries(red->client, entry->key, entry->exact_match_only);
    red->result = g_slist_concat(red->result, gconf_entries);
}

GSList*
whitelist_get_gconf_entries_set_locally(GConfClient *client)
{
    ReadEntriesData red;

    red.client = client;
    red.result = NULL;

    scan_all_directories();

    /* it might be mildy more efficient to do any_subkey_entries first
     * if the two hash tables overlap, since it does batch reads of
     * whole directories
     */
    g_hash_table_foreach(any_subkey_entries, read_entries_foreach, &red);
    g_hash_table_foreach(exact_match_entries, read_entries_foreach, &red);
    
    return red.result;
}
