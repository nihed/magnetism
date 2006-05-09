/* HippoGSignal.h: Utils for dealing with GObject signals
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <stdafx.h>
#include <glib-object.h>

class Slot
{
public:

    void ref ()
    {
        ref_count_ += 1;
    }

    void unref ();

    void sink ();

protected:
    Slot ()
        : ref_count_(1), floating_(true)
    {
    }

    virtual ~Slot () = 0;

private:
    unsigned int ref_count_;
    bool floating_;

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

protected:
    GConnection()
        : obj_(NULL), id_(0)
    {
    }

    void connect_impl(GObject *object, const char *signal, GCallback callback, Slot *slot);

private:
    GObject *obj_;
    guint id_;
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

private:
    static ReturnType gcallback(GObject *object, void *data) {
        Slot1<ReturnType> *slot = data;
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

private:
    static ReturnType gcallback(GObject *object, Arg1Type arg1, void *data) {
        Slot1<ReturnType,Arg1Type> *slot = data;
        return slot->invoke(arg1);
    }
};
