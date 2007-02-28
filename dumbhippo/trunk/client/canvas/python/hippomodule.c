#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

/* include this first, before NO_IMPORT_PYGOBJECT is defined */
#include <pygobject.h>

#include <pycairo.h>
Pycairo_CAPI_t *Pycairo_CAPI;

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-util.h>

void pyhippo_register_classes (PyObject *d);
void pyhippo_add_constants(PyObject *module, const gchar *strip_prefix);

extern PyMethodDef pyhippo_functions[];

static void
sink_hippocanvasbox(GObject *object)
{
    if (HIPPO_CANVAS_BOX(object)->floating) {
        g_object_ref(object);
        hippo_canvas_item_sink(HIPPO_CANVAS_ITEM(object));
    }
}

static PyObject *
_cairo_surface_from_gvalue(const GValue *value)
{
    return PycairoSurface_FromSurface((cairo_surface_t *) g_value_get_boxed(value), NULL);
}

static int
_cairo_surface_to_gvalue(GValue *value, PyObject *obj)
{
    //if (!(PyObject_IsInstance(obj, (PyObject *) &PycairoSurface_Type)))
    //    return -1;

    g_value_set_boxed(value, ((PycairoSurface*)(obj))->surface);
    return 0;
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
    PyModule_AddObject(m, "TYPE_CAIRO_SURFACE", pyg_type_wrapper_new(HIPPO_TYPE_CAIRO_SURFACE));
    pyg_register_gtype_custom(HIPPO_TYPE_CAIRO_SURFACE,
			      _cairo_surface_from_gvalue,
			      _cairo_surface_to_gvalue);


    if (PyErr_Occurred ()) {
        Py_FatalError ("can't initialise module hippo");
    }
}
