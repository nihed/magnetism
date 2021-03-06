/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.multiplex;

import org.dom4j.Element;
import org.xmpp.packet.Packet;

import java.util.Iterator;
import java.util.List;

/**
 * Route packets identify target sessions by their stream ID and contain a single
 * wrapped stanza that should be processed by the target session.
 *
 * @author Gaston Dombiak
 */
public class Route extends Packet {

    /**
     * Constructs a new Route.
     *
     * @param streamID the stream ID that identifies the connection that is actually sending
     *                 the wrapped stanza.
     */
    public Route(String streamID) {
        this.element = docFactory.createDocument().addElement("route");
        // Set the stream ID that identifies the target session
        element.addAttribute("streamid", streamID);
    }

     /**
     * Constructs a new Route using an existing Element. This is useful
     * for parsing incoming route Elements into Route objects.
     *
     * @param element the route Element.
     */
    public Route(Element element) {
        super(element, true);
    }

    public Route(Route route) {
        Element elementCopy = route.element.createCopy();
        docFactory.createDocument().add(elementCopy);
        this.element = elementCopy;
        // Copy cached JIDs (for performance reasons)
        this.toJID = route.toJID;
        this.fromJID = route.fromJID;
    }

    /**
     * Returns the wrapped stanza that is being routed. Route packets must have
     * a single wrapped stanza. This is a convenience method to avoid manipulating
     * the underlying packet's Element instance directly.
     *
     * @return the wrapped stanza.
     */
    public Element getChildElement() {
        List elements = element.elements();
        if (elements.isEmpty()) {
            return null;
        }
        else {
            // Return the first child element
            return (Element) elements.get(0);
        }
    }

    /**
     * Sets the wrapped stanza by this Route packet. Route packets may have a single child
     * element. This is a convenience method to avoid manipulating this underlying packet's
     * Element instance directly.
     *
     * @param childElement the child element.
     */
    public void setChildElement(Element childElement) {
        for (Iterator i=element.elementIterator(); i.hasNext(); ) {
            element.remove((Element)i.next());
        }
        element.add(childElement);
    }

    /**
     * Return the stream ID that identifies the connection that is actually sending
     * the wrapped stanza.
     *
     * @return the stream ID that identifies the connection that is actually sending
     *         the wrapped stanza.
     */
    public String getStreamID() {
        return element.attributeValue("streamid");
    }

    /**
     * Returns a deep copy of this route packet.
     *
     * @return a deep copy of this route packet.
     */
    public Route createCopy() {
        return new Route(this);
    }
}
