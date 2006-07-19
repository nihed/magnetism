/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <gtk/gtkcontainer.h>
#include <gtk/gtknotebook.h>
#include <gtk/gtkhbox.h>
#include "hippo-bubble.h"
#include "main.h"
#include "hippo-embedded-image.h"

/* If you shrink the min width, one thing to check is that toggling 
 * between someone said, whos there does not resize the bubble;
 * that happens if the largest width in the bubble comes from the bottom
 * "extra widgets" area due to the bolding of the font
 */
#define MIN_BUBBLE_WIDTH 335
#define MAX_TITLE_WIDTH 200
#define MAX_DESCRIPTION_WIDTH 200
#define MAX_RECIPIENTS_WIDTH 190
#define MAX_SENDER_WIDTH 76
#define MAX_CHAT_MESSAGE_WIDTH 190

typedef enum {
    LINK_CLICK_VISIT_SENDER,
    LINK_CLICK_VISIT_POST,
    LINK_CLICK_VISIT_LAST_MESSAGE_SENDER,
    LINK_CLICK_ACTIVATE_WHOS_THERE,
    LINK_CLICK_ACTIVATE_SOMEONE_SAID,
    LINK_CLICK_ACTIVATE_TOTAL_VIEWERS,    
    LINK_CLICK_JOIN_CHAT,
    LINK_CLICK_IGNORE_POST,
    LINK_CLICK_INVITE    
} LinkClickAction;

typedef enum {
    PAGE_ACTION_NEXT,
    PAGE_ACTION_PREVIOUS
} PageAction;

typedef enum {
    ACTIVE_EXTRA_TOTAL_VIEWERS,
    ACTIVE_EXTRA_WHOS_THERE,
    ACTIVE_EXTRA_SOMEONE_SAID,
    ACTIVE_EXTRA_MEMBERSHIP_CHANGE
} ActiveExtraInfo;

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

static void      hippo_bubble_link_click_action   (HippoBubble       *bubble,
                                                   LinkClickAction    action);

struct _HippoBubble {
    GtkFixed parent;
    GtkWidget *sender_photo;
    GtkWidget *sender_name;
    GtkWidget *header_logo;
    GtkWidget *link_title;
    GtkWidget *link_description;
    GtkWidget *recipients;
    GtkWidget *close_event_box;
    GtkWidget *n_of_n;
    GtkWidget *left_arrow;
    GtkWidget *right_arrow;
    GtkWidget *total_viewers;    
    GtkWidget *whos_there;
    GtkWidget *someone_said;
    GtkWidget *join_chat;
    GtkWidget *join_chat_label;
    GtkWidget *chat_count_label;
    GtkWidget *ignore;
    GtkWidget *ignore_label;
    GtkWidget *invite;
    GtkWidget *invite_label;     
    GtkWidget *last_message;
    GtkWidget *last_message_photo;
    GtkWidget *viewers;
    HippoBubbleColor color;
    char      *user_link_header;
    char      *sender_id;
    char      *post_id;
    char      *group_id;
    char      *assoc_id;       
    char      *last_message_sender_id;
    int        actions;
    int        page; /* [0,total_pages) */
    int        total_pages;
    unsigned int total_viewers_set : 1;    
    unsigned int whos_there_set : 1;
    unsigned int someone_said_set : 1;
    ActiveExtraInfo active_extra;
    int extra_widgets_height_last_allocation;
};

struct _HippoBubbleClass {
    GtkFixedClass parent_class;

};

G_DEFINE_TYPE(HippoBubble, hippo_bubble, GTK_TYPE_FIXED);

/* whee, circumvent GtkEventBoxPrivate */
static GdkWindow*
event_box_get_event_window(GtkEventBox *event_box)
{
    GList *children;
    GdkWindow *event_window;
    GList *link;
    void *user_data;
    GtkWidget *widget;
    
    g_return_val_if_fail(GTK_IS_EVENT_BOX(event_box), NULL);
    g_return_val_if_fail(GTK_WIDGET_REALIZED(event_box), NULL);
    
    widget = GTK_WIDGET(event_box);
    g_return_val_if_fail(widget->window != NULL, NULL);
    
    if (gtk_event_box_get_visible_window(event_box)) {
        return widget->window;
    }
    
    /* event_box->window is the parent window of the event box */
    
    children = gdk_window_get_children(widget->window);
    
    event_window = NULL;
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
        g_warning("did not find event box input window, %d children of %s",
                  g_list_length(children), G_OBJECT_TYPE_NAME(event_box));
    }
    
    g_list_free(children);
    
    return event_window;
}

static void
set_hand_cursor(GtkEventBox *event_box)
{
    GdkCursor *cursor;
    GdkWindow *event_window;
    GtkWidget *widget;
    
    widget = GTK_WIDGET(event_box);
    
    cursor = gdk_cursor_new_for_display(gtk_widget_get_display(widget),
                                        GDK_HAND2);
    event_window = event_box_get_event_window(event_box);
    gdk_window_set_cursor(event_window, cursor);
    
    gdk_display_flush(gtk_widget_get_display(widget));
    gdk_cursor_unref(cursor);
}

static void
connect_hand_cursor_on_realize(GtkEventBox *event_box)
{
    g_signal_connect_after(G_OBJECT(event_box), "realize", G_CALLBACK(set_hand_cursor), NULL);
}

static gboolean
is_button_release_over_widget(GtkWidget *widget,
                              GdkEvent  *event)
{
    int width, height;
    
    if (event->type != GDK_BUTTON_RELEASE ||
        event->button.button != 1) {
        return FALSE;
    }
    
    gdk_drawable_get_size(event->button.window, &width, &height);
    
    if (event->button.x < 0 || event->button.y < 0 ||
        event->button.x > width || event->button.y > height)
        return FALSE;

    return TRUE;
}                              

static void
delete_toplevel(GtkWidget *widget)
{
    GtkWidget *toplevel;
    
    toplevel = gtk_widget_get_ancestor(widget, GTK_TYPE_WINDOW);
    
    if (toplevel != NULL) {
        /* Synthesize delete_event */
        GdkEvent *event;

        event = gdk_event_new(GDK_DELETE);
  
        event->any.window = g_object_ref(toplevel->window);
        event->any.send_event = TRUE;
  
        gtk_main_do_event(event);
        gdk_event_free(event);
    }
}

static gboolean
delete_toplevel_on_click(GtkWidget *widget,
                         GdkEvent  *event,
                         void      *ignored)
{
    if (!is_button_release_over_widget(widget, event))
        return FALSE;
        
    delete_toplevel(widget);

    return FALSE;        
}

static gboolean
link_click_action_on_click(GtkWidget *widget,
                           GdkEvent  *event,
                           void      *data)
{
    LinkClickAction action;
    GtkWidget *bubble;
 
    if (!is_button_release_over_widget(widget, event))
        return FALSE;

    action = GPOINTER_TO_INT(data);

    bubble = gtk_widget_get_ancestor(widget, HIPPO_TYPE_BUBBLE);
    hippo_bubble_link_click_action(HIPPO_BUBBLE(bubble), action);
    return FALSE;
}

static gboolean
page_notebook_on_click(GtkWidget *widget,
                       GdkEvent  *event,
                       void      *data)
{
    GtkWidget *notebook;
    PageAction action;
        
    if (!is_button_release_over_widget(widget, event))
        return FALSE;
    
    notebook = gtk_widget_get_ancestor(widget, GTK_TYPE_NOTEBOOK);
    action = GPOINTER_TO_INT(data);
    
    if (notebook != NULL) {
        if (action == PAGE_ACTION_NEXT) {
            gtk_notebook_next_page(GTK_NOTEBOOK(notebook));
        } else {
            gtk_notebook_prev_page(GTK_NOTEBOOK(notebook));
        }
    }

    return FALSE;
}

static void
set_max_label_width(GtkWidget   *label,
                    int          max_width,
                    gboolean     ellipsize)
{
    GtkRequisition req;
    
    gtk_widget_set_size_request(label, -1, -1);
    gtk_label_set_ellipsize(GTK_LABEL(label), PANGO_ELLIPSIZE_NONE);

    gtk_widget_size_request(label, &req);
    
    if (req.width > max_width) {
        gtk_widget_set_size_request(label, max_width, -1);
        if (ellipsize)
            gtk_label_set_ellipsize(GTK_LABEL(label), PANGO_ELLIPSIZE_END);
    }
}

static void
connect_link_action(GtkWidget      *widget,
                    LinkClickAction action)
{
    g_signal_connect(G_OBJECT(widget), "button-release-event",
                     G_CALLBACK(link_click_action_on_click),
                     GINT_TO_POINTER(action));                  
    connect_hand_cursor_on_realize(GTK_EVENT_BOX(widget));
}

static void
connect_page_action(GtkWidget      *widget,
                    PageAction      action)
{
    g_signal_connect(G_OBJECT(widget), "button-release-event",
                     G_CALLBACK(page_notebook_on_click),
                     GINT_TO_POINTER(action));
    connect_hand_cursor_on_realize(GTK_EVENT_BOX(widget));                     
}

static void
set_label_sizes(HippoBubble *bubble)
{
    set_max_label_width(GTK_BIN(bubble->link_title)->child, MAX_TITLE_WIDTH, TRUE);
    set_max_label_width(bubble->link_description, MAX_DESCRIPTION_WIDTH, FALSE);
    set_max_label_width(bubble->recipients, MAX_RECIPIENTS_WIDTH, TRUE);
    set_max_label_width(GTK_BIN(bubble->sender_name)->child, MAX_SENDER_WIDTH, TRUE);
    set_max_label_width(bubble->last_message, MAX_CHAT_MESSAGE_WIDTH, TRUE);
}

static void
hookup_widget(HippoBubble *bubble,
              GtkWidget  **widget_p)
{
    g_signal_connect(G_OBJECT(*widget_p), "destroy", G_CALLBACK(gtk_widget_destroyed), widget_p);
    if ((*widget_p)->parent == NULL)
        gtk_container_add(GTK_CONTAINER(bubble), *widget_p);
    gtk_widget_show(*widget_p);
}

static void
set_widget_fg(GtkWidget *widget, guint16 red, guint16 green, guint16 blue)
{
    GdkColor color;

    color.red = red;
    color.green = green;
    color.blue = blue;
    color.pixel = 0;
    gtk_widget_modify_fg(widget, GTK_STATE_NORMAL, &color);
}

static void
set_blue_fg(GtkWidget *widget)
{
    /* #3A6EA5 (matches the arrow images) */
    set_widget_fg(widget, 0x3a3a, 0x6e6e, 0xa5a5);
}

static void
set_white_fg(GtkWidget *widget)
{
    set_widget_fg(widget, 0xffff, 0xffff, 0xffff);
}

static void
set_default_fg(GtkWidget *widget)
{
    gtk_widget_modify_fg(widget, GTK_STATE_NORMAL, NULL);
}

static void
hippo_bubble_init(HippoBubble       *bubble)
{
    GdkColor white;
    GtkWidget *widget;
    GtkWidget *box;

    GTK_WIDGET_UNSET_FLAGS(bubble, GTK_NO_WINDOW);
    bubble->page = 0;
    bubble->total_pages = 1;
    bubble->active_extra = ACTIVE_EXTRA_WHOS_THERE;
    bubble->actions = HIPPO_BUBBLE_ACTION_JOIN_CHAT | HIPPO_BUBBLE_ACTION_IGNORE;

    white.red = 0xFFFF;
    white.green = 0xFFFF;
    white.blue = 0xFFFF;
    white.pixel = 0;
    
    /* we want a white background */
    
    gtk_widget_modify_bg(GTK_WIDGET(bubble), GTK_STATE_NORMAL, &white);
    
    /* Detect clicks on the close button */

    bubble->close_event_box = g_object_new(GTK_TYPE_EVENT_BOX,
                                           "visible-window", FALSE,
                                           NULL);

    hookup_widget(bubble, &bubble->close_event_box);
    
    g_signal_connect(G_OBJECT(bubble->close_event_box), "button-release-event",
                     G_CALLBACK(delete_toplevel_on_click), NULL);
                     
    /* Sender's photo */

    bubble->sender_photo = gtk_event_box_new();
    gtk_event_box_set_visible_window(GTK_EVENT_BOX(bubble->sender_photo), TRUE);
    gtk_widget_modify_bg(bubble->sender_photo, GTK_STATE_NORMAL, &white);
    connect_link_action(bubble->sender_photo, LINK_CLICK_VISIT_SENDER);

    widget = gtk_image_new();
    /* photo is always supposed to be 60x60, we want 2px white border */
    gtk_widget_set_size_request(widget, 62, 62);
    gtk_widget_show(widget);
    gtk_container_add(GTK_CONTAINER(bubble->sender_photo), widget);

    hookup_widget(bubble, &bubble->sender_photo);
    
    /* Sender's name label */
    
    bubble->sender_name = g_object_new(GTK_TYPE_EVENT_BOX,
                                       "visible-window", FALSE,
                                       "above-child", TRUE,
                                       NULL);

    hookup_widget(bubble, &bubble->sender_name);
    connect_link_action(bubble->sender_name, LINK_CLICK_VISIT_SENDER);
    
    widget = gtk_label_new(NULL);
    gtk_container_add(GTK_CONTAINER(bubble->sender_name), widget);
    gtk_widget_show(widget);
    
    gtk_misc_set_alignment(GTK_MISC(widget), 0.0, 0.0);
    
	bubble->header_logo = gtk_image_new();
	hookup_widget(bubble, &bubble->header_logo);

    /* link title */
    
    bubble->link_title = g_object_new(GTK_TYPE_EVENT_BOX,
                                      "visible-window", FALSE,
                                      "above-child", TRUE,
                                      NULL);
    hookup_widget(bubble, &bubble->link_title);
    connect_link_action(bubble->link_title, LINK_CLICK_VISIT_POST);
    
    widget = gtk_label_new(NULL);
    gtk_container_add(GTK_CONTAINER(bubble->link_title), widget);
    gtk_widget_show(widget);
    
    gtk_label_set_use_markup(GTK_LABEL(widget), TRUE);    
    gtk_label_set_single_line_mode(GTK_LABEL(widget), TRUE);
    gtk_misc_set_alignment(GTK_MISC(widget), 0.0, 0.0);
    set_white_fg(widget);

    /* Link description text */

    bubble->link_description = gtk_label_new(NULL);

    hookup_widget(bubble, &bubble->link_description);

    set_white_fg(bubble->link_description);    
    gtk_label_set_line_wrap(GTK_LABEL(bubble->link_description), TRUE);
    gtk_label_set_use_markup(GTK_LABEL(bubble->link_description), TRUE);
    gtk_misc_set_alignment(GTK_MISC(bubble->link_description), 0.0, 0.0);

    /* "sent to you, ..." label */

    bubble->recipients = gtk_label_new(NULL);

    hookup_widget(bubble, &bubble->recipients);
    
    gtk_misc_set_alignment(GTK_MISC(bubble->recipients), 1.0, 1.0);
    gtk_label_set_line_wrap(GTK_LABEL(bubble->recipients), TRUE);
    gtk_label_set_use_markup(GTK_LABEL(bubble->recipients), TRUE);

    /* Left page arrow */

    bubble->left_arrow = g_object_new(GTK_TYPE_EVENT_BOX,
                                      "visible-window", FALSE,
                                      "above-child", TRUE,
                                      NULL);
    connect_page_action(bubble->left_arrow, PAGE_ACTION_PREVIOUS);

    widget = gtk_image_new();
    gtk_widget_show(widget);
    gtk_container_add(GTK_CONTAINER(bubble->left_arrow), widget);
    hookup_widget(bubble, &bubble->left_arrow);
    gtk_widget_hide(bubble->left_arrow); /* override hookup_widget */

    /* Right page arrow */

    bubble->right_arrow = g_object_new(GTK_TYPE_EVENT_BOX,
                                       "visible-window", FALSE,
                                       "above-child", TRUE,
                                       NULL);
    connect_page_action(bubble->right_arrow, PAGE_ACTION_NEXT);

    widget = gtk_image_new();
    gtk_widget_show(widget);
    gtk_container_add(GTK_CONTAINER(bubble->right_arrow), widget);
    hookup_widget(bubble, &bubble->right_arrow);
    gtk_widget_hide(bubble->right_arrow); /* override hookup_widget */    

    /* page N of N label */

    bubble->n_of_n = gtk_label_new(NULL);
    hookup_widget(bubble, &bubble->n_of_n);
    gtk_widget_hide(bubble->n_of_n); /* override hookup_widget */        

    /* photo of person who sent last message */

    bubble->last_message_photo = g_object_new(GTK_TYPE_EVENT_BOX,
                                      "visible-window", FALSE,
                                      "above-child", TRUE,
                                      NULL);
    connect_link_action(bubble->last_message_photo,
                        LINK_CLICK_VISIT_LAST_MESSAGE_SENDER);

    widget = gtk_image_new();
    gtk_widget_show(widget);
    gtk_container_add(GTK_CONTAINER(bubble->last_message_photo), widget);
    hookup_widget(bubble, &bubble->last_message_photo);
    gtk_widget_hide(bubble->last_message_photo); /* override hookup_widget */

    /* Text of last chat message */
    bubble->last_message = gtk_label_new(NULL);
    hookup_widget(bubble, &bubble->last_message);
    gtk_widget_hide(bubble->last_message); /* override hookup_widget */        

    /* People who've looked at it */
    bubble->viewers = gtk_label_new(NULL);
    hookup_widget(bubble, &bubble->viewers);
    gtk_widget_hide(bubble->viewers); /* override hookup_widget */        
    
    /* World share total viewers */
    bubble->total_viewers = g_object_new(GTK_TYPE_EVENT_BOX,
                                         "visible-window", FALSE,
                                         "above-child", TRUE,
                                         NULL);
    hookup_widget(bubble, &bubble->total_viewers);
    gtk_widget_hide(bubble->total_viewers); /* override hookup_widget */     
    connect_link_action(bubble->total_viewers, LINK_CLICK_ACTIVATE_TOTAL_VIEWERS);    
    
    widget = gtk_label_new(NULL);
    gtk_container_add(GTK_CONTAINER(bubble->total_viewers), widget);
    gtk_widget_show(widget);    

    /* The "someone said" link */

    bubble->someone_said = g_object_new(GTK_TYPE_EVENT_BOX,
                                      "visible-window", FALSE,
                                      "above-child", TRUE,
                                      NULL);
    hookup_widget(bubble, &bubble->someone_said);
    gtk_widget_hide(bubble->someone_said); /* override hookup_widget */
    connect_link_action(bubble->someone_said, LINK_CLICK_ACTIVATE_SOMEONE_SAID);
        
    widget = gtk_label_new(NULL);
    gtk_container_add(GTK_CONTAINER(bubble->someone_said), widget);
    gtk_widget_show(widget);
    
    /* The "who's there" link */

    bubble->whos_there = g_object_new(GTK_TYPE_EVENT_BOX,
                                      "visible-window", FALSE,
                                      "above-child", TRUE,
                                      NULL);
    hookup_widget(bubble, &bubble->whos_there);
    gtk_widget_hide(bubble->whos_there); /* override hookup_widget */     
    connect_link_action(bubble->whos_there, LINK_CLICK_ACTIVATE_WHOS_THERE);

    widget = gtk_label_new(NULL);
    gtk_container_add(GTK_CONTAINER(bubble->whos_there), widget);
    gtk_widget_show(widget);
    
    /* The "Join chat" link */

    bubble->join_chat = g_object_new(GTK_TYPE_EVENT_BOX,
                                     "visible-window", FALSE,
                                     "above-child", TRUE,
                                     NULL);
    hookup_widget(bubble, &bubble->join_chat);
    connect_link_action(bubble->join_chat, LINK_CLICK_JOIN_CHAT);

    box = gtk_hbox_new(FALSE, 2);
    gtk_container_add(GTK_CONTAINER(bubble->join_chat), box);
    gtk_widget_show(box);
    
    widget = gtk_image_new_from_pixbuf(hippo_embedded_image_get("chaticon"));
    gtk_widget_show(widget);
    gtk_box_pack_start(GTK_BOX(box), widget, FALSE, FALSE, 0);
    
    bubble->join_chat_label = gtk_label_new(NULL);
    gtk_label_set_markup(GTK_LABEL(bubble->join_chat_label), "<u>Join chat</u>");
    set_blue_fg(bubble->join_chat_label);    
    gtk_box_pack_start(GTK_BOX(box), bubble->join_chat_label, FALSE, FALSE, 0);
    hookup_widget(bubble, &bubble->join_chat_label);    

    bubble->chat_count_label = gtk_label_new(NULL);
    gtk_box_pack_start(GTK_BOX(box), bubble->chat_count_label, FALSE, FALSE, 0);
    hookup_widget(bubble, &bubble->chat_count_label);
    
    /* The "Ignore" link */

    bubble->ignore = g_object_new(GTK_TYPE_EVENT_BOX,
                                  "visible-window", FALSE,
                                  "above-child", TRUE,
                                  NULL);
    hookup_widget(bubble, &bubble->ignore);
    connect_link_action(bubble->ignore, LINK_CLICK_IGNORE_POST);

    box = gtk_hbox_new(FALSE, 2);
    gtk_container_add(GTK_CONTAINER(bubble->ignore), box);
    gtk_widget_show(box);
    
    widget = gtk_image_new_from_pixbuf(hippo_embedded_image_get("ignoreicon"));
    gtk_widget_show(widget);
    gtk_box_pack_start(GTK_BOX(box), widget, FALSE, FALSE, 0);
    
    bubble->ignore_label = gtk_label_new(NULL);
    set_blue_fg(bubble->ignore_label);    
    gtk_box_pack_start(GTK_BOX(box), bubble->ignore_label, FALSE, FALSE, 0);
    hookup_widget(bubble, &bubble->ignore_label);   
    
    /* The "Invite to Group" link */

    bubble->invite = g_object_new(GTK_TYPE_EVENT_BOX,
                                  "visible-window", FALSE,
                                  "above-child", TRUE,
                                  NULL);
    hookup_widget(bubble, &bubble->invite);
    connect_link_action(bubble->invite, LINK_CLICK_INVITE);

    box = gtk_hbox_new(FALSE, 2);
    gtk_container_add(GTK_CONTAINER(bubble->invite), box);
    gtk_widget_show(box);
    
    widget = gtk_image_new_from_pixbuf(hippo_embedded_image_get("add"));
    gtk_widget_show(widget);
    gtk_box_pack_start(GTK_BOX(box), widget, FALSE, FALSE, 0);
    
    bubble->invite_label = gtk_label_new(NULL);
    gtk_label_set_markup(GTK_LABEL(bubble->invite_label), "<u>Invite</u>");
    set_blue_fg(bubble->invite_label);    
    gtk_box_pack_start(GTK_BOX(box), bubble->invite_label, FALSE, FALSE, 0);
    hookup_widget(bubble, &bubble->invite_label);         

    /* Get the right initial size request */
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

    g_free(bubble->sender_id);
    g_free(bubble->post_id);
    g_free(bubble->last_message_sender_id);

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

/* width of the area with the paging arrows */
#define PAGING_AREA_WIDTH 60

/* border_rect is the rect of our bubble border, not of the whole 
 * widget e.g. it wouldn't include GtkContainer::border-width
 * or the paging/extras areas outside the bubble
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
        /* "size allocate" mode: "base" is our entire size allocation 
         * for the widget. We want to subtract everything that isn't
         * the bubble to get down to border_rect
         */
        
        border_rect = *base;

        border_rect.width -= 2 * container->border_width;
        if (bubble->total_pages > 1)
            border_rect.width -= PAGING_AREA_WIDTH;        
        border_rect.height -= 2 * container->border_width;
    } else if (mode == BASE_IS_CONTENT_REQUISITION) {
        /* "size request" mode: "base" is the size request of the stuff 
         * inside the bubble area, i.e. the content_rect. We want to add
         * the bubble borders to get border_rect.
         */
            
        border_rect = *base;

        border_rect.width += left_edge_width + right_edge_width;
        border_rect.height += top_edge_height + bottom_edge_height;
    } else {
        g_assert_not_reached();
    }

    border_rect.x += container->border_width;
    border_rect.y += container->border_width;
    if (bubble->total_pages > 1) {
        border_rect.x += PAGING_AREA_WIDTH;
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

static GdkPixbuf *
lookup_corner(HippoBubbleColor color, const char *suffix)
{
	GdkPixbuf *pixbuf;
	char *filename = g_strdup_printf("%c%s", color == HIPPO_BUBBLE_COLOR_ORANGE ? 'o' : 'p', suffix);
    pixbuf = hippo_embedded_image_get(filename);
    g_free(filename);
    return pixbuf;
}

static gboolean
hippo_bubble_expose_event(GtkWidget      *widget,
            		      GdkEventExpose *event)
{    
    HippoBubble *bubble;
    GdkGC *gc;
    GdkPixbuf *pixbuf;
    GdkRectangle bubble_allocation;
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
    
    bubble = HIPPO_BUBBLE(widget);
    
    child_clip = event->area;
    
    bubble_allocation = widget->allocation;
    bubble_allocation.height -= bubble->extra_widgets_height_last_allocation;
    compute_layout(widget, &bubble_allocation,
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

    /* IT'S ORANGE (or purple) BABY */
    gdk_rgb_gc_set_foreground(gc, bubble->color == HIPPO_BUBBLE_COLOR_ORANGE ? 0xF16D1C : 0xAA86B6);
    gdk_draw_rectangle(widget->window, gc, TRUE,
                        content_rect.x, content_rect.y,
                        content_rect.width, content_rect.height);

    /* now stamp the little pixmap pieces all over */    
	pixbuf = lookup_corner(bubble->color, "bubcnr_tr");	
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_NORTH_EAST,
                border_rect.x + border_rect.width,
                border_rect.y);
    
    pixbuf = lookup_corner(bubble->color, "bubcnr_br");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_SOUTH_EAST,
                border_rect.x + border_rect.width,
                border_rect.y + border_rect.height);
    
    pixbuf = lookup_corner(bubble->color, "bubcnr_bl");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_SOUTH_WEST,
                border_rect.x,
                border_rect.y + border_rect.height);
                                
    pixbuf = lookup_corner(bubble->color, "bubcnr_tl");
    draw_pixbuf(widget, gc, pixbuf, GDK_GRAVITY_NORTH_WEST,
                border_rect.x,
                border_rect.y);
                
    pixbuf = lookup_corner(bubble->color, "bubedge_b");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_SOUTH,
                &border_clip, &bottom_edge_rect);

    pixbuf = lookup_corner(bubble->color, "bubedge_t");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_NORTH,
                &border_clip, &top_edge_rect);

    pixbuf = lookup_corner(bubble->color, "bubedge_l");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_WEST,
                &border_clip, &left_edge_rect);

    pixbuf = lookup_corner(bubble->color, "bubedge_r");
    tile_pixbuf(widget, gc, pixbuf, GDK_WINDOW_EDGE_EAST,
                &border_clip, &right_edge_rect);

    /* tough to set child_clip without messing up the drawing */
    GTK_WIDGET_CLASS(hippo_bubble_parent_class)->expose_event(widget, event);

    g_object_unref(gc);
    
    return FALSE;
}

static void
hippo_bubble_map(GtkWidget *widget)
{
    HippoBubble *bubble;
    
    bubble = HIPPO_BUBBLE(widget);
    
    GTK_WIDGET_CLASS(hippo_bubble_parent_class)->map(widget); 

    /* OK, this is probably a questionable location for this... */    
    set_label_sizes(HIPPO_BUBBLE(widget));
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
                               GdkRectangle *header_logo_p,
                               GdkRectangle *link_title_p,
                               GdkRectangle *link_description_p)
{
    GdkRectangle sender_photo;
    GdkRectangle sender_name;
    GdkRectangle header_logo;
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
    
    /* web swarm logo aligned top with photo */
    GET_REQ(header_logo);
    header_logo.x = sender_photo.x + sender_photo.width + 10;
    header_logo.y = sender_photo.y;

    GET_REQ(link_title);
    link_title.x = header_logo.x;
    link_title.y = header_logo.y + header_logo.height + 10;
    
    GET_REQ(link_description);
    link_description.x = link_title.x;
    link_description.y = link_title.y + link_title.height + 5;
        
#define OUT(what) do { if (what ## _p) { * what ## _p = what; }  } while(0)
    OUT(sender_photo);
    OUT(sender_name);
    OUT(header_logo);
    OUT(link_title);
    OUT(link_description);
}

/* this computes with respect to coordinates 0,0 */
static void
compute_extra_widgets_layout(HippoBubble  *bubble,
                             GdkRectangle *total_viewers_p,
                             GdkRectangle *whos_there_p,
                             GdkRectangle *someone_said_p,
                             GdkRectangle *join_chat_p,
                             GdkRectangle *ignore_p,
                             GdkRectangle *invite_p,                             
                             GdkRectangle *last_message_photo_p,
                             GdkRectangle *last_message_p,
                             GdkRectangle *viewers_p,
                             GdkRectangle *all_extra_widgets_p)
{
    GdkRectangle total_viewers;
    GdkRectangle whos_there;
    GdkRectangle someone_said;
    GdkRectangle join_chat;
    GdkRectangle ignore;
    GdkRectangle invite;    
    GdkRectangle last_message_photo;
    GdkRectangle last_message;
    GdkRectangle viewers;
    GdkRectangle all_extra_widgets;
    int total_width;
    int message_pane_width;
    int message_pane_height;
    int links_width = -1;
    int links_height;
    int panes_height;
    int panes_y;
    
    /* First get all the width, height */

    GET_REQ(total_viewers);      
    GET_REQ(whos_there);    
    GET_REQ(someone_said);
    GET_REQ(join_chat);
    GET_REQ(ignore);
    GET_REQ(invite);    
    GET_REQ(last_message_photo);
    GET_REQ(last_message);
    GET_REQ(viewers);

#define PADDING 4
#define BETWEEN_LINKS 6
#define BETWEEN_PHOTO_AND_MESSAGE 10

    if (bubble->someone_said_set) {    
        message_pane_width = last_message_photo.width + BETWEEN_PHOTO_AND_MESSAGE + last_message.width;
        message_pane_height = MAX(last_message_photo.height, last_message.height);
    } else {
        message_pane_width = 0;
        message_pane_height = 0;
    }
    
    if (bubble->total_viewers_set) {
        // Currently this is exclusive with whos_there and someone_said
    	links_width = total_viewers.width;
    	links_height = total_viewers.height;
    	panes_height = viewers.height;
    } else if (bubble->whos_there_set && bubble->someone_said_set) {
        links_width = whos_there.width + BETWEEN_LINKS + someone_said.width;
        links_height = MAX(whos_there.height, someone_said.height);
        panes_height = MAX(message_pane_height, viewers.height);        
    } else if (bubble->whos_there_set) {
        links_width = whos_there.width;
        links_height = whos_there.height;
        panes_height = viewers.height;
    } else if (bubble->someone_said_set) {
        links_width = someone_said.width;
        links_height = someone_said.height;
        panes_height = message_pane_height;
    } else {
        links_width = 0;
        links_height = 0;
        panes_height = 0;
    }

    total_width = links_width + PADDING + join_chat.width + BETWEEN_LINKS + ignore.width + BETWEEN_LINKS + invite.width;
    if (bubble->someone_said_set)
        total_width = MAX(total_width, message_pane_width);
    if (bubble->whos_there_set || bubble->total_viewers_set)
        total_width = MAX(total_width, viewers.width);
    total_width += PADDING * 2;

    /* left-align the "notebook tabs", note 
     * that if one is !set then it is invisible anyhow but 
     * important to be sure it doesn't "grow" the area
     */
    whos_there.y = PADDING;
    someone_said.y = PADDING;
    total_viewers.y = PADDING;

    whos_there.x = 0;
    someone_said.x = 0;
    total_viewers.x = 0;

	if (bubble->total_viewers_set) {
	    total_viewers.x = PADDING;
    } else if (bubble->whos_there_set && bubble->someone_said_set) {
        whos_there.x = PADDING;
        someone_said.x = whos_there.x + whos_there.width + BETWEEN_LINKS;
    } else if (bubble->whos_there_set) {
        whos_there.x = PADDING;
        someone_said.x = 0; /* irrelevant */
    } else if (bubble->someone_said_set) {
        someone_said.x = PADDING;
        whos_there.x = 0; /* irrelevant */
    }

    /* Right-align the "join chat" and "ignore" */
    join_chat.y = PADDING;
    ignore.y = PADDING;
    invite.y = PADDING;
    ignore.x = total_width - PADDING - ignore.width;
    join_chat.x = ignore.x - BETWEEN_LINKS - join_chat.width;
    invite.x = join_chat.x - BETWEEN_LINKS - invite.width;

    panes_y = PADDING + links_height + PADDING;

    /* left-align the "tab panes" in their area */
    last_message_photo.x = PADDING;
    last_message.x = last_message_photo.x + last_message_photo.width + BETWEEN_PHOTO_AND_MESSAGE;
    /* and center vertically */
    last_message_photo.y = panes_y + (panes_height - message_pane_height) / 2;
    last_message.y = last_message_photo.y;
    
    viewers.x = PADDING;
    viewers.y = panes_y + (panes_height - viewers.height) / 2;

    if (!(bubble->whos_there_set || bubble->someone_said_set || bubble->total_viewers_set)) {
        all_extra_widgets.x = 0;
        all_extra_widgets.y = 0;
        all_extra_widgets.width = total_width;
        all_extra_widgets.height = 0;
    } else {
        all_extra_widgets.x = 0;
        all_extra_widgets.y = 0;
        all_extra_widgets.width = total_width;
        all_extra_widgets.height = panes_y + panes_height;
    }
    gdk_rectangle_union(&all_extra_widgets, &join_chat, &all_extra_widgets);
    gdk_rectangle_union(&all_extra_widgets, &ignore, &all_extra_widgets);
    gdk_rectangle_union(&all_extra_widgets, &invite, &all_extra_widgets);    
    
    all_extra_widgets.height += PADDING;
    
    OUT(total_viewers);    
    OUT(whos_there);
    OUT(someone_said);
    OUT(join_chat);
    OUT(ignore);
    OUT(invite);    
    OUT(last_message_photo);
    OUT(last_message);
    OUT(viewers);
    OUT(all_extra_widgets);
}
#undef OUT
#undef GET_REQ

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
    GdkRectangle header_logo_rect;
    GdkRectangle link_title_rect;
    GdkRectangle link_description_rect;
    GdkRectangle recipients_rect;
    GdkRectangle border_rect;
    GdkRectangle offset_content_rect;
    GdkRectangle close_event_box_rect;
    GdkRectangle all_extra_widgets_rect;    
    int xoffset;
    int yoffset;
    int right_clearance;
    
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
                   &header_logo_rect, &link_title_rect, &link_description_rect);
    
    /* compute union of these rects */
    content_child_rect.x = 0;
    content_child_rect.y = 0;
    content_child_rect.width = 0;
    content_child_rect.height = 0;
    
    gdk_rectangle_union(&content_child_rect, &sender_photo_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &sender_name_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &header_logo_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &link_title_rect, &content_child_rect);
    gdk_rectangle_union(&content_child_rect, &link_description_rect, &content_child_rect);
    
    /* have to special case the recipients thing */
    gtk_widget_get_child_requisition(bubble->recipients, &req);
    recipients_rect.width = req.width;
    recipients_rect.height = req.height;
    recipients_rect.x = content_child_rect.x + content_child_rect.width - recipients_rect.width - 10;
    recipients_rect.y = content_child_rect.y + content_child_rect.height + 10;
    
    gdk_rectangle_union(&content_child_rect, &recipients_rect, &content_child_rect);

    /* if the WEB SWARM is the longest thing, it tends to overlap the close button; 
     * so add a little padding then
     */
    right_clearance = (content_child_rect.x + content_child_rect.width) - 
                      (header_logo_rect.x + header_logo_rect.width);
    if (right_clearance < 20) {
        content_child_rect.width += (20 - right_clearance);
    }
    
    /* see what other stuff goes around the content widgets */
    compute_layout(widget, &content_child_rect, BASE_IS_CONTENT_REQUISITION, &border_rect,
                   &offset_content_rect, NULL, NULL, NULL, NULL, &close_event_box_rect);

    /* get offset to the gtk_fixed_put() child positions,
     * which does not include container->border_width, but does include any 
     * other offsets that compute_layout took into account.
     */
    xoffset = offset_content_rect.x - container->border_width;
    yoffset = offset_content_rect.y - container->border_width;
        
    /* side effect, we update where GtkFixed will display widgets on expose,
     * and help it gets its size allocate right
     */
#define OFFSET(what) do {                                      \
        fixed_move_no_queue_resize(bubble, bubble->what,       \
            what##_rect.x + xoffset, what##_rect.y + yoffset); \
    } while(0)
    
    OFFSET(sender_photo);
    OFFSET(sender_name);
    OFFSET(header_logo);
    OFFSET(link_title);
    OFFSET(link_description);
    OFFSET(close_event_box);
    OFFSET(recipients);
#undef OFFSET    

    /* Now we need to lay out the area below the bubble (the whosthere/someonesaid extra info).
     * compute_extra_widgets computes the extra area based at the origin (0,0)
     */
     
    compute_extra_widgets_layout(bubble, NULL, NULL, NULL,
                                 NULL, NULL, NULL,
                                 NULL, NULL, NULL,
                                 &all_extra_widgets_rect);
    all_extra_widgets_rect.x += container->border_width;
    if (bubble->total_pages > 1)
        all_extra_widgets_rect.x += PAGING_AREA_WIDTH;
    all_extra_widgets_rect.y += border_rect.y + border_rect.height;

    /* don't bother setting the gtk_fixed_put on the extra widgets, we have to dynamically
     * adjust them in size_allocate to stick them to the bottom of the allocation.
     */

    /* Now we know our requisition. border_rect has border_width and the "paging area" offsets
     * in its x,y while all_extra_widgets does not.
     */
    requisition->width = MAX(border_rect.x + border_rect.width,
                        all_extra_widgets_rect.x + all_extra_widgets_rect.width);
    requisition->height = all_extra_widgets_rect.y + all_extra_widgets_rect.height;

    if (requisition->width < MIN_BUBBLE_WIDTH)
        requisition->width = MIN_BUBBLE_WIDTH;

#if 0
    g_print("Size req on %p, %d x %d; border_rect %d,%d %dx%d\n"
            "  offset_content_rect %d,%d %dx%d\n"
            "  content_child_rect  %d,%d %dx%d\n",
        bubble, requisition->width, requisition->height,
            border_rect.x, border_rect.y, border_rect.width, border_rect.height,
            offset_content_rect.x, offset_content_rect.y, offset_content_rect.width, offset_content_rect.height,
            content_child_rect.x, content_child_rect.y, content_child_rect.width, content_child_rect.height);
#endif
}

static void
hippo_bubble_size_allocate(GtkWidget         *widget,
            	       	   GtkAllocation     *allocation)
{
    /* We make no real effort to handle getting a too-small allocation... if you're using 
     * a busted window manager that ignores hints, we take patches.
     */
    HippoBubble *bubble;
    GtkContainer *container;
    GdkRectangle bubble_allocation;    
    GdkRectangle border_rect;
    GdkRectangle content_rect;
    GdkRectangle close_event_box_rect;
    GtkRequisition requisition;
    GdkRectangle recipients_rect;
    GdkRectangle left_arrow_rect;
    GdkRectangle right_arrow_rect;
    GdkRectangle n_of_n_rect;
    int both_arrows_width;
    GdkRectangle total_viewers_rect;    
    GdkRectangle whos_there_rect;
    GdkRectangle someone_said_rect;
    GdkRectangle join_chat_rect;
    GdkRectangle ignore_rect;
    GdkRectangle invite_rect;    
    GdkRectangle last_message_photo_rect;
    GdkRectangle last_message_rect;
    GdkRectangle viewers_rect;
    GdkRectangle all_extra_widgets_rect;
    int xoffset_left, xoffset_right, yoffset;
    int extra_widgets_width_allocation;

    bubble = HIPPO_BUBBLE(widget);
    container = GTK_CONTAINER(widget);

    /* Give every widget its size request and GtkFixed position, computed
     * at size request time
     */
    GTK_WIDGET_CLASS(hippo_bubble_parent_class)->size_allocate(widget, allocation);
    
    /* Now change our mind on some of them ... */
    
    compute_extra_widgets_layout(bubble, 
                                 &total_viewers_rect,
                                 &whos_there_rect,
                                 &someone_said_rect, 
                                 &join_chat_rect,
                                 &ignore_rect,
                                 &invite_rect,                                 
                                 &last_message_photo_rect,
                                 &last_message_rect,
                                 &viewers_rect,
                                 &all_extra_widgets_rect);

    /* used in expose_event and below in this function */
    bubble->extra_widgets_height_last_allocation = all_extra_widgets_rect.height;
    
    bubble_allocation = *allocation;
    bubble_allocation.height -= bubble->extra_widgets_height_last_allocation;
    compute_layout(widget, &bubble_allocation,
                   BASE_IS_WIDGET_ALLOCATION,
                   &border_rect, &content_rect, 
                   NULL, NULL, NULL, NULL,
                   &close_event_box_rect);
    gtk_widget_size_allocate(bubble->close_event_box, &close_event_box_rect);
    
    /* Move recipients out to the corner,
     * if we got a larger allocation than we wanted 
     */
    gtk_widget_get_child_requisition(bubble->recipients, &requisition);
    recipients_rect.width = requisition.width;
    recipients_rect.height = requisition.height;
    recipients_rect.x = content_rect.x + content_rect.width - recipients_rect.width;
    recipients_rect.y = content_rect.y + content_rect.height - recipients_rect.height;
    gtk_widget_size_allocate(bubble->recipients, &recipients_rect);

    /* put the paging stuff on the bottom of paging area. */
    gtk_widget_get_child_requisition(bubble->left_arrow, &requisition);
    left_arrow_rect.width = requisition.width;
    left_arrow_rect.height = requisition.height;

    gtk_widget_get_child_requisition(bubble->right_arrow, &requisition);
    right_arrow_rect.width = requisition.width;
    right_arrow_rect.height = requisition.height;
    
    gtk_widget_get_child_requisition(bubble->n_of_n, &requisition);
    n_of_n_rect.width = requisition.width;
    n_of_n_rect.height = requisition.height;

    both_arrows_width = MIN(PAGING_AREA_WIDTH, right_arrow_rect.width + left_arrow_rect.width + 10);

    /* center the arrows */
    left_arrow_rect.x = container->border_width + (PAGING_AREA_WIDTH - both_arrows_width) / 2;
    left_arrow_rect.y = allocation->height - 10 - left_arrow_rect.height;
    right_arrow_rect.x = left_arrow_rect.x + left_arrow_rect.width + 10;
    right_arrow_rect.y = allocation->height - 10 - right_arrow_rect.height;

    /* Label has a size_request set of the whole PAGING_AREA_WIDTH, so we put it at border_width, 
     * and it centers self horizontally; we just need to set it vertically.
     */
    n_of_n_rect.x = container->border_width;
    n_of_n_rect.y = MIN(left_arrow_rect.y, right_arrow_rect.y) - 10 - n_of_n_rect.height;
    
    /* After doing all paging area layout, grow the event boxes on the arrows
     * so there's a larger hit target. 
     * This should not change the visuals, only the input-only event box window. GtkImage is a
     * GtkMisc so will center itself in the expanded event box. The padding between the 
     * two arrows is 10, so if changing this avoid overlapping the two boxes.
     */
    left_arrow_rect.x -= 5;
    left_arrow_rect.width += 10;
    left_arrow_rect.y -= 5;
    left_arrow_rect.height += 10;
    right_arrow_rect.x -= 5;
    right_arrow_rect.width += 10;
    right_arrow_rect.y -= 5;
    right_arrow_rect.height += 10;
    
    gtk_widget_size_allocate(bubble->left_arrow, &left_arrow_rect);
    gtk_widget_size_allocate(bubble->right_arrow, &right_arrow_rect);
    gtk_widget_size_allocate(bubble->n_of_n, &n_of_n_rect);
    
    /* Extra widgets area - we didn't gtk_fixed_put any of these
     * since we want to stick them to the bottom of the allocation.
     */
    extra_widgets_width_allocation = allocation->width - container->border_width * 2;
    if (bubble->total_pages > 1) {
        extra_widgets_width_allocation -= PAGING_AREA_WIDTH;
        xoffset_left = xoffset_right = PAGING_AREA_WIDTH;
    } else {
        xoffset_left = xoffset_right = 0;
    }
    xoffset_right += MAX(0, (extra_widgets_width_allocation - all_extra_widgets_rect.width));

    /* glue to bottom */
    yoffset = (allocation->height - all_extra_widgets_rect.height);
#define ALLOC_LEFT(what) do {                                  \
        what##_rect.x += xoffset_left;                         \
        what##_rect.y += yoffset;                              \
        gtk_widget_size_allocate(bubble->what, &what##_rect);  \
    } while(0)
#define ALLOC_RIGHT(what) do {                                 \
        what##_rect.x += xoffset_right;                        \
        what##_rect.y += yoffset;                              \
        gtk_widget_size_allocate(bubble->what, &what##_rect);  \
    } while(0)

    ALLOC_LEFT(total_viewers);    
    ALLOC_LEFT(whos_there);
    ALLOC_LEFT(someone_said);
    ALLOC_RIGHT(join_chat);
    ALLOC_RIGHT(ignore);   
    ALLOC_RIGHT(invite);      
    ALLOC_LEFT(last_message_photo);
    ALLOC_LEFT(last_message);
    ALLOC_LEFT(viewers);

#undef ALLOC_LEFT
#undef ALLOC_RIGHT
}

void
hippo_bubble_set_header_image(HippoBubble  *bubble,
                              const char   *img_name)
{
	GdkPixbuf *pixbuf;
    pixbuf = hippo_embedded_image_get(img_name);
    gtk_image_set_from_pixbuf(GTK_IMAGE(bubble->header_logo), pixbuf);
}    

void
hippo_bubble_set_foreground_color(HippoBubble        *bubble,
                                  HippoBubbleColor    color)
{
	bubble->color = color;
}                                 

void
hippo_bubble_set_actions (HippoBubble *bubble, 
                          int          actions)
{
	bubble->actions = actions;
	if (bubble->actions & HIPPO_BUBBLE_ACTION_JOIN_CHAT)
		gtk_widget_show(bubble->join_chat);
	else
		gtk_widget_hide(bubble->join_chat);
	if (bubble->actions & HIPPO_BUBBLE_ACTION_IGNORE)
		gtk_widget_show(bubble->ignore);
	else
		gtk_widget_hide(bubble->ignore);
	if (bubble->actions & HIPPO_BUBBLE_ACTION_INVITE)
		gtk_widget_show(bubble->invite);
	else
		gtk_widget_hide(bubble->invite);			
}

void
hippo_bubble_set_sender_guid(HippoBubble *bubble,
                             const char  *value)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    
    if (bubble->sender_id != value) {
        g_free(bubble->sender_id);
        bubble->sender_id = g_strdup(value);
    }
}

void
hippo_bubble_set_post_guid(HippoBubble *bubble,
                           const char  *value)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));

    if (bubble->post_id != value) {
        g_free(bubble->post_id);
        bubble->post_id = g_strdup(value);
    }
    g_free(bubble->group_id);
    bubble->group_id = NULL;
}

void
hippo_bubble_set_group_guid(HippoBubble *bubble,
                            const char  *value)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));

    if (bubble->group_id != value) {
        g_free(bubble->group_id);
        bubble->group_id = g_strdup(value);
    }
    g_free(bubble->post_id);
    bubble->post_id = NULL;    
}

/* Set the id of an entity associated with this bubble.
   Presently used to keep track of the user to invite
   for group membership changes.
 */
void
hippo_bubble_set_assoc_guid(HippoBubble *bubble,
                            const char  *value)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));

    g_free(bubble->assoc_id);
    bubble->assoc_id = g_strdup(value);    
}
                           
void
hippo_bubble_set_sender_name(HippoBubble *bubble, 
                             const char  *value)
{
    char *s;
    GtkWidget *label;

    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    
    label = GTK_BIN(bubble->sender_name)->child;
    
    s = g_markup_printf_escaped("<u>%s</u>", value);
    gtk_label_set_markup(GTK_LABEL(label), s);
    g_free(s);
    
    set_label_sizes(bubble);
}

void
hippo_bubble_set_sender_photo(HippoBubble *bubble, 
                              GdkPixbuf   *pixbuf)
{
    GtkWidget *image;

    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    
    image = GTK_BIN(bubble->sender_photo)->child;
    gtk_image_set_from_pixbuf(GTK_IMAGE(image), pixbuf);
}

void
hippo_bubble_set_link_title(HippoBubble *bubble, 
                            const char  *title)
{
    GtkWidget *label;
    char *s;

    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    
    label = GTK_BIN(bubble->link_title)->child;    
    
    s = g_markup_printf_escaped("<u>%s</u>", title);
    gtk_label_set_markup(GTK_LABEL(label), s);
    g_free(s);
    set_label_sizes(bubble);    
}
       
void
hippo_bubble_set_link_description(HippoBubble *bubble, 
                                  const char  *value)
{
    GtkWidget *label;
    char *s;
    
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));    
    
    label = bubble->link_description;
    
    s = g_markup_printf_escaped("<small>%s</small>", value);
    gtk_label_set_markup(GTK_LABEL(label), s);
    g_free(s);
    set_label_sizes(bubble);
}
                                  
void
hippo_bubble_set_recipients(HippoBubble *bubble, 
                            const HippoRecipientInfo *recipients,
                            int          n_recipients)
{
    int i;
    GString *gstr;
    char *s;
    GtkWidget *label;
    
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
        
    /* though we don't use it right now, note that the guid in the 
     * HippoRecipientInfo is NULL for "the world"
     */
        
    label = bubble->recipients;
    
    gstr = g_string_new(NULL);
    
    for (i = 0; i < n_recipients; ++i) {
        g_string_append(gstr, recipients[i].name);
        if ((i + 1) != n_recipients)
            g_string_append(gstr, ", ");
    }
 
    s = g_markup_printf_escaped(_("Sent to %s"), gstr->str);
    gtk_label_set_markup(GTK_LABEL(label), s);
    
    g_free(s);
    g_string_free(gstr, TRUE);
    
    set_label_sizes(bubble);
}

static void
update_extra_info(HippoBubble *bubble)
{
    gboolean total_viewers_active;
    gboolean whos_there_active;
    gboolean someone_said_active;    
    gboolean membership_change_active;    
    
    total_viewers_active = bubble->active_extra == ACTIVE_EXTRA_TOTAL_VIEWERS;
    whos_there_active = (bubble->whos_there_set &&
                         !total_viewers_active &&
                         !bubble->someone_said_set) ||
                        (bubble->whos_there_set &&
                         bubble->active_extra == ACTIVE_EXTRA_WHOS_THERE);
    membership_change_active = (bubble->someone_said_set && bubble->active_extra == ACTIVE_EXTRA_MEMBERSHIP_CHANGE);
    someone_said_active = (bubble->someone_said_set && bubble->active_extra == ACTIVE_EXTRA_SOMEONE_SAID);     
     
    if (bubble->whos_there_set && !total_viewers_active) {
        GtkWidget *label = GTK_BIN(bubble->whos_there)->child;
        if (whos_there_active) {
            gtk_label_set_markup(GTK_LABEL(label), _("<b>Who's there</b>"));
            set_default_fg(label);
        } else {
            gtk_label_set_markup(GTK_LABEL(label),
                                 _("<u>Who's there</u>"));
            set_blue_fg(label);
        }
        gtk_widget_show(bubble->whos_there);
    } else {
        gtk_widget_hide(bubble->whos_there);    
    }

    if (bubble->someone_said_set && !total_viewers_active) {
        GtkWidget *label = GTK_BIN(bubble->someone_said)->child;
        if (someone_said_active) {
            gtk_label_set_markup(GTK_LABEL(label), _("<b>Latest comment</b>"));
            set_default_fg(label);
        } else if (membership_change_active) {
        	char *markup;
        	char *markup_content;
        	markup_content = g_markup_escape_text(bubble->user_link_header, -1);
        	markup = g_strdup_printf("<b>%s</b>", markup_content);
        	g_free(markup_content);
        	
            gtk_label_set_markup(GTK_LABEL(label), markup);
            set_default_fg(label);       
        } else {
            gtk_label_set_markup(GTK_LABEL(label),
                                 _("<u>Latest comment</u>"));
            set_blue_fg(label);
        }
        gtk_widget_show(bubble->someone_said);
    } else {
        gtk_widget_hide(bubble->someone_said);
    }
    
    if (total_viewers_active) {
	    GtkWidget *label = GTK_BIN(bubble->total_viewers)->child;
        gtk_label_set_markup(GTK_LABEL(label), _("<b>Viewers</b>"));
        gtk_widget_show(bubble->total_viewers);
    } else {
    	gtk_widget_hide(bubble->total_viewers);
    }

    if (whos_there_active || total_viewers_active) {
        gtk_widget_show(bubble->viewers);
    } else {
        gtk_widget_hide(bubble->viewers);
    }
        
    if (someone_said_active || membership_change_active) {
        gtk_widget_show(bubble->last_message);
        gtk_widget_show(bubble->last_message_photo);
    } else {
        gtk_widget_hide(bubble->last_message);
        gtk_widget_hide(bubble->last_message_photo);    
    }
}

void
hippo_bubble_set_viewers(HippoBubble *bubble,
                         const HippoViewerInfo *viewers,
                         int          n_viewers)
{
    GtkWidget *label;
    
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
        
    label = bubble->viewers;
    
    if (n_viewers > 0) {
        int i;
        GString *gstr;
    
        gstr = g_string_new(NULL);
        
        for (i = 0; i < n_viewers; ++i) {
            const char *format;
            char *s;
            if (viewers[i].chatting) {
                format = "<b>%s</b>";
            } else {
                format = "%s";
            }
            s = g_markup_printf_escaped(format, viewers[i].name);
            g_string_append_printf(gstr, s);
            g_free(s);
            
            if ((i + 1) != n_viewers)
                g_string_append(gstr, ", ");
        }
     
        gtk_label_set_markup(GTK_LABEL(label), gstr->str);
        
        g_string_free(gstr, TRUE);
        
        bubble->whos_there_set = TRUE;
    } else {
        gtk_label_set_text(GTK_LABEL(label), "");
        bubble->whos_there_set = FALSE;
    }    

    set_label_sizes(bubble);
    
    update_extra_info(bubble);
}

void
hippo_bubble_set_total_viewers(HippoBubble *bubble,
                               int          n_viewers)
{
	if (n_viewers > 0) {
	    char *fmt_single;
    	char *fmt_plural;
    
	    fmt_single = g_strdup_printf(_("%d person has viewed this share"), n_viewers);
    	fmt_plural = g_strdup_printf(_("%d people have viewed this share"), n_viewers);
    
	    gtk_label_set_text(GTK_LABEL(bubble->viewers), ngettext(fmt_single, fmt_plural, n_viewers));
    
    	g_free(fmt_single);
	    g_free(fmt_plural);
    
    	bubble->total_viewers_set = TRUE; 
    } else {
        gtk_label_set_text(GTK_LABEL(bubble->viewers), "");    
    	bubble->total_viewers_set = FALSE;
    }

    set_label_sizes(bubble);    
    update_extra_info(bubble);
}

static void
set_last_chat_message(HippoBubble *bubble,
                      const char  *message,
                      const char  *sender_id,
                      gboolean     quote)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    
    if (sender_id != bubble->last_message_sender_id) {
        g_free(bubble->last_message_sender_id);
        bubble->last_message_sender_id = g_strdup(sender_id);
    }

    if (message) {
        char *s;
        if (quote)
	        s = g_strdup_printf("\"%s\"", message);
	    else
	    	s = g_strdup(message);
        gtk_label_set_text(GTK_LABEL(bubble->last_message), s);
        g_free(s);
        bubble->someone_said_set = TRUE;
    } else {
        gtk_label_set_text(GTK_LABEL(bubble->last_message), "");
        bubble->someone_said_set = FALSE;
    }
    set_label_sizes(bubble);
    update_extra_info(bubble);
}

void
hippo_bubble_set_last_chat_message(HippoBubble *bubble,
                                   const char  *message,
                                   const char  *sender_id)
{
	set_last_chat_message(bubble, message, sender_id, TRUE);
}

void
hippo_bubble_set_swarm_user_link(HippoBubble *bubble,
								 const char  *header,
                                 const char  *text,
                                 const char  *user_id)
{
	g_free(bubble->user_link_header);
	bubble->user_link_header = g_strdup(header);
	set_last_chat_message(bubble, text, user_id, FALSE);
}

void
hippo_bubble_set_swarm_photo(HippoBubble *bubble,
                                 GdkPixbuf   *pixbuf)
{
    GtkWidget *image;
    GdkPixbuf *scaled;
    
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));

    image = GTK_BIN(bubble->last_message_photo)->child;

    if (pixbuf == NULL) {
        scaled = NULL;
    } else {
        /* halve it for our normal size (60) */
        scaled = gdk_pixbuf_scale_simple(pixbuf, 30, 30, GDK_INTERP_BILINEAR);
    }

    gtk_image_set_from_pixbuf(GTK_IMAGE(image), scaled);
    
    if (scaled)
        g_object_unref(scaled);

    update_extra_info(bubble);        
}

void
hippo_bubble_set_chat_count(HippoBubble *bubble,
                            int          count)
{
    char *s;
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    
    s = g_strdup_printf("[%d]", count);    
    gtk_label_set_text(GTK_LABEL(bubble->chat_count_label), s);
    g_free(s);
}

static void
set_arrow(GtkWidget  *event_box,
          const char *image_name)
{
    GtkWidget *image;
    GdkPixbuf *pixbuf;

    image = GTK_BIN(event_box)->child;
    pixbuf = hippo_embedded_image_get(image_name);
    g_return_if_fail(pixbuf != NULL);
    gtk_image_set_from_pixbuf(GTK_IMAGE(image), pixbuf);
}
                                 
void
hippo_bubble_set_page_n_of_total(HippoBubble *bubble,
                                 int          n,
                                 int          total)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));
    g_return_if_fail(total > 0);    
    g_return_if_fail(n < total);
    
    if (bubble->page == n && bubble->total_pages == total)
        return;
        
    bubble->page = n;
    bubble->total_pages = total;
    
    /* Update widgets */
    if (total == 1) {
        gtk_widget_hide(bubble->n_of_n);
        gtk_widget_hide(bubble->left_arrow);
        gtk_widget_hide(bubble->right_arrow);
    } else {
        char *s;
    
        s = g_strdup_printf("%d of %d", n + 1, total);
        gtk_label_set_text(GTK_LABEL(bubble->n_of_n), s);
        g_free(s);

        /* FIXME we could also drop the hand cursor off the 
         * insensitive ones, would be nicer
         */
        if (n == 0) {
            set_arrow(bubble->left_arrow, "grayleftarrow");
        } else {
            set_arrow(bubble->left_arrow, "blueleftarrow");
        }
        if ((n + 1) == total) {
            set_arrow(bubble->right_arrow, "grayrightarrow");
        } else {
            set_arrow(bubble->right_arrow, "bluerightarrow");
        }

        /* label will center in the paging area */
        gtk_widget_set_size_request(bubble->n_of_n, PAGING_AREA_WIDTH, -1);
        
        /* rest of positioning these widgets is in size_request / size_allocate */                      

        gtk_widget_show(bubble->n_of_n);
        gtk_widget_show(bubble->left_arrow);
        gtk_widget_show(bubble->right_arrow);    
    }
}

void
hippo_bubble_set_ignore_text(HippoBubble *bubble,
                             const char  *link_text,
                             const char  *details_text)
{
	char *markup = g_markup_printf_escaped("<u>%s</u>%s", link_text, details_text);
    gtk_label_set_markup(GTK_LABEL(bubble->ignore_label), markup);
	g_free(markup);
}                             

void
hippo_bubble_notify_reason(HippoBubble      *bubble,
                           HippoBubbleReason reason)
{
    g_return_if_fail(HIPPO_IS_BUBBLE(bubble));

    switch (reason) {
    case HIPPO_BUBBLE_REASON_CHAT:
        bubble->active_extra = ACTIVE_EXTRA_SOMEONE_SAID;
        update_extra_info(bubble);
        break;
    case HIPPO_BUBBLE_REASON_VIEWER:
        bubble->active_extra = ACTIVE_EXTRA_WHOS_THERE;
        update_extra_info(bubble);
        break;
    case HIPPO_BUBBLE_REASON_MEMBERSHIP_CHANGE:
        bubble->active_extra = ACTIVE_EXTRA_MEMBERSHIP_CHANGE;
        update_extra_info(bubble);
        break;
    case HIPPO_BUBBLE_REASON_ACTIVITY:
    	bubble->active_extra = ACTIVE_EXTRA_TOTAL_VIEWERS;
    	update_extra_info(bubble);
    	break;
    case HIPPO_BUBBLE_REASON_NEW:
        break;
    }
}

static void
hippo_bubble_link_click_action(HippoBubble       *bubble,
                               LinkClickAction    action)
{
    switch (action) {
    case LINK_CLICK_VISIT_SENDER:
        if (bubble->sender_id)
            hippo_app_visit_entity_id(hippo_get_app(), bubble->sender_id);
        break;
    case LINK_CLICK_VISIT_POST:
        if (bubble->post_id)
            hippo_app_visit_post_id(hippo_get_app(), bubble->post_id);
        else if (bubble->group_id)
        	hippo_app_visit_entity_id(hippo_get_app(), bubble->group_id);
        break;
    case LINK_CLICK_VISIT_LAST_MESSAGE_SENDER:
        if (bubble->last_message_sender_id)
            hippo_app_visit_entity_id(hippo_get_app(), bubble->last_message_sender_id);
        break;
    case LINK_CLICK_ACTIVATE_TOTAL_VIEWERS:
        bubble->active_extra = ACTIVE_EXTRA_TOTAL_VIEWERS;
        update_extra_info(bubble);  
    	break;
    case LINK_CLICK_ACTIVATE_WHOS_THERE:
        bubble->active_extra = ACTIVE_EXTRA_WHOS_THERE;
        update_extra_info(bubble);
        break;
    case LINK_CLICK_ACTIVATE_SOMEONE_SAID:
        bubble->active_extra = ACTIVE_EXTRA_SOMEONE_SAID;
        update_extra_info(bubble);
        break;
    case LINK_CLICK_JOIN_CHAT:
        if (bubble->post_id)
            hippo_app_join_chat(hippo_get_app(), bubble->post_id);
        else if (bubble->group_id)
        	hippo_app_join_chat(hippo_get_app(), bubble->group_id);
        break;
    case LINK_CLICK_IGNORE_POST:
        if (bubble->post_id) {
            hippo_app_ignore_post_id(hippo_get_app(), bubble->post_id);
        } else if (bubble->group_id) {
        	if (bubble->active_extra == ACTIVE_EXTRA_MEMBERSHIP_CHANGE) {
	        	hippo_app_ignore_entity_id(hippo_get_app(), bubble->group_id);
	        } else {
	        	hippo_app_ignore_entity_chat_id(hippo_get_app(), bubble->group_id);	        
			}
        }
        break;
    case LINK_CLICK_INVITE:
		if (bubble->group_id && bubble->assoc_id) {
        	hippo_app_invite_to_group(hippo_get_app(), bubble->group_id, bubble->assoc_id);
        }
        break;        
    }
}
