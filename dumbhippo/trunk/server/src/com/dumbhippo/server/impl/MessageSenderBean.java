package com.dumbhippo.server.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.RandomToken;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.Mailer.NoAddressKnownException;

/**
 * Send out messages when events happen (for now, when a link is shared).
 * 
 * Can use Jabber for account holders and email etc. in other situations.
 * 
 * The way this class works is that it has inner classes that work as delegates.
 * The outer class just picks the right delegate to send a particular thing
 * in a particular context.
 * 
 * @author hp
 * 
 */
@Stateless
public class MessageSenderBean implements MessageSender {
	static private final Log logger = GlobalSetup.getLog(MessageSenderBean.class);

	// Injected beans, some are logically used by delegates but we can't 
	// inject into the delegate objects.
	
	@EJB
	private Configuration config;
	
	@EJB
	private AccountSystem accountSystem;	

	@EJB
	private Mailer mailer;

	@EJB
	private IdentitySpider identitySpider;
	
	// Our delegates
	
	private XMPPSender xmppSender;
	private EmailSender emailSender;
	
	private static class LinkExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "link";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private String senderName;
		
		private String url;

		private Guid guid;

		private String title;
		
		private String description;

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("link", "id", guid.toString(), "xmlns", NAMESPACE, "href", url);
			builder.appendTextNode("senderName", senderName, "isCache", "true");			
			builder.appendTextNode("title", title);
			builder.appendTextNode("description", description);
			builder.closeElement();
			return builder.toString();
		}
		public LinkExtension(String senderName, Guid postId, String url, String title, String description) {
			this.senderName = senderName;
			this.guid = postId;
			this.url = url;
			this.title = title;
			this.description = description;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}
	}
	
	private class XMPPSender {

		private XMPPConnection connection;
		
		private synchronized XMPPConnection getConnection() {
			if (connection != null && !connection.isConnected()) {
				logger.debug("got disconnected from XMPP server");
			}
			if (connection == null || !connection.isConnected()) {			
				try {
					String addr = config.getPropertyNoDefault(HippoProperty.XMPP_ADDRESS);
					String port = config.getPropertyNoDefault(HippoProperty.XMPP_PORT);
					String user = config.getPropertyNoDefault(HippoProperty.XMPP_ADMINUSER);
					String password = config.getPropertyNoDefault(HippoProperty.XMPP_PASSWORD);
					connection = new XMPPConnection(addr, Integer.parseInt(port.trim()));
					// We need to use a separate resource ID for each connection
					// TODO create an overoptimized XMPP connection pool 
					RandomToken token = RandomToken.createNew();
					connection.login(user, password, StringUtils.hexEncode(token.getBytes()));
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

		public synchronized void sendPostNotification(Person recipient, Post post) {
			XMPPConnection connection = getConnection();
			PersonView recipientView;
			
			if (connection == null)
				return;

			StringBuilder recipientJid = new StringBuilder();
			recipientJid.append(recipient.getId().toString());
			recipientJid.append("@dumbhippo.com");

			Message message = new Message(recipientJid.toString(), Message.Type.HEADLINE);

			String title = post.getTitle();
			Set<Resource> resources = post.getResources();
			
			// for now, hardcode to "find the first url we see"
			
			String url = null;
			if (!resources.isEmpty()) {
				for (Resource r : resources) {
					if (r instanceof LinkResource) {
						url = ((LinkResource)r).getUrl();
						break;
					}
				}
			}
			
			if (url == null) {
				// this particular jabber message protocol has no point without an url
				logger.debug("no url found on post");
				return;
			}

			recipientView = identitySpider.getViewpoint(recipient, post.getPoster());
			String senderName = recipientView.getHumanReadableName();
			message.addExtension(new LinkExtension(senderName, post.getGuid(), url, title, post.getText()));

			message.setBody(String.format("%s\n%s", title, url));

			logger.info("Sending jabber message to " + message.getTo());
			connection.sendPacket(message);
		}
	}

	private class EmailSender {

		public void sendPostNotification(Person recipient, Post post) throws NoAddressKnownException {
			String baseurl = config.getProperty(HippoProperty.BASEURL);
			PersonView posterViewedByRecipient = identitySpider.getViewpoint(recipient, post.getPoster());
			
			StringBuilder messageText = new StringBuilder();
			XmlBuilder messageHtml = new XmlBuilder();
			
			messageHtml.appendHtmlHead("");
			messageHtml.append("<body>\n");
			
			Set<Resource> resources = post.getResources();
			
			String title = post.getTitle();
			if (title.length() == 0) {
				title = null;
			}
			
			for (Resource r : resources) {
				if (r instanceof LinkResource) {
					String url = ((LinkResource)r).getUrl();
					
					if (title == null)
						title = url;
					
					StringBuilder redirectUrl = new StringBuilder();
					redirectUrl.append(baseurl);
					redirectUrl.append("/redirect?url=");
					try {
						redirectUrl.append(URLEncoder.encode(url, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException("This should not be a checked exception, sheesh", e);
					}
					redirectUrl.append("&postId=");
					redirectUrl.append(post.getId()); // no need to encode, we know it is OK

					// FIXME we'll put in the inviteKey= param here also 
					
					messageText.append(url);
					messageText.append("\n");
					
					messageHtml.append("<p><a href=\"");
					messageHtml.appendEscaped(redirectUrl.toString());
					messageHtml.append("\">" + title + "</a> (link goes via DumbHippo)</p>\n");
				}
			}

			if (title == null) {
				// if this doesn't sound like spam I'm not sure what does...
				title = "Check out this link!";
			}
			
			messageText.append("\n");
			messageHtml.append("<br/>\n");
			
			// TEXT: append post text
			messageText.append(post.getText());

			// HTML: append post text
			messageHtml.append("<p>\n");
			// I suppose this is a wasteful way to be able to use getLine()
			BufferedReader reader = new BufferedReader(new StringReader(post.getText()));
			try {
				boolean lastLineEmpty = false;
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					if (line.matches("^\\s*$")) {
						if (!lastLineEmpty) {
							messageHtml.append("\n</p>\n<p>");
						}
						lastLineEmpty = true;
					} else {
						messageHtml.appendEscaped(line);
						lastLineEmpty = false;
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("should not get IOException on a StringReader", e);
			}
			messageHtml.append("</p>\n");
			
			// TEXT: append footer
			messageText.append("\n\n");
			messageText.append("                    (Message sent by " + posterViewedByRecipient.getHumanReadableName() + " using " + baseurl + ")\n");
			
			// HTML: append footer
			messageHtml.append("<p style=\"font-size: smaller; font-style: italic; text-align: center;\">(Message sent by ");
			messageHtml.appendEscaped(posterViewedByRecipient.getHumanReadableName());
			messageHtml.append(" using <a href=\"");
			messageHtml.appendEscaped(baseurl);
			messageHtml.append("\">DumbHippo</a>)</p>\n");
			messageHtml.append("</body>\n</html>\n");
					
			MimeMessage msg = mailer.createMessage(post.getPoster(), recipient);
			
			try {
				msg.setSubject(title);
				
				MimeBodyPart textPart = new MimeBodyPart();
				textPart.setText(messageText.toString(), "UTF-8");
				
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(messageHtml.toString(), "text/html; charset=UTF-8");
				
				MimeMultipart multiPart = new MimeMultipart();
				// "alternative" means display only one or the other, "mixed" means both
				multiPart.setSubType("alternative");
				
				// I read something on the internet saying to put the text part first
				// so sucktastic mail clients see it first
				multiPart.addBodyPart(textPart);
				multiPart.addBodyPart(htmlPart);
				
				msg.setContent(multiPart);
				
			} catch (MessagingException e) {
				throw new RuntimeException("failed to put together MIME message", e);
			}
			
			logger.debug("Sending mail to " + recipient.toString());
			mailer.sendMessage(msg);
		}
	}
	
	public MessageSenderBean() {
		this.emailSender = new EmailSender();
		this.xmppSender = new XMPPSender();
	}
	
	public void sendPostNotification(Person recipient, Post post) {
		HippoAccount account = accountSystem.lookupAccountByPerson(recipient);
		
		// in the future the test could be "account != null && logged on to jabber recently" or something
		if (account != null) {
			xmppSender.sendPostNotification(recipient, post);
		} else {
			try {
				emailSender.sendPostNotification(recipient, post);
			} catch (NoAddressKnownException e) {
				logger.debug("no clue how to send notification to " + recipient + " (we have no email address)");
			}
		}
	}
}
