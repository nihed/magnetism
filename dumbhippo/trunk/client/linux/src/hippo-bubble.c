#include <config.h>
#include <gtk/gtkcontainer.h>
#include "hippo-bubble.h"
#include "main.h"
#include "hippo-embedded-image.h"

static void      hippo_bubble_init                (HippoBubble       *bubble);
static void      hippo_bubble_class_init          (HippoBubbleClass  *klass);

static void      hippo_bubble_finalize            (GObject           *object);

static gboolean  hippo_bubble_expose_event        (GtkWidget         *widget,
            	       	                           GdkEventExpose    *event);

struct _HippoBubble {
    GtkFixed parent;
};

struct _HippoBubbleClass {
    GtkFixedClass parent_class;

};

G_DEFINE_TYPE(HippoBubble, hippo_bubble, GTK_TYPE_FIXED);

static void
hippo_bubble_init(HippoBubble       *bubble)
{

}

static void
hippo_bubble_class_init(HippoBubbleClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS(klass);

    object_class->finalize = hippo_bubble_finalize;
    
    widget_class->expose_event = hippo_bubble_expose_event;
}

HippoBubble*
hippo_bubble_new(void)
{
    HippoBubble *bubble;

    bubble = g_object_new(HIPPO_TYPE_BUBBLE, NULL);

    return HIPPO_BUBBLE(bubble);
}

static void
hippo_bubble_finalize(GObject *object)
{
    HippoBubble *bubble = HIPPO_BUBBLE(object);

    G_OBJECT_CLASS(hippo_bubble_parent_class)->finalize(object);
}

static void
draw_pixbuf(GtkWidget *widget,
            GdkGC     *gc,
            GdkPixbuf *pixbuf,
            GdkGravity gravity,
            int        x,
            int        y)
{
    int width;
    int height;
    
    width = gdk_pixbuf_get_width(pixbuf);
    height = gdk_pixbuf_get_height(pixbuf);
    
    switch (gravity) {
        case GDK_GRAVITY_EAST:
        case GDK_GRAVITY_NORTH_EAST:
        case GDK_GRAVITY_SOUTH_EAST:
            x -= width;
            break;
        default:
            break;
    }
    
    switch (gravity) {
        case GDK_GRAVITY_SOUTH:
        case GDK_GRAVITY_SOUTH_EAST:
        case GDK_GRAVITY_SOUTH_WEST:
            y -= height;
            break;
        default:
            break;
    }
    
    switch (gravity) {
        case GDK_GRAVITY_CENTER:
        case GDK_GRAVITY_SOUTH:
        case GDK_GRAVITY_NORTH:
            x -= width / 2;
            break;
        default:
            break;
    }
    
    switch (gravity) {
        case GDK_GRAVITY_CENTER:
        case GDK_GRAVITY_EAST:
        case GDK_GRAVITY_WEST:
            y -= height / 2;
            break;
        default:
            break;
    }
    
    gdk_draw_pixbuf(widget->window, gc, pixbuf, 0, 0, x, y,
                    width, height, GDK_RGB_DITHER_NORMAL, 0, 0);
}

static void
tile_pixbuf(GtkWidget    *widget,
            GdkGC        *gc,
            GdkPixbuf    *pixbuf,
            GdkWindowEdge edge,
            GdkRectangle *clip_area,
            GdkRectangle *tile_area)
{
    int x;
    int y; 
    int stop_at;
    GdkRectangle clip_rect;

    if (!gdk_rectangle_intersect(tile_area, clip_area, &clip_rect))
        return;
    
    switch (edge) {
        case GDK_WINDOW_EDGE_WEST:
            x = tile_area->x;
            y = tile_area->y;
            stop_at = tile_area->y + tile_area->height;
            break;
        case GDK_WINDOW_EDGE_EAST:
            x = tile_area->x + tile_area->width;
            y = tile_area->y;
            stop_at = tile_area->y + tile_area->height;
            break;
           
        case GDK_WINDOW_EDGE_NORTH:
            x = tile_area->x;
            y = tile_area->y;
            stop_at = tile_area->x + tile_area->width;
            break;
            
        case GDK_WINDOW_EDGE_SOUTH:
            x = tile_area->x;
            y = tile_area->y + tile_area->height;
            stop_at = tile_area->x + tile_area->width;
            break;

        default:
            break;
    }

    gdk_gc_set_clip_rectangle(gc, &clip_rect);
    
    switch (edge) {
        case GDK_WINDOW_EDGE_WEST:
        case GDK_WINDOW_EDGE_EAST:
            {
                int next = y;
                int height = gdk_pixbuf_get_height(pixbuf);
                while (next < stop_at) {
                    draw_pixbuf(widget, gc, pixbuf,
                                edge == GDK_WINDOW_EDGE_EAST ? 
                                    GDK_GRAVITY_NORTH_EAST : GDK_GRAVITY_NORTH_WEST,
                                x, next);
                    next += height;
                }
            }
            break;
        case GDK_WINDOW_EDGE_NORTH:
        case GDK_WINDOW_EDGE_SOUTH:
            {
                int next = x;
                int width = gdk_pixbuf_get_width(pixbuf);
                while (next < stop_at) {
                    draw_pixbuf(widget, gc, pixbuf,
                                edge == GDK_WINDOW_EDGE_SOUTH ? 
                                    GDK_GRAVITY_SOUTH_WEST : GDK_GRAVITY_NORTH_WEST,
                                next, y);
                    next += width;
                }
            }
            break;
        default:
            g_warning("can't tile pixbuf with edge %d", edge);
            break;
    }
    
    /* put clip rect back */
    gdk_gc_set_clip_rectangle(gc, clip_area);
}

static gboolean
hippo_bubble_expose_event(GtkWidget      *widget,
            		      GdkEventExpose *event)
{
    if (GTK_WIDGET_DRAWABLE(widget)) {
        GtkContainer *container = GTK_CONTAINER(widget);
        GdkGC *gc;
        GdkPixbuf *pixbuf;
        GdkRectangle orange_rect;
        GdkRectangle bottom_edge_rect;
        GdkRectangle top_edge_rect;
        GdkRectangle left_edge_rect;
        GdkRectangle right_edge_rect;                
        
        gc = gdk_gc_new(widget->window);
        gdk_gc_set_clip_rectangle(gc, &event->area);
        
        orange_rect.x = widget->allocation.x;
        orange_rect.y = widget->allocation.y;
        orange_rect.width = widget->allocation.width;
        orange_rect.height = widget->allocation.height;
        
        orange_rect.x += container->border_width;
        orange_rect.y += container->border_width;
        orange_rect.width -= 2 * container->border_width;
        orange_rect.height -= 2 * container->border_width;
        
        /* IT'S ORANGE BABY */
        gdk_rgb_gc_set_foreground(gc, 0xF16D1C);        
        gdk_draw_rectangle(widget->window, gc, TRUE,
                           orange_rect.x, orange_rect.y,
                           orange_rect.width, orange_rect.height);

        /* now stamp the little pixmap pieces all over */
        bottom_edge_rect = orange_rect;
        top_edge_rect = orange_rect;
        left_edge_rect = orange_rect;
        right_edge_rect = orange_rect;
        
        pixbuf = hippo_embedded_image_get("obubcnr_tr");
        draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_NORTH_EAST,
                    orange_rect.x + orange_rect.width,
                    orange_rect.y);
        top_edge_rect.width -= gdk_pixbuf_get_width(pixbuf);
        right_edge_rect.y += gdk_pixbuf_get_height(pixbuf);
        right_edge_rect.height -= gdk_pixbuf_get_height(pixbuf);
        
        pixbuf = hippo_embedded_image_get("obubcnr_br");
        draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_SOUTH_EAST,
                    orange_rect.x + orange_rect.width,
                    orange_rect.y + orange_rect.height);
        bottom_edge_rect.width -= gdk_pixbuf_get_width(pixbuf);
        right_edge_rect.height -= gdk_pixbuf_get_height(pixbuf);
        
        pixbuf = hippo_embedded_image_get("obubcnr_bl");
        draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_SOUTH_WEST,
                    orange_rect.x,
                    orange_rect.y + orange_rect.height);
        bottom_edge_rect.x += gdk_pixbuf_get_width(pixbuf);
        bottom_edge_rect.width -= gdk_pixbuf_get_width(pixbuf);
        left_edge_rect.height -= gdk_pixbuf_get_height(pixbuf);                   
                                 
        pixbuf = hippo_embedded_image_get("obubcnr_tl");
        draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_NORTH_WEST,
                    orange_rect.x,
                    orange_rect.y);
        left_edge_rect.y += gdk_pixbuf_get_height(pixbuf);
        left_edge_rect.height -= gdk_pixbuf_get_height(pixbuf);
        top_edge_rect.x += gdk_pixbuf_get_width(pixbuf);
        top_edge_rect.width -= gdk_pixbuf_get_width(pixbuf);
                    
        pixbuf = hippo_embedded_image_get("obubedge_b");
        tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_SOUTH,
                    &event->area, &bottom_edge_rect);

        pixbuf = hippo_embedded_image_get("obubedge_t");
        tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_NORTH,
                    &event->area, &top_edge_rect);

        pixbuf = hippo_embedded_image_get("obubedge_l");
        tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_WEST,
                    &event->area, &left_edge_rect);

        pixbuf = hippo_embedded_image_get("obubedge_r");
        tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_EAST,
                    &event->area, &right_edge_rect);

                    
        g_object_unref(gc);
    }
    /* Now draw the child widgets on there */  
    return GTK_WIDGET_CLASS(hippo_bubble_parent_class)->expose_event(widget, event);
}
