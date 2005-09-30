/**
 * 
 */
package com.dumbhippo.jive;

import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.Log;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

/**
 * @author hp
 *
 */
public class LinkReflectorComponent implements Component {

	private static final String NAME = "LinkReflectorComponent";
	private static final String DESCRIPTION = "Component used to notify clients of new shared links";

	public static final String DOMAIN = "link-reflector";
	
	private Log logger = null;
	private ComponentManager componentManager = null;
	
	public LinkReflectorComponent() {
		org.jivesoftware.util.Log.debug("Constructing LinkReflectorComponent");
	}
	
	public String getName() {
		return NAME;
	}

	public String getDescription() {
		return DESCRIPTION;
	}

	public void processPacket(Packet packet) {
		logger.debug("LinkReflectorComponent processPacket(), from: " + packet.getFrom());
		if (packet instanceof Message) {
			Message message = (Message) packet;
			logger.debug(message.toXML());
			JID current = message.getTo();
			String domain = current.getDomain();
			logger.debug("  domain is " + domain);
			String newDomain = domain.substring(DOMAIN.length()+1);
			logger.debug("  new domain is " + newDomain);
			packet.setTo(new JID(current.getNode(), newDomain, current.getResource()));
			
			packet.setFrom(new JID("TheMan", "dumbhippo.com", "system"));
			
			try {
				componentManager.sendPacket(this, packet);
			} catch (ComponentException e) {
				logger.error(e);
			}
		}
	}

	public void initialize(JID jid, ComponentManager componentManager) throws ComponentException {
		this.componentManager = componentManager;
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
