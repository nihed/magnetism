/**
 * 
 */
package com.dumbhippo.jive;

import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.Log;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

/**
 * @author hp
 *
 */
public class LinkReflectorComponent implements Component {

	private static final String name = "LinkReflectorComponent";
	private static final String description = "Component used to notify clients of new shared links";
	
	private Log logger = null;
	
	public LinkReflectorComponent() {
		org.jivesoftware.util.Log.debug("Constructing LinkReflectorComponent");
	}
	
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void processPacket(Packet packet) {
		logger.debug("LinkReflectorComponent processPacket(), from: " + packet.getFrom());
	}

	public void initialize(JID jid, ComponentManager componentManager) throws ComponentException {
		logger = componentManager.getLog();
		
		logger.debug("Initializing LinkReflectorComponent, jid = " + jid);
	}

	public void start() {
		logger.debug("LinkReflectorComponent start()");
		
		
	}

	public void shutdown() {
		logger.debug("LinkReflectorComponent shutdown()");
	}
}
