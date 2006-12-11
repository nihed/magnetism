package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;

import org.jboss.annotation.IgnoreDependency;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.RandomToken;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostType;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;

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
	static private final Logger logger = GlobalSetup.getLogger(MessageSenderBean.class);

	// Injected beans, some are logically used by delegates but we can't 
	// inject into the delegate objects.
	
	@EJB
	private Configuration config;

	@EJB
	private Mailer mailer;

	@EJB
	@IgnoreDependency
	private IdentitySpider identitySpider;
	
	@EJB
	@IgnoreDependency
	private PersonViewer personViewer;
	
	@EJB
	@IgnoreDependency
	private PostingBoard postingBoard;
	
	@EJB
	@IgnoreDependency
	private InvitationSystem invitationSystem;

	@EJB
	@IgnoreDependency
	private NoMailSystem noMail;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	// Our delegates
	
	private XMPPSender xmppSender;
	private EmailSender emailSender;
	
	private static class PrefsChangedExtension implements PacketExtension {
		private static final String ELEMENT_NAME = "prefs";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/prefs";
		
		Map<String,String> prefs;
		
		public PrefsChangedExtension(Map<String,String> prefs) {
			this.prefs = new HashMap<String,String>(prefs);
		}

		public PrefsChangedExtension(String key, String value) {
			this(Collections.singletonMap(key, value)); 
		}
		
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement(ELEMENT_NAME, "xmlns", NAMESPACE);
			for (String key : prefs.keySet()) {
				builder.appendTextNode("prop", prefs.get(key), "key", key);
			}
			builder.closeElement();
			return builder.toString();
		}
	}

	private class XMPPSender {

		private XMPPConnection connection;
		
		class NotConnectedException extends Exception {

			private static final long serialVersionUID = 1L;

			NotConnectedException(String message) {
				super(message);
			}
			
			NotConnectedException(String message, Throwable cause) {
				super(message, cause);
			}
		}
		
		private synchronized XMPPConnection getConnection() throws NotConnectedException {
			if (connection != null && !connection.isConnected()) {
				logger.info("Disconnected from XMPP server");
			}
			if (connection == null || !connection.isConnected()) {			
				try {
					String addr = config.getPropertyNoDefault(HippoProperty.BIND_HOST);
					String port = config.getPropertyNoDefault(HippoProperty.XMPP_PORT);
					String user = config.getPropertyNoDefault(HippoProperty.XMPP_ADMINUSER);
					String password = config.getPropertyNoDefault(HippoProperty.XMPP_PASSWORD);
					connection = new XMPPConnection(addr, Integer.parseInt(port.trim()));
					// We need to use a separate resource ID for each connection
					// TODO create an overoptimized XMPP connection pool 
					RandomToken token = RandomToken.createNew();
					connection.login(user, password, StringUtils.hexEncode(token.getBytes()));
					logger.info("Successfully reconnected to XMPP server");
				} catch (XMPPException e) {
					connection = null;
					throw new NotConnectedException("Failed to log in to XMPP server: " + e.getMessage(), e);
				} catch (PropertyNotFoundException e) { 
					logger.error("configuration is f'd up, can't connect to XMPP"); 
					connection = null;
					throw new NotConnectedException("Configuration properties missing so can't connect to XMPP", e);
				}
			}
			
			if (connection == null)
				throw new RuntimeException("connection == null and we didn't throw a NotConnectedException");

			return connection;
		}

		private Message createMessageFor(Guid userId, Message.Type type) {
			// FIXME should dumbhippo.com domain be hardcoded here?			
			return new Message(userId.toJabberId("dumbhippo.com"), type);
		}		
		
		private Message createMessageFor(User user, Message.Type type) {
			return createMessageFor(user.getGuid(), type);
		}
		
		public synchronized void sendPostNotification(User recipient, Post post, List<User> viewers, PostType postType) {
		}
		
		public synchronized void sendPrefChanged(User user, String key, String value) {
			XMPPConnection connection; 
			try {
				connection = getConnection();
			} catch (NotConnectedException e) {
				logger.warn("Not sending pref changed notification because not connected to xmpp: " + e.getMessage());
				return;
			}
			Message message = createMessageFor(user, Message.Type.HEADLINE);
			message.addExtension(new PrefsChangedExtension(key, value));
			logger.debug("Sending prefs changed message to {}", message.getTo());			
			connection.sendPacket(message);
		}
	}

	private class EmailSender {

		public void sendPostNotification(EmailResource recipient, Post post, PostType postType) {
			User poster = post.getPoster();
			
			// We only send out notifications for posts that come from users on the system
			// not for FeedPost, and similar
			if (poster == null)
				return;
			
			// We really want to use the viewpoint of the recipient, not the
			// viewpoint of the sender, but the recipient doesn't have an
			// account and thus can't have a viewpoint. Using an anonymous
			// viewpoint wouldn't work since the anonymous viewpoint wouldn't
			// be able to see the post details.
			UserViewpoint viewpoint = new UserViewpoint(poster);
			
			// We don't want to send email notifications if, say, the recipient
			// is a member of a public group that the post was sent to, since
			// that is too spammy for people who have just been invited to the
			// system.
			if (!postingBoard.worthEmailNotification(post, recipient)) {
				logger.debug("Not sending email notification because it would be spammy");
				return;
			}
			
			if (!noMail.getMailEnabled(recipient)) {
				logger.debug("Mail is disabled to {} not sending post notification", recipient);
				return;
			}
			
			String baseurl = config.getProperty(HippoProperty.BASEURL);
			
			// If the sender has addressed their post directly to this recipient, then
			// we want to "pile on" to any invitation that exists for the recipient.
			// But we don't want to do that if the recipient is just a group member
			boolean addToInvitation = post.getPersonRecipients().contains(recipient);
			
			// may be null!
			InvitationToken invitation = invitationSystem.updateValidInvitation(poster, recipient, addToInvitation); 
			String recipientInviteUrl;
			if (invitation != null) 
				recipientInviteUrl = invitation.getAuthURL(baseurl); 
			else
				recipientInviteUrl = null;
				
			String recipientStopUrl;
			
			if (recipientInviteUrl == null) {
				recipientStopUrl = noMail.getNoMailUrl(recipient, NoMailSystem.Action.NO_MAIL_PLEASE);
			} else {
				recipientStopUrl = recipientInviteUrl + "&disable=true";
			}
			
			// Since the recipient doesn't have an account, we can't get the recipient's view
			// of the poster. Send out information from the poster's view of themself.
			PersonView posterViewedBySelf = personViewer.getPersonView(viewpoint, 
					                                                     poster,
					                                                     PersonViewExtra.PRIMARY_EMAIL);
			
			StringBuilder messageText = new StringBuilder();
			XmlBuilder messageHtml = new XmlBuilder();
			
			messageHtml.appendHtmlHead("");
			messageHtml.append("<body>\n");
			
			messageHtml.append("<div style=\"width:500px\">\n");
			messageHtml.append("  <div style=\"border:1px solid black;min-height:100px;\"><!-- bubble div -->\n");
			
			PostView postView = new PostView(ejbContext, post, recipient);
			
			String url = postView.getUrl();
			// For group shares to non-account members, we want to actually redirect them to /download
			// so they download the client, accept terms of use etc.  Then the initial share will
			// be for the group.
			if (postType == PostType.GROUP) {
				url = baseurl + "/download"; 
			}
			
			StringBuilder redirectUrl = new StringBuilder();
			redirectUrl.append(baseurl);
			redirectUrl.append("/redirect?url=");
			redirectUrl.append(StringUtils.urlEncode(url));
			redirectUrl.append("&postId=");
			redirectUrl.append(post.getId()); // no need to encode, we know it is OK

			if (invitation != null) {
				redirectUrl.append("&inviteKey=");
				redirectUrl.append(invitation.getAuthKey());
			}
			
			// TEXT
			
			messageText.append(redirectUrl);
			messageText.append("\n");
			
			// HTML		
			
			String f = "<div style=\"margin:0.3em;\">\n" 
				+ "<a style=\"font-weight:bold;font-size:150%%;\" title=\"%s\" href=\"%s\">%s</a>\n"
				+ "</div>\n";
			 
			messageHtml.append(String.format(f, XmlBuilder.escape(url),
					XmlBuilder.escape(redirectUrl.toString()),
					postView.getTitleAsHtml()));

			// TEXT: append post text
			messageText.append("\n");
			messageText.append(postView.getText());
			messageText.append("\n");
			
			// HTML: append post text
			messageHtml.append("<div style=\"font-size:120%;margin:0.5em;\">\n");
			messageHtml.append(postView.getTextAsHtml());
			messageHtml.append("</div>\n");

			messageHtml.append("  </div><!-- close bubble div -->\n");
			
			// TEXT: "link shared by"
			
			messageText.append("  (Link shared by " + posterViewedBySelf.getName() + ")");
			
			String viewPersonPageId = posterViewedBySelf.getViewPersonPageId();
			String posterPublicPageUrl = null;
			if (viewPersonPageId != null)
				posterPublicPageUrl = baseurl + "/person?who=" + viewPersonPageId;
						
			// HTML: "link shared by"
			String recipientLink;
			if (recipientInviteUrl != null) {
				recipientLink = String.format("<a href=\"%s\">%s</a>",
						XmlBuilder.escape(recipientInviteUrl),
						XmlBuilder.escape(recipient.getEmail())); 
			} else {
				recipientLink = XmlBuilder.escape(recipient.getEmail());
			}
			messageHtml.append("<div style=\"margin:0.2em;font-style:italic;text-align:right;font-size:small;vertical-align:bottom;\">");
			String format = "(Link shared from "
				+ "<a title=\"%s\" href=\"%s\">%s</a> "
				+ "to %s)\n"
				+ "</div>\n";
			messageHtml.append(String.format(format, XmlBuilder.escape(posterViewedBySelf.getEmail().getEmail()),
					// FIXME posterPublicPageUrl in theory could be null (not actually right now afaik)
						XmlBuilder.escape(posterPublicPageUrl),
						XmlBuilder.escape(posterViewedBySelf.getName()),
						recipientLink)); 
			
			// TEXT: append footer
			messageText.append("\n\n");
			if (recipientInviteUrl != null && addToInvitation) {
				messageText.append("      " + posterViewedBySelf.getName()
						+ " extended an invitation for you: " + recipientInviteUrl + "\n");
			} else if (recipientInviteUrl != null) {
				messageText.append("      " + "There is an invitation for you to join Mugshot: " + recipientInviteUrl + "\n");				
			}
			if (recipientStopUrl != null) {
				messageText.append("      To stop getting these mails, go to " + recipientStopUrl + "\n");
			}
			
			// HTML: append footer
			
			if (recipientInviteUrl != null) {
				messageHtml.append("<div style=\"text-align:center;margin-top:1em;font-size:9pt;\">\n");
				if (addToInvitation) {
					format = "<a href=\"%s\">%s</a> extended an "
					+ "<a href=\"%s\">invitation for you</a> to join <a href=\"%s\">Mugshot</a>\n";

					messageHtml.append(String.format(format, 
							// FIXME handle null public page url
							XmlBuilder.escape(posterPublicPageUrl),
							XmlBuilder.escape(posterViewedBySelf.getName()),
							XmlBuilder.escape(recipientInviteUrl),
							XmlBuilder.escape(baseurl)));					
				} else {
					format = "There is an <a href=\"%s\">invitation for you</a> to join <a href=\"%s\">Mugshot</a>\n";
					messageHtml.append(String.format(format, 
							XmlBuilder.escape(recipientInviteUrl),
							XmlBuilder.escape(baseurl)));						
				}
				messageHtml.append( "</div>\n");

			}
			
			String stopLink;
			if (recipientStopUrl != null)
				stopLink = String.format("| <a style=\"font-size:8pt;\" href=\"%s\">Stop Getting These Mails</a>",
						XmlBuilder.escape(recipientStopUrl));
			else
				stopLink = "";
			
			format = "<div style=\"text-align:center;margin-top:1em;font-size:8pt;\">\n" 
				+ "<a style=\"font-size:8pt;\" href=\"%s\">What's Mugshot?</a> %s\n"
				+ "</div>\n";
			messageHtml.append(String.format(format,
					recipientInviteUrl != null ? XmlBuilder.escape(recipientInviteUrl) : XmlBuilder.escape(baseurl),
					stopLink));
 			
			messageHtml.append("</div>\n");
			messageHtml.append("</body>\n</html>\n");
					
			MimeMessage msg = mailer.createMessage(viewpoint, recipient.getEmail());
			
			mailer.setMessageContent(msg, postView.getTitle(), messageText.toString(), messageHtml.toString(), false);
			
			logger.debug("Sending mail to {}", recipient);
			mailer.sendMessage(msg);
		}
	}
	
	public MessageSenderBean() {
		this.emailSender = new EmailSender();
		this.xmppSender = new XMPPSender();
	}
	
	public void sendPostNotification(Resource recipient, Post post, PostType postType) {
		User user = identitySpider.getUser(recipient);
		if (user != null) {
			// Nothing to do here; will be sent out from the XMPP server based on the 
			// PostCreatedEvent notification
		} else if (recipient instanceof EmailResource) {
			emailSender.sendPostNotification((EmailResource)recipient, post, postType);
		} else {
			throw new IllegalStateException("Don't know how to send a notification to resource: " + recipient);
		}
	}
	public void sendPrefChanged(User user, String key, String value) {
		xmppSender.sendPrefChanged(user, key, value);
	}
}
