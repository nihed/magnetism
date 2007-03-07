/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "bigboard-native.h"

#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>

#include <glib.h>

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

