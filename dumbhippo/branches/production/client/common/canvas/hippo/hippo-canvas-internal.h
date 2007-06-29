/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_INTERNAL_H__
#define __HIPPO_CANVAS_INTERNAL_H__

#include <glib-object.h>

G_BEGIN_DECLS

/* we don't do i18n on Windows yet */
#ifndef G_OS_WIN32
#include <config.h>
#endif
#ifdef GETTEXT_PACKAGE
#include <glib/gi18n-lib.h>
#else
#define _(x) x
#endif

/* These don't really belong here, but whatever... */
#define HIPPO_ADD_WEAK(ptr)    g_object_add_weak_pointer(G_OBJECT(*(ptr)), (void**) (char*) (ptr))
#define HIPPO_REMOVE_WEAK(ptr) do { if (*ptr) { g_object_remove_weak_pointer(G_OBJECT(*(ptr)), (void**) (char*) (ptr)); *ptr = NULL; } } while(0)

G_END_DECLS

#endif /* __HIPPO_CANVAS_INTERNAL_H__ */
