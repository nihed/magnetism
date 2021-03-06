/**
 * $RCSfile$
 * $Revision: 569 $
 * $Date: 2004-12-01 13:31:18 -0500 (Wed, 01 Dec 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger;

import org.xmpp.packet.JID;

/**
 *
 *
 * @author Matt Tucker
 */
public interface RoutableChannelHandler extends ChannelHandler {

    /**
      * Returns the XMPP address. The address is used by services like the core
      * server packet router to determine if a packet should be sent to the handler.
      * Handlers that are working on behalf of the server should use the generic server
      * hostname address (e.g. server.com).
      *
      * @return the XMPP address.
      */
     public JID getAddress();
}
