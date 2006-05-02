#include <hippo/hippo-connection.h>

int
main (int argc, char **argv)
{
    HippoConnection *connection;
  
    g_type_init ();
  
    connection = hippo_connection_new ();
  
    g_object_unref (connection);
  
    return 0;
}
