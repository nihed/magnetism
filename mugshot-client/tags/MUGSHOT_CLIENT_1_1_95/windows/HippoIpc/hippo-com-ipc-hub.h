/* hippo-com-ipc-hub.h: Process wide singleton to manage connections to clients
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <windows.h>

#include "hippo-ipc.h"
#include "HippoThreadExecutor.h"
#include "HippoUtil.h"

/**
 * Callback interface for global events related to DumbHippo.
 */
class HippoComIpcListener
{
public:
    /**
     * An (unspecified) instance of the DumbHippo client was started; if you are
     * maintaining a pointer to the DumbhHppo client, you should check it is 
     * still valid. If you are waiting for an instance to appear, you should 
     * check again using HippoComIpcHub::getUI().
     */
    virtual void uiStarted() = 0;
};

/**
 * This enumeration identifies the three different instances of DumbHippo that
 * might be started within the current session. A normal user will only have
 * a production instance.
 */
enum HippoUIInstance {
    HIPPO_UI_INSTANCE_PRODUCTION,
    HIPPO_UI_INSTANCE_DOGFOOD,
    HIPPO_UI_INSTANCE_DEBUG
};

/**
 * The HippoComIpcHub class serves as the process-wide grounding point for 
 * communication with one or more DumbHippo clients. The singleton instance
 * of this class creates a thread and a window within that thread to watch
 * for broadcast messages.
 * 
 * This same thread is used as the home for HippoComIpcProvider instances
 * that do the communication with a particular instance. 
 */
class HippoComIpcHub
{
public:
    /**
     * Initializes the HippoComIpcHub inner mechanisms on DLL startup.
     */
    static void startup(HINSTANCE instance);

    /**
     * Method to call when the DLL is being unloaded.
     */
    static void shutdown();

    /**
     * Get the Singleton global instance of HippoComIpcHub
     */
    static HippoComIpcHub *getInstance();

    /**
     * Get a pointer to the remote COM object for the specified instance
     * of the DumbHippo client (production, dogfood, debug), if one exists.
     */
    virtual HRESULT getUI(HippoUIInstance instance, IHippoUI **ui) = 0;

    /**
     * Get a pointer to the remote COM object for a DumbHippo client connecting
     * to the specified server. (The server is in host:port form and refers
     * to the web server of the dumbhippo instance. The port must be present
     * even if it is the default port 80.)
     */
    virtual HRESULT getUI(const char *serverName, IHippoUI **ui) = 0;

    /**
     * Add a listener for notification on global events related to DumbHippo.
     * This can be called safely from any thread, but the the callback will 
     * always be invoked in the HippoComIpcHub executor thread.
     */
    virtual void addListener(HippoComIpcListener *listener) = 0;

    /**
     * Remove a listener added with addListener()
     */
    virtual void removeListener(HippoComIpcListener *listener) = 0;

    /**
     * Returns the HippoThreadExecutor that manages the global thread. The
     * executor can be to execute tasks synchronously or asynchronously in 
     * that thread.
     */
    virtual HippoThreadExecutor *getExecutor() = 0;

    // Convenience for getExecutor()->doSync() / getExecutor->doAsync();
    virtual void doSync(HippoThreadTask *task) = 0;
    virtual void doAsync(HippoThreadTask *task) = 0;

    /**
     * Get the HippoComIpcLocator singleton. This is an internal implementation
     * detail of HippoComIpcLocator::getInstance().
     */
    virtual HippoIpcLocator *getLocator() = 0;

};
