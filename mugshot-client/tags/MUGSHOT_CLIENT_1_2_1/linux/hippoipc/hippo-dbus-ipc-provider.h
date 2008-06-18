/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IPC_PROVIDER_H__
#define __HIPPO_DBUS_IPC_PROVIDER_H__

#include "hippo-ipc.h"

class HippoDBusIpcProvider : public HippoIpcProvider
{
public:
    static HippoDBusIpcProvider *createInstance(const char *serverName);
};

#endif /* __HIPPO_DBUS_IPC_PROVIDER_H__ */
