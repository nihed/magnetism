#! /bin/sh

/bin/rm -f massif.*.ps massif.*.txt

G_SLICE=always-malloc G_DEBUG=gc-friendly \
   valgrind --tool=massif --depth=5 \
   --alloc-fn=g_malloc --alloc-fn=g_malloc0 --alloc-fn=posix_memalign \
   --alloc-fn=g_slice_alloc --alloc-fn=g_mem_chunk_alloc --alloc-fn=g_try_malloc \
   --alloc-fn=dbus_malloc --alloc-fn=dbus_malloc0 --alloc-fn=g_realloc \
   --alloc-fn=dbus_realloc --alloc-fn=g_strdup --alloc-fn=g_cclosure_new \
   --alloc-fn=g_closure_new_simple \
   --alloc-fn=FT_Alloc --alloc-fn=FT_QAlloc \
   --alloc-fn=g_slice_alloc0 \
   --alloc-fn=g_type_create_instance --alloc-fn=g_object_newv --alloc-fn=g_object_new \
  ./mugshot $*

echo massif*

