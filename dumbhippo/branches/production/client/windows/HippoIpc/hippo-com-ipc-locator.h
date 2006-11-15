/* hippo-com-ipc-locator.h: HippoIpcLocator implementation via COM for Windows
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include "hippo-ipc.h"

/**
 * This class is an implementation of HippoIpcLocator that tallks to the running
 * DumbHippo client via COM. Another distinguishing feature of this class as
 * compared to the features of the base HippoIpcLocator is that the controllers
 * returned HippoIpcLocator::getController() are thread safe. (See the docs for
 * HippoBrigedIpcController.) While this isn't directly related to communication
 * via COM, it is needed for operation on Windows since the thread in which 
 * controls are instantiated isn't a known fixed thread. (In Gecko on Linux
 * we know that all our code will be run in main GTK+/GLib thread.)
 *
 * Thread safety is accomplished by proxying calls to the HippoComIpcHub global
 * thread. Using that thread for communication also allows us to carefully
 * control reentrancy, and make sure that, for example, a call to
 * HippoIpcControl::joinChatRoom() won't result in listener callbacks before
 * it returns.
 */
class HippoComIpcLocator : public HippoIpcLocator {
public:
    /**
     * Get the singleton global instance of HippoComIpcLocator.
     */
    static HippoIpcLocator *getInstance();

    // Internal implentation, use getInstance() instead
    static HippoComIpcLocator *createInstance();
};
