/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BOX_H__
#define __HIPPO_CANVAS_BOX_H__

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-container.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasStyle      HippoCanvasStyle;
typedef struct _HippoCanvasStyleClass HippoCanvasStyleClass;

typedef enum
{
    HIPPO_PACK_EXPAND = 1,  /**< This is equivalent to both EXPAND and FILL for GtkBox,
                             * the way you'd get FILL=false is to set the alignment
                             * on the child item
                             */
    HIPPO_PACK_END = 2,
    HIPPO_PACK_FIXED = 4,   /**< Like position: absolute or GtkFixed */
    HIPPO_PACK_IF_FITS = 8, /**< Can hide this child to make space if allocation is too small
                             * for the child's width request. 
                             * Include child width in box's natural width but not box's request.
                             * (doesn't work in vertical boxes for now)
                             */
    /* Floated children: only works with vertical box, and cannot be used in combination
     * with HIPPO_PACK_EXPAND or HIPPO_PACK_END
     */
    HIPPO_PACK_FLOAT_LEFT = 16,   /* Float to the left */
    HIPPO_PACK_FLOAT_RIGHT = 32,  /* Float to the right */
    HIPPO_PACK_CLEAR_LEFT = 64,   /* Pack below left-floated children */
    HIPPO_PACK_CLEAR_RIGHT = 128, /* Pack below right-floated children */
    HIPPO_PACK_CLEAR_BOTH = 192   /* Pack below left-and right floated children */
} HippoPackFlags;

typedef enum {
    HIPPO_CASCADE_MODE_NONE,
    HIPPO_CASCADE_MODE_INHERIT
} HippoCascadeMode;

typedef int  (* HippoCanvasCompareChildFunc) (HippoCanvasItem *child_a,
                                              HippoCanvasItem *child_b,
                                              void            *data);
typedef void (* HippoCanvasForeachChildFunc) (HippoCanvasItem *child,
                                              void            *data);  

typedef struct _HippoCanvasBox      HippoCanvasBox;
typedef struct _HippoCanvasBoxClass HippoCanvasBoxClass;

typedef struct _HippoCanvasBoxChild HippoCanvasBoxChild;

/* Declare here to avoid circular header file dependency */
typedef struct _HippoCanvasLayout   HippoCanvasLayout;

#define HIPPO_TYPE_CANVAS_BOX              (hippo_canvas_box_get_type ())
#define HIPPO_CANVAS_BOX(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BOX, HippoCanvasBox))
#define HIPPO_CANVAS_BOX_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BOX, HippoCanvasBoxClass))
#define HIPPO_IS_CANVAS_BOX(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BOX))
#define HIPPO_IS_CANVAS_BOX_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BOX))
#define HIPPO_CANVAS_BOX_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BOX, HippoCanvasBoxClass))

struct _HippoCanvasBox {
    GObject base;
    HippoCanvasContainer *parent;
    HippoCanvasContext *context;
    HippoCanvasStyle *style; /* may be NULL if no relevant props set */
    GSList *children;

    HippoCanvasLayout *layout;

    char *tooltip;

    /* If set, we debug-spew about size allocation prefixed with this name */
    char *debug_name;

    /* Cache of last requested sizes of content. This largely duplicates the caching
     * in HippoCanvasBoxChild; removing the caching in HippoCanvasBoxChild would
     * save 20 bytes per item at the expense of having to continually call into
     * the child and have the child add back on the padding border */
    int content_min_width;       /* -1 if invalid */
    int content_natural_width;   /* always valid and >= 0 when min_width is valid */
    int content_min_height;      /* -1 if invalid */
    int content_natural_height;  /* always valid and >= 0 when min_height is valid */
    int content_height_request_for_width; /* width the height_request is valid for */
    
    int allocated_width;
    int allocated_height;

    /* these are -1 if unset, which means use natural size request */
    int box_width;
    int box_height;

    guint32 background_color_rgba;
    guint32 border_color_rgba;

    /* padding is empty space around all children with the
     * background color
     */
    guint8 padding_top;
    guint8 padding_bottom;
    guint8 padding_left;
    guint8 padding_right;

    /* padding is empty space around the padding, with
     * the border color
     */
    guint8 border_top;
    guint8 border_bottom;
    guint8 border_left;
    guint8 border_right;
    
    guint8 spacing;

    guint floating : 1;
    guint needs_width_request : 1;
    guint needs_height_request : 1;
    guint needs_allocate : 1;
    guint orientation : 2; /* enum only has 2 values so it fits with extra */
    guint x_align : 3;     /* enum only has 4 values so it fits with extra */
    guint y_align : 3;     /* enum only has 4 values so it fits with extra */
    guint clickable : 1;   /* show a hand pointer and emit activated signal */
    guint hovering : 1;    /* the box or some child contains the pointer (have gotten enter without leave) */
    guint color_cascade : 2; /* enum has only 2 values */
    guint font_cascade : 2;  /* enum has only 2 values */
};

struct _HippoCanvasBoxClass {
    GObjectClass base_class;

    guint32  default_color;
    
    void     (* paint_background)             (HippoCanvasBox   *box,
                                               cairo_t          *cr,
                                               HippoRectangle   *damaged_box);
    void     (* paint_children)               (HippoCanvasBox   *box,
                                               cairo_t          *cr,
                                               HippoRectangle   *damaged_box);
    void     (* paint_below_children)         (HippoCanvasBox   *box,
                                               cairo_t          *cr,
                                               HippoRectangle   *damaged_box);
    void     (* paint_above_children)         (HippoCanvasBox   *box,
                                               cairo_t          *cr,
                                               HippoRectangle   *damaged_box);
    
    void     (* get_content_width_request)    (HippoCanvasBox   *box,
                                               int              *min_width_p,
                                               int              *natural_width_p);
    void     (* get_content_height_request)   (HippoCanvasBox   *box,
                                               int               for_width,
                                               int              *min_height_p,
                                               int              *natural_height_p);

    void     (* hovering_changed)             (HippoCanvasBox   *box,
                                               gboolean          hovering);
};

GType            hippo_canvas_box_get_type          (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_box_new               (void);

void             hippo_canvas_box_prepend           (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     HippoPackFlags               flags);
void             hippo_canvas_box_append            (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     HippoPackFlags               flags);

void             hippo_canvas_box_move              (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     HippoGravity                 gravity,
                                                     int                          x,
                                                     int                          y);
void             hippo_canvas_box_set_position      (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     int                          x,
                                                     int                          y);
void             hippo_canvas_box_get_position      (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     int                         *x,
                                                     int                         *y);
void             hippo_canvas_box_clear             (HippoCanvasBox              *box);
void             hippo_canvas_box_remove            (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child);
void             hippo_canvas_box_remove_all        (HippoCanvasBox              *box);
GList*           hippo_canvas_box_get_children      (HippoCanvasBox              *box);
gboolean         hippo_canvas_box_is_empty          (HippoCanvasBox              *box);
void             hippo_canvas_box_foreach           (HippoCanvasBox              *box,
                                                     HippoCanvasForeachChildFunc  func,
                                                     void                        *data);
void             hippo_canvas_box_reverse           (HippoCanvasBox              *box);
void             hippo_canvas_box_sort              (HippoCanvasBox              *box,
                                                     HippoCanvasCompareChildFunc  compare_func,
                                                     void                        *data); 
void             hippo_canvas_box_insert_after     (HippoCanvasBox              *box,
                                                    HippoCanvasItem             *child,
                                                    HippoCanvasItem             *ref_child,
                                                    HippoPackFlags               flags);
void             hippo_canvas_box_insert_before     (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     HippoCanvasItem             *ref_child,
                                                     HippoPackFlags               flags);
void             hippo_canvas_box_insert_sorted     (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     HippoPackFlags               flags,
                                                     HippoCanvasCompareChildFunc  compare_func,
                                                     void                        *data);
void             hippo_canvas_box_set_child_packing (HippoCanvasBox              *box,
                                                     HippoCanvasItem             *child,
                                                     HippoPackFlags               flags);

void hippo_canvas_box_set_layout(HippoCanvasBox    *box,
                                 HippoCanvasLayout *layout);
    
/* Protected accessors for subclasses */
HippoCanvasContext* hippo_canvas_box_get_context         (HippoCanvasBox *box);
void                hippo_canvas_box_get_background_area (HippoCanvasBox *box,
                                                          HippoRectangle *area);
void                hippo_canvas_box_align               (HippoCanvasBox *box,
                                                          int             content_width,
                                                          int             content_height,
                                                          int            *x_p,
                                                          int            *y_p,
                                                          int            *width_p,
                                                          int            *height_p);

void                hippo_canvas_box_set_clickable       (HippoCanvasBox *box,
                                                          gboolean        clickable);

gboolean            hippo_canvas_box_is_clickable        (HippoCanvasBox *box);

/* API for layout managers */

HippoCanvasBoxChild *hippo_canvas_box_find_box_child (HippoCanvasBox      *box,
                                                      HippoCanvasItem     *item);

GList *hippo_canvas_box_get_layout_children (HippoCanvasBox *box);

#define HIPPO_TYPE_CANVAS_BOX_CHILD    (hippo_canvas_box_child_get_type ())
#define HIPPO_CANVAS_BOX_CHILD(object) (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BOX_CHILD, HippoCanvasBoxChild))

/**
 * HippoCanvasBoxChild:
 * 
 * #HippoCanvasBoxChild holds data associated with an item that has been
 * added to a canvas box. It is used by implementations of #HippoCanvasLayout
 * when implementing methods like get_width_request and size_allocate.
 *
 * The life-cycle of a #HippoCanvasBoxChild is effectively until the item
 * is removed from its parent. If a reference is held beyond that point, calling
 * methods on the box child is safe, but the methods will have no effect,
 * and defaults results will be returned.
 */
struct _HippoCanvasBoxChild {
    HippoCanvasItem *item;

    /* If this is false, layout managers should ignore this item */
    guint            in_layout : 1;

    guint            expand : 1;
    guint            end : 1;
    guint            fixed : 1;
    guint            if_fits : 1;
    guint            float_left : 1;
    guint            float_right : 1;
    guint            clear_left : 1;
    guint            clear_right : 1;
    guint            visible : 1;
};

GType     hippo_canvas_box_child_get_type           (void);

HippoCanvasBoxChild *hippo_canvas_box_child_ref   (HippoCanvasBoxChild *child);
void                 hippo_canvas_box_child_unref (HippoCanvasBoxChild *child);

void     hippo_canvas_box_child_set_qdata (HippoCanvasBoxChild *child,
                                           GQuark               key,
                                           gpointer             data,
                                           GDestroyNotify       notify);
gpointer hippo_canvas_box_child_get_qdata (HippoCanvasBoxChild *child,
                                           GQuark               key);

void      hippo_canvas_box_child_get_width_request  (HippoCanvasBoxChild *child,
                                                     int                 *min_width_p,
                                                     int                 *natural_width_p);
void      hippo_canvas_box_child_get_height_request (HippoCanvasBoxChild *child,
                                                     int                  for_width,
                                                     int                 *min_height_p,
                                                     int                 *natural_height_p);
void     hippo_canvas_box_child_allocate            (HippoCanvasBoxChild *child,
                                                     int                  x,
                                                     int                  y,
                                                     int                  width,
                                                     int                  height,
                                                     gboolean             origin_changed);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BOX_H__ */
