#include <nspr.h>
#include <plbase64.h>
#include <prtypes.h>
#include <nss.h>
#include <pk11pub.h>
#include <pk11sdr.h>

/* incompatibility between nspr and Python */
#undef HAVE_LONG_LONG

#include <Python.h>

#include <stdlib.h>
#include <unistd.h>

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

static PyObject *
pynss_decrypt(PyObject *self, PyObject *args)
{
  SECStatus s;
  SECItem request;
  SECItem reply;
  
  if (!PyArg_ParseTuple(args, "s#:pynss_decrypt", &(request.data), &(request.len)))
    return NULL;
  
  reply.data = 0;
  reply.len = 0;

  s = PK11SDR_Decrypt(&request, &reply, NULL);
  if (s != SECSuccess) {
    PyErr_Format(PyExc_KeyError, "failed to decrypt data (code %d)", s);
    return NULL;
  }
  return Py_BuildValue("z#", reply.data, reply.len);
}

static PyObject *
pynss_init(PyObject *self, PyObject *args)
{
  SECStatus s;
  char *profpath;
  
  if (!PyArg_ParseTuple(args, "s:pynss_init", &profpath))
    return NULL;
  if ((s = NSS_Init(profpath) != SECSuccess)) {
    PyErr_Format(PyExc_RuntimeError, "failed to init NSS (code %d)", s);
    return NULL;
  }
  Py_INCREF(Py_None);
  return Py_None;
}

void initnssdecrypt(void);

PyMethodDef pynssdecrypt_functions[] = {
  {"init", pynss_init, METH_VARARGS, "Initialize the NSS library"},
  {"decrypt", pynss_decrypt, METH_VARARGS, "Decrypt a byte array using default key"},
  {NULL, NULL, 0, NULL}        /* Sentinel */
};

PyMODINIT_FUNC
initnssdecrypt(void)
{
  PyObject *m, *d;
  m = Py_InitModule("nssdecrypt", pynssdecrypt_functions);
  d = PyModule_GetDict(m);
}
