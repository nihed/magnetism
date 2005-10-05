/**
 * 
 */
package com.dumbhippo.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

import com.dumbhippo.XMLBuilder;

/**
 * This is probably a temporary hack until JiveMessenger monitors the server.
 * 
 * @author hp
 *
 */
public class MessageSender {
	static private MessageSender instance;
	private Log logger;
	private XMPPConnection connection;
	
	static public MessageSender getInstance() {
		synchronized (MessageSender.class) {
			if (instance == null) {
				instance = new MessageSender();
			}
		}
		
		return instance;
	}
	
	private MessageSender() {
		logger = LogFactory.getLog(MessageSender.class);
	}
	
	private synchronized XMPPConnection getConnection() {
		if (connection == null) {
			try {
				connection = new XMPPConnection("192.168.1.10");
				connection.loginAnonymously();
			} catch (XMPPException e) {
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
		newRecipient.append("@link-reflector.dumbhippo.com");
		
		Message message = new Message(newRecipient.toString(),
				Message.Type.HEADLINE);
		
		message.addExtension(new LinkExtension(url, title));

		message.setBody(String.format("%s\n%s", title, url));
		
		connection.sendPacket(message);
	}
	
	public static void main(String[] args) {
		MessageSender sender = MessageSender.getInstance();
		
		sender.sendShareLink("hp@dumbhippo.com",
				"http://badgerbadgerbadger.com",
				"Badger Badger Badger (Escaping check: &<>'\")");
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
			XMLBuilder builder = new XMLBuilder();
			
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
