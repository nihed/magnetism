/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __BIGBOARD_NATIVE_H__
#define __BIGBOARD_NATIVE_H__

#include <Python.h>
#include <glib.h>

G_BEGIN_DECLS

PyObject * bigboard_set_log_handler			(PyObject *self, PyObject *func);

G_END_DECLS

#endif /* __BIGBOARD_NATIVE_H__ */
