/* HippoMessageHook.h: Manafge functions to be run on messages for a particular window
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include <HippoUtilExport.h>

class HippoMessageHook
{
public:
    // Possibly handle the given message; returns true if handled, 
    // false if not handled
    virtual bool hookMessage(MSG *message) = 0;
};

/**
 * Manages a list of message hooks to be applied for different windows
 * and all descendants; the main use for the message hook mechanism is 
 * deal with accelerator handling for embedded copies of Internet Explorer.
 */
class HippoMessageHookListImpl;

class HippoMessageHookList
{
public:
    DLLEXPORT HippoMessageHookList();
    DLLEXPORT ~HippoMessageHookList();
    // Call the hook any time a message is received for window or any descendant
    DLLEXPORT void registerMessageHook(HWND window, HippoMessageHook *hook);
    // Unregister a hook registered with registerMessageHook
    DLLEXPORT void unregisterMessageHook(HWND window);

    // Call any hooks releavnt to the message; if any of the hooks returns
    // true, stops processing and returns true
    DLLEXPORT bool processMessage(MSG *message);

private:
    HippoMessageHookList(const HippoMessageHookList &other) {}
    HippoMessageHookList &operator=(const HippoMessageHookList &other) {}
    HippoMessageHookListImpl *impl_;
};
