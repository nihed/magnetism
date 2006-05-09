#include <stdafx.h>

#include "HippoGSignal.h"

Slot::~Slot ()
{


}

void
Slot::unref ()
{
  ref_count_ -= 1;
  if (ref_count_ == 0)
    delete this;
}

void
Slot::sink ()
{
if (floating_)
    {
      floating_ = false;
      unref ();
    }
}

static void
free_slot_closure(void *data, GClosure *closure)
{
    Slot *slot = static_cast<Slot*>(data);
    slot->unref();
}

static void
free_slot(void *data)
{
    Slot *slot = static_cast<Slot*>(data);
    slot->unref();
}


void
GConnection::disconnect()
{
    if (obj_ != NULL) {
        g_assert(id_ != 0);
        g_signal_handler_disconnect(obj_, id_);
        g_object_remove_weak_pointer(obj_, reinterpret_cast<void**>(& obj_));
        obj_ = NULL;
        id_ = 0;
    }
}

// it's probably fine to disconnect by data and not store the ids, since nobody 
// is ever going to bother reusing slots
void
GConnection::connect_impl(GObject *object, const char *signal, GCallback callback, Slot *slot)
{
    if (id_ != 0)
        disconnect();
    g_assert(id_ == 0);
    g_assert(obj_ == NULL);

    slot->ref();
    slot->sink();
    id_ = g_signal_connect_data(object, signal, callback, slot, free_slot_closure, G_CONNECT_AFTER);
    obj_ = object;
    g_object_add_weak_pointer(object, reinterpret_cast<void**>(& obj_));
}

GAbstractSource::~GAbstractSource()
{
    remove();
}

void
GAbstractSource::remove()
{
    if (source_ != NULL) {
        g_source_destroy(source_);
        source_ = NULL;
    }
}

void
GAbstractSource::add_impl(GSource *source, GSourceFunc callback, Slot *slot)
{
    if (source_ != NULL)
        remove();

    g_assert(source_ == NULL);

    slot->ref();
    slot->sink();

    source_ = source;
    g_source_set_callback(source_, callback, slot, free_slot);

    g_source_attach(source_, NULL);
    g_source_unref (source_);
}

void
GIdle::add(Slot0<bool> *slot)
{
    GSource *source = g_idle_source_new ();
    add_impl(source, gcallback, slot);
}

gboolean
GIdle::gcallback(void *data)
{
    Slot0<bool> *slot = static_cast<Slot0<bool>*>(data);
    return slot->invoke();
}

void
GTimeout::add(unsigned int milliseconds, Slot0<bool> *slot)
{
    GSource *source = g_timeout_source_new(milliseconds);
    add_impl(source, gcallback, slot);
}

gboolean
GTimeout::gcallback(void *data)
{
    Slot0<bool> *slot = static_cast<Slot0<bool>*>(data);
    return slot->invoke();
}
