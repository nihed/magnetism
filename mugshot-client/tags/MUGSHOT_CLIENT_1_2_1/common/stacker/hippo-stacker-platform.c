/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-platform.h"

static void hippo_stacker_platform_base_init(void *klass);

GType
hippo_stacker_platform_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info = 
        {
            sizeof(HippoStackerPlatformClass),
            hippo_stacker_platform_base_init,
            NULL /* base_finalize */
        };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoStackerPlatform",
                                      &info, 0);
    }
    
    return type;
}

static void
hippo_stacker_platform_base_init(void *klass)
{
    static gboolean initialized = FALSE;
    
    if (!initialized) {
        /* create signals in here */      
        
        initialized = TRUE;   
    }
}

HippoWindow*
hippo_stacker_platform_create_window(HippoStackerPlatform *platform)
{
    g_return_val_if_fail(HIPPO_IS_STACKER_PLATFORM(platform), NULL);
    
    return HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->create_window(platform);
}

void
hippo_stacker_platform_get_screen_info(HippoStackerPlatform *platform,
                                       HippoRectangle       *monitor_rect_p,
                                       HippoRectangle       *tray_icon_rect_p,
                                       HippoOrientation     *tray_icon_orientation_p)
{
    g_return_if_fail(HIPPO_IS_STACKER_PLATFORM(platform));
    
    HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->get_screen_info(platform,
                                                                monitor_rect_p,
                                                                tray_icon_rect_p,
                                                                tray_icon_orientation_p);
}

gboolean
hippo_stacker_platform_get_pointer_position(HippoStackerPlatform *platform,
                                            int                  *x_p,
                                            int                  *y_p)
{
    g_return_val_if_fail(HIPPO_IS_STACKER_PLATFORM(platform), FALSE);
    
    return HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->get_pointer_position(platform, x_p, y_p);
}

void
hippo_stacker_platform_http_request(HippoStackerPlatform   *platform,
                                    const char             *url,
                                    HippoHttpFunc           func,
                                    void                   *data)
{
    g_return_if_fail(HIPPO_IS_STACKER_PLATFORM(platform));

    HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->http_request(platform,
                                                     url,
                                                     func,
                                                     data);
}

gboolean
hippo_stacker_platform_can_play_song_download(HippoStackerPlatform *platform,
                                              HippoSongDownload    *song_download)
{
    g_return_val_if_fail(HIPPO_IS_STACKER_PLATFORM(platform), FALSE);

    return HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->can_play_song_download(platform, song_download);
}

void
hippo_stacker_platform_show_chat_window(HippoStackerPlatform *platform,
                                        const char           *chat_id)
{
    g_return_if_fail(HIPPO_IS_STACKER_PLATFORM(platform));

    HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->show_chat_window(platform,
                                                         chat_id);
}

HippoWindowState 
hippo_stacker_platform_get_chat_window_state (HippoStackerPlatform *platform,
                                              const char           *chat_id)
{
    g_return_val_if_fail(HIPPO_IS_STACKER_PLATFORM(platform), HIPPO_WINDOW_STATE_CLOSED);

    return HIPPO_STACKER_PLATFORM_GET_CLASS(platform)->get_chat_window_state(platform, chat_id);
}
