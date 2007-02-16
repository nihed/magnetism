#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <Python.h>
#include "bigboard-native.h"

void initbignative(void);

PyMethodDef bignative_functions[] = {
    {"set_log_handler", bigboard_set_log_handler, METH_VARARGS,
     "Set the GLib log handler."},
    {NULL, NULL, 0, NULL}        /* Sentinel */
};

PyMODINIT_FUNC
initbignative(void)
{
	(void) Py_InitModule("bignative", bignative_functions);	
}
