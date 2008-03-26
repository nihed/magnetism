/* hippo-com-ipc-provider.h: HippoIpcProvider implementation via COM for Windows
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include "hippo-ipc.h"

/**
 * This class implements the HippoIpcProvider interface for communicating with the
 * Dumbhippo client via COM. Note that it is not threadsafe and must be created
 * and used only from the HippoComIpcHub executor thread.
 */
class HippoComIpcProvider : public HippoIpcProvider {
public:
    /**
     * Create a new instance that connects to the server specificied by serverName.
     * The instance must be freed with HippoIpcProvider::unref().
     **/
    static HippoComIpcProvider *createInstance(const char *serverName);
};
