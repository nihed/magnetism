/**
 * $RCSfile$
 * $Revision: 128 $
 * $Date: 2004-10-25 19:42:00 -0400 (Mon, 25 Oct 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger;

/**
 * Implement and register with a connection to receive notification
 * of the connection closing.
 *
 * @author Iain Shigeoka
 */
public interface ConnectionCloseListener {
    /**
     * Called when a connection is closed.
     *
     * @param handback The handback object associated with the connection listener during Connection.registerCloseListener()
     */
    public void onConnectionClose(Object handback);
}
