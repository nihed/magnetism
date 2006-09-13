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
#include "hippo-canvas-entity-photo.h"
#include "hippo-canvas-box.h"
#include "hippo-canvas-image.h"

static void      hippo_canvas_entity_photo_init                (HippoCanvasEntityPhoto       *image);
static void      hippo_canvas_entity_photo_class_init          (HippoCanvasEntityPhotoClass  *klass);
static void      hippo_canvas_entity_photo_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_entity_photo_dispose             (GObject                *object);
static void      hippo_canvas_entity_photo_finalize            (GObject                *object);

static void hippo_canvas_entity_photo_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_canvas_entity_photo_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);


/* Our own methods */
static void hippo_canvas_entity_photo_set_entity (HippoCanvasEntityPhoto *canvas_entity_photo,
                                                  HippoEntity            *entity);

static void hippo_canvas_entity_photo_set_actions (HippoCanvasEntityPhoto *canvas_entity_photo,
                                                   HippoActions           *actions);

static void hippo_canvas_entity_photo_update_image(HippoCanvasEntityPhoto *entity_photo);

struct _HippoCanvasEntityPhoto {
    HippoCanvasImage canvas_image;
    HippoActions *actions;
    HippoEntity  *entity;
};

struct _HippoCanvasEntityPhotoClass {
    HippoCanvasImageClass parent_class;

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


G_DEFINE_TYPE_WITH_CODE(HippoCanvasEntityPhoto, hippo_canvas_entity_photo, HIPPO_TYPE_CANVAS_IMAGE,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_entity_photo_iface_init));

static void
hippo_canvas_entity_photo_init(HippoCanvasEntityPhoto *entity_photo)
{
    hippo_canvas_entity_photo_update_image(entity_photo);
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_entity_photo_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_entity_photo_class_init(HippoCanvasEntityPhotoClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    /* HippoCanvasImageClass *canvas_image_class = HIPPO_CANVAS_IMAGE_CLASS(klass); */

    object_class->set_property = hippo_canvas_entity_photo_set_property;
    object_class->get_property = hippo_canvas_entity_photo_get_property;

    object_class->dispose = hippo_canvas_entity_photo_dispose;
    object_class->finalize = hippo_canvas_entity_photo_finalize;

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
hippo_canvas_entity_photo_dispose(GObject *object)
{
    HippoCanvasEntityPhoto *entity_photo = HIPPO_CANVAS_ENTITY_PHOTO(object);

    hippo_canvas_entity_photo_set_entity(entity_photo, NULL);
    hippo_canvas_entity_photo_set_actions(entity_photo, NULL);

    G_OBJECT_CLASS(hippo_canvas_entity_photo_parent_class)->dispose(object);
}

static void
hippo_canvas_entity_photo_finalize(GObject *object)
{
    /* HippoCanvasEntityPhoto *image = HIPPO_CANVAS_ENTITY_PHOTO(object); */

    G_OBJECT_CLASS(hippo_canvas_entity_photo_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_entity_photo_new(void)
{
    HippoCanvasEntityPhoto *entity_photo;

    entity_photo = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_PHOTO, NULL);

    return HIPPO_CANVAS_ITEM(entity_photo);
}

static void
hippo_canvas_entity_photo_set_property(GObject         *object,
                                       guint            prop_id,
                                       const GValue    *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasEntityPhoto *entity_photo;

    entity_photo = HIPPO_CANVAS_ENTITY_PHOTO(object);

    switch (prop_id) {
    case PROP_ENTITY:
        {
            HippoEntity *new_entity = (HippoEntity*) g_value_get_object(value);
            hippo_canvas_entity_photo_set_entity(entity_photo, new_entity);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_entity_photo_set_actions(entity_photo, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entity_photo_get_property(GObject         *object,
                                       guint            prop_id,
                                       GValue          *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasEntityPhoto *entity_photo;

    entity_photo = HIPPO_CANVAS_ENTITY_PHOTO (object);

    switch (prop_id) {
    case PROP_ENTITY:
        g_value_set_object(value, (GObject*) entity_photo->entity);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) entity_photo->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entity_photo_update_image(HippoCanvasEntityPhoto *entity_photo)
{
    if (entity_photo->entity) {
        /* FIXME This creates a race where we can change the entity
         * while the old async load is still in progress.
         * Fixing it requires adding a way to cancel this load.
         */
#if 1
        hippo_actions_load_entity_photo_async(entity_photo->actions,
                                              entity_photo->entity,
                                              HIPPO_CANVAS_ITEM(entity_photo));
#endif
    } else {
        g_object_set(G_OBJECT(entity_photo),
                     "image-name", "nophoto",
                     NULL);
    }
}

static void
on_entity_changed(HippoEntity *entity,
                  void        *data)
{
    HippoCanvasEntityPhoto *entity_photo = HIPPO_CANVAS_ENTITY_PHOTO(data);

    hippo_canvas_entity_photo_update_image(entity_photo);
}

static void
hippo_canvas_entity_photo_set_entity(HippoCanvasEntityPhoto *entity_photo,
                                     HippoEntity            *entity)
{
    if (entity == entity_photo->entity)
        return;

    if (entity_photo->entity) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(entity_photo->entity),
                                             G_CALLBACK(on_entity_changed),
                                             entity_photo);
        
        g_object_unref(entity_photo->entity);
        entity_photo->entity = NULL;
    }

    if (entity) {
        g_object_ref(entity);
        entity_photo->entity = entity;

        g_signal_connect(G_OBJECT(entity), "changed",
                         G_CALLBACK(on_entity_changed),
                         entity_photo);
    }

    hippo_canvas_entity_photo_update_image(entity_photo);
    
    g_object_notify(G_OBJECT(entity_photo), "entity");
}

static void
hippo_canvas_entity_photo_set_actions(HippoCanvasEntityPhoto *entity_photo,
                                      HippoActions            *actions)
{
    if (actions == entity_photo->actions)
        return;

    if (entity_photo->actions) {
        g_object_unref(entity_photo->actions);
        entity_photo->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        entity_photo->actions = actions;
    }

    g_object_notify(G_OBJECT(entity_photo), "actions");
}

