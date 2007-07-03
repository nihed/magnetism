/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <Python.h>
#include "bigboard-native.h"

/* we don't define NO_IMPORT so here we define the _PyGObject_API variable */
#include <pygobject.h>

void initbignative(void);
static void register_classes(PyObject *d);

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
    register_classes(d);
}

/* ----------- GnomeKeyringFound ----------- */
#include <gnome-keyring.h>

static int
pygobject_no_constructor(PyObject *self, PyObject *args, PyObject *kwargs)
{
    gchar buf[512];

    g_snprintf(buf, sizeof(buf), "%s is an abstract widget", self->ob_type->tp_name);
    PyErr_SetString(PyExc_NotImplementedError, buf);
    return -1;
}


static PyObject *
pygnome_keyring_attribute_list_as_pyobject(GnomeKeyringAttributeList *attrlist)
{
    PyObject *py_attrlist;
    int i, len = ((GArray *) attrlist)->len;

    py_attrlist = PyDict_New();
    for (i = 0; i < len; ++i) {
        GnomeKeyringAttribute *attr;
        PyObject *val = NULL;

        attr = &gnome_keyring_attribute_list_index(attrlist, i);

        switch (attr->type)
        {
        case GNOME_KEYRING_ATTRIBUTE_TYPE_STRING:
            val = PyString_FromString(attr->value.string);
            break;
	case GNOME_KEYRING_ATTRIBUTE_TYPE_UINT32:
            val = PyLong_FromUnsignedLong(attr->value.integer);
            break;
        default:
            Py_DECREF(py_attrlist);
            PyErr_SetString(PyExc_AssertionError, "invalided GnomeKeyringAttributeType"
                            " (congratulations, you found bug in bindings or C library)");
            return NULL;
        }

        if (PyDict_SetItemString(py_attrlist, attr->name, val)) {
            Py_DECREF(py_attrlist);
            return NULL;
        }
    }

    return py_attrlist;
}


static GnomeKeyringFound *
pygnome_keyring_found_copy (GnomeKeyringFound *found)
{
    GnomeKeyringFound *copy;

    copy = g_new (GnomeKeyringFound, 1);
    memcpy (copy, found, sizeof (GnomeKeyringFound));

    copy->keyring = g_strdup (copy->keyring);
    copy->attributes = gnome_keyring_attribute_list_copy (copy->attributes);
    copy->secret = g_strdup (copy->secret);

    return copy;
}

GType
pygnome_keyring_found_get_type(void)
{
    static GType our_type = 0;
  
    if (our_type == 0)
        our_type = g_boxed_type_register_static("PyGnomeKeyringFound",
                                                (GBoxedCopyFunc)pygnome_keyring_found_copy,
                                                (GBoxedFreeFunc)gnome_keyring_found_free);
    return our_type;
}

static PyObject *
_wrap_gnome_keyring_found__get_keyring(PyObject *self, void *closure)
{
    const gchar *ret;

    ret = pyg_boxed_get(self, GnomeKeyringFound)->keyring;
    if (ret)
        return PyString_FromString(ret);
    Py_INCREF(Py_None);
    return Py_None;
}

static PyObject *
_wrap_gnome_keyring_found__get_item_id(PyObject *self, void *closure)
{
    guint ret;

    ret = pyg_boxed_get(self, GnomeKeyringFound)->item_id;
    return PyLong_FromUnsignedLong(ret);
}

static PyObject *
_wrap_gnome_keyring_found__get_attributes(PyObject *self, void *closure)
{
    GnomeKeyringAttributeList* ret;

    ret = pyg_boxed_get(self, GnomeKeyringFound)->attributes;
    return pygnome_keyring_attribute_list_as_pyobject(ret);
}

static PyObject *
_wrap_gnome_keyring_found__get_secret(PyObject *self, void *closure)
{
    const gchar *ret;

    ret = pyg_boxed_get(self, GnomeKeyringFound)->secret;
    if (ret)
        return PyString_FromString(ret);
    Py_INCREF(Py_None);
    return Py_None;
}

static const PyGetSetDef gnome_keyring_found_getsets[] = {
    { "keyring", (getter)_wrap_gnome_keyring_found__get_keyring, (setter)0 },
    { "item_id", (getter)_wrap_gnome_keyring_found__get_item_id, (setter)0 },
    { "attributes", (getter)_wrap_gnome_keyring_found__get_attributes, (setter)0 },
    { "secret", (getter)_wrap_gnome_keyring_found__get_secret, (setter)0 },
    { NULL, (getter)0, (setter)0 },
};

PyTypeObject G_GNUC_INTERNAL PyGnomeKeyringFound_Type = {
    PyObject_HEAD_INIT(NULL)
    0,                                 /* ob_size */
    "Found",                   /* tp_name */
    sizeof(PyGBoxed),          /* tp_basicsize */
    0,                                 /* tp_itemsize */
    /* methods */
    (destructor)0,        /* tp_dealloc */
    (printfunc)0,                      /* tp_print */
    (getattrfunc)0,       /* tp_getattr */
    (setattrfunc)0,       /* tp_setattr */
    (cmpfunc)0,           /* tp_compare */
    (reprfunc)0,             /* tp_repr */
    (PyNumberMethods*)0,     /* tp_as_number */
    (PySequenceMethods*)0, /* tp_as_sequence */
    (PyMappingMethods*)0,   /* tp_as_mapping */
    (hashfunc)0,             /* tp_hash */
    (ternaryfunc)0,          /* tp_call */
    (reprfunc)0,              /* tp_str */
    (getattrofunc)0,     /* tp_getattro */
    (setattrofunc)0,     /* tp_setattro */
    (PyBufferProcs*)0,  /* tp_as_buffer */
    Py_TPFLAGS_DEFAULT | Py_TPFLAGS_BASETYPE,                      /* tp_flags */
    NULL,                        /* Documentation string */
    (traverseproc)0,     /* tp_traverse */
    (inquiry)0,             /* tp_clear */
    (richcmpfunc)0,   /* tp_richcompare */
    0,             /* tp_weaklistoffset */
    (getiterfunc)0,          /* tp_iter */
    (iternextfunc)0,     /* tp_iternext */
    (struct PyMethodDef*)NULL, /* tp_methods */
    (struct PyMemberDef*)0,              /* tp_members */
    (struct PyGetSetDef*)gnome_keyring_found_getsets,  /* tp_getset */
    NULL,                              /* tp_base */
    NULL,                              /* tp_dict */
    (descrgetfunc)0,    /* tp_descr_get */
    (descrsetfunc)0,    /* tp_descr_set */
    0,                 /* tp_dictoffset */
    (initproc)pygobject_no_constructor,             /* tp_init */
    (allocfunc)0,           /* tp_alloc */
    (newfunc)0,               /* tp_new */
    (freefunc)0,             /* tp_free */
    (inquiry)0              /* tp_is_gc */
};


static void
register_classes(PyObject *d)
{
    pyg_register_boxed(d, "Found", GNOME_KEYRING_TYPE_FOUND, &PyGnomeKeyringFound_Type);
}
