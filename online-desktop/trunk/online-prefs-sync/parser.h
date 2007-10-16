/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __PARSER_H__
#define __PARSER_H__

#include <glib.h>
#include "whitelist.h"

G_BEGIN_DECLS

typedef enum {
    ENTRY_PRIORITY_LOWEST, /* for stuff we ship with online-prefs-sync since apps don't ship anything yet */
    ENTRY_PRIORITY_PROVIDED_BY_APP, /* app upstream provided */
    ENTRY_PRIORITY_PROVIDED_BY_OS_VENDOR, /* distribution provided override of app upstream */
    ENTRY_PRIORITY_PROVIDED_BY_LOCAL_SITE, /* provided by local admin to override OS vendor */
    ENTRY_PRIORITY_PROVIDED_BY_USER,  /* user-provided override */
    ENTRY_PRIORITY_PROVIDED_BY_LOCAL_SITE_AND_LOCKED_DOWN /* provided by local admin to override user */
} EntryPriority;

typedef struct {
    char *key;
    guint scope : 8; /* KeyScope */
    guint priority : 8; /* EntryPriority */
    /* whitelist only the exact key, or anything with the key as prefix */
    guint exact_match_only : 1;
} ParsedEntry;

gboolean parse_entries(const char    *filename,
                       ParsedEntry ***entries,
                       int           *entries_len);

void parsed_entry_free(ParsedEntry *entry);

G_END_DECLS

#endif /* __PARSER_H__ */
