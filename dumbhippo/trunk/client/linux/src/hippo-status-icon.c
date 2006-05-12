#include <config.h>
#include "hippo-status-icon.h"
#include <gtk/gtkstatusicon.h>

static void      hippo_status_icon_init                (HippoStatusIcon       *icon);
static void      hippo_status_icon_class_init          (HippoStatusIconClass  *klass);

static void      hippo_status_icon_finalize            (GObject                 *object);


struct _HippoStatusIcon {
    GtkStatusIcon parent;
    HippoDataCache *cache;
};

struct _HippoStatusIconClass {
    GtkStatusIconClass parent_class;

};

G_DEFINE_TYPE(HippoStatusIcon, hippo_status_icon, GTK_TYPE_STATUS_ICON);
                       

static void
hippo_status_icon_init(HippoStatusIcon       *icon)
{
    
}

static void
hippo_status_icon_class_init(HippoStatusIconClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_status_icon_finalize;
}

HippoStatusIcon*
hippo_status_icon_new(HippoDataCache *cache)
{
    HippoStatusIcon *icon =
        g_object_new(HIPPO_TYPE_STATUS_ICON,
                     "icon-name", "gnome-fish",
                     NULL);

    icon->cache = cache;
    g_object_ref(icon->cache);

    return HIPPO_STATUS_ICON(icon);
}

static void
hippo_status_icon_finalize(GObject *object)
{
    HippoStatusIcon *icon = HIPPO_STATUS_ICON(object);

    g_object_unref(icon->cache);
    
    G_OBJECT_CLASS(hippo_status_icon_parent_class)->finalize(object);
}
