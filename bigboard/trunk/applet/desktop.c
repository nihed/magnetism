/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include "desktop.h"
#include <string.h>
#include <libgnome/gnome-desktop-item.h>
#include <libgnomevfs/gnome-vfs.h>

static GSList*
get_xdg_data_dirs(void)
{
    const char *env;
    GSList *list;
    char **dirs;
    
    list = NULL;
    
    env = g_getenv("XDG_DATA_DIRS");

    if (env) {
        int i;
        dirs = g_strsplit(env, ":", -1);
        for (i = 0; dirs[i] != NULL; ++i) {
            list = g_slist_append(list, dirs[i]);
            /* steal the memory */
            dirs[i] = NULL;
        }
        g_free(dirs); /* no g_strfreev since we took the individual strings out */
    }

    list = g_slist_append(list, g_strdup(DATADIR));
    /* not sure I'm supposed to do this */
    list = g_slist_append(list, g_strdup("/usr/share"));

    return list;
}

static GnomeDesktopItem*
find_desktop_item(const char *desktop_name)
{
    GnomeDesktopItem *result;
    GSList *dirs;
    GSList *l;

    result = NULL;
    
    dirs = get_xdg_data_dirs();

    for (l = dirs; l != NULL; l = l->next) {
        const char *dir;
        char *path;
        char *filename;

        dir = l->data;
        filename = g_strconcat(desktop_name, ".desktop", NULL);

        path = g_build_filename(dir, "applications", filename, NULL);
        
        if (g_file_test(path, G_FILE_TEST_EXISTS)) {
            result = gnome_desktop_item_new_from_file(path,
                                                      GNOME_DESKTOP_ITEM_LOAD_ONLY_IF_EXISTS | GNOME_DESKTOP_ITEM_LOAD_NO_TRANSLATIONS, NULL);
        }

        g_free(filename);
        g_free(path);
        
        if (result != NULL)
            goto out;
    }
    
 out:
    g_slist_foreach(dirs, (GFunc) g_free, NULL);
    g_slist_free(dirs);

    return result;
}

gboolean
desktop_launch(GdkScreen  *screen,
               const char *desktop_name,
               GError    **error)
{
    GnomeDesktopItem *item;
    int result;

    gnome_vfs_init(); /* gnome desktop item uses it internally */
    
    item = find_desktop_item(desktop_name);
    if (item == NULL) {
        g_set_error(error, G_FILE_ERROR,
                    G_FILE_ERROR_FAILED,
                    "Could not find the file %s.desktop needed to start this application",
                    desktop_name);
        return FALSE;
    }
    
    result = gnome_desktop_item_launch_on_screen(item,
                                                 NULL, 0,
                                                 screen, -1,
                                                 error);

    gnome_desktop_item_unref(item);
    
    if (result < 0) {
        return FALSE;
    } else {
        return TRUE;
    }                   
}

gboolean
desktop_launch_list(GdkScreen  *screen,
                    const char *desktop_names,
                    GError    **error)
{
    GError *first_error;
    char **names;
    int i;
    gboolean succeeded;

    succeeded = FALSE;
    first_error = NULL;

    names = g_strsplit(desktop_names, ";", -1);
    
    for (i = 0; names[i] != NULL; ++i) {
        if (desktop_launch(screen, names[i], first_error ? NULL : &first_error)) {
            succeeded = TRUE;
            break;
        }
    }

    g_strfreev(names);
    
    if (succeeded && first_error)
        g_error_free(first_error);
    if (!succeeded) {
        if (first_error)
            g_propagate_error(error, first_error);
        else
            g_set_error(error, G_FILE_ERROR,
                        G_FILE_ERROR_FAILED,
                        "Sadly, the computer doesn't know how to launch this application");
    }

    return succeeded;
}

