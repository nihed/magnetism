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
	
	public void sendShareLink(String recipient, String url, String title) {
		XMPPConnection connection = getConnection();
		
		if (connection == null)
			return;
		
		if (!recipient.endsWith("@dumbhippo.com")) {
			logger.error("Currently can only send link sharing to @dumbhippo.com domain, not " + recipient);
			return;
		}
		
		StringBuilder newRecipient = new StringBuilder();
		newRecipient.append(recipient.substring(0,recipient.indexOf("@")));
		newRecipient.append("@dumbhippo.com");
		
		Message message = new Message(newRecipient.toString(),
				Message.Type.HEADLINE);
		
		message.addExtension(new LinkExtension(url, title));

		message.setBody(String.format("%s\n%s", title, url));
		
		logger.info("Sending jabber message to " + message.getTo());
		connection.sendPacket(message);
	}
	
	public static class LinkExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "link";
		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private String url;
		private String title;
		
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			
	        builder.getStringBuilder().append(String.format("<link xmlns=\"%s\" href=\"%s\"><title>",
					NAMESPACE, getUrl()));
			builder.appendEscaped(getTitle());
			builder.getStringBuilder().append("</title></link>");
	        return builder.toString();
		}

		public LinkExtension(String url, String title) {
			this.url = url;
			this.title = title;
		}
		
		public String getTitle() {
			return title;
		}
		

		public void setTitle(String title) {
			this.title = title;
		}
		

		public String getUrl() {
			return url;
		}
		

		public void setUrl(String url) {
			this.url = url;
		}
		
		
	}
}
