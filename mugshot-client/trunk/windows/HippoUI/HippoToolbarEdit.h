/* HippoToolbarEdit.h: Make sure that we are actually on the IE toolbar.
 *
 * Copyright Red Hat, Inc. 2005
 **/

// The very existance of this class pains the soul. The problem we're solving
// here is that our toolbar button won't appear on the Internet Explorer
// toolbar if the user has previously customized the toolbar button. What
// this class does is hack the Internet Explorer registry entries to add
// our button as if the user had added it themselves.
//
// We remember if we've done this, to avoid fighting the user; if the user
// removed us explicitely, so be it.

class HippoToolbarEdit
{
public:
    HippoToolbarEdit();
    
    /**
     * If the user has customized the IE toolbar, and we aren't in the toolbar,
     * and we haven't added ourselves previously, add ourselves. Internet
     * Explorer should not be running when this is called.
     * @return false if an error occurred, otherwise true
     */
    bool ensureToolbarButton();

private:
    bool wasPreviouslyAdded();
    void setPreviouslyAdded();
    DWORD getCommandId();
    bool findEntry(const BYTE *oldData,
                   DWORD       oldLength,
                   DWORD       commandId);
    bool addNewEntry(const BYTE  *oldData,
                     DWORD        oldLength,
                     BYTE       **newData,
                     DWORD       *newLength,
                     DWORD        commandId);
};
