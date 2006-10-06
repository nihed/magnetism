#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

/* include this first, before NO_IMPORT_PYGOBJECT is defined */
#include <pygobject.h>

#include <pycairo.h>
Pycairo_CAPI_t *Pycairo_CAPI;

#include <hippo/hippo-canvas-box.h>

void pyhippo_register_classes (PyObject *d);
void pyhippo_add_constants(PyObject *module, const gchar *strip_prefix);

extern PyMethodDef pyhippo_functions[];

static void
sink_hippocanvasbox(GObject *object)
{
    HIPPO_CANVAS_BOX(object)->floating = FALSE;
}

DL_EXPORT(void)
inithippo(void)
{
    PyObject *m, *d;

    init_pygobject ();

    Pycairo_IMPORT;

    m = Py_InitModule("hippo", pyhippo_functions);
    d = PyModule_GetDict(m);

    pygobject_register_sinkfunc(HIPPO_TYPE_CANVAS_BOX, sink_hippocanvasbox);

    pyhippo_register_classes(d);
    pyhippo_add_constants(m, "HIPPO_");

    if (PyErr_Occurred ()) {
        Py_FatalError ("can't initialise module hippo");
    }
}
