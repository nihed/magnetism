/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __BIGBOARD_NATIVE_H__
#define __BIGBOARD_NATIVE_H__

#include <Python.h>
#include <glib-object.h>

G_BEGIN_DECLS

PyObject * bigboard_set_log_handler			(PyObject *self, PyObject *func);
PyObject*  bigboard_set_application_name                (PyObject *self, PyObject *args);
PyObject*  bigboard_set_program_name                    (PyObject *self, PyObject *args);
PyObject*  bigboard_gnomekeyring_find_items_sync        (PyObject *self, PyObject *args, PyObject *kwargs);
PyObject*  bigboard_install_focus_docks_hack            (PyObject *self, PyObject *args);
PyObject*  bigboard_utf8_collate                        (PyObject *self, PyObject *args);

G_END_DECLS

#endif /* __BIGBOARD_NATIVE_H__ */
