/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IPC_LOCATOR_H__
#define __HIPPO_DBUS_IPC_LOCATOR_H__

#include "hippoipc/hippo-ipc.h"

class HippoDBusIpcLocator : public HippoIpcLocator
{
public:
    static HippoDBusIpcLocator *getInstance();
};

#endif /* __HIPPO_DBUS_IPC_LOCATOR_H__ */
