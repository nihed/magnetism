/* HippoToolbarEdit.cpp: Make sure that we are actually on the IE toolbar.
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include <ShlGuid.h> // For CLSID_InternetButtons
#include <HippoUtil.h>
#include "HippoRegKey.h"
#include "HippoToolbarEdit.h"

// Note that we don't bother splitting this into normal and debug, since there
// is only one toolbar button that is used for both.
static const WCHAR HIPPO_SUBKEY[] = HIPPO_REGISTRY_KEY L"\\Client";
static const WCHAR COMMAND_MAPPING_SUBKEY[] = L"Software\\Microsoft\\Internet Explorer\\Extensions\\CmdMapping";
static const WCHAR TOOLBAR_SUBKEY[] = L"Software\\Microsoft\\Internet Explorer\\Toolbar";

// Define these as strings; a little simpler than using StringFromIID()

// CLSID_InternetButtons
static const WCHAR INTERNET_BUTTONS_GUIDSTR[] = L"{1E796980-9CC5-11D1-A83F-00C04FC99D61}";
// GUID_HippoToolbarButton
static const WCHAR HIPPO_TOOLBAR_BUTTON_GUIDSTR[] = L"{7197AC86-5F8A-43be-806C-C03BB363C85E}";

// Number of bytes before we start getting button entries
static const DWORD HEADER_SIZE = 4;

// structure of a button entry
struct ToolbarEntry {
    DWORD dummy1;       // An ID for this button (?)
    CLSID commandGroup; // the command group for the command that the button executes
    DWORD commandId;    // the command ID for the executed command
    DWORD dummy4;       // flags (?)
};

HippoToolbarEdit::HippoToolbarEdit()
{
}
    
bool
HippoToolbarEdit::ensureToolbarButton()
{
    if (wasPreviouslyAdded())
        return true;

    BYTE *oldData = NULL;
    DWORD oldLength;
    BYTE *newData = NULL;
    DWORD newLength;
    bool result = true;

    HippoRegKey key(HKEY_CURRENT_USER, TOOLBAR_SUBKEY, true);

    // If there is no entry for the main toolbar, the user hasn't customized it,
    // so nothing to do
    if (!key.loadBinary(INTERNET_BUTTONS_GUIDSTR, &oldData, &oldLength))
        goto out;

    // Get the existing command ID for our toolbar, or add a new one
    DWORD commandId = getCommandId();

    // If our toolbar button isn't in the main toolbar, add it
    if (findEntry(oldData, oldLength, commandId))
        goto out;

    if (!addNewEntry(oldData, oldLength, &newData, &newLength, commandId)) {
        result = false;
        goto out;
    }

    if (!key.saveBinary(INTERNET_BUTTONS_GUIDSTR, newData, newLength)) {
        result = false;
        goto out;
    }

out:
    if (result)
        setPreviouslyAdded();

    if (oldData)
        CoTaskMemFree(oldData);
    if (newData)
        CoTaskMemFree(newData);

    return result;
}

// Check if we added ourselves previously (we also count it adding
// ourselves if we checked and the toolbar wasn't customized, so there
// was nothing to do.)
bool
HippoToolbarEdit::wasPreviouslyAdded()
{
    bool previouslyAdded = false;

    HippoRegKey key(HKEY_CURRENT_USER, HIPPO_SUBKEY, false);
    key.loadBool(L"AddedToolbarButton", &previouslyAdded);

    return previouslyAdded;
}

// Remember that we added ourselves (or that there was nothing to do)
void
HippoToolbarEdit::setPreviouslyAdded()
{
    HippoRegKey key(HKEY_CURRENT_USER, HIPPO_SUBKEY, true);
    key.saveBool(L"AddedToolbarButton", true);
}


// Find an existing command ID for our extension button, if one exists,
// otherwise add one.
DWORD
HippoToolbarEdit::getCommandId()
{
    long commandId;
    long nextId;

    HippoRegKey key(HKEY_CURRENT_USER, COMMAND_MAPPING_SUBKEY, true);
    if (key.loadLong(HIPPO_TOOLBAR_BUTTON_GUIDSTR, &commandId))
        return commandId;

    if (!key.loadLong(L"NextId", &nextId))
        nextId = 0x2000;
       
    key.saveLong(L"NextId", nextId + 1);

    commandId = nextId;
    key.saveLong(HIPPO_TOOLBAR_BUTTON_GUIDSTR, commandId);

    return commandId;
}

// Find an existing entry in the binary blob that represents the toolbar layout
// for the main toolbar
bool
HippoToolbarEdit::findEntry(const BYTE *oldData,
                            DWORD       oldLength,
                            DWORD       commandId)
{
    DWORD offset;
    for (offset = HEADER_SIZE; offset + sizeof(ToolbarEntry) <= oldLength; offset += sizeof(ToolbarEntry)) {
        ToolbarEntry entry;

        memcpy((void *)&entry, oldData + offset, sizeof(ToolbarEntry));
        if (IsEqualGUID(entry.commandGroup, CLSID_InternetButtons) &&
            entry.commandId == commandId)
            return true;
    }

    return false;
}

// Append ourselves to the toolbar layout for the main toolbar
bool
HippoToolbarEdit::addNewEntry(const BYTE  *oldData,
                              DWORD        oldLength,
                              BYTE       **newData,
                              DWORD       *newLength,
                              DWORD        commandId)
{
    DWORD nEntries = (oldLength - HEADER_SIZE) / sizeof(ToolbarEntry);
    if (HEADER_SIZE + nEntries * sizeof(ToolbarEntry) != oldLength) // Don't understand the format
        return false;

    *newLength = oldLength + sizeof(ToolbarEntry);
    *newData = (BYTE *)CoTaskMemAlloc(*newLength);
    if (!*newData) // Out of memory
        return false;

    // The function of the first item in the ToolbarEntry structure isn't clear, but it
    // appears that picking a value one more than any existing entry (ignoring ones with
    // the value -1) will work.

    ToolbarEntry entry;
    long maxId = 0;

    DWORD offset;
    for (offset = HEADER_SIZE; offset + sizeof(ToolbarEntry) <= oldLength; offset += sizeof(ToolbarEntry)) {
        ToolbarEntry entry;

        memcpy((void *)&entry, oldData + offset, sizeof(ToolbarEntry));
        long id = (long)entry.dummy1;
        if (id > 0 && id > maxId)
            maxId = id;
    }

    entry.dummy1 = maxId + 1;
    entry.commandGroup = CLSID_InternetButtons;
    entry.commandId = commandId;
    entry.dummy4 = 0x04;

    memcpy(*newData, oldData, oldLength);
    memcpy(*newData + oldLength, (void *)&entry, sizeof(ToolbarEntry));

    return true;
}
