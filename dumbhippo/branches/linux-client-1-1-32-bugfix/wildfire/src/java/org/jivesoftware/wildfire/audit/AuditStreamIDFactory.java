/**
 * $RCSfile$
 * $Revision: 38 $
 * $Date: 2004-10-21 03:30:10 -0300 (Thu, 21 Oct 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.audit;

import org.jivesoftware.wildfire.StreamID;
import org.jivesoftware.wildfire.StreamIDFactory;
import org.jivesoftware.wildfire.spi.BasicStreamIDFactory;

/**
 * Factory for producing audit stream IDs. We use a factory so that
 * audit information can be identified using an appropriate storage
 * key (typically a long for RDBMS).
 *
 * @author Iain Shigeoka
 */
public class AuditStreamIDFactory implements StreamIDFactory {

    private BasicStreamIDFactory factory = new BasicStreamIDFactory();

    public AuditStreamIDFactory() {
    }

    public StreamID createStreamID() {
        return factory.createStreamID();
    }
}
