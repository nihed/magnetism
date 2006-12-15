/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-quip-window.h"
#include "hippo-image-cache.h"
#include "hippo-stack-manager.h"
#include "hippo-window.h"
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-widgets.h>
#include <string.h>


static void      hippo_quip_window_init                (HippoQuipWindow       *quip_window);
static void      hippo_quip_window_class_init          (HippoQuipWindowClass  *klass);

static void      hippo_quip_window_dispose             (GObject            *object);
static void      hippo_quip_window_finalize            (GObject            *object);

struct _HippoQuipWindow {
    GObject parent;
    HippoDataCache *cache;

    HippoChatKind chat_kind;
    char *chat_id;

    char *title;
    HippoSentiment sentiment;
    
    HippoWindow *window;
    HippoCanvasBox *love_box;
    HippoCanvasBox *hate_box;
    HippoCanvasItem *entry;

    guint visible : 1;
    guint active : 1;
};

struct _HippoQuipWindowClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoQuipWindow, hippo_quip_window, G_TYPE_OBJECT);

static void hippo_quip_window_hide(HippoQuipWindow *quip_window);
static void on_notify_active(GObject         *object,
                             GParamSpec      *param_spec,
                             HippoQuipWindow *quip_window);

static void
hippo_quip_window_init(HippoQuipWindow  *quip_window)
{
    quip_window->sentiment = HIPPO_SENTIMENT_INDIFFERENT;
}

static void
hippo_quip_window_class_init(HippoQuipWindowClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = hippo_quip_window_dispose;
    object_class->finalize = hippo_quip_window_finalize;
}

static void
hippo_quip_window_dispose(GObject *object)
{
    HippoQuipWindow *quip_window = HIPPO_QUIP_WINDOW(object);

    hippo_quip_window_hide(quip_window);

    if (quip_window->window) {
        g_signal_handlers_disconnect_by_func(quip_window->window,
                                             (void *)on_notify_active,
                                             quip_window);
        
        g_object_unref(quip_window->window);
        quip_window->window = NULL;
    }

    G_OBJECT_CLASS(hippo_quip_window_parent_class)->dispose(object);
}

static void
hippo_quip_window_finalize(GObject *object)
{
     HippoQuipWindow *quip_window = HIPPO_QUIP_WINDOW(object);

    g_free(quip_window->title);
    quip_window->title = NULL;

    G_OBJECT_CLASS(hippo_quip_window_parent_class)->finalize(object);
}

static HippoConnection*
get_connection(HippoQuipWindow *quip_window)
{
    return hippo_data_cache_get_connection(quip_window->cache);
}

static HippoPlatform*
get_platform(HippoQuipWindow *quip_window)
{
    return hippo_connection_get_platform(get_connection(quip_window));
}

static void
on_love_activated(HippoCanvasItem       *item,
                  HippoQuipWindow       *quip_window)
{
    hippo_quip_window_set_sentiment(quip_window, HIPPO_SENTIMENT_LOVE);
}

static void
on_hate_activated(HippoCanvasItem       *item,
                  HippoQuipWindow       *quip_window)
{
    hippo_quip_window_set_sentiment(quip_window, HIPPO_SENTIMENT_HATE);
}

static void
send_quip(HippoQuipWindow *quip_window)
{
#if 0
    const char *text = NULL;
    switch (quip_window->sentiment) {
    case HIPPO_SENTIMENT_INDIFFERENT:
        text = "Who cares!";
        break;
    case HIPPO_SENTIMENT_LOVE:
        text = "It rulez!";
        break;
    case HIPPO_SENTIMENT_HATE:
        text = "It sucks!";
        break;
    }
#endif

    char *text = NULL;
    g_object_get(G_OBJECT(quip_window->entry), "text", &text, NULL);

    hippo_connection_send_quip(get_connection(quip_window),
                               quip_window->chat_kind, quip_window->chat_id,
                               text, quip_window->sentiment);
    g_free(text);
    
    hippo_quip_window_hide(quip_window);
}

static void
on_send_activated(HippoCanvasItem       *item,
                  HippoQuipWindow       *quip_window)
{
    send_quip(quip_window);
}

static void
on_close_activated(HippoCanvasItem       *item,
                   HippoQuipWindow       *quip_window)
{
    hippo_quip_window_hide(quip_window);
}

static void 
on_notify_active(GObject         *object,
                 GParamSpec      *param_spec,
                 HippoQuipWindow *quip_window)
{
    gboolean active;
    gboolean hide;

    g_object_get(object, "active", &active, NULL);

    /* We pop down the quip window when it loses focus, to properly handle
     * the user clicking/tabbing away elsewhere; this may be a little fragile
     * against window managers that don't keep focus stable; we'll see how
     * it works in practice.
     */
    hide = quip_window->active && !active;

    quip_window->active = active;

    if (hide)
        hippo_quip_window_hide(quip_window);
}

static gboolean
on_key_press_event(HippoCanvasItem *item,
                   HippoEvent      *event,
                   HippoQuipWindow *quip_window)
{
    switch (event->u.key.key) {
    case HIPPO_KEY_RETURN:
        send_quip(quip_window);
        return TRUE;
    case HIPPO_KEY_ESCAPE:
        hippo_quip_window_hide(quip_window);
        return TRUE;
    default:
        return FALSE;
    }
}

HippoQuipWindow*
hippo_quip_window_new(HippoDataCache *cache)
{
    HippoQuipWindow *quip_window;
    HippoCanvasBox *outer_box;
    HippoCanvasBox *top_box;
    HippoCanvasBox *bottom_box;
    HippoCanvasItem *item;

    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    quip_window = g_object_new(HIPPO_TYPE_QUIP_WINDOW,
                           NULL);

    g_object_ref(cache);
    quip_window->cache = cache;

    quip_window->window = hippo_platform_create_window(get_platform(quip_window));

    g_signal_connect(quip_window->window, "notify::active",
                     G_CALLBACK(on_notify_active),  quip_window);

    g_object_set(quip_window->window, "role", HIPPO_WINDOW_ROLE_INPUT_POPUP, NULL);    

    outer_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                             "box-width", 250,
                             "padding", 8,
                             "border", 1,
                             "background-color", 0xffffffff,
                             "border-color", 0x000000ff,
                             "spacing", 8,
                             NULL);
    hippo_window_set_contents(quip_window->window, HIPPO_CANVAS_ITEM(outer_box));
    
    top_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                           NULL);
    hippo_canvas_box_append(outer_box, HIPPO_CANVAS_ITEM(top_box), 0);
    
    quip_window->love_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                         "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                         "border", 1,
                                         "padding", 4,
                                         NULL);
    hippo_canvas_box_append(top_box, HIPPO_CANVAS_ITEM(quip_window->love_box), 0);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "quiplove_icon",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    HIPPO_CANVAS_BOX(item)->clickable = TRUE; /* Hack */
    hippo_canvas_box_append(quip_window->love_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_love_activated), quip_window);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I love it!",
                        NULL);
    hippo_canvas_box_append(quip_window->love_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_love_activated), quip_window);
    
    quip_window->hate_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                         "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                         "border", 1,
                                         "padding", 4,
                                         NULL);
    hippo_canvas_box_append(top_box, HIPPO_CANVAS_ITEM(quip_window->hate_box), 0);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "quiphate_icon",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    HIPPO_CANVAS_BOX(item)->clickable = TRUE; /* Hack */
    hippo_canvas_box_append(quip_window->hate_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", 
                     G_CALLBACK(on_hate_activated), quip_window);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I hate it!",
                        NULL);
    hippo_canvas_box_append(quip_window->hate_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_hate_activated), quip_window);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 8,
                        "text", "X",
                        NULL);
    hippo_canvas_box_append(top_box, item, HIPPO_PACK_END);
    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_close_activated), quip_window);
    
    bottom_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                              NULL);
    hippo_canvas_box_append(outer_box, HIPPO_CANVAS_ITEM(bottom_box), 0);

    quip_window->entry = hippo_canvas_entry_new();
    hippo_canvas_box_append(bottom_box, quip_window->entry, HIPPO_PACK_EXPAND);
    g_signal_connect(G_OBJECT(quip_window->entry), "key-press-event",
                     G_CALLBACK(on_key_press_event), quip_window);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 8,
                        "text", "Send",
                        NULL);
    hippo_canvas_box_append(bottom_box, item, HIPPO_PACK_END);
    g_signal_connect(G_OBJECT(item), "activated",
                     G_CALLBACK(on_send_activated), quip_window);
    
    return quip_window;
}

void 
hippo_quip_window_set_chat(HippoQuipWindow *quip_window,
                           HippoChatKind    chat_kind,
                           const char      *chat_id)
{
    g_return_if_fail(HIPPO_IS_QUIP_WINDOW(quip_window));
    
    quip_window->chat_kind = chat_kind;

    if (chat_id != quip_window->chat_id) {
        g_free(quip_window->chat_id);
        quip_window->chat_id = g_strdup(chat_id);
    }
}

void
hippo_quip_window_set_sentiment(HippoQuipWindow *quip_window,
                                HippoSentiment   sentiment)
{
    g_return_if_fail(HIPPO_IS_QUIP_WINDOW(quip_window));

    if (quip_window->sentiment == sentiment)
        return;
    
    quip_window->sentiment = sentiment;

    if (sentiment == HIPPO_SENTIMENT_LOVE)
        g_object_set(quip_window->love_box,
                     "background-color", 0xeeeeeeff,
                     "border-color",     0x000000ff,
                     NULL);
    else
        g_object_set(quip_window->love_box,
                     "background-color", 0x00000000,
                     "border-color",     0x00000000,
                     NULL);

    if (sentiment == HIPPO_SENTIMENT_HATE)
        g_object_set(quip_window->hate_box,
                     "background-color", 0xeeeeeeff,
                     "border-color",     0x000000ff,
                     NULL);
    else
        g_object_set(quip_window->hate_box,
                     "background-color", 0x00000000,
                     "border-color",     0x00000000,
                     NULL);

}

void
hippo_quip_window_set_title(HippoQuipWindow *quip_window,
                            const char      *title)
{
    g_return_if_fail(HIPPO_IS_QUIP_WINDOW(quip_window));

    if (title == quip_window->title)
        return;

    if (quip_window->title)
        g_free(quip_window->title);

    quip_window->title = g_strdup(title);
}

/* Offsets of pointer from upper right of window */
#define POINTER_X_OFFSET  30
#define POINTER_Y_OFFSET  -5

void 
hippo_quip_window_show(HippoQuipWindow *quip_window)
{
    HippoRectangle monitor_rect;
    int pointer_x, pointer_y;
    int window_width, window_height;
    int window_x, window_y;
    
    g_return_if_fail(HIPPO_IS_QUIP_WINDOW(quip_window));

    if (quip_window->visible)
        return;

    quip_window->visible = TRUE;
    g_object_ref(quip_window);

    hippo_platform_get_screen_info(get_platform(quip_window), &monitor_rect, NULL, NULL);
    
    if (!hippo_platform_get_pointer_position(get_platform(quip_window), &pointer_x, &pointer_y)) {
        /* Pointer on a different X screen, we'll just position at lower right */
        pointer_x = monitor_rect.x + monitor_rect.width;
        pointer_y = monitor_rect.y + monitor_rect.height;
    }

    hippo_window_get_size(quip_window->window, &window_width, &window_height);

    /* Try to position the window window so the pointer is near the upper left, but force it
     * within the workarea
     */
    window_x = pointer_x + POINTER_X_OFFSET - window_width;
    window_y = pointer_y + POINTER_Y_OFFSET;

    if (window_x + window_width > monitor_rect.x + monitor_rect.width)
        window_x = monitor_rect.x + monitor_rect.width - window_width;
    if (window_x < 0)
        window_x = 0;

    if (window_y + window_height > monitor_rect.y + monitor_rect.height)
        window_y = monitor_rect.y + monitor_rect.height - window_height;
    if (window_y < 0)
        window_y = 0;

    hippo_window_set_position(quip_window->window, window_x, window_y);

    hippo_window_present(quip_window->window);

}

static void
hippo_quip_window_hide(HippoQuipWindow *quip_window)
{
    if (!quip_window->visible)
        return;

    hippo_window_set_visible(quip_window->window, FALSE);
    
    quip_window->visible = FALSE;
    g_object_unref(quip_window);
}
