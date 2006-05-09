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

static void
free_slot(void *data, GClosure *closure)
{
    Slot *slot = static_cast<Slot*>(data);
    slot->unref();
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
    id_ = g_signal_connect_data(object, signal, callback, slot, free_slot, G_CONNECT_AFTER);
    obj_ = object;
    g_object_add_weak_pointer(object, reinterpret_cast<void**>(& obj_));
}
