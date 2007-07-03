/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <Python.h>
#include "bigboard-native.h"

/* we don't define NO_IMPORT so here we define the _PyGObject_API variable */
#include <pygobject.h>

void initbignative(void);

PyMethodDef bignative_functions[] = {
    {"set_log_handler", bigboard_set_log_handler, METH_VARARGS,
     "Set the GLib log handler."},
    {"set_application_name", bigboard_set_application_name, METH_VARARGS,
     "Set the GLib app name."},
    {"set_program_name", bigboard_set_program_name, METH_VARARGS,
     "Set the GLib program name."},
    {"install_focus_docks_hack", bigboard_install_focus_docks_hack, METH_VARARGS,
     "Focus dock windows when clicking focusable widgets in them."},    
    {"keyring_find_items_sync", (PyCFunction) bigboard_gnomekeyring_find_items_sync, METH_VARARGS | METH_KEYWORDS,
     "Find gnomekeyring items."},    
    {NULL, NULL, 0, NULL}        /* Sentinel */
};

PyMODINIT_FUNC
initbignative(void)
{
    PyObject *m, *d;
    init_pygobject();
    
    m = Py_InitModule("bignative", bignative_functions);
    d = PyModule_GetDict(m);

    /* Our gnome-keyring workaround depends on the gnomekeyring native types being
     * registered first, so we import here to force that. This leaks a reference
     * the loaded module, but module couldn't be unloaded anyways, since it registers
     * GObject types, so that shouldn't matter.
     */
    PyImport_ImportModule("gnomekeyring");
}
