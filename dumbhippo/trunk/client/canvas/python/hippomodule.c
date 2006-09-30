#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

/* include this first, before NO_IMPORT_PYGOBJECT is defined */
#include <pygobject.h>

#include <pycairo.h>
Pycairo_CAPI_t *Pycairo_CAPI;

void pyhippo_register_classes (PyObject *d);

DL_EXPORT(void)
inithippo(void)
{
    PyObject *m, *d;

    init_pygobject ();

    Pycairo_IMPORT;

    m = Py_InitModule ("hippo", NULL);
    d = PyModule_GetDict (m);

    pyhippo_register_classes (d);

    if (PyErr_Occurred ()) {
        Py_FatalError ("can't initialise module hippo");
    }
}
