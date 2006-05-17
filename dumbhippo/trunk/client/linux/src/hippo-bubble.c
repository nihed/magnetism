#include <config.h>
#include "hippo-bubble.h"
#include "main.h"

static void      hippo_bubble_init                (HippoBubble       *bubble);
static void      hippo_bubble_class_init          (HippoBubbleClass  *klass);

static void      hippo_bubble_finalize            (GObject                 *object);


struct _HippoBubble {
    GtkWidget parent;

};

struct _HippoBubbleClass {
    GtkWidgetClass parent_class;

};

G_DEFINE_TYPE(HippoBubble, hippo_bubble, GTK_TYPE_WIDGET);
                       

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
