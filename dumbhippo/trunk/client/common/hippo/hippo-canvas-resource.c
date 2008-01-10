/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-resource.h"
#include "hippo-resource.h"

static void      hippo_canvas_resource_init                (HippoCanvasResource       *resource);
static void      hippo_canvas_resource_class_init          (HippoCanvasResourceClass  *klass);
static void      hippo_canvas_resource_dispose             (GObject                *object);
static void      hippo_canvas_resource_finalize            (GObject                *object);

static void hippo_canvas_resource_set_property (GObject      *object,
                                                  guint         prop_id,
                                                  const GValue *value,
                                                  GParamSpec   *pspec);
static void hippo_canvas_resource_get_property (GObject      *object,
                                                  guint         prop_id,
                                                  GValue       *value,
                                                  GParamSpec   *pspec);

static void set_resource (HippoCanvasResource *canvas_resource,
                                DDMDataResource       *resource);
static void set_actions    (HippoCanvasResource *canvas_resource,
                            HippoActions          *actions);

static GObject* hippo_canvas_resource_constructor (GType                  type,
                                                    guint                  n_construct_properties,
                                                    GObjectConstructParam *construct_properties);

enum {
    PROP_0,
    PROP_RESOURCE,
    PROP_ACTIONS
};

G_DEFINE_TYPE(HippoCanvasResource, hippo_canvas_resource, HIPPO_TYPE_CANVAS_BOX)

static void
hippo_canvas_resource_init(HippoCanvasResource *resource)
{
}

static void
hippo_canvas_resource_class_init(HippoCanvasResourceClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    /* HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass); */

    object_class->set_property = hippo_canvas_resource_set_property;
    object_class->get_property = hippo_canvas_resource_get_property;
    object_class->constructor = hippo_canvas_resource_constructor;
    
    object_class->dispose = hippo_canvas_resource_dispose;
    object_class->finalize = hippo_canvas_resource_finalize;

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));
    
    g_object_class_install_property(object_class,
                                    PROP_RESOURCE,
                                    g_param_spec_pointer("resource",
                                                         _("Resource"),
                                                         _("Resource rendered by the item"),
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static GObject*
hippo_canvas_resource_constructor (GType                  type,
                                   guint                  n_construct_properties,
                                   GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_resource_parent_class)->constructor(type,
                                                                                      n_construct_properties,
                                                                                      construct_properties);
    HippoCanvasResource *canvas_resource = HIPPO_CANVAS_RESOURCE(object);

    if (HIPPO_CANVAS_RESOURCE_GET_CLASS(object)->create_children)
        HIPPO_CANVAS_RESOURCE_GET_CLASS(object)->create_children(canvas_resource);
    
    return object;
}

static void
hippo_canvas_resource_dispose(GObject *object)
{
    HippoCanvasResource *resource = HIPPO_CANVAS_RESOURCE(object);

    set_actions(resource, NULL);
    set_resource(resource, NULL);

    G_OBJECT_CLASS(hippo_canvas_resource_parent_class)->dispose(object);
}

static void
hippo_canvas_resource_finalize(GObject *object)
{
    /* HippoCanvasResource *resource = HIPPO_CANVAS_RESOURCE(object); */

    G_OBJECT_CLASS(hippo_canvas_resource_parent_class)->finalize(object);
}


DDMDataResource *
hippo_canvas_resource_get_resource (HippoCanvasResource *canvas_resource)
{
    return canvas_resource->resource;
}

static void
hippo_canvas_resource_set_property(GObject         *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasResource *canvas_resource;

    canvas_resource = HIPPO_CANVAS_RESOURCE(object);

    switch (prop_id) {
    case PROP_RESOURCE:
        {
            DDMDataResource *resource = g_value_get_pointer(value);
            set_resource(canvas_resource, resource);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *actions = (HippoActions*) g_value_get_object(value);
            set_actions(canvas_resource, actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_resource_get_property(GObject         *object,
                                     guint            prop_id,
                                     GValue          *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasResource *resource;

    resource = HIPPO_CANVAS_RESOURCE (object);

    switch (prop_id) {
    case PROP_RESOURCE:
        g_value_set_pointer(value, resource->resource);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, resource->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
on_resource_changed(DDMDataResource *resource,
                    GSList          *changed_properties,
                    gpointer         data)
{
    HippoCanvasResource *canvas_resource = data;
    
    if (HIPPO_CANVAS_RESOURCE_GET_CLASS(canvas_resource)->update)
        HIPPO_CANVAS_RESOURCE_GET_CLASS(canvas_resource)->update(canvas_resource);
}

static void
set_resource(HippoCanvasResource *canvas_resource,
                   DDMDataResource       *resource)
{
    if (canvas_resource->resource == resource)
        return;

    if (canvas_resource->resource) {
        ddm_data_resource_disconnect(canvas_resource->resource,
                                     on_resource_changed,
                                     canvas_resource);
        
        ddm_data_resource_unref(canvas_resource->resource);

        canvas_resource->resource = NULL;
    }
    
    if (resource) {
        ddm_data_resource_ref(resource);
        canvas_resource->resource = resource;
        
        ddm_data_resource_connect(canvas_resource->resource,
                                  NULL,
                                  on_resource_changed,
                                  canvas_resource);
    }

    on_resource_changed(resource, NULL, canvas_resource);

    g_object_notify(G_OBJECT(canvas_resource), "resource");
}

static void
set_actions(HippoCanvasResource *canvas_resource,
            HippoActions          *actions)
{
    if (actions == canvas_resource->actions)
        return;

    if (canvas_resource->actions) {
        g_object_unref(canvas_resource->actions);
        canvas_resource->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        canvas_resource->actions = actions;
    }

    g_object_notify(G_OBJECT(canvas_resource), "actions");
}
