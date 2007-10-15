/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __WHITELIST_H__
#define __WHITELIST_H__

#include <glib.h>
#include <gconf/gconf-client.h>

G_BEGIN_DECLS

typedef enum
{
    KEY_SCOPE_NOT_SAVED_REMOTELY,
    KEY_SCOPE_SAVED_PER_MACHINE,
    KEY_SCOPE_SAVED_PER_USER
} KeyScope;

KeyScope whitelist_get_key_scope(const char *gconf_key);

GSList*  whitelist_get_gconf_entries_set_locally(GConfClient *client);

G_END_DECLS

#endif /* __WHITELIST_H__ */
