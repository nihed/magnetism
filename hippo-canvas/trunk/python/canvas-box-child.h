/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __CANVAS_BOX_CHILD_H__
#define __CANVAS_BOX_CHILD_H__

#include <Python.h>
#include "pygobject.h"
#include <hippo/hippo-canvas-box.h>

G_BEGIN_DECLS

extern PyTypeObject PyHippoCanvasBoxChild_Type;

PyObject *py_hippo_canvas_box_child_new(HippoCanvasBoxChild *child);

G_END_DECLS

#endif /* __CANVAS_BOX_CHILD_H__ */
