/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "canvas-box-child.h"

typedef struct {
    PyObject_HEAD
    HippoCanvasBoxChild *child;
    PyObject *dict;
} PyHippoCanvasBoxChild;


static int
no_constructor(PyObject *self, PyObject *args, PyObject *kwargs)
{
    PyErr_SetString(PyExc_NotImplementedError, "Cannot create a new HippoCanvasBoxChild");
    return -1;
}

static GQuark
pyhippo_proxy_quark(void)
{
    static GQuark quark = 0;
    if (quark == 0)
	quark = g_quark_from_static_string("pyhippo-proxy");
    
    return quark;
}

static void
py_hippo_canvas_box_child_dealloc(PyHippoCanvasBoxChild* self)
{
    if (self->child != NULL) {
	g_warning("Python proxy freed before box child");
	hippo_canvas_box_child_set_qdata(self->child, pyhippo_proxy_quark(), NULL, NULL);
    }
    
    Py_CLEAR(self->dict);
    self->ob_type->tp_free(self);
}

static PyObject *
_wrap_hippo_canvas_box_child_get_width_request(PyHippoCanvasBoxChild *self)
{
    int min_width = 0;
    int natural_width = 0;

    if (self->child == NULL) {
	PyErr_SetString(PyExc_RuntimeError, "HippoCanvasBoxChild is destroyed");
	return NULL;
    }

    hippo_canvas_box_child_get_width_request(self->child, &min_width, &natural_width);

    return Py_BuildValue("(ii)", min_width, natural_width);
}

static PyObject *
_wrap_hippo_canvas_box_child_get_height_request(PyHippoCanvasBoxChild *self, PyObject *args, PyObject *kwargs)
{
    static char *kwlist[] = { "for_width", NULL };
    int min_height = 0;
    int natural_height = 0;
    int for_width;

    if (self->child == NULL) {
	PyErr_SetString(PyExc_RuntimeError, "HippoCanvasBoxChild is destroyed");
	return NULL;
    }

    if (!PyArg_ParseTupleAndKeywords(args, kwargs,"i:HippoCanvasBoxChild.get_height_request", kwlist, &for_width))
        return NULL;

    hippo_canvas_box_child_get_height_request(self->child, for_width, &min_height, &natural_height);

    return Py_BuildValue("(ii)", min_height, natural_height);
}

static PyObject *
_wrap_hippo_canvas_box_child_allocate(PyHippoCanvasBoxChild *self, PyObject *args, PyObject *kwargs)
{
    static char *kwlist[] = { "x", "y", "width", "height", "origin_changed", NULL };
    int x, y, width, height, origin_changed;

    if (self->child == NULL) {
	PyErr_SetString(PyExc_RuntimeError, "HippoCanvasBoxChild is destroyed");
	return NULL;
    }

    if (!PyArg_ParseTupleAndKeywords(args, kwargs,"iiiii:HippoCanvasBoxChild.allocate", kwlist, &x, &y, &width, &height, &origin_changed))
        return NULL;
    
    hippo_canvas_box_child_allocate(self->child, x, y, width, height, origin_changed);
    
    Py_INCREF(Py_None);
    return Py_None;
}

static const PyMethodDef _PyHippoCanvasBoxChild_methods[] = {
    { "get_width_request", (PyCFunction)_wrap_hippo_canvas_box_child_get_width_request, METH_NOARGS,
      NULL },
    { "get_height_request", (PyCFunction)_wrap_hippo_canvas_box_child_get_height_request, METH_VARARGS|METH_KEYWORDS,
      NULL },
    { "allocate", (PyCFunction)_wrap_hippo_canvas_box_child_allocate, METH_VARARGS|METH_KEYWORDS,
      NULL },
    { NULL, NULL, 0, NULL }
};

static PyObject *
_wrap_hippo_canvas_box_child__get_item(PyObject *self, void *closure)
{
    HippoCanvasItem *ret;

    ret = HIPPO_CANVAS_BOX_CHILD(pygobject_get(self))->item;
    /* pygobject_new handles NULL checking */
    return pygobject_new((GObject *)ret);
}

static PyObject *
_wrap_hippo_canvas_box_child__get_visible(PyObject *self, void *closure)
{
    gboolean ret;
    
    ret = HIPPO_CANVAS_BOX_CHILD(pygobject_get(self))->item;

    return PyBool_FromLong(ret);
}

static const PyGetSetDef hippo_canvas_box_child_getsets[] = {
    { "item",    (getter)_wrap_hippo_canvas_box_child__get_item, (setter)0 },
    { "visible", (getter)_wrap_hippo_canvas_box_child__get_visible, (setter)0 },    
    { NULL, (getter)0, (setter)0 },
};

PyTypeObject G_GNUC_INTERNAL PyHippoCanvasBoxChild_Type = {
    PyObject_HEAD_INIT(NULL)
    0,                                            /* ob_size */
    "hippo.CanvasBoxChild",                       /* tp_name */
    sizeof(PyHippoCanvasBoxChild),                /* tp_basicsize */
    0,                                            /* tp_itemsize */
    /* methods */
    (destructor)py_hippo_canvas_box_child_dealloc, /* tp_dealloc */
    (printfunc)0,                                 /* tp_print */
    (getattrfunc)0,                               /* tp_getattr */
    (setattrfunc)0,                               /* tp_setattr */
    (cmpfunc)0,                                   /* tp_compare */
    (reprfunc)0,                                  /* tp_repr */
    (PyNumberMethods*)0,                          /* tp_as_number */
    (PySequenceMethods*)0,                        /* tp_as_sequence */
    (PyMappingMethods*)0,                         /* tp_as_mapping */
    (hashfunc)0,                                  /* tp_hash */
    (ternaryfunc)0,                               /* tp_call */
    (reprfunc)0,                                  /* tp_str */
    (getattrofunc)0,                              /* tp_getattro */
    (setattrofunc)0,                              /* tp_setattro */
    (PyBufferProcs*)0,                            /* tp_as_buffer */
    Py_TPFLAGS_HAVE_CLASS,                        /* tp_flags */
    NULL,                                         /* Documentation string */
    (traverseproc)0,                              /* tp_traverse */
    (inquiry)0,                                   /* tp_clear */
    (richcmpfunc)0,                               /* tp_richcompare */
    0,                                            /* tp_weaklistoffset */
    (getiterfunc)0,                               /* ;tp_iter */
    (iternextfunc)0,                              /* tp_iternext */
    (struct PyMethodDef*)_PyHippoCanvasBoxChild_methods, /* tp_methods */
    (struct PyMemberDef*)0,                       /* tp_members */
    (struct PyGetSetDef*)hippo_canvas_box_child_getsets, /* tp_getset */
    NULL,                                         /* tp_base */
    NULL,                                         /* tp_dict */
    (descrgetfunc)0,                              /* tp_descr_get */
    (descrsetfunc)0,                              /* tp_descr_set */
    offsetof(PyHippoCanvasBoxChild, dict),        /* tp_dictoffset */
    (initproc)no_constructor,                     /* tp_init */
    (allocfunc)0,                                 /* tp_alloc */
    (newfunc)0,                                   /* tp_new */
    (freefunc)0,                                  /* tp_free */
    (inquiry)0                                    /* tp_is_gc */
};

static void
free_python_proxy(void *data)
{
    PyGILState_STATE state = pyg_gil_state_ensure();
    PyHippoCanvasBoxChild *obj = data;

    obj->child = NULL;
    Py_DECREF(obj);
    
    pyg_gil_state_release(state);
}

PyObject *
py_hippo_canvas_box_child_new(HippoCanvasBoxChild *child)
{
    PyHippoCanvasBoxChild *obj = hippo_canvas_box_child_get_qdata(child, pyhippo_proxy_quark());
    if (obj  == NULL) {
	obj = PyObject_NEW(PyHippoCanvasBoxChild, &PyHippoCanvasBoxChild_Type);

	obj->child = child;
	obj->dict = NULL;

	hippo_canvas_box_child_set_qdata(child, pyhippo_proxy_quark(), obj, free_python_proxy);
    }

    Py_INCREF(obj);
    return (PyObject *)obj;
}
