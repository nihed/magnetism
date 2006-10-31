/* hippo-bridged-ipc-controller.h: Thread-safe wrapper for HippoIpcController
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "hippo-ipc.h"

/**
 * This class wraps an internal HippoIpcController instance so that it can be
 * used in a thread-safe manner. Calls against the HippoIpcController methods
 * from any thread are proxied to the thread where the bridged controller was
 * created. If a listener is added using HippoIpcController::addListener(), then
 * callbacks made to that listener will be proxied back to the thread where the
 * listener was added. (The listener is wrapped with a HippoBridgedIpcListener.)
 */
class HippoBridgedIpcController : public HippoIpcController
{
public:
    /**
     * Create an new implementatio of HippoBridgedIpcController that references
     * provider. The new controller instance must be deleted from the same thread
     * where it was created.
     *
     * Don't use this method directly, instead use HippoComIpcLocator.
     */
    static HippoBridgedIpcController *createInstance(HippoIpcProvider *provider);
};