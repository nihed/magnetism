#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

/* include this first, before NO_IMPORT_PYGOBJECT is defined */
#include <pygobject.h>

void pyhippocanvas_register_classes (PyObject *d);

extern PyMethodDef pyhippocanvas_functions[];

DL_EXPORT(void)
inithippocanvas(void)
{
    PyObject *m, *d;

    init_pygobject ();

    m = Py_InitModule ("hippocanvas", pyhippocanvas_functions);
    d = PyModule_GetDict (m);

    pyhippocanvas_register_classes (d);

    if (PyErr_Occurred ()) {
        Py_FatalError ("can't initialise module hippocanvas");
    }
}
