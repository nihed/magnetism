#include "stdafx-hippoui.h"

#include "HippoGSignal.h"

Slot::~Slot ()
{
    // a non-deleted slot has to have refcount > 0 and !deleted_,
    // so we have two chances to detected a deleted slot and throw
    // a nice assertion failure
    assert(ref_count_ == 0);
    deleted_ = true;
}

void
Slot::ref()
{
    g_return_if_fail(ref_count_ > 0);
    g_return_if_fail(!deleted_);

    ref_count_ += 1;
}

void
Slot::unref ()
{
    g_return_if_fail(ref_count_ > 0);
    g_return_if_fail(!deleted_);

    ref_count_ -= 1;
    if (ref_count_ == 0)
        delete this;
}

void
Slot::sink ()
{
    g_return_if_fail(ref_count_ > 0);
    g_return_if_fail(!deleted_);

    if (floating_) {
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
// is ever going to bother reusing slots. I guess storing the slot* is just as 
// expensive though.
void
GConnection::connect_impl(GObject *object, const char *signal, GCallback callback, Slot *slot)
{
    if (id_ != 0)
        disconnect();
    g_assert(id_ == 0);
    g_assert(obj_ == NULL);

    id_ = unmanaged_connect_impl(object, signal, callback, slot);
    obj_ = object;
    // FIXME I bet we could stick this on the closure somehow and avoid the extra weak ref
    // book-keeping
    g_object_add_weak_pointer(object, reinterpret_cast<void**>(& obj_));
}

void
GConnection::unmanaged_disconnect(GObject *object, unsigned int id)
{
    g_signal_handler_disconnect(object, id);
}

unsigned int
GConnection::unmanaged_connect_impl(GObject *object, const char *signal, GCallback callback, Slot *slot)
{
    slot->ref();
    slot->sink();
    return g_signal_connect_data(object, signal, callback, slot, free_slot_closure, G_CONNECT_AFTER);
}

void
GConnection::named_disconnect(GObject *object, const char *connection_name)
{
    void *id_ptr = g_object_get_data(object, connection_name);
    // to fix the warning we need to use C++-style casts, but GPOINTER_TO_UINT 
    // has a little magic beyond just a cast so skipping it for now FIXME
    unsigned int id = GPOINTER_TO_UINT(id_ptr);
    if (id != 0) {
        g_signal_handler_disconnect(object, id);
        g_object_set_data(object, connection_name, NULL);
    }
}

void
GConnection::named_connect_impl(GObject *object, const char *connection_name,
                                const char *signal, GCallback callback, Slot *slot)
{
    void *id_ptr = g_object_get_data(object, connection_name);
    // to fix the warning we need to use C++-style casts, but GPOINTER_TO_UINT 
    // has a little magic beyond just a cast so skipping it for now FIXME
    unsigned int id = GPOINTER_TO_UINT(id_ptr);
    
    if (id != 0) {
        g_signal_handler_disconnect(object, id);
    }

    id = unmanaged_connect_impl(object, signal, callback, slot);    
    id_ptr = GUINT_TO_POINTER(id);
    g_object_set_data(object, connection_name, id_ptr);
}

GAbstractSource::~GAbstractSource()
{
    remove();
}

void
GAbstractSource::remove()
{
    if (source_ != NULL) {
        // source may already be destroyed...
        if ((source_->flags & G_HOOK_FLAG_ACTIVE) != 0)
            g_source_destroy(source_);
        g_source_unref (source_);
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
    // keep source ref so we can handle the source returning false to remove itself
}

void
GIdle::add(Slot0<bool> *slot)
{
    GSource *source = g_idle_source_new ();
    add_impl(source, gcallback, slot);
}

void
GIdle::add(Slot0<bool> *slot, int priority)
{
    GSource *source = g_idle_source_new ();
    g_source_set_priority (source, priority);
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
