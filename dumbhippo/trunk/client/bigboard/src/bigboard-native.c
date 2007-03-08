/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "bigboard-native.h"

#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>

#include <glib.h>

/*  NO_IMPORT declares _PyGObject_Functions extern instead of definint them, which happens in bignative.c */
#define NO_IMPORT_PYGOBJECT
#include <pygobject.h>

#include <gnome-keyring.h>

static PyObject *logging_cb = NULL;

static gboolean initialized_loghandler = FALSE;
static void 
log_handler(const char    *log_domain,
            GLogLevelFlags log_level,
            const char    *message,
            void          *user_data)
{
    PyObject *arglist = NULL;
    PyObject *result;
    
    if (!initialized_loghandler)
        return;
    
    arglist = Py_BuildValue("(sis)", log_domain, log_level, message);
    result = PyEval_CallObject(logging_cb, arglist);
    Py_DECREF(arglist);
    if (result == NULL)
        return;
    Py_DECREF(result);	
}

PyObject *
bigboard_set_log_handler(PyObject *self, PyObject *args)
{
    PyObject *result = NULL;
    PyObject *temp;

    if (PyArg_ParseTuple(args, "O:bigboard_set_log_handler", &temp)) {
        if (!PyCallable_Check(temp)) {
            PyErr_SetString(PyExc_TypeError, "parameter must be callable");
            return NULL;
        }
        Py_XINCREF(temp);
        Py_XDECREF(logging_cb);
      	logging_cb = temp;
      	if (!initialized_loghandler) {
    		g_log_set_handler(NULL,
            		          (GLogLevelFlags) (G_LOG_LEVEL_MASK | G_LOG_FLAG_FATAL | G_LOG_FLAG_RECURSION),
                    	      log_handler, NULL);      		
      		initialized_loghandler = TRUE;
      	}
        Py_INCREF(Py_None);
        result = Py_None;
    }
    return result;
}

PyObject*
bigboard_set_application_name(PyObject *self, PyObject *args)
{
    PyObject *result = NULL;
    char *s;

    if (PyArg_ParseTuple(args, "s:bigboard_set_application_name", &s)) {
        /* my impression from the python docs is that "s" is not owned by us so not freed */
        g_set_application_name(s);
        
        Py_INCREF(Py_None);
        result = Py_None;
    }
    return result;
}

PyObject*
bigboard_set_program_name(PyObject *self, PyObject *args)
{
    PyObject *result = NULL;
    char *s;

    if (PyArg_ParseTuple(args, "s:bigboard_set_program_name", &s)) {
        /* my impression from the python docs is that "s" is not owned by us so not freed */
        g_set_prgname(s);
        
        Py_INCREF(Py_None);
        result = Py_None;
    }
    return result;
}


/* gnome-keyring stuff here is because find_items_sync() is broken in the official bindings */

static GnomeKeyringAttributeList *
pygnome_keyring_attribute_list_from_pyobject(PyObject *py_attrlist)
{
    GnomeKeyringAttributeList *attrlist;
    int iter = 0; /* Unfortunately this is supposed to be Py_ssize_t with newer python I think, but using ssize_t or gssize or long just warns on this version I'm using */
    PyObject *key, *value;
    
    if (!PyDict_Check(py_attrlist)) {
        PyErr_SetString(PyExc_TypeError, "dict expected for attribute list parameter");
        return NULL;
    }

    attrlist = gnome_keyring_attribute_list_new();
    while (PyDict_Next(py_attrlist, &iter, &key, &value)) {
        char *name;
        if (!PyString_Check(key)) {
            PyErr_SetString(PyExc_TypeError, "dict keys must be strings, when converting attribute list parameter");
            gnome_keyring_attribute_list_free(attrlist);
            return NULL;
        }
        name = PyString_AsString(key);
        if (PyInt_Check(value))
            gnome_keyring_attribute_list_append_uint32(attrlist, name,
                                                       PyInt_AsLong(value));
        else if (PyLong_Check(value)) {
            gnome_keyring_attribute_list_append_uint32(attrlist, name,
                                                       PyLong_AsUnsignedLong(value));
            if (PyErr_Occurred()) {
                gnome_keyring_attribute_list_free(attrlist);
                return NULL;
            }
        }
        else if (PyString_Check(value))
            gnome_keyring_attribute_list_append_string(attrlist, name,
                                                       PyString_AsString(value));
        else {
            PyErr_SetString(PyExc_TypeError, "dict values must be strings, ints or longs,"
                            " when converting attribute list parameter");
            gnome_keyring_attribute_list_free(attrlist);
            return NULL;
        }
    }
    return attrlist;
}

PyObject*
bigboard_gnomekeyring_find_items_sync(PyObject *self, PyObject *args, PyObject *kwargs)
{
    static char *kwlist[] = { "type", "attributes", NULL };
    PyObject *py_type = NULL;
    GnomeKeyringAttributeList * attributes;
    int type; /* GnomeKeyringItemType */
    gint ret;
    PyObject * py_attributes = NULL;
    GList *found = NULL, *l;
    PyObject *py_found;
    
    if (!PyArg_ParseTupleAndKeywords(args, kwargs,"OO:find_items_sync", kwlist, &py_type, &py_attributes))
        return NULL;
    if (pyg_enum_get_value(G_TYPE_NONE, py_type, &type))
        return NULL;
    attributes = pygnome_keyring_attribute_list_from_pyobject(py_attributes);
    if (!attributes)
        return NULL;
    pyg_begin_allow_threads;
    ret = gnome_keyring_find_items_sync(type, attributes, &found);
    pyg_end_allow_threads;
    gnome_keyring_attribute_list_free(attributes);

    py_found = PyList_New(0);
    for (l = found; l; l = l->next)
    {
        PyObject *item = pyg_boxed_new(GNOME_KEYRING_TYPE_FOUND, l->data, FALSE, TRUE);
        PyList_Append(py_found, item);
        Py_DECREF(item);
    }
    g_list_free(found);

    if (ret == GNOME_KEYRING_RESULT_OK) {
        return py_found;
    } else {
        PyErr_SetString(PyExc_TypeError, "gnome-keyring returned not OK (TypeError is just bogus, ignore that)");
        return NULL;
    }
}
