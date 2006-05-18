#ifndef __HIPPO_BUBBLE_H__
#define __HIPPO_BUBBLE_H__

#include <hippo/hippo-common.h>
#include <gtk/gtkwidget.h>

G_BEGIN_DECLS

typedef struct _HippoBubble      HippoBubble;
typedef struct _HippoBubbleClass HippoBubbleClass;

#define HIPPO_TYPE_BUBBLE              (hippo_bubble_get_type ())
#define HIPPO_BUBBLE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BUBBLE, HippoBubble))
#define HIPPO_BUBBLE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BUBBLE, HippoBubbleClass))
#define HIPPO_IS_BUBBLE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BUBBLE))
#define HIPPO_IS_BUBBLE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BUBBLE))
#define HIPPO_BUBBLE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BUBBLE, HippoBubbleClass))

GType        	 hippo_bubble_get_type               (void) G_GNUC_CONST;

GtkWidget*       hippo_bubble_new                    (void);

G_END_DECLS

#endif /* __HIPPO_BUBBLE_H__ */
