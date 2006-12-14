/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-timestamp.h"

static void      hippo_canvas_timestamp_init                (HippoCanvasTimestamp       *timestamp);
static void      hippo_canvas_timestamp_class_init          (HippoCanvasTimestampClass  *klass);
static void      hippo_canvas_timestamp_iface_init          (HippoCanvasItemIface       *item_class);
static void      hippo_canvas_timestamp_dispose             (GObject                    *object);
static void      hippo_canvas_timestamp_finalize            (GObject                    *object);

static void hippo_canvas_timestamp_set_property (GObject      *object,
                                                 guint         prop_id,
                                                 const GValue *value,
                                                 GParamSpec   *pspec);
static void hippo_canvas_timestamp_get_property (GObject      *object,
                                                 guint         prop_id,
                                                 GValue       *value,
                                                 GParamSpec   *pspec);

/* Our own methods */
static void hippo_canvas_timestamp_set_actions (HippoCanvasTimestamp *timestamp,
                                                HippoActions         *actions);

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_ACTIONS,
    PROP_TIME
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasTimestamp, hippo_canvas_timestamp, HIPPO_TYPE_CANVAS_TEXT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_timestamp_iface_init));

static void
hippo_canvas_timestamp_init(HippoCanvasTimestamp *timestamp)
{
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_timestamp_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_timestamp_class_init(HippoCanvasTimestampClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    box_class->default_color = 0x0033ffff;
    
    object_class->set_property = hippo_canvas_timestamp_set_property;
    object_class->get_property = hippo_canvas_timestamp_get_property;

    object_class->dispose = hippo_canvas_timestamp_dispose;
    object_class->finalize = hippo_canvas_timestamp_finalize;
    
    g_object_class_install_property(object_class,
                                    PROP_TIME,
                                    g_param_spec_int("time",
                                                     _("Time"),
                                                     _("Time to display"),
                                                     G_MININT32, G_MAXINT32, -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY)); 
}

static void
hippo_canvas_timestamp_dispose(GObject *object)
{
    HippoCanvasTimestamp *timestamp = HIPPO_CANVAS_TIMESTAMP(object);

    hippo_canvas_timestamp_set_actions(timestamp, NULL);

    G_OBJECT_CLASS(hippo_canvas_timestamp_parent_class)->dispose(object);
}

static void
hippo_canvas_timestamp_finalize(GObject *object)
{
    /*    HippoCanvasTimestamp *timestamp = HIPPO_CANVAS_TIMESTAMP(object); */

    G_OBJECT_CLASS(hippo_canvas_timestamp_parent_class)->finalize(object);
}

HippoCanvasItem *
hippo_canvas_timestamp_new(void)
{
    HippoCanvasTimestamp *timestamp = g_object_new(HIPPO_TYPE_CANVAS_TIMESTAMP, NULL);

    return HIPPO_CANVAS_ITEM(timestamp);
}

static void
update_time(HippoCanvasTimestamp *timestamp)
{
    gint64 server_time_now;
    char *when;

    server_time_now = hippo_current_time_ms() + hippo_actions_get_server_time_offset(timestamp->actions);
    
    when = hippo_format_time_ago((GTime) (server_time_now / 1000), timestamp->time);

    g_object_set(G_OBJECT(timestamp), "text", when, NULL);

    g_free(when);
}

static void
hippo_canvas_timestamp_set_time(HippoCanvasTimestamp *timestamp,
                                GTime                 time)
{

    if (time == timestamp->time)
        return;

    timestamp->time = time;

    update_time(timestamp);
}

static void
on_minute_ticked(HippoActions         *actions,
                 HippoCanvasTimestamp *timestamp)
{
    update_time(timestamp);
}

static void
hippo_canvas_timestamp_set_actions(HippoCanvasTimestamp *timestamp,
                                  HippoActions          *actions)
{
    if (actions == timestamp->actions)
        return;

    if (timestamp->actions) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(timestamp->actions),
                                             G_CALLBACK(on_minute_ticked),
                                             timestamp);
        
        g_object_unref(timestamp->actions);
        timestamp->actions = NULL;
        
        g_signal_connect(G_OBJECT(timestamp->actions),
                         "minute-ticked",
                         G_CALLBACK(on_minute_ticked),
                         timestamp);
    }
    
    if (actions) {
        g_object_ref(actions);
        timestamp->actions = actions;
    }

    g_object_notify(G_OBJECT(timestamp), "actions");
}

static void
hippo_canvas_timestamp_set_property(GObject        *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasTimestamp *timestamp;

    timestamp = HIPPO_CANVAS_TIMESTAMP(object);

    switch (prop_id) {
    case PROP_TIME:
        hippo_canvas_timestamp_set_time(timestamp, g_value_get_int(value));
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_timestamp_set_actions(timestamp, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_timestamp_get_property(GObject        *object,
                                   guint            prop_id,
                                   GValue          *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasTimestamp *timestamp;

    timestamp = HIPPO_CANVAS_TIMESTAMP (object);

    switch (prop_id) {
    case PROP_TIME:
        g_value_set_int(value, timestamp->time);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) timestamp->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}
