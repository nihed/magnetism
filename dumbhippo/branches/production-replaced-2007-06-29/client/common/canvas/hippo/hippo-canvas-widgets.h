/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_WIDGETS_H__
#define __HIPPO_CANVAS_WIDGETS_H__

/* Collection of widget canvas items, implemented per-platform.
 * This is basically a lazy hack to stuff them all in one file like this.
 * The idea is to create token "no-op" canvas item GTypes for each possible
 * native widget that can be contained in an item, then have methods
 * that just pull out the native widget and do stuff to it.
 *
 * If we ever wanted to have methods or state on the widget items beyond just
 * chaining to the contained native widget, we'd probably want to split
 * things out into a separate file in a more normal way.
 */
#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

#define HIPPO_DECLARE_WIDGET_ITEM(lower, Camel)                         \
  typedef struct _HippoCanvas##Camel      HippoCanvas##Camel;           \
  typedef struct _HippoCanvas##Camel##Class HippoCanvas##Camel##Class;  \
  GType            hippo_canvas_##lower##_get_type(void);               \
  HippoCanvasItem* hippo_canvas_##lower##_new(void);

/* BUTTON */

HIPPO_DECLARE_WIDGET_ITEM(button, Button)

#define HIPPO_TYPE_CANVAS_BUTTON              (hippo_canvas_button_get_type ())
#define HIPPO_CANVAS_BUTTON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BUTTON, HippoCanvasButton))
#define HIPPO_CANVAS_BUTTON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BUTTON, HippoCanvasButtonClass))
#define HIPPO_IS_CANVAS_BUTTON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BUTTON))
#define HIPPO_IS_CANVAS_BUTTON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BUTTON))
#define HIPPO_CANVAS_BUTTON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BUTTON, HippoCanvasButtonClass))

/* SCROLLBARS */

HIPPO_DECLARE_WIDGET_ITEM(scrollbars, Scrollbars)

#define HIPPO_TYPE_CANVAS_SCROLLBARS              (hippo_canvas_scrollbars_get_type ())
#define HIPPO_CANVAS_SCROLLBARS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_SCROLLBARS, HippoCanvasScrollbars))
#define HIPPO_CANVAS_SCROLLBARS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_SCROLLBARS, HippoCanvasScrollbarsClass))
#define HIPPO_IS_CANVAS_SCROLLBARS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_SCROLLBARS))
#define HIPPO_IS_CANVAS_SCROLLBARS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_SCROLLBARS))
#define HIPPO_CANVAS_SCROLLBARS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_SCROLLBARS, HippoCanvasScrollbarsClass))

/* ENTRY */

HIPPO_DECLARE_WIDGET_ITEM(entry, Entry)

#define HIPPO_TYPE_CANVAS_ENTRY              (hippo_canvas_entry_get_type ())
#define HIPPO_CANVAS_ENTRY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_ENTRY, HippoCanvasEntry))
#define HIPPO_CANVAS_ENTRY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_ENTRY, HippoCanvasEntryClass))
#define HIPPO_IS_CANVAS_ENTRY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_ENTRY))
#define HIPPO_IS_CANVAS_ENTRY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_ENTRY))
#define HIPPO_CANVAS_ENTRY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_ENTRY, HippoCanvasEntryClass))


/* METHODS */

void hippo_canvas_scrollbars_set_root    (HippoCanvasScrollbars *scrollbars,
                                          HippoCanvasItem       *item);

typedef enum {
    HIPPO_SCROLLBAR_NEVER,
    HIPPO_SCROLLBAR_AUTOMATIC,
    HIPPO_SCROLLBAR_ALWAYS
} HippoScrollbarPolicy;

void hippo_canvas_scrollbars_set_policy (HippoCanvasScrollbars *scrollbars,
                                         HippoOrientation       orientation,
                                         HippoScrollbarPolicy   policy);


G_END_DECLS

#endif /* __HIPPO_CANVAS_WIDGETS_H__ */
