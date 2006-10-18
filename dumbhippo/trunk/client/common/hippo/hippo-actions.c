/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-actions.h"
#include "hippo-image-cache.h"
#include "hippo-stack-manager.h"
#include <string.h>


static void      hippo_actions_init                (HippoActions       *actions);
static void      hippo_actions_class_init          (HippoActionsClass  *klass);

static void      hippo_actions_dispose             (GObject            *object);
static void      hippo_actions_finalize            (GObject            *object);

struct _HippoActions {
    GObject parent;
    HippoDataCache *cache;

    /* We have an image cache for each kind of
     * image, because otherwise we can't really predict
     * cache behavior.
     */
    HippoImageCache *entity_photo_cache;

    guint minute_timeout_id;
};

struct _HippoActionsClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoActions, hippo_actions, G_TYPE_OBJECT);

enum {
    MINUTE_TICKED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static gboolean
minute_tick_timeout(void *data)
{
    HippoActions *actions;

    actions = HIPPO_ACTIONS(data);

    g_signal_emit(G_OBJECT(actions), signals[MINUTE_TICKED], 0);

    return TRUE;
}

static void
hippo_actions_init(HippoActions  *actions)
{
    /* This is so every canvas item displaying a time doesn't have to
     * install its own timeout
     */
    actions->minute_timeout_id = g_timeout_add(60 * 1000, minute_tick_timeout, actions);
}

static void
hippo_actions_class_init(HippoActionsClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = hippo_actions_dispose;
    object_class->finalize = hippo_actions_finalize;

    signals[MINUTE_TICKED] =
        g_signal_new ("minute-ticked",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

static void
hippo_actions_dispose(GObject *object)
{
    HippoActions *actions = HIPPO_ACTIONS(object);

    if (actions->minute_timeout_id) {
        g_source_remove(actions->minute_timeout_id);
        actions->minute_timeout_id = 0;
    }
    
    if (actions->cache) {
        g_object_unref(actions->cache);
        actions->cache = NULL;
    }

    if (actions->entity_photo_cache) {
        g_object_run_dispose(G_OBJECT(actions->entity_photo_cache));
        g_object_unref(actions->entity_photo_cache);
        actions->entity_photo_cache = NULL;
    }
    
    G_OBJECT_CLASS(hippo_actions_parent_class)->dispose(object);
}

static void
hippo_actions_finalize(GObject *object)
{
    /* HippoActions *actions = HIPPO_ACTIONS(object); */

    G_OBJECT_CLASS(hippo_actions_parent_class)->finalize(object);
}

HippoActions*
hippo_actions_new(HippoDataCache *cache)
{
    HippoActions *actions;

    actions = g_object_new(HIPPO_TYPE_ACTIONS,
                           NULL);

    g_object_ref(cache);
    actions->cache = cache;
    
    return actions;
}

static HippoConnection*
get_connection(HippoActions *actions)
{
    return hippo_data_cache_get_connection(actions->cache);
}

static HippoPlatform*
get_platform(HippoActions *actions)
{
    return hippo_connection_get_platform(get_connection(actions));
}

void
hippo_actions_visit_post(HippoActions   *actions,
                         HippoPost      *post)
{
    hippo_connection_visit_post(get_connection(actions), post);
}

void
hippo_actions_visit_entity(HippoActions    *actions,
                           HippoEntity     *entity)
{
    hippo_connection_visit_entity(get_connection(actions), entity);
}

static void
image_set_on_canvas_item_func(HippoSurface *surface,
                              void         *data)
{
    HippoCanvasItem *item = HIPPO_CANVAS_ITEM(data);

    if (surface != NULL) {
        g_object_set(G_OBJECT(item),
                     "image",
                     hippo_surface_get_surface(surface),
                     NULL);
    }

    /* this function held a ref */
    g_object_unref(item);
}

void
hippo_actions_load_entity_photo_async(HippoActions    *actions,
                                      HippoEntity     *entity,
                                      int              size,
                                      HippoCanvasItem *image_item)
{
    const char *url;
    char *sized;
    char *absolute;        
    
    url = hippo_entity_get_photo_url(entity);
    
    g_debug("Loading photo for entity '%s' url '%s' to a canvas item",
            hippo_entity_get_guid(entity),
            url ? url : "null");
    
    if (url == NULL) {
        /* not gonna succeed in loading this... */
        return;
    }

    sized = hippo_size_photo_url(url, size);

    if (actions->entity_photo_cache == NULL) {
        actions->entity_photo_cache = hippo_image_cache_new(get_platform(actions));
    }
    
    absolute = hippo_connection_make_absolute_url(get_connection(actions),
                                                  sized);
    g_object_ref(image_item); /* held by the loader func */
    hippo_image_cache_load(actions->entity_photo_cache, absolute,
                           image_set_on_canvas_item_func,
                           image_item);
    
    g_free(absolute);
    g_free(sized);
}

HippoEntity*
hippo_actions_lookup_entity(HippoActions    *actions,
                            const char      *entity_guid)
{
    return hippo_data_cache_lookup_entity(actions->cache, entity_guid);
}

gint64
hippo_actions_get_server_time_offset (HippoActions *actions)
{
    return hippo_connection_get_server_time_offset(get_connection(actions));
}

void
hippo_actions_close_browser(HippoActions    *actions)
{
    hippo_stack_manager_close_browser(actions->cache);
}

void
hippo_actions_close_notification(HippoActions    *actions)
{
    hippo_stack_manager_close_notification(actions->cache);
}

void
hippo_actions_hush_notification(HippoActions    *actions)
{
    hippo_stack_manager_hush(actions->cache);
}

void
hippo_actions_expand_notification(HippoActions *actions)
{
    hippo_stack_manager_close_notification(actions->cache);
    hippo_stack_manager_toggle_browser(actions->cache);
}

void
hippo_actions_open_home_page(HippoActions    *actions)
{
    hippo_connection_open_relative_url(get_connection(actions), "/");
}

void
hippo_actions_open_absolute_url(HippoActions    *actions,
                                const char      *url)
{
    hippo_platform_open_url(get_platform(actions),
                            HIPPO_BROWSER_UNKNOWN,
                            url);
}

void
hippo_actions_hush_block(HippoActions    *actions,
                         HippoBlock      *block)
{
    hippo_connection_set_block_hushed(get_connection(actions), hippo_block_get_guid(block), TRUE);
}

void
hippo_actions_unhush_block(HippoActions    *actions,
                           HippoBlock      *block)
{
    hippo_connection_set_block_hushed(get_connection(actions), hippo_block_get_guid(block), FALSE);
}

void
hippo_actions_add_to_faves(HippoActions    *actions,
                           HippoBlock      *block)
{
    /* FIXME */
}

void
hippo_actions_join_chat_id(HippoActions    *actions,
                           const char      *chat_id)
{
    hippo_platform_show_chat_window(get_platform(actions), chat_id);
}

void
hippo_actions_invite_to_group(HippoActions    *actions,
                              HippoGroup      *group,
                              HippoPerson     *person)
{
    hippo_connection_do_invite_to_group(get_connection(actions),
                                        hippo_entity_get_guid(HIPPO_ENTITY(group)),
                                        hippo_entity_get_guid(HIPPO_ENTITY(person)));
}

gboolean
hippo_actions_can_play_song_download(HippoActions      *actions,
                                     HippoSongDownload *song_download)
{
    return hippo_platform_can_play_song_download(get_platform(actions), song_download);
}

