#include <config.h>
#include "hippo-person-renderer.h"
#include "main.h"
#include <gtk/gtkcellrenderer.h>
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
	PROP_PERSON,
	PROP_PHOTO
};

struct _HippoPersonRenderer {
    GtkCellRenderer parent;
    HippoPerson *person;
    GdkPixbuf *photo;
    PangoLayout *name_layout;
    PangoLayout *song_layout;
    PangoLayout *artist_layout;
    GdkPixbuf *note_on;
    GdkPixbuf *note_off;
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
                            							
    g_object_class_install_property(object_class,
                				    PROP_PHOTO,
                                    g_param_spec_object("photo",
                             							_("Photo pixbuf"),
                            							_("The pixbuf to render for this person"),
                                                        GDK_TYPE_PIXBUF,
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
    if (renderer->song_layout) {
        g_object_unref(renderer->song_layout);
        renderer->song_layout = NULL;
    }
    if (renderer->artist_layout) {
        g_object_unref(renderer->artist_layout);
        renderer->artist_layout = NULL;
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

    if (renderer->photo) {
        g_object_unref(renderer->photo);
        renderer->photo = NULL;
    }

    nuke_caches(renderer);

    if (renderer->note_on) {
        g_object_unref(renderer->note_on);
    }
    if (renderer->note_off) {
        g_object_unref(renderer->note_off);
    }
    
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
        g_value_set_object(value, G_OBJECT(renderer->person));
        break;
    case PROP_PHOTO:
        g_value_set_object(value, G_OBJECT(renderer->photo));
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
    case PROP_PHOTO:        
        {
            GdkPixbuf *new_pixbuf;
            new_pixbuf = (GdkPixbuf*) g_value_dup_object(value);
            
            if (renderer->photo)
                g_object_unref(renderer->photo);
            renderer->photo = new_pixbuf;
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, param_id, pspec);
        break;
    }
}

static void
make_layout(GtkWidget    *widget,
            PangoLayout **layout_p,
            const char   *value)
{
    if (value == NULL)
        value = "";

    if (*layout_p == NULL) {
        *layout_p = gtk_widget_create_pango_layout(widget, value);
    } else {
        const char *old = pango_layout_get_text(*layout_p);
        if (old && strcmp(old, value) == 0)
            /* nothing */;
        else
            pango_layout_set_text(*layout_p, value, -1);
    }
}

#define PHOTO_SIZE 60
#define PHOTO_MARGIN_RIGHT 5
#define NOTE_SIZE 16
/* we truncate to this */
#define TOTAL_WIDTH 140
#define NAME_MARGIN_BOTTOM 5
#define NOTE_MARGIN_RIGHT 3
#define SONG_MARGIN_BOTTOM 3

static void
update_caches(HippoPersonRenderer *renderer,
              GtkWidget           *widget)
{
    const char *name;
    const char *song;
    const char *artist;
    GtkIconTheme *icon_theme;
    GdkPixbuf *note_on;
    GdkPixbuf *note_off;            
    
    if (renderer->person == NULL) {
        name = NULL;
        song = NULL;
        artist = NULL;
    } else {
        /* some of these can return NULL */
        name = hippo_entity_get_name(HIPPO_ENTITY(renderer->person));
        song = hippo_person_get_current_song(renderer->person);
        artist = hippo_person_get_current_artist(renderer->person);
    }
    
    make_layout(widget, &renderer->name_layout, name);
    /* FIXME better to omit the note and the space for music 
     * entirely, but this keeps it from looking broken for now
     */
    if (song == NULL || *song == '\0')
        song = _("No song");
    make_layout(widget, &renderer->song_layout, song);
    make_layout(widget, &renderer->artist_layout, artist);
    
    /* this strongly relies on the icon theme caching the pixbufs
     * so it typically ends up as a no-op
     */
    icon_theme = gtk_icon_theme_get_for_screen(gtk_widget_get_screen(widget));
    note_on = gtk_icon_theme_load_icon(icon_theme, "mugshot_note_on", NOTE_SIZE, 0, NULL);
    note_off = gtk_icon_theme_load_icon(icon_theme, "mugshot_note_on", NOTE_SIZE, 0, NULL);
    
    if (note_on) {
        if (renderer->note_on)
            g_object_unref(renderer->note_on);
        renderer->note_on = note_on;
    }
    if (note_off) {
        if (renderer->note_off)
            g_object_unref(renderer->note_off);
        renderer->note_off = note_off;
    }
}

static void
compute_size(GtkCellRenderer      *cell,
             GtkWidget            *widget,
             GdkRectangle         *cell_area,
             /* content rect relative to cell area */
             GdkRectangle         *content_rect_p,
             /* individual items relative to content_rect.x, content_rect.y */
             GdkRectangle         *photo_rect_p,
             GdkRectangle         *name_rect_p,
             GdkRectangle         *song_rect_p,
             GdkRectangle         *artist_rect_p,
             GdkRectangle         *note_rect_p)
{
    HippoPersonRenderer *renderer;
    PangoRectangle prect;
    GdkRectangle content_rect;
    GdkRectangle photo_rect;
    GdkRectangle name_rect;
    GdkRectangle song_rect;
    GdkRectangle artist_rect;
    GdkRectangle note_rect;
    int padded_width;
    int padded_height;
    int photo_margin;

    renderer = HIPPO_PERSON_RENDERER(cell);

    update_caches(renderer, widget);

    /* First get all the sizes, then compute positions */

    if (renderer->photo) {
        photo_rect.width = PHOTO_SIZE;
        photo_rect.height = PHOTO_SIZE;
        photo_margin = PHOTO_MARGIN_RIGHT;
    } else {
        photo_rect.width = 0;
        photo_rect.height = 0;
        photo_margin = 0;
    }
    
    note_rect.width = NOTE_SIZE;
    note_rect.height = NOTE_SIZE;

    pango_layout_get_pixel_extents(renderer->name_layout, NULL, &prect);
    name_rect.width = prect.width;
    name_rect.height = prect.height;
    
    pango_layout_get_pixel_extents(renderer->song_layout, NULL, &prect);
    song_rect.width = prect.width;
    song_rect.height = prect.height;    

    pango_layout_get_pixel_extents(renderer->artist_layout, NULL, &prect);
    artist_rect.width = prect.width;
    artist_rect.height = prect.height;    

    /* now positions */

    photo_rect.x = 0;
    photo_rect.y = 0;
    
    name_rect.x = photo_rect.x + photo_rect.width + photo_margin;
    name_rect.y = 0;
    
    note_rect.x = name_rect.x;
    note_rect.y = name_rect.y + name_rect.height + NAME_MARGIN_BOTTOM;
    
    song_rect.x = note_rect.x + note_rect.y + NOTE_MARGIN_RIGHT;
    song_rect.y = note_rect.y;
    
    artist_rect.x = song_rect.x;
    artist_rect.y = song_rect.y + song_rect.height + SONG_MARGIN_BOTTOM;

    /* Now compute content rect by union of all other rects, then 
     * translate to be relative to the cell
     */
    content_rect.x = 0;
    content_rect.y = 0;
    content_rect.width = 0;
    content_rect.height = 0;

    gdk_rectangle_union(&content_rect, &photo_rect, &content_rect);    
    gdk_rectangle_union(&content_rect, &name_rect, &content_rect);
    gdk_rectangle_union(&content_rect, &note_rect, &content_rect);
    gdk_rectangle_union(&content_rect, &song_rect, &content_rect);    
    gdk_rectangle_union(&content_rect, &artist_rect, &content_rect);

    if (photo_rect.height < content_rect.height) {
        /* center photo rect vertically */
        photo_rect.y = (content_rect.height - photo_rect.height) / 2;
    }

    padded_width  = (int) cell->xpad * 2 + content_rect.width;
    padded_height = (int) cell->ypad * 2 + content_rect.height;

    if (cell_area && content_rect.width > 0 && content_rect.height > 0) {
        content_rect.x = (((gtk_widget_get_direction (widget) == GTK_TEXT_DIR_RTL) ?
                            1.0 - cell->xalign : cell->xalign) * 
                            (cell_area->width - padded_width - cell->xpad * 2));
        content_rect.x = MAX (content_rect.x, 0) + cell->xpad;

        content_rect.y = (cell->yalign *
                           (cell_area->height - padded_height - 2 * cell->ypad));
        content_rect.y = MAX (content_rect.y, 0) + cell->ypad;
    }
    
    content_rect.width = padded_width;
    content_rect.height = padded_height;
    
#define OUT(what) do { if (what ## _p) { * what ## _p = what; }  } while(0)
    OUT(content_rect);
    OUT(photo_rect);
    OUT(name_rect);
    OUT(song_rect);
    OUT(artist_rect);
    OUT(note_rect);
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
    GdkRectangle content_rect;
    
    compute_size(cell, widget, cell_area, &content_rect, 
                 NULL, NULL, NULL, NULL, NULL);
    if (x_offset_p)                 
        *x_offset_p = content_rect.x;
    if (y_offset_p)
        *y_offset_p = content_rect.y;
    if (width_p)
        *width_p = content_rect.width;
    if (height_p)
        *height_p = content_rect.height;
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
    GdkRectangle photo_rect;
    GdkRectangle name_rect;
    GdkRectangle song_rect;
    GdkRectangle artist_rect;
    GdkRectangle note_rect;    
    GdkRectangle draw_rect;
    GtkStateType state;
    /* cairo_t *cr; */

    renderer = HIPPO_PERSON_RENDERER(cell);

    /* content_rect relative to cell area, others relative to content_rect */
    compute_size(cell, widget, cell_area, &content_rect, &photo_rect,
                 &name_rect, &song_rect, &artist_rect, &note_rect);
               
    /* move content_rect relative to the window */
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

    /* draw name */
#define PAINT_LAYOUT(what) \
    gtk_paint_layout(widget->style, window, state, TRUE, &draw_rect, widget, "hippopersonrenderer",   \
                     content_rect.x + what##_rect.x,                                                   \
                     content_rect.y + what##_rect.y,                                                   \
                     renderer->what##_layout)
    PAINT_LAYOUT(name);
    PAINT_LAYOUT(song);
    PAINT_LAYOUT(artist);
        
    /* draw photo */
    if (renderer->photo) {
        gdk_draw_pixbuf(window, widget->style->fg_gc[state], renderer->photo, 
                        0, 0, content_rect.x + photo_rect.x, content_rect.y + photo_rect.y,
                        -1, -1, GDK_RGB_DITHER_NORMAL, 0, 0);
    }                        

    /* draw note */
    if (renderer->person) {
        gboolean playing = hippo_person_get_music_playing(renderer->person);
        GdkPixbuf *note;
        if (playing)
            note = renderer->note_on;
        else
            note = renderer->note_off;
        if (note) {
            gdk_draw_pixbuf(window, widget->style->fg_gc[state], note,
                            0, 0, content_rect.x + note_rect.x, content_rect.y + note_rect.y,
                            -1, -1, GDK_RGB_DITHER_NORMAL, 0, 0);
        }
    }

    /*
    cr = gdk_cairo_create(window);
    gdk_cairo_rectangle (cr, &draw_rect);
    cairo_fill(cr);
    cairo_destroy (cr);
    */
}
