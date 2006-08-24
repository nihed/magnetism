/* HippoGSignal.h: Utils for dealing with GObject signals
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <glib-object.h>

// Docs can be found at http://developer.mugshot.org/wiki/HippoGSignal. If you make changes
// here, please make sure that the docs stay up to date.
//
// This file has underscore_names instead of studlyCaps for no particular reason 
// (well, it's copied from the old Inti codebase that was like that, and it 
// feels sort of natural since it's a GLib related file)
// change it all at once, or else stay consistent, don't mix in caps at random.

class Slot
{
public:

    void ref ();

    void unref ();

    void sink ();

protected:
    Slot ()
        : ref_count_(1), floating_(true), deleted_(false)
    {
    }

    virtual ~Slot () = 0;

private:
    unsigned int ref_count_;
    unsigned int floating_ : 1;
    unsigned int deleted_ : 1;

    // not copyable
    Slot (const Slot&);
    Slot& operator=(const Slot&);
}; // end class Slot

#include "HippoSignals.h"

class GConnection
{
public:
    ~GConnection() {
        disconnect();
    }

    void disconnect();

    static void unmanaged_disconnect(GObject *object, unsigned int id);
    static void named_disconnect(GObject *object, const char *connection_name);

protected:
    GConnection()
        : obj_(NULL), id_(0)
    {
    }

    void connect_impl(GObject *object, const char *signal, GCallback callback, Slot *slot);

    static unsigned int unmanaged_connect_impl(GObject *object, const char *signal, GCallback callback, Slot *slot);
    static void named_connect_impl(GObject *object, const char *connection_name,
                                   const char *signal, GCallback callback, Slot *slot);

private:
    GObject *obj_;
    guint id_;
    
    // disallow copying; to allow this we'd need a refcounted "impl" object inside the connection
    GConnection(const GConnection &other);
    const GConnection &operator=(const GConnection &other);

}; // end of class Connection

/*
 * 
 * Use GConnection by having a member variable like:
 *   GConnection1<void,int> hotnessChanged_;
 * 
 * then when the object you want to connect to is available:
 * 
 *   hotnessChangedConnection_.connect(obj, "hotness-changed", slot(this, &HippoUI::onHotnessChanged));
 * 
 * If obj is destroyed the connection is automatically disconnected, and if you connect
 * to another object it's disconnected, and the GConnection's destructor also disconnects.
 * So there's no cleanup to worry about after the initial connect.
 * 
 * FIXME the little codegen.py script needs extending to autogenerate the GConnection variants
 */

// A connection to a signal with 0 arguments
template <class ReturnType>
class GConnection0 : public GConnection
{
public:
    GConnection0()
    {
    }

    void connect(GObject *object, const char *signal, Slot0<ReturnType> *slot) {
        connect_impl(object, signal, (GCallback) gcallback, slot);
    }

    static unsigned int unmanaged_connect(GObject *object, const char *signal, Slot0<ReturnType> *slot) {
        return unmanaged_connect_impl(object, signal, (GCallback) gcallback, slot);
    }

    static void named_connect(GObject *object, const char *connection_name, const char *signal, Slot0<ReturnType> *slot) {
        return named_connect_impl(object, connection_name, signal, (GCallback) gcallback, slot);
    }

private:
    static ReturnType gcallback(GObject *object, void *data) {
        Slot0<ReturnType> *slot = static_cast<Slot0<ReturnType>*>(data);
        return slot->invoke();
    }
};

// A connection to a signal with 1 arguments
template <class ReturnType, class Arg1Type>
class GConnection1 : public GConnection
{
public:
    GConnection1()
    {
    }

    void connect(GObject *object, const char *signal, Slot1<ReturnType,Arg1Type> *slot) {
        connect_impl(object, signal, (GCallback) gcallback, slot);
    }

    static unsigned int unmanaged_connect(GObject *object, const char *signal, Slot1<ReturnType,Arg1Type> *slot) {
        return unmanaged_connect_impl(object, signal, (GCallback) gcallback, slot);
    }
    
    static void named_connect(GObject *object, const char *connection_name, const char *signal, Slot1<ReturnType,Arg1Type> *slot) {
        return named_connect_impl(object, connection_name, signal, (GCallback) gcallback, slot);
    }

private:
    static ReturnType gcallback(GObject *object, Arg1Type arg1, void *data) {
        Slot1<ReturnType,Arg1Type> *slot = static_cast<Slot1<ReturnType,Arg1Type>*>(data);
        return slot->invoke(arg1);
    }
};

// A connection to a signal with 2 arguments
template <class ReturnType, class Arg1Type, class Arg2Type>
class GConnection2 : public GConnection
{
public:
    GConnection2()
    {
    }

    void connect(GObject *object, const char *signal, Slot2<ReturnType,Arg1Type,Arg2Type> *slot) {
        connect_impl(object, signal, (GCallback) gcallback, slot);
    }

    static unsigned int unmanaged_connect(GObject *object, const char *signal, Slot2<ReturnType,Arg1Type,Arg2Type> *slot) {
        return unmanaged_connect_impl(object, signal, (GCallback) gcallback, slot);
    }
    
    static void named_connect(GObject *object, const char *connection_name, const char *signal, Slot2<ReturnType,Arg1Type,Arg2Type> *slot) {
        return named_connect_impl(object, connection_name, signal, (GCallback) gcallback, slot);
    }

private:
    static ReturnType gcallback(GObject *object, Arg1Type arg1, Arg2Type arg2, void *data) {
        Slot2<ReturnType,Arg1Type,Arg2Type> *slot = static_cast<Slot2<ReturnType,Arg1Type,Arg2Type>*>(data);
        return slot->invoke(arg1, arg2);
    }
};

// A connection to a signal with 3 arguments
template <class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
class GConnection3 : public GConnection
{
public:
    GConnection3()
    {
    }

    void connect(GObject *object, const char *signal, Slot3<ReturnType,Arg1Type,Arg2Type,Arg3Type> *slot) {
        connect_impl(object, signal, (GCallback) gcallback, slot);
    }

    static unsigned int unmanaged_connect(GObject *object, const char *signal, Slot3<ReturnType,Arg1Type,Arg2Type,Arg3Type> *slot) {
        return unmanaged_connect_impl(object, signal, (GCallback) gcallback, slot);
    }
    
    static void named_connect(GObject *object, const char *connection_name, const char *signal, Slot3<ReturnType,Arg1Type,Arg2Type,Arg3Type> *slot) {
        return named_connect_impl(object, connection_name, signal, (GCallback) gcallback, slot);
    }

private:
    static ReturnType gcallback(GObject *object, Arg1Type arg1, Arg2Type arg2, Arg3Type arg3, void *data) {
        Slot3<ReturnType,Arg1Type,Arg2Type,Arg3Type> *slot = static_cast<Slot3<ReturnType,Arg1Type,Arg2Type,Arg3Type>*>(data);
        return slot->invoke(arg1, arg2, arg3);
    }
};


class GAbstractSource
{
public:
    virtual ~GAbstractSource();

    void remove();

protected:
    GAbstractSource()
        : source_(NULL)
    {
    }

    void add_impl(GSource *source, GSourceFunc callback, Slot *slot);

private:
    GSource *source_;

    GAbstractSource(const GAbstractSource &other);
    const GAbstractSource &operator=(const GAbstractSource &other);

}; // end of class GAbstractSource

class GIdle
    : public GAbstractSource
{
public:
    GIdle()
    {
    }

    void add(Slot0<bool> *slot);

private:
    static gboolean gcallback(void *data);

}; // end of class GIdle

class GTimeout
    : public GAbstractSource
{
public:
    GTimeout()
    {
    }

    void add(unsigned int milliseconds, Slot0<bool> *slot);

private:
    static gboolean gcallback(void *data);

}; // end of class GTimeout
