/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoScrollRibbon based on EogThumbNav
 *
 * Copyright (C) 2007 Red Hat, Inc.
 * Copyright (C) 2006 The Free Software Foundation
 *
 * Author: Lucas Rocha <lucasr@gnome.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

#include "config.h"

#include "hippo-scroll-ribbon.h"

#include <glib/gi18n.h>
#include <string.h>

#define HIPPO_SCROLL_RIBBON_GET_PRIVATE(object)                         \
    (G_TYPE_INSTANCE_GET_PRIVATE ((object), HIPPO_TYPE_SCROLL_RIBBON, HippoScrollRibbonPrivate))

G_DEFINE_TYPE (HippoScrollRibbon, hippo_scroll_ribbon, GTK_TYPE_VBOX);

#define HIPPO_SCROLL_RIBBON_SCROLL_INC      20
#define HIPPO_SCROLL_RIBBON_SCROLL_MOVE     20
#define HIPPO_SCROLL_RIBBON_SCROLL_TIMEOUT  20

enum {
    PROP_ORIENTATION = 1,
    PROP_CONTENTS
};

struct _HippoScrollRibbonPrivate {
    GtkOrientation orientation;

    gboolean          scroll_dir;
    gint              scroll_pos;
    gint              scroll_id;

    GtkWidget        *button_left;
    GtkWidget        *button_right;
    GtkWidget        *sw;
    GtkAdjustment    *adj;

    GtkWidget        *contents;
};

static gboolean
hippo_scroll_ribbon_scroll_event (GtkWidget *widget, GdkEventScroll *event, gpointer user_data)
{
    HippoScrollRibbon *nav = HIPPO_SCROLL_RIBBON (user_data);
    gint inc = HIPPO_SCROLL_RIBBON_SCROLL_INC * 3;

    if (nav->priv->mode != HIPPO_SCROLL_RIBBON_MODE_ONE_ROW)
        return FALSE;

    switch (event->direction) {
    case GDK_SCROLL_UP:
    case GDK_SCROLL_LEFT:
        inc *= -1;
        break;

    case GDK_SCROLL_DOWN:
    case GDK_SCROLL_RIGHT:
        break;

    default:
        g_assert_not_reached ();
        return FALSE;
    }

    if (inc < 0)
        nav->priv->adj->value = MAX (0, nav->priv->adj->value + inc);
    else
        nav->priv->adj->value = MIN (nav->priv->adj->upper - nav->priv->adj->page_size, nav->priv->adj->value + inc);

    gtk_adjustment_value_changed (nav->priv->adj);

    return TRUE;
}

static void
hippo_scroll_ribbon_adj_changed (GtkAdjustment *adj, gpointer user_data)
{
    HippoScrollRibbon *nav;
    HippoScrollRibbonPrivate *priv;

    nav = HIPPO_SCROLL_RIBBON (user_data);
    priv = HIPPO_SCROLL_RIBBON_GET_PRIVATE (nav);

    gtk_widget_set_sensitive (priv->button_right, adj->upper > adj->page_size);
}

static void
hippo_scroll_ribbon_adj_value_changed (GtkAdjustment *adj, gpointer user_data)
{
    HippoScrollRibbon *nav;
    HippoScrollRibbonPrivate *priv;

    nav = HIPPO_SCROLL_RIBBON (user_data);
    priv = HIPPO_SCROLL_RIBBON_GET_PRIVATE (nav);

    gtk_widget_set_sensitive (priv->button_left, adj->value > 0);

    gtk_widget_set_sensitive (priv->button_right,
                              adj->value < adj->upper - adj->page_size);
}

static gboolean
hippo_scroll_ribbon_scroll_step (gpointer user_data)
{
    HippoScrollRibbon *nav = HIPPO_SCROLL_RIBBON (user_data);
    gint delta;

    if (nav->priv->scroll_pos < 10)
        delta = HIPPO_SCROLL_RIBBON_SCROLL_INC;
    else if (nav->priv->scroll_pos < 20)
        delta = HIPPO_SCROLL_RIBBON_SCROLL_INC * 2;
    else if (nav->priv->scroll_pos < 30)
        delta = HIPPO_SCROLL_RIBBON_SCROLL_INC * 2 + 5;
    else
        delta = HIPPO_SCROLL_RIBBON_SCROLL_INC * 2 + 12;

    if (!nav->priv->scroll_dir)
        delta *= -1;

    if ((gint) (nav->priv->adj->value + (gdouble) delta) >= 0 &&
        (gint) (nav->priv->adj->value + (gdouble) delta) <= nav->priv->adj->upper - nav->priv->adj->page_size) {
        nav->priv->adj->value += (gdouble) delta;
        nav->priv->scroll_pos++;
        gtk_adjustment_value_changed (nav->priv->adj);
    } else {
        if (delta > 0)
            nav->priv->adj->value = nav->priv->adj->upper - nav->priv->adj->page_size;
        else
            nav->priv->adj->value = 0;

        nav->priv->scroll_pos = 0;

        gtk_adjustment_value_changed (nav->priv->adj);

        return FALSE;
    }

    return TRUE;
}

static void
hippo_scroll_ribbon_button_clicked (GtkButton *button, HippoScrollRibbon *nav)
{
    nav->priv->scroll_pos = 0;

    nav->priv->scroll_dir = (GTK_WIDGET (button) == nav->priv->button_right);

    hippo_scroll_ribbon_scroll_step (nav);
}

static void
hippo_scroll_ribbon_start_scroll (GtkButton *button, HippoScrollRibbon *nav)
{
    nav->priv->scroll_dir = (GTK_WIDGET (button) == nav->priv->button_right);

    nav->priv->scroll_id = g_timeout_add (HIPPO_SCROLL_RIBBON_SCROLL_TIMEOUT,
                                          hippo_scroll_ribbon_scroll_step,
                                          nav);
}

static void
hippo_scroll_ribbon_stop_scroll (GtkButton *button, HippoScrollRibbon *nav)
{
    if (nav->priv->scroll_id > 0) {
        g_source_remove (nav->priv->scroll_id);
        nav->priv->scroll_id = 0;
        nav->priv->scroll_pos = 0;
    }
}

static void
hippo_scroll_ribbon_get_property (GObject    *object,
                                  guint       property_id,
                                  GValue     *value,
                                  GParamSpec *pspec)
{
    HippoScrollRibbon *nav = HIPPO_SCROLL_RIBBON (object);

    switch (property_id)
	{
	case PROP_ORIENTATION:
            g_value_set_int (value,
                             nav->priv->orientation);
            break;

	case PROP_CONTENTS:
            g_value_set_object (value, nav->priv->contents);
            break;
	}
}

static void
hippo_scroll_ribbon_set_property (GObject      *object,
                                  guint         property_id,
                                  const GValue *value,
                                  GParamSpec   *pspec)
{
    HippoScrollRibbon *nav = HIPPO_SCROLL_RIBBON (object);

    switch (property_id)
	{
	case PROP_ORIENTATION:
            {
                GtkOrientation orientation;
                orientation = g_value_get_int (value);
                if (orientation != nav->priv->orientation) {
                    nav->priv->orientation = orientation;
                    /* Hum, have to change hbox to vbox ...
                     * some old and disastrous code comes to mind.
                     */
                    /* FIXME */
                }
            }
            break;

	case PROP_CONTENTS:
            {
                GtkWidget *contents;

                contents = GTK_WIDGET (g_value_get_object (value));
                if (nav->priv->contents) {
                    gtk_container_remove(GTK_CONTAINER(nav), nav->priv->contents);
                    nav->priv->contents = NULL;
                }

                /* FIXME add the new contents, figure out if get_object refs */
            }
            break;
	}
}

static GObject *
hippo_scroll_ribbon_constructor (GType type,
                                 guint n_construct_properties,
                                 GObjectConstructParam *construct_params)
{
    GObject *object;
    HippoScrollRibbonPrivate *priv;

    object = G_OBJECT_CLASS (hippo_scroll_ribbon_parent_class)->constructor
        (type, n_construct_properties, construct_params);

    priv = HIPPO_SCROLL_RIBBON (object)->priv;

    if (priv->contents != NULL) {
        gtk_container_add (GTK_CONTAINER (priv->sw), priv->contents);
        gtk_widget_show_all (priv->sw);
    }

    return object;
}

static void
hippo_scroll_ribbon_class_init (HippoScrollRibbonClass *class)
{
    GObjectClass *g_object_class = (GObjectClass *) class;

    g_object_class->constructor  = hippo_scroll_ribbon_constructor;
    g_object_class->get_property = hippo_scroll_ribbon_get_property;
    g_object_class->set_property = hippo_scroll_ribbon_set_property;

    g_object_class_install_property (g_object_class,
                                     PROP_SHOW_BUTTONS,
                                     g_param_spec_int ("orientation",
                                                       "Orientation",
                                                       "Vertical or horizontal",
                                                       GTK_ORIENTATION_VERTICAL,
                                                       G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property (g_object_class,
                                     PROP_CONTENTS,
                                     g_param_spec_object ("contents",
                                                          "Contents to scroll",
                                                          "The thing to be scrolled",
                                                          GTK_TYPE_WIDGET,
                                                          G_PARAM_READABLE |
                                                          G_PARAM_WRITABLE));


    g_type_class_add_private (g_object_class, sizeof (HippoScrollRibbonPrivate));
}

static void
hippo_scroll_ribbon_init (HippoScrollRibbon *nav)
{
    HippoScrollRibbonPrivate *priv;
    GtkWidget *arrow;

    nav->priv = HIPPO_SCROLL_RIBBON_GET_PRIVATE (nav);

    priv = nav->priv;

    priv->orientation = GTK_ORIENTATION_VERTICAL;

    priv->button_left = gtk_button_new ();
    gtk_button_set_relief (GTK_BUTTON (priv->button_left), GTK_RELIEF_NONE);

    arrow = gtk_arrow_new (GTK_ARROW_LEFT, GTK_SHADOW_ETCHED_IN);
    gtk_container_add (GTK_CONTAINER (priv->button_left), arrow);

    gtk_widget_set_size_request (GTK_WIDGET (priv->button_left), 25, 0);

    gtk_box_pack_start (GTK_BOX (nav), priv->button_left, FALSE, FALSE, 0);

    g_signal_connect (priv->button_left,
                      "clicked",
                      G_CALLBACK (hippo_scroll_ribbon_button_clicked),
                      nav);

    g_signal_connect (priv->button_left,
                      "pressed",
                      G_CALLBACK (hippo_scroll_ribbon_start_scroll),
                      nav);

    g_signal_connect (priv->button_left,
                      "released",
                      G_CALLBACK (hippo_scroll_ribbon_stop_scroll),
                      nav);

    priv->sw = gtk_scrolled_window_new (NULL, NULL);

    gtk_scrolled_window_set_shadow_type (GTK_SCROLLED_WINDOW (priv->sw),
                                         GTK_SHADOW_NONE);

    gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (priv->sw),
                                    GTK_POLICY_AUTOMATIC,
                                    GTK_POLICY_NEVER);

    g_signal_connect (priv->sw,
                      "scroll-event",
                      G_CALLBACK (hippo_scroll_ribbon_scroll_event),
                      nav);

    priv->adj = gtk_scrolled_window_get_hadjustment (GTK_SCROLLED_WINDOW (priv->sw));

    g_signal_connect (priv->adj,
                      "changed",
                      G_CALLBACK (hippo_scroll_ribbon_adj_changed),
                      nav);

    g_signal_connect (priv->adj,
                      "value-changed",
                      G_CALLBACK (hippo_scroll_ribbon_adj_value_changed),
                      nav);

    gtk_box_pack_start (GTK_BOX (nav), priv->sw, TRUE, TRUE, 0);

    priv->button_right = gtk_button_new ();
    gtk_button_set_relief (GTK_BUTTON (priv->button_right), GTK_RELIEF_NONE);

    arrow = gtk_arrow_new (GTK_ARROW_RIGHT, GTK_SHADOW_NONE);
    gtk_container_add (GTK_CONTAINER (priv->button_right), arrow);

    gtk_widget_set_size_request (GTK_WIDGET (priv->button_right), 25, 0);

    gtk_box_pack_start (GTK_BOX (nav), priv->button_right, FALSE, FALSE, 0);

    g_signal_connect (priv->button_right,
                      "clicked",
                      G_CALLBACK (hippo_scroll_ribbon_button_clicked),
                      nav);

    g_signal_connect (priv->button_right,
                      "pressed",
                      G_CALLBACK (hippo_scroll_ribbon_start_scroll),
                      nav);

    g_signal_connect (priv->button_right,
                      "released",
                      G_CALLBACK (hippo_scroll_ribbon_stop_scroll),
                      nav);

    gtk_adjustment_value_changed (priv->adj);
}

GtkWidget *
hippo_scroll_ribbon_new (GtkOrientation orientation)
{
    GObject *nav;

    nav = g_object_new (HIPPO_TYPE_SCROLL_RIBBON,
                        "orientation", orientation,
                        NULL);

    return GTK_WIDGET (nav);
}
