#include <config.h>
#include "hippo-person-renderer.h"
#include "main.h"
#include <gtk/gtk.h>
#include <string.h>

static void      hippo_person_renderer_init                (HippoPersonRenderer       *renderer);
static void      hippo_person_renderer_class_init          (HippoPersonRendererClass  *klass);

static void      hippo_person_renderer_finalize            (GObject               *object);

static void hippo_person_renderer_get_property (GObject              *object,
                                                guint                 param_id,
                                                GValue               *value,
                                                GParamSpec           *pspec);
static void hippo_person_renderer_set_property (GObject              *object,
                                                guint                 param_id,
                                                const GValue         *value,
                                                GParamSpec           *pspec);
static void hippo_person_renderer_get_size     (GtkCellRenderer      *cell,
                                                GtkWidget            *widget,
                                                GdkRectangle         *rectangle,
                                                gint                 *x_offset,
                                                gint                 *y_offset,
                                                gint                 *width,
                                                gint                 *height);
static void hippo_person_renderer_render       (GtkCellRenderer      *cell,
                                                GdkDrawable          *window,
                                                GtkWidget            *widget,
                                                GdkRectangle         *background_area,
                                                GdkRectangle         *cell_area,
                                                GdkRectangle         *expose_area,
                                                GtkCellRendererState  flags);

enum {
	PROP_ZERO,
	PROP_PERSON
};

struct _HippoPersonRenderer {
    GtkCellRenderer parent;
    HippoPerson *person;
    PangoLayout *name_layout;
};

struct _HippoPersonRendererClass {
    GtkCellRendererClass parent_class;

};

G_DEFINE_TYPE(HippoPersonRenderer, hippo_person_renderer, GTK_TYPE_CELL_RENDERER);

static void
hippo_person_renderer_init(HippoPersonRenderer  *renderer)
{
}

static void
hippo_person_renderer_class_init(HippoPersonRendererClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    GtkCellRendererClass *cell_class = GTK_CELL_RENDERER_CLASS(klass);

    object_class->finalize = hippo_person_renderer_finalize;
    
    object_class->get_property = hippo_person_renderer_get_property;
    object_class->set_property = hippo_person_renderer_set_property;

    cell_class->get_size = hippo_person_renderer_get_size;
    cell_class->render = hippo_person_renderer_render;

    g_object_class_install_property(object_class,
                				    PROP_PERSON,
                                    g_param_spec_object("person",
                             							_("Person Object"),
                            							_("The person to render"),
                            							HIPPO_TYPE_PERSON,
                            							G_PARAM_READWRITE));
}

GtkCellRenderer*
hippo_person_renderer_new(void)
{
    HippoPersonRenderer *renderer;
    
    renderer = g_object_new(HIPPO_TYPE_PERSON_RENDERER,
                            NULL);
    
    return GTK_CELL_RENDERER(renderer);
}

static void
nuke_caches(HippoPersonRenderer *renderer)
{
    if (renderer->name_layout) {
        g_object_unref(renderer->name_layout);
        renderer->name_layout = NULL;
    }
}

static void
hippo_person_renderer_finalize(GObject *object)
{
    HippoPersonRenderer *renderer = HIPPO_PERSON_RENDERER(object);
    
    if (renderer->person) {
        g_object_unref(renderer->person);
        renderer->person = NULL;
    }

    nuke_caches(renderer);
    
    G_OBJECT_CLASS(hippo_person_renderer_parent_class)->finalize(object);
}

static void
hippo_person_renderer_get_property(GObject              *object,
                                   guint                 param_id,
                                   GValue               *value,
                                   GParamSpec           *pspec)
{
    HippoPersonRenderer *renderer;
    
    renderer = HIPPO_PERSON_RENDERER(object);

    switch (param_id) {
    case PROP_PERSON:
        g_value_set_object (value, G_OBJECT(renderer->person));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, param_id, pspec);
        break;
    }
}
                                   
static void
hippo_person_renderer_set_property(GObject              *object,
                                   guint                 param_id,
                                   const GValue         *value,
                                   GParamSpec           *pspec)
{
    HippoPersonRenderer *renderer;
    
    renderer = HIPPO_PERSON_RENDERER(object);

    switch (param_id) {
    case PROP_PERSON:
        {
            HippoPerson *new_person;
            new_person = (HippoPerson*) g_value_dup_object(value);
            
            if (new_person != renderer->person)
                nuke_caches(renderer);
            if (renderer->person)
                g_object_unref(renderer->person);
            renderer->person = new_person;
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, param_id, pspec);
        break;
    }
}

static void
update_caches(HippoPersonRenderer *renderer,
              GtkWidget           *widget)
{
    const char *name;
 
    /* FIXME right now the caches really aren't since we fully recreate each time */
    
    if (renderer->person == NULL) {
        name = NULL;
    } else {
        /* some of these can return NULL */
        name = hippo_entity_get_name(HIPPO_ENTITY(renderer->person));
    }
    
    if (name == NULL)
        name = "";

    if (renderer->name_layout == NULL) {
        renderer->name_layout = gtk_widget_create_pango_layout(widget, name);
    } else {
        pango_layout_set_text(renderer->name_layout, name, -1);
    }        
}

static void
hippo_person_renderer_get_size(GtkCellRenderer      *cell,
                               GtkWidget            *widget,
                               GdkRectangle         *cell_area,
                               gint                 *x_offset_p,
                               gint                 *y_offset_p,
                               gint                 *width_p,
                               gint                 *height_p)
{
    HippoPersonRenderer *renderer;
    int content_width;
    int content_height;
    int full_width;
    int full_height;
    PangoRectangle name_rect;

    renderer = HIPPO_PERSON_RENDERER(cell);

    update_caches(renderer, widget);
  
    pango_layout_get_pixel_extents(renderer->name_layout, NULL, &name_rect);

    content_width = name_rect.width;
    content_height = name_rect.height;
  
    full_width  = (int) cell->xpad * 2 + content_width;
    full_height = (int) cell->ypad * 2 + content_height;
  
    if (x_offset_p)
        *x_offset_p = 0;
    if (y_offset_p)
        *y_offset_p = 0;

    if (cell_area && content_width > 0 && content_height > 0) {
        if (x_offset_p) {
            *x_offset_p = (((gtk_widget_get_direction (widget) == GTK_TEXT_DIR_RTL) ?
                            1.0 - cell->xalign : cell->xalign) * 
                            (cell_area->width - full_width - cell->xpad * 2));
            *x_offset_p = MAX (*x_offset_p, 0) + cell->xpad;
        }
        if (y_offset_p) {
            *y_offset_p = (cell->yalign *
                           (cell_area->height - full_height - 2 * cell->ypad));
            *y_offset_p = MAX (*y_offset_p, 0) + cell->ypad;
	    }
    }

    if (width_p)
        *width_p = full_width;
  
    if (height_p)
        *height_p = full_height;
}

static void
hippo_person_renderer_render(GtkCellRenderer      *cell,
                             GdkDrawable          *window,
                             GtkWidget            *widget,
                             GdkRectangle         *background_area,
                             GdkRectangle         *cell_area,
                             GdkRectangle         *expose_area,
                             GtkCellRendererState  flags)
{
    HippoPersonRenderer *renderer;
    GdkRectangle content_rect;
    GdkRectangle draw_rect;
    GtkStateType state;
    /* cairo_t *cr; */

    renderer = HIPPO_PERSON_RENDERER(cell);
    
    hippo_person_renderer_get_size(cell, widget, cell_area, 
                                   &content_rect.x, &content_rect.y,
                                   &content_rect.width, &content_rect.height);
    
    content_rect.x += cell_area->x;
    content_rect.y += cell_area->y;
    
    if (!gdk_rectangle_intersect (cell_area, &content_rect, &draw_rect) ||
        !gdk_rectangle_intersect (expose_area, &draw_rect, &draw_rect))
        return;

    if ((flags & (GTK_CELL_RENDERER_SELECTED|GTK_CELL_RENDERER_PRELIT)) != 0) {
        if ((flags & GTK_CELL_RENDERER_SELECTED) != 0) {
            if (GTK_WIDGET_HAS_FOCUS (widget))
        	    state = GTK_STATE_SELECTED;
            else
                state = GTK_STATE_ACTIVE;
	    } else {
           state = GTK_STATE_PRELIGHT;
        }
    } else {
        state = GTK_STATE_NORMAL;
    }

    gtk_paint_layout(widget->style, window, state, TRUE, expose_area, widget, "hippopersonrenderer",
                     content_rect.x, content_rect.y, renderer->name_layout);
    /*
    cr = gdk_cairo_create(window);
    gdk_cairo_rectangle (cr, &draw_rect);
    cairo_fill(cr);
    cairo_destroy (cr);
    */
}
