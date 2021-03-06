/**
 * $RCSfile$
 * $Revision: 2674 $
 * $Date: 2005-08-17 12:36:11 -0400 (Wed, 17 Aug 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger;

import org.xmpp.packet.Message;
import org.dom4j.Element;

import java.util.Date;

/**
 * Subclass of Message that keeps the date when the offline message was stored in the database.
 * The creation date and the user may be used as a unique identifier of the offline message.
 *
 * @author Gaston Dombiak
 */
public class OfflineMessage extends Message {

    private Date creationDate;

    public OfflineMessage(Date creationDate, Element element) {
        super(element);
        this.creationDate = creationDate;
    }

    /**
     * Returns the date when the offline message was stored in the database.
     *
     * @return the date the offline message was stored.
     */
    public Date getCreationDate() {
        return creationDate;
    }
}
