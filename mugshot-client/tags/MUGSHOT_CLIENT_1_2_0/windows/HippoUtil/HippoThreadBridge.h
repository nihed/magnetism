/* HippoThreadBridge.h: Execute callbacks across threads
 *
 * Copyright Red Hat, Inc. 2006
 */
#include <HippoUtilExport.h>

class HippoThreadBridgeImpl;

/**
 * This class is used to bridge callbacks from other threads into the thread
 * that creates the HippoThreadBridge. (Actually, technically speaking, this
 * doesn't marshal between threads, it marshals between COM apartments; if the
 * HippoThreadBridge was created from a multi-threaded apartment, any call
 * from a thread within that apartment wouldn't be marshalled.)
 */
class HippoThreadBridge {
public:
    DLLEXPORT HippoThreadBridge();
    DLLEXPORT ~HippoThreadBridge();

    /**
     * Execute the callback. Can be called from any thread and the call will be
     * marshalled to the thread that created the HippoThreadBridge. Note that in some 
     * circumstances this, like any marshalled COM call, can result in reentrancy to the 
     * calling thread.
     *
     * The operation will not fail in normal operation if you've
     * properly synchronized calls from other threads so that they don't happen
     * during or after the destruction of the HippoThreadBridge, but it wouldn't hurt to
     * check the result before relying on the return value.
     */
    DLLEXPORT HRESULT invoke(void *param, void **retval = 0);

    /**
     * Like invoke, but there is no return value, and the actual call will be made
     * at some later point; this version cannot cause reentrancy into the calling
     * thread and will never block.
     */
    DLLEXPORT HRESULT invokeAsync(void *param);

    /**
     * Callback function to execute. To use HippoThreadBridge you derive from it
     * and override call() to do whatever you want.
     */
    virtual void *call(void *param) = 0;

    /**
     * Called when the bridge is destroyed between the point of call to invokeAsync
     * and when the callback is actually run
     */
    virtual void cancel(void *param) = 0;

private:
    HippoThreadBridgeImpl *impl_;
};
