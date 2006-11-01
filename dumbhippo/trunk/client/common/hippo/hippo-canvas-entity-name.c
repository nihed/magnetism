/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-entity.h>
#include "hippo-actions.h"
#include "hippo-canvas-entity-name.h"
#include "hippo-feed.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_entity_name_init                (HippoCanvasEntityName       *text);
static void      hippo_canvas_entity_name_class_init          (HippoCanvasEntityNameClass  *klass);
static void      hippo_canvas_entity_name_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_entity_name_dispose             (GObject                *object);
static void      hippo_canvas_entity_name_finalize            (GObject                *object);

static void hippo_canvas_entity_name_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_canvas_entity_name_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);


/* Our own methods */
static void hippo_canvas_entity_name_set_entity  (HippoCanvasEntityName *canvas_entity_name,
                                                  HippoEntity           *entity);
static void hippo_canvas_entity_name_update_text (HippoCanvasEntityName *entity_name);


struct _HippoCanvasEntityName {
    HippoCanvasUrlLink parent;
    HippoEntity  *entity;
};

struct _HippoCanvasEntityNameClass {
    HippoCanvasUrlLinkClass parent_class;
};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_ENTITY
};


G_DEFINE_TYPE_WITH_CODE(HippoCanvasEntityName, hippo_canvas_entity_name, HIPPO_TYPE_CANVAS_URL_LINK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_entity_name_iface_init));

static void
hippo_canvas_entity_name_init(HippoCanvasEntityName *entity_name)
{
    g_object_set(G_OBJECT(entity_name),
                 "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                 NULL);
    hippo_canvas_entity_name_update_text(entity_name);
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_entity_name_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_entity_name_class_init(HippoCanvasEntityNameClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    /* HippoCanvasTextClass *canvas_text_class = HIPPO_CANVAS_TEXT_CLASS(klass); */

    object_class->set_property = hippo_canvas_entity_name_set_property;
    object_class->get_property = hippo_canvas_entity_name_get_property;

    object_class->dispose = hippo_canvas_entity_name_dispose;
    object_class->finalize = hippo_canvas_entity_name_finalize;

    g_object_class_install_property(object_class,
                                    PROP_ENTITY,
                                    g_param_spec_object("entity",
                                                        _("Entity"),
                                                        _("Entity to display"),
                                                        HIPPO_TYPE_ENTITY,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_entity_name_dispose(GObject *object)
{
    HippoCanvasEntityName *entity_name = HIPPO_CANVAS_ENTITY_NAME(object);

    hippo_canvas_entity_name_set_entity(entity_name, NULL);

    G_OBJECT_CLASS(hippo_canvas_entity_name_parent_class)->dispose(object);
}

static void
hippo_canvas_entity_name_finalize(GObject *object)
{
    /* HippoCanvasEntityName *text = HIPPO_CANVAS_ENTITY_NAME(object); */

    G_OBJECT_CLASS(hippo_canvas_entity_name_parent_class)->finalize(object);
}

static void
hippo_canvas_entity_name_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasEntityName *entity_name;

    entity_name = HIPPO_CANVAS_ENTITY_NAME(object);

    switch (prop_id) {
    case PROP_ENTITY:
        {
            HippoEntity *new_entity = (HippoEntity*) g_value_get_object(value);
            hippo_canvas_entity_name_set_entity(entity_name, new_entity);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entity_name_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasEntityName *entity_name;

    entity_name = HIPPO_CANVAS_ENTITY_NAME (object);

    switch (prop_id) {
    case PROP_ENTITY:
        g_value_set_object(value, (GObject*) entity_name->entity);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entity_name_update_text(HippoCanvasEntityName *entity_name)
{
    if (entity_name->entity) {
        char *tooltip = NULL;

        if (hippo_entity_get_home_url(entity_name->entity) != NULL) {
            /* We let feeds get their default tooltip, which is the URL */
            if (!HIPPO_IS_FEED(entity_name->entity))
                tooltip = g_strdup_printf(_("%s's Mugshot"), hippo_entity_get_name(entity_name->entity));
        }

        g_debug("============ %s %s %s\n", hippo_entity_get_name(entity_name->entity), tooltip, hippo_entity_get_home_url(entity_name->entity));
        g_object_set(G_OBJECT(entity_name),
                     "text", hippo_entity_get_name(entity_name->entity),
                     "tooltip", tooltip,
                     "url", hippo_entity_get_home_url(entity_name->entity),
                     NULL);
        
        g_free(tooltip);
    } else {
        g_object_set(G_OBJECT(entity_name),
                     "text", NULL,
                     "tooltip", NULL,
                     "url", NULL,
                     NULL);
    }
}

static void
on_entity_changed(HippoEntity *entity,
                  void        *data)
{
    HippoCanvasEntityName *entity_name = HIPPO_CANVAS_ENTITY_NAME(data);

    hippo_canvas_entity_name_update_text(entity_name);
}

static void
hippo_canvas_entity_name_set_entity(HippoCanvasEntityName *entity_name,
                                    HippoEntity            *entity)
{
    if (entity == entity_name->entity)
        return;

    if (entity_name->entity) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(entity_name->entity),
                                             G_CALLBACK(on_entity_changed),
                                             entity_name);
        
        g_object_unref(entity_name->entity);
        entity_name->entity = NULL;
    }

    if (entity) {
        g_object_ref(entity);
        entity_name->entity = entity;

        g_signal_connect(G_OBJECT(entity), "changed",
                         G_CALLBACK(on_entity_changed),
                         entity_name);
    }

    hippo_canvas_entity_name_update_text(entity_name);
    
    g_object_notify(G_OBJECT(entity_name), "entity");
}
