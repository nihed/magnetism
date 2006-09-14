/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include <hippo/hippo-entity.h>
#include "hippo-actions.h"
#include "hippo-canvas-entity-name.h"
#include "hippo-canvas-box.h"
#include "hippo-canvas-link.h"

static void      hippo_canvas_entity_name_init                (HippoCanvasEntityName       *text);
static void      hippo_canvas_entity_name_class_init          (HippoCanvasEntityNameClass  *klass);
static void      hippo_canvas_entity_name_iface_init          (HippoCanvasItemClass   *item_class);
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


/* Canvas item methods */
static void hippo_canvas_entity_name_activated   (HippoCanvasItem       *item);

/* Our own methods */
static void hippo_canvas_entity_name_set_entity  (HippoCanvasEntityName *canvas_entity_name,
                                                  HippoEntity           *entity);
static void hippo_canvas_entity_name_set_actions (HippoCanvasEntityName *canvas_entity_name,
                                                  HippoActions          *actions);
static void hippo_canvas_entity_name_update_text (HippoCanvasEntityName *entity_name);


struct _HippoCanvasEntityName {
    HippoCanvasLink canvas_link;
    HippoActions *actions;
    HippoEntity  *entity;
};

struct _HippoCanvasEntityNameClass {
    HippoCanvasLinkClass parent_class;

    void (* activated) (HippoCanvasLink *link);
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
    PROP_ENTITY,
    PROP_ACTIONS
};


G_DEFINE_TYPE_WITH_CODE(HippoCanvasEntityName, hippo_canvas_entity_name, HIPPO_TYPE_CANVAS_LINK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_entity_name_iface_init));

static void
hippo_canvas_entity_name_init(HippoCanvasEntityName *entity_name)
{
    g_object_set(G_OBJECT(entity_name),
                 "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                 NULL);
    hippo_canvas_entity_name_update_text(entity_name);
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_entity_name_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->activated = hippo_canvas_entity_name_activated;
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

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE)); 
}

static void
hippo_canvas_entity_name_dispose(GObject *object)
{
    HippoCanvasEntityName *entity_name = HIPPO_CANVAS_ENTITY_NAME(object);

    hippo_canvas_entity_name_set_entity(entity_name, NULL);
    hippo_canvas_entity_name_set_actions(entity_name, NULL);

    G_OBJECT_CLASS(hippo_canvas_entity_name_parent_class)->dispose(object);
}

static void
hippo_canvas_entity_name_finalize(GObject *object)
{
    /* HippoCanvasEntityName *text = HIPPO_CANVAS_ENTITY_NAME(object); */

    G_OBJECT_CLASS(hippo_canvas_entity_name_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_entity_name_new(void)
{
    HippoCanvasEntityName *entity_name;

    entity_name = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_NAME, NULL);

    return HIPPO_CANVAS_ITEM(entity_name);
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
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_entity_name_set_actions(entity_name, new_actions);
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
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) entity_name->actions);
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
        g_object_set(G_OBJECT(entity_name),
                     "text", hippo_entity_get_name(entity_name->entity),
                     NULL);
    } else {
        g_object_set(G_OBJECT(entity_name),
                     "text", NULL,
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

static void
hippo_canvas_entity_name_set_actions(HippoCanvasEntityName *entity_name,
                                     HippoActions            *actions)
{
    if (actions == entity_name->actions)
        return;

    if (entity_name->actions) {
        g_object_unref(entity_name->actions);
        entity_name->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        entity_name->actions = actions;
    }

    g_object_notify(G_OBJECT(entity_name), "actions");
}

static void
hippo_canvas_entity_name_activated(HippoCanvasItem *item)
{
    HippoCanvasEntityName *entity_name = HIPPO_CANVAS_ENTITY_NAME(item);

    if (entity_name->actions && entity_name->entity) {
        hippo_actions_visit_entity(entity_name->actions,
                                   entity_name->entity);
    }
}
