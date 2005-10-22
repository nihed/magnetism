/**
 * 
 */
package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

/**
 * This is probably a temporary hack until JiveMessenger monitors the server.
 * 
 * @author hp
 *
 */
@Stateless
public class MessageSenderBean implements MessageSender {
	static private final Log logger = GlobalSetup.getLog(MessageSenderBean.class);
	private XMPPConnection connection;

	@EJB
	private Configuration config;

	private synchronized XMPPConnection getConnection() {
		if (connection == null) {
			try {
				String addr = config.getProperty("dumbhippo.server.xmpp.address");
				String port = config.getProperty("dumbhippo.server.xmpp.port");
				String user = config.getProperty("dumbhippo.server.xmpp.adminuser");
				String password = config.getProperty("dumbhippo.server.xmpp.password");				
				connection = new XMPPConnection(addr, Integer.parseInt(port.trim()));
				connection.login(user, password);
				logger.debug("logged in OK");
			} catch (XMPPException e) {
				e.printStackTrace(System.out);
				logger.error(e);
				connection = null;
			} catch (PropertyNotFoundException e) {
				e.printStackTrace(System.out);				
				logger.error(e);
				connection = null;
			} 
		}
		
		return connection;
	}
	
	public void sendShareLink(Person recipient, Guid postGuid, String url, String title) {
		XMPPConnection connection = getConnection();
		
		if (connection == null)
			return;

		StringBuilder recipientJid = new StringBuilder();
		recipientJid.append(recipient.getId().toString());
		recipientJid.append("@dumbhippo.com");
		
		Message message = new Message(recipientJid.toString(),
				Message.Type.HEADLINE);
		
		message.addExtension(new LinkExtension(postGuid, url, title));

		message.setBody(String.format("%s\n%s", title, url));
		
		logger.info("Sending jabber message to " + message.getTo());
		connection.sendPacket(message);
	}
	
	public static class LinkExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "link";
		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private String url;
		private Guid guid;
		private String title;

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("link", "id", guid.toString(), "xmlns", NAMESPACE, "href", url);
			builder.appendTextNode("title", title);
			builder.closeElement();
	        return builder.toString();
		}

		public LinkExtension(Guid postId, String url, String title) {
			this.guid = postId;
			this.url = url;
			this.title = title;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}
	}
}
