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

/* Workaround gunge for gnome-keyring python binding bugs */
GType pygnome_keyring_found_get_type(void);
# define GNOME_KEYRING_TYPE_FOUND pygnome_keyring_found_get_type()

G_END_DECLS

#endif /* __BIGBOARD_NATIVE_H__ */
