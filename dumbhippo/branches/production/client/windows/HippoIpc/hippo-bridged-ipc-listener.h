/* hippo-bridged-ipc-controller.h: Wrapper for HippoIpcListener that proxies across threads
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "hippo-ipc.h"

/**
 * This class dispatches wraps a HippoIpcListener so that calls against the 
 * wrapper can be made safely from any thread and will be dispatched to the
 * thread where the wrapper was created. This is used as an implementation
 * detial of HippoBridgedIpcController.
 */
class HippoBridgedIpcListener : public HippoIpcListener
{
public:
    /**
     * Create a wrapper for inner. Must be deleted from the same thread where
     * it was created, but the HippoIpcListener methods can be called from
     * any thread.
     */
    static HippoBridgedIpcListener *createInstance(HippoIpcListener *inner);

    /**
     * Get the wrapped HippoIpcListener.
     */
    virtual HippoIpcListener *getInner() = 0;
};

