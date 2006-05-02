#include "hippo-connection.h"

static void hippo_connection_finalize (GObject *object);

struct _HippoConnection {
    GObjectClass parent;
};

struct _HippoConnectionClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoConnection, hippo_connection, G_TYPE_OBJECT);

static void
hippo_connection_init(HippoConnection *connection)
{

}

static void
hippo_connection_class_init(HippoConnectionClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
  
    object_class->finalize = hippo_connection_finalize;
}

static void
hippo_connection_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_connection_parent_class)->finalize(object); 
}

HippoConnection*
hippo_connection_new(void)
{
    HippoConnection *connection = g_object_new(HIPPO_TYPE_CONNECTION, NULL);
    
    return connection;
}
