/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <hippo/hippo-event.h>

GType
hippo_event_get_type (void)
{
    static GType type = 0;

    if (G_UNLIKELY(type == 0)) {
        type = g_boxed_type_register_static("HippoEvent",
                                            (GBoxedCopyFunc)hippo_event_copy,
                                            (GBoxedFreeFunc)hippo_event_free);
    }

    return type;
}

HippoEvent *
hippo_event_copy(HippoEvent *event)
{
    g_return_val_if_fail(event != NULL, NULL);

    return (HippoEvent *)g_memdup(event, sizeof(HippoEvent));
}

void
hippo_event_free(HippoRectangle *event)
{
    g_return_if_fail(event != NULL);

    g_free(event);
}
