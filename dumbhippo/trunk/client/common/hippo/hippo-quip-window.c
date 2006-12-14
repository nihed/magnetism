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
};

struct _HippoQuipWindowClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoQuipWindow, hippo_quip_window, G_TYPE_OBJECT);

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

    if (quip_window->visible) {
        quip_window->visible = FALSE;
        g_object_unref(quip_window);
    }

    if (quip_window->window) {
        hippo_window_set_visible(quip_window->window, FALSE);
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
on_send_activated(HippoCanvasItem       *item,
                  HippoQuipWindow       *quip_window)
{
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

    hippo_connection_send_quip(get_connection(quip_window),
                               quip_window->chat_kind, quip_window->chat_id,
                               text, quip_window->sentiment);
    g_object_run_dispose(G_OBJECT(quip_window));
}

static void
on_close_activated(HippoCanvasItem       *item,
                   HippoQuipWindow       *quip_window)
{
    hippo_quip_window_set_sentiment(quip_window, HIPPO_SENTIMENT_HATE);
    g_object_run_dispose(G_OBJECT(quip_window));
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

    outer_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                             "box-width", 250,
                             "padding", 8,
                             "border", 1,
                             "background-color", 0xffffffff,
                             "border-color", 0x00000000,
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
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_love_activated), quip_window);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I love it!",
                        NULL);
    hippo_canvas_box_append(quip_window->love_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_love_activated), quip_window);
    
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
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hate_activated), quip_window);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I hate it!",
                        NULL);
    hippo_canvas_box_append(quip_window->hate_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hate_activated), quip_window);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 8,
                        "text", "X",
                        NULL);
    hippo_canvas_box_append(top_box, item, HIPPO_PACK_END);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_close_activated), quip_window);
    
    bottom_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                              NULL);
    hippo_canvas_box_append(outer_box, HIPPO_CANVAS_ITEM(bottom_box), 0);

    quip_window->entry = hippo_canvas_entry_new();
    hippo_canvas_box_append(bottom_box, quip_window->entry, HIPPO_PACK_EXPAND);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 8,
                        "text", "Send",
                        NULL);
    hippo_canvas_box_append(bottom_box, item, HIPPO_PACK_END);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_send_activated), quip_window);
    
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
                     "background-color", 0xaaaaaaff,
                     "border-color",     0xffffffff,
                     NULL);
    else
        g_object_set(quip_window->love_box,
                     "background-color", 0x00000000,
                     "border-color",     0x00000000,
                     NULL);

    if (sentiment == HIPPO_SENTIMENT_HATE)
        g_object_set(quip_window->hate_box,
                     "background-color", 0xaaaaaaff,
                     "border-color",     0xffffffff,
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

void hippo_quip_window_show(HippoQuipWindow *quip_window)
{
    g_return_if_fail(HIPPO_IS_QUIP_WINDOW(quip_window));

    if (quip_window->visible)
        return;

    quip_window->visible = TRUE;
    g_object_ref(quip_window);

    hippo_window_present(quip_window->window);
}
