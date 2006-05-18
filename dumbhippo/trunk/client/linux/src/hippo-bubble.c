#include <config.h>
#include <gtk/gtkcontainer.h>
#include "hippo-bubble.h"
#include "main.h"
#include "hippo-embedded-image.h"

static void      hippo_bubble_init                (HippoBubble       *bubble);
static void      hippo_bubble_class_init          (HippoBubbleClass  *klass);

static void      hippo_bubble_finalize            (GObject           *object);

static void      hippo_bubble_map                 (GtkWidget         *widget);

static gboolean  hippo_bubble_expose_event        (GtkWidget         *widget,
            	       	                           GdkEventExpose    *event);

static void      hippo_bubble_size_request        (GtkWidget         *widget,
            	       	                           GtkRequisition    *requisition);
static void      hippo_bubble_size_allocate       (GtkWidget         *widget,
            	       	                           GtkAllocation     *allocation);

struct _HippoBubble {
    GtkFixed parent;
    GtkWidget *sender_photo;
    GtkWidget *sender_name;
    GtkWidget *link_swarm_logo;
    GtkWidget *link_title;
    GtkWidget *link_description;
    GtkWidget *recipients;
    GtkWidget *close_event_box;
};

struct _HippoBubbleClass {
    GtkFixedClass parent_class;

};

G_DEFINE_TYPE(HippoBubble, hippo_bubble, GTK_TYPE_FIXED);

static gboolean
destroy_toplevel_on_click(GtkWidget *widget,
                          GdkEvent  *event,
                          void      *ignored)
{
    GtkWidget *toplevel;
    int width, height;
    
    if (event->type != GDK_BUTTON_RELEASE ||
        event->button.button != 1) {
        return FALSE;
    }
    
    gdk_drawable_get_size(event->button.window, &width, &height);
    
    if (event->button.x < 0 || event->button.y < 0 ||
        event->button.x > width || event->button.y > height)
        return FALSE;
        
    toplevel = gtk_widget_get_ancestor(widget, GTK_TYPE_WINDOW);
    
    if (toplevel != NULL)
        gtk_widget_destroy(toplevel);

    return FALSE;        
}


static void
set_max_label_width(GtkWidget   *label,
                    int          max_width,
                    gboolean     ellipsize)
{
    GtkRequisition req;
    
    gtk_widget_set_size_request(label, -1, -1);

    gtk_widget_size_request(label, &req);
    
    if (req.width > max_width) {
        gtk_widget_set_size_request(label, max_width, -1);
        if (ellipsize)
            gtk_label_set_ellipsize(GTK_LABEL(label), PANGO_ELLIPSIZE_END);
    }
}

static void
set_label_sizes(HippoBubble *bubble)
{
    set_max_label_width(bubble->link_title, 280, TRUE);
    set_max_label_width(bubble->link_description, 300, FALSE);
    set_max_label_width(bubble->recipients, 280, TRUE);
}


static void
hookup_widget(HippoBubble *bubble,
              GtkWidget  **widget_p)
{
    g_signal_connect(G_OBJECT(*widget_p), "destroy", G_CALLBACK(gtk_widget_destroyed), widget_p);
    gtk_container_add(GTK_CONTAINER(bubble), *widget_p);
    gtk_widget_show(*widget_p);
}

static void
hippo_bubble_init(HippoBubble       *bubble)
{
    GdkColor white;
    GdkPixbuf *pixbuf;
    GtkWidget *widget;

    GTK_WIDGET_UNSET_FLAGS(bubble, GTK_NO_WINDOW);
    
    /* we want a white background */    
    
    white.red = 0xFFFF;
    white.green = 0xFFFF;
    white.blue = 0xFFFF;
    white.pixel = 0;
    gtk_widget_modify_bg(GTK_WIDGET(bubble), GTK_STATE_NORMAL, &white);
    
    /* create widgets */

    bubble->close_event_box = g_object_new(GTK_TYPE_EVENT_BOX,
                                           "visible-window", FALSE,
                                           NULL);

    hookup_widget(bubble, &bubble->close_event_box);
    
    gtk_widget_add_events(bubble->close_event_box,
                          GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK);
    g_signal_connect(G_OBJECT(bubble->close_event_box), "button-release-event",
                     G_CALLBACK(destroy_toplevel_on_click), NULL);
                     

    bubble->sender_photo = gtk_event_box_new();
    gtk_widget_modify_bg(bubble->sender_photo, GTK_STATE_NORMAL, &white);
    gtk_container_set_border_width(GTK_CONTAINER(bubble->sender_photo), 2);
#if 0
    wigdet = gtk_image_new();
#else
    widget = gtk_image_new_from_stock(GTK_STOCK_STOP, GTK_ICON_SIZE_LARGE_TOOLBAR);
#endif
    /* photo is always supposed to be this size */
    gtk_widget_set_size_request(widget, 60, 60);
    gtk_container_add(GTK_CONTAINER(bubble->sender_photo), widget);

    hookup_widget(bubble, &bubble->sender_photo);
    
#if 0
    bubble->sender_name = gtk_label_new(NULL);
#else
    bubble->sender_name = gtk_label_new("Stevo");
#endif
    hookup_widget(bubble, &bubble->sender_name);
    gtk_misc_set_alignment(GTK_MISC(bubble->sender_name), 0.0, 0.0);
    
    pixbuf = hippo_embedded_image_get("bublinkswarm");
    bubble->link_swarm_logo = gtk_image_new_from_pixbuf(pixbuf);
    hookup_widget(bubble, &bubble->link_swarm_logo);
    
    
    bubble->link_title = g_object_new(GTK_TYPE_EVENT_BOX,
                                      "visible-window", FALSE,
                                      "above-child", TRUE,
                                      NULL);
    hookup_widget(bubble, &bubble->link_title);
    
#if 0
    widget = gtk_label_new(NULL);
#else
    widget = gtk_label_new("<u>Space Monkeys Invade Downtown</u>");
#endif
    gtk_container_add(GTK_CONTAINER(bubble->link_title), widget);
    gtk_widget_show(widget);
    
    gtk_label_set_use_markup(GTK_LABEL(widget), TRUE);    
    gtk_label_set_single_line_mode(GTK_LABEL(widget), TRUE);
    gtk_misc_set_alignment(GTK_MISC(widget), 0.0, 0.0);
    gtk_widget_modify_fg(widget, GTK_STATE_NORMAL, &white);

#if 0
    bubble->link_description = gtk_label_new(NULL);
#else
    bubble->link_description = gtk_label_new("<small>I wouldn't have believed it unless I saw it with my own two eyes!</small>");
#endif
    hookup_widget(bubble, &bubble->link_description);
    
    gtk_widget_modify_fg(bubble->link_description, GTK_STATE_NORMAL, &white);
    gtk_label_set_line_wrap(GTK_LABEL(bubble->link_description), TRUE);
    gtk_label_set_use_markup(GTK_LABEL(bubble->link_description), TRUE);
    gtk_misc_set_alignment(GTK_MISC(bubble->link_description), 0.0, 0.0);

#if 0
    bubble->recipients = gtk_label_new(NULL);
#else
    bubble->recipients = gtk_label_new("Sent to you, John, Anne");
#endif
    hookup_widget(bubble, &bubble->recipients);
    
    gtk_misc_set_alignment(GTK_MISC(bubble->recipients), 1.0, 1.0);
    gtk_label_set_line_wrap(GTK_LABEL(bubble->recipients), TRUE);
    gtk_label_set_use_markup(GTK_LABEL(bubble->recipients), TRUE);
    
    set_label_sizes(bubble);
}

static void
hippo_bubble_class_init(HippoBubbleClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS(klass);

    object_class->finalize = hippo_bubble_finalize;
    
    widget_class->expose_event = hippo_bubble_expose_event;
    widget_class->size_request = hippo_bubble_size_request;
    widget_class->size_allocate = hippo_bubble_size_allocate;
    widget_class->map = hippo_bubble_map;
}

GtkWidget*
hippo_bubble_new(void)
{
    HippoBubble *bubble;

    bubble = g_object_new(HIPPO_TYPE_BUBBLE, NULL);

    return GTK_WIDGET(bubble);
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
    int x = 0;
    int y = 0; 
    int stop_at = 0;
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

typedef enum {
    BASE_IS_WIDGET_ALLOCATION,
    BASE_IS_CONTENT_REQUISITION
} BaseMode;

/* border_rect is the rect of our bubble border, not of the whole 
 * widget e.g. it wouldn't include GtkContainer::border-width
 */
static void
compute_layout(GtkWidget          *widget,
               const GdkRectangle *base,
               BaseMode            mode,
               GdkRectangle       *border_rect_p,
               GdkRectangle       *content_rect_p,
               GdkRectangle       *bottom_edge_rect_p,
               GdkRectangle       *top_edge_rect_p,
               GdkRectangle       *left_edge_rect_p,
               GdkRectangle       *right_edge_rect_p,
               GdkRectangle       *close_rect_p)
{
    GtkContainer *container;
    HippoBubble *bubble;
    GdkPixbuf *tl_pixbuf;
    GdkPixbuf *bl_pixbuf;
    GdkPixbuf *tr_pixbuf;
    GdkPixbuf *br_pixbuf;
    GdkRectangle border_rect;
    GdkRectangle content_rect;
    GdkRectangle bottom_edge_rect;
    GdkRectangle top_edge_rect;
    GdkRectangle left_edge_rect;
    GdkRectangle right_edge_rect;
    GdkRectangle close_rect;
    int left_edge_width;
    int right_edge_width;
    int top_edge_height;    
    int bottom_edge_height;

    container = GTK_CONTAINER(widget);
    bubble = HIPPO_BUBBLE(widget);

    tr_pixbuf = hippo_embedded_image_get("obubcnr_tr");
    bl_pixbuf = hippo_embedded_image_get("obubcnr_bl");
    tl_pixbuf = hippo_embedded_image_get("obubcnr_tl");    
    br_pixbuf = hippo_embedded_image_get("obubcnr_br");

    /* don't use tr_pixbuf in this since it's the close button and 
     * "too big"
     */
    left_edge_width = MAX(gdk_pixbuf_get_width(tl_pixbuf), gdk_pixbuf_get_width(bl_pixbuf));
    right_edge_width = gdk_pixbuf_get_width(br_pixbuf);
    top_edge_height = gdk_pixbuf_get_height(tl_pixbuf);
    bottom_edge_height = MAX(gdk_pixbuf_get_height(br_pixbuf), gdk_pixbuf_get_height(bl_pixbuf));

    if (mode == BASE_IS_WIDGET_ALLOCATION) {
        /* "size allocate" mode */
        
        border_rect = *base;

        border_rect.x += container->border_width;
        border_rect.y += container->border_width;
        border_rect.width -= 2 * container->border_width;
        border_rect.height -= 2 * container->border_width;
    } else if (mode == BASE_IS_CONTENT_REQUISITION) {
        /* "size request" mode and the x/y are essentially meaningless */
            
        border_rect = *base;
        
        border_rect.width += left_edge_width + right_edge_width;
        border_rect.height += top_edge_height + bottom_edge_height;
    } else {
        g_assert_not_reached();
    }
    
    bottom_edge_rect = border_rect;
    top_edge_rect = border_rect;
    left_edge_rect = border_rect;
    right_edge_rect = border_rect;
    
    left_edge_rect.y += gdk_pixbuf_get_height(tl_pixbuf);
    left_edge_rect.height -= gdk_pixbuf_get_height(tl_pixbuf);
    left_edge_rect.height -= gdk_pixbuf_get_height(bl_pixbuf);
    left_edge_rect.width = left_edge_width;
    
    right_edge_rect.y += gdk_pixbuf_get_height(tr_pixbuf);
    right_edge_rect.height -= gdk_pixbuf_get_height(tr_pixbuf);    
    right_edge_rect.height -= gdk_pixbuf_get_height(br_pixbuf);
    right_edge_rect.width = right_edge_width;
    
    right_edge_rect.x = border_rect.x + border_rect.width - right_edge_rect.width;
            
    top_edge_rect.x += gdk_pixbuf_get_width(tl_pixbuf);    
    top_edge_rect.width -= gdk_pixbuf_get_width(tl_pixbuf);
    top_edge_rect.width -= gdk_pixbuf_get_width(tr_pixbuf);    
    top_edge_rect.height = top_edge_height;
    
    bottom_edge_rect.x += gdk_pixbuf_get_width(bl_pixbuf);
    bottom_edge_rect.width -= gdk_pixbuf_get_width(bl_pixbuf);
    bottom_edge_rect.width -= gdk_pixbuf_get_width(br_pixbuf);
    bottom_edge_rect.height = bottom_edge_height;

    bottom_edge_rect.y = border_rect.y + border_rect.height - bottom_edge_rect.height;

    content_rect.x = left_edge_rect.x + left_edge_rect.width;
    content_rect.y = top_edge_rect.y + top_edge_rect.height;
    content_rect.width = border_rect.width - left_edge_rect.width - right_edge_rect.width;
    content_rect.height = border_rect.height - top_edge_rect.height - bottom_edge_rect.height;
    
    close_rect.x = border_rect.x + border_rect.width - gdk_pixbuf_get_width(tr_pixbuf);
    close_rect.y = border_rect.y;
    close_rect.width = gdk_pixbuf_get_width(tr_pixbuf);
    close_rect.height = gdk_pixbuf_get_height(tr_pixbuf);
    
#define OUT(what) do { if (what ## _p) { * what ## _p = what; }  } while(0)
    OUT(border_rect);
    OUT(content_rect);
    OUT(bottom_edge_rect);
    OUT(top_edge_rect);
    OUT(left_edge_rect);
    OUT(right_edge_rect);
    OUT(close_rect);
#undef OUT   
}

static gboolean
hippo_bubble_expose_event(GtkWidget      *widget,
            		      GdkEventExpose *event)
{    
    GdkGC *gc;
    GdkPixbuf *pixbuf;
    GdkRectangle child_clip;        
    GdkRectangle border_clip;
    GdkRectangle border_rect;
    GdkRectangle content_rect;
    GdkRectangle bottom_edge_rect;
    GdkRectangle top_edge_rect;
    GdkRectangle left_edge_rect;
    GdkRectangle right_edge_rect;
    
    if (!GTK_WIDGET_DRAWABLE(widget))
        return FALSE;
    
    child_clip = event->area;
        
    compute_layout(widget, &widget->allocation,
                    BASE_IS_WIDGET_ALLOCATION,
                    &border_rect, &content_rect, 
                    &bottom_edge_rect, &top_edge_rect,
                    &left_edge_rect, &right_edge_rect, NULL);
    
    gdk_rectangle_intersect(&border_rect, &event->area, &border_clip);
    gdk_rectangle_intersect(&content_rect, &event->area, &child_clip);
    
    gc = gdk_gc_new(widget->window);
    gdk_gc_set_clip_rectangle(gc, &border_clip); 
    
    gdk_rgb_gc_set_foreground(gc, 0xFFFFFF);
    gdk_draw_rectangle(widget->window, gc, TRUE,
                        border_rect.x, border_rect.y,
                        border_rect.width, border_rect.height);

    /* IT'S ORANGE BABY */
    gdk_rgb_gc_set_foreground(gc, 0xF16D1C);
    gdk_draw_rectangle(widget->window, gc, TRUE,
                        content_rect.x, content_rect.y,
                        content_rect.width, content_rect.height);

    /* now stamp the little pixmap pieces all over */    
    pixbuf = hippo_embedded_image_get("obubcnr_tr");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_NORTH_EAST,
                border_rect.x + border_rect.width,
                border_rect.y);
    
    pixbuf = hippo_embedded_image_get("obubcnr_br");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_SOUTH_EAST,
                border_rect.x + border_rect.width,
                border_rect.y + border_rect.height);
    
    pixbuf = hippo_embedded_image_get("obubcnr_bl");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_SOUTH_WEST,
                border_rect.x,
                border_rect.y + border_rect.height);
                                
    pixbuf = hippo_embedded_image_get("obubcnr_tl");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_NORTH_WEST,
                border_rect.x,
                border_rect.y);
                
    pixbuf = hippo_embedded_image_get("obubedge_b");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_SOUTH,
                &border_clip, &bottom_edge_rect);

    pixbuf = hippo_embedded_image_get("obubedge_t");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_NORTH,
                &border_clip, &top_edge_rect);

    pixbuf = hippo_embedded_image_get("obubedge_l");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_WEST,
                &border_clip, &left_edge_rect);

    pixbuf = hippo_embedded_image_get("obubedge_r");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_EAST,
                &border_clip, &right_edge_rect);

    /* tough to set child_clip without messing up the drawing */
    GTK_WIDGET_CLASS(hippo_bubble_parent_class)->expose_event(widget, event);

    g_object_unref(gc);
    
    return FALSE;
}

/* whee, circumvent GtkEventBoxPrivate */
static GdkWindow*
event_box_get_event_window(GtkWidget *event_box)
{
    GList *children;
    GdkWindow *event_window;
    GList *link;
    void *user_data;
    
    g_return_val_if_fail(GTK_IS_EVENT_BOX(event_box), NULL);
    g_return_val_if_fail(event_box->window != NULL, NULL);
    
    children = gdk_window_get_children(event_box->window);
    
    for (link = children; link != NULL; link = link->next) {
        event_window = children->data;
        user_data = NULL;
        gdk_window_get_user_data(event_window, &user_data);
        if (GDK_WINDOW_OBJECT(event_window)->input_only &&
            user_data == event_box) {
            break;
        }
        event_window = NULL;
    }

    if (event_window == NULL) {
        g_warning("did not find event box input window, %d children", g_list_length(children));
    }
    
    g_list_free(children);
    
    return event_window;
}

static void
hippo_bubble_map(GtkWidget *widget)
{
    GdkCursor *cursor;
    HippoBubble *bubble;
    GdkWindow *event_window;
    
    bubble = HIPPO_BUBBLE(widget);
    
    /* this is in map not realize since in realize our children aren't
     * realized yet... there's probably a better way. container_map maps
     * all children but container_realize does not realize all children
     */
    GTK_WIDGET_CLASS(hippo_bubble_parent_class)->map(widget); 
    
    set_label_sizes(HIPPO_BUBBLE(widget));
    
    cursor = gdk_cursor_new_for_display(gtk_widget_get_display(widget),
                                        GDK_HAND2);
    event_window = event_box_get_event_window(bubble->link_title);
    gdk_window_set_cursor(event_window, cursor);
    gdk_display_flush(gtk_widget_get_display(widget));
    gdk_cursor_unref(cursor);
}

static GtkFixedChild*
find_fixed_child(GtkFixed  *fixed,
                 GtkWidget *widget)
{
    GList *link;
    
    for (link = fixed->children; link != NULL; link = link->next) {
        GtkFixedChild *child = link->data;
        if (child->widget == widget)
            return child;
    }

    return NULL;
}

/* If this hack breaks, we just have to change to a direct container 
 * subclass instead of fixed, but this is convenient for now
 */
static void
fixed_move_no_queue_resize(HippoBubble    *bubble,
                           GtkWidget      *widget,
                           int             x,
                           int             y)
{
    GtkFixedChild *child;

    child = find_fixed_child(GTK_FIXED(bubble), widget);

    gtk_widget_freeze_child_notify (widget);
    child->x = x;
    gtk_widget_child_notify (widget, "x");
    child->y = y;
    gtk_widget_child_notify (widget, "y");

    gtk_widget_thaw_child_notify (widget);

    /* DO NOT QUEUE RESIZE */
}

static void
compute_content_widgets_layout(HippoBubble  *bubble,
                               GdkRectangle *sender_photo_p,
                               GdkRectangle *sender_name_p,
                               GdkRectangle *link_swarm_logo_p,
                               GdkRectangle *link_title_p,
                               GdkRectangle *link_description_p)
{
    GdkRectangle sender_photo;
    GdkRectangle sender_name;
    GdkRectangle link_swarm_logo;
    GdkRectangle link_title;
    GdkRectangle link_description;

    /* assumes widget requisitions are up-to-date */
#define GET_REQ(what) do {                                  \
      GtkRequisition req;                                   \
      gtk_widget_get_child_requisition(bubble->what, &req); \
      what.width = req.width;                               \
      what.height = req.height;                             \
    } while(0)
        
    GET_REQ(sender_photo);
    sender_photo.x = 10;
    sender_photo.y = 10;

    /* center name under photo */
    GET_REQ(sender_name);
    sender_name.x = sender_photo.x + (sender_photo.width - sender_name.width) / 2;
    sender_name.y = sender_photo.y + sender_photo.height + 5;
    
    /* link swarm logo aligned top with photo */
    GET_REQ(link_swarm_logo);
    link_swarm_logo.x = sender_photo.x + sender_photo.width + 10;
    link_swarm_logo.y = sender_photo.y;

    GET_REQ(link_title);
    link_title.x = link_swarm_logo.x;
    link_title.y = link_swarm_logo.y + link_swarm_logo.height + 10;
    
    GET_REQ(link_description);
    link_description.x = link_title.x;
    link_description.y = link_title.y + link_title.height + 5;
        
#define OUT(what) do { if (what ## _p) { * what ## _p = what; }  } while(0)
    OUT(sender_photo);
    OUT(sender_name);
    OUT(link_swarm_logo);
    OUT(link_title);
    OUT(link_description);
#undef OUT
}

static void
hippo_bubble_size_request(GtkWidget         *widget,
            	       	  GtkRequisition    *requisition)
{
    HippoBubble *bubble;
    GtkContainer *container;
    GtkFixed *fixed;    
    GList *link;
    GtkRequisition req;
    GdkRectangle content_child_rect;
    GdkRectangle sender_photo_rect;
    GdkRectangle sender_name_rect;
    GdkRectangle link_swarm_logo_rect;
    GdkRectangle link_title_rect;
    GdkRectangle link_description_rect;
    GdkRectangle recipients_rect;
    GdkRectangle border_rect;
    GdkRectangle offset_content_rect;
    GdkRectangle close_event_box_rect;
    int xoffset;
    int yoffset;
    
    bubble = HIPPO_BUBBLE(widget);
    container = GTK_CONTAINER(widget);
    fixed = GTK_FIXED(widget);
        
    /* update all the widget->requisition */
    for (link = fixed->children; link != NULL; link = link->next) {
        GtkFixedChild *child = link->data;
        if (GTK_WIDGET_VISIBLE(child->widget)) {
            gtk_widget_size_request(child->widget, NULL);
        }            
    }
    
    /* layout children starting from 0,0 assuming no border or anything */
    compute_content_widgets_layout(bubble, &sender_photo_rect, &sender_name_rect,
                   &link_swarm_logo_rect, &link_title_rect, &link_description_rect);
    
    /* compute union of these rects */
    content_child_rect.x = 0;
    content_child_rect.y = 0;
    content_child_rect.width = 0;
    content_child_rect.height = 0;    
    gdk_rectangle_union(&content_child_rect, &sender_photo_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &sender_name_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &link_swarm_logo_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &link_title_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &link_description_rect, &content_child_rect);
    
    /* have to special case the recipients thing */
    gtk_widget_get_child_requisition(bubble->recipients, &req);
    recipients_rect.width = req.width;
    recipients_rect.height = req.height;
    recipients_rect.x = content_child_rect.x + content_child_rect.width - recipients_rect.width - 10;
    recipients_rect.y = content_child_rect.y + content_child_rect.height + 10;
    
    gdk_rectangle_union(&content_child_rect, &recipients_rect, &content_child_rect);
    
    /* see what other stuff goes around the content widgets */
    compute_layout(widget, &content_child_rect, BASE_IS_CONTENT_REQUISITION, &border_rect,
                   &offset_content_rect, NULL, NULL, NULL, NULL, &close_event_box_rect);

    /* note that all x,y coord stuff at this point is purely to set the GtkFixed child position,
     * which does not include container->border_width
     */
    xoffset = offset_content_rect.x - border_rect.x; /* we expect border_rect.x, y to be 0 */
    yoffset = offset_content_rect.y - border_rect.y;
    
    /* Now we know our requisition */
    requisition->width = border_rect.width + container->border_width * 2;
    requisition->height = border_rect.height + container->border_width * 2;
    
    /* side effect, we update where GtkFixed will display widgets on expose,
     * and help it gets its size allocate right
     */
#define OFFSET(what) do {                                      \
        fixed_move_no_queue_resize(bubble, bubble->what,       \
            what##_rect.x + xoffset, what##_rect.y + yoffset); \
    } while(0)
    
    OFFSET(sender_photo);
    OFFSET(sender_name);
    OFFSET(link_swarm_logo);
    OFFSET(link_title);
    OFFSET(link_description);
    OFFSET(close_event_box);
    OFFSET(recipients);
#undef OFFSET    
}

static void
hippo_bubble_size_allocate(GtkWidget         *widget,
            	       	   GtkAllocation     *allocation)
{
    /* We make no real effort to handle getting a too-small allocation... if you're using 
     * a busted window manager that ignores hints, we take patches.
     */
    HippoBubble *bubble;
    GdkRectangle border_rect;
    GdkRectangle content_rect;
    GdkRectangle close_event_box_rect;    

    bubble = HIPPO_BUBBLE(widget);

    /* Give every widget its size request and GtkFixed position, computed
     * at size request time
     */
    GTK_WIDGET_CLASS(hippo_bubble_parent_class)->size_allocate(widget, allocation);
    
    /* Now change our mind on some of them ... */
    
    compute_layout(widget, &widget->allocation,
                   BASE_IS_WIDGET_ALLOCATION,
                   &border_rect, &content_rect, 
                   NULL, NULL, NULL, NULL,
                   &close_event_box_rect);
    gtk_widget_size_allocate(bubble->close_event_box, &close_event_box_rect);                   
}
