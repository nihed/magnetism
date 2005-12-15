package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;

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
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.LinkResource;
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
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

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
	private Mailer mailer;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private InvitationSystem invitationSystem;

	@EJB
	private NoMailSystem noMail;
	
	// Our delegates
	
	private XMPPSender xmppSender;
	private EmailSender emailSender;
	
	private static class LinkExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "link";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private String senderName;
		private Guid senderGuid;
		
		private Set<String> recipientNames;
		
		private String url;
		private String postInfo;

		private Guid guid;

		private String title;
		
		private String description;

		private Set<String> groupRecipients;
		
		private List<String> viewers;

		public LinkExtension() {
		}
		
		public void setSenderName(String senderName) {
			this.senderName = senderName;
		}
		public void setSenderGuid(Guid senderGuid) {
			this.senderGuid = senderGuid;
		}
		public void setPostId(Guid postId) {
			this.guid = postId;
		}
		public void setRecipientNames(Set<String> recipientNames) {
			this.recipientNames = recipientNames;
		}
		public void setGroupRecipients(Set<String> groupRecipients) {
			this.groupRecipients = groupRecipients;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public void setViewers(List<String> viewers) {
			this.viewers = viewers;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("link", "id", guid.toString(), "xmlns", NAMESPACE, "href", url);
			builder.appendTextNode("senderName", senderName, "isCache", "true");
			builder.appendTextNode("senderGuid", senderGuid.toString());			
			builder.appendTextNode("title", title);
			builder.appendTextNode("description", description);
			builder.appendTextNode("postInfo", postInfo);
			builder.openElement("recipients");
			for (String recipient : recipientNames) {
				builder.appendTextNode("recipient", recipient);
			}
			builder.closeElement();
			builder.openElement("groupRecipients");
			for (String recipient : groupRecipients) {
				builder.appendTextNode("recipient", recipient);
			}
			builder.closeElement();
			if (viewers != null) {
				builder.openElement("viewers");
				for (String recipient : viewers) {
					builder.appendTextNode("viewer", recipient);
				}
				builder.closeElement();					
			}
			builder.closeElement();
			return builder.toString();
		}
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String getPostInfo() {
			return postInfo;
		}

		public void setPostInfo(String postInfo) {
			this.postInfo = postInfo;
		}
	}
	
	private static class LinkClickedExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "linkClicked";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private Guid swarmerGuid;
		private String clickerName;

		private Guid guid;

		private String title;
		
		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("linkClicked", "xmlns", NAMESPACE, "id", guid.toString(), "swarmerId", swarmerGuid.toString());
			builder.appendTextNode("swarmerName", clickerName, "isCache", "true");	
			builder.appendTextNode("postTitle", title, "isCache", "true");
			builder.closeElement();
			return builder.toString();
		}
		public LinkClickedExtension(Guid swarmerGuid, String clickerName, Guid postId, String title) {
			this.swarmerGuid = swarmerGuid;
			this.clickerName = clickerName;
			this.guid = postId;
			this.title = title;
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
		
		private String makeJid(User recipient) {
			StringBuilder recipientJid = new StringBuilder();
			recipientJid.append(recipient.getId().toString());
			recipientJid.append("@dumbhippo.com");
			
			return recipientJid.toString();
		}
		
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

		public synchronized void sendPostNotification(User recipient, Post post, List<User> viewers) {
			XMPPConnection connection = getConnection();

			if (connection == null || !connection.isConnected()) {
				logger.error("Connection to XMPP is not active, not sending notification");
				return;
			}
			
			Message message = new Message(makeJid(recipient), Message.Type.HEADLINE);

			String title = post.getTitle();
			
			String url = post.getUrl() != null ? post.getUrl().toExternalForm() : null;
			
			if (url == null) {
				// this particular jabber message protocol has no point without an url
				logger.debug("no url found on post");
				return;
			}
			
			Viewpoint viewpoint = new Viewpoint(recipient);

			PersonView recipientView = identitySpider.getPersonView(viewpoint, post.getPoster());
			String senderName = recipientView.getName();
			Set<String> recipientNames = new HashSet<String>();
			for (Resource r : post.getPersonRecipients()) {
				PersonView viewedP = identitySpider.getPersonView(viewpoint, r, PersonViewExtra.PRIMARY_RESOURCE);
				recipientNames.add(viewedP.getName());
			}
			
			Set<String> groupRecipientNames = new HashSet<String>();
			for (Group g : post.getGroupRecipients()) {
				groupRecipientNames.add(g.getName());
			}
			
			LinkExtension extension = new LinkExtension();
			extension.setPostId(post.getGuid());
			extension.setSenderName(senderName);
			extension.setSenderGuid(post.getPoster().getGuid());
			extension.setRecipientNames(recipientNames);
			extension.setGroupRecipients(groupRecipientNames);
			extension.setUrl(url);
			extension.setTitle(title);
			extension.setDescription(post.getText());
			extension.setPostInfo(post.getInfo());
			
			if (viewers != null) {
				List<String> viewerNames = new ArrayList<String>();
				for (User u : viewers) {
					PersonView viewedP = identitySpider.getPersonView(viewpoint, u);
					viewerNames.add(viewedP.getName());
				}
				extension.setViewers(viewerNames);
			}
			
			message.addExtension(extension);

			message.setBody(String.format("%s\n%s", title, url));

			logger.info("Sending jabber message to " + message.getTo());
			connection.sendPacket(message);
		}
		
		public synchronized void sendPostClickedNotification(Post post, User clicker) {
			XMPPConnection connection = getConnection();

			for (Resource recipientResource : post.getExpandedRecipients()) {
				User recipient = identitySpider.getUser(recipientResource);
				if (recipient == null) {
					logger.debug("No user for " + recipientResource.getId());
				}
				
				Message message = new Message(makeJid(recipient), Message.Type.HEADLINE);

				Viewpoint viewpoint = new Viewpoint(recipient);
				PersonView senderView = identitySpider.getPersonView(viewpoint, clicker);
				String clickerName = senderView.getName();
				String title = post.getTitle();
				if (title == null || title.equals("")) {
					LinkResource link = null;
					// FIXME don't assume link resources
					Set<Resource> resources = post.getResources();					
					for (Resource r : resources) {
						if (r instanceof LinkResource) {
							link = (LinkResource) r;
							break;
						}
					}
					if (link != null)
						title = link.getHumanReadableString();
					else
						title = "(unknown)";
				}
				message.addExtension(new LinkClickedExtension(recipient.getGuid(), clickerName, post.getGuid(), title));
				message.setBody("");
				logger.info("Sending jabber message to " + message.getTo());
				connection.sendPacket(message);
			}
		}
	}

	private class EmailSender {

		public void sendPostNotification(EmailResource recipient, Post post) {
			if (!noMail.getMailEnabled(recipient)) {
				logger.debug("Mail is disabled to " + recipient + " not sending post notification");
				return;
			}
			
			String baseurl = config.getProperty(HippoProperty.BASEURL);
			
			// may be null!
			InvitationToken invitation = invitationSystem.getInvitation(post.getPoster(), recipient); 
			String recipientInviteUrl;
			if (invitation != null) 
				recipientInviteUrl = invitation.getAuthURL(config.getProperty(HippoProperty.BASEURL)); 
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
			PersonView posterViewedBySelf = identitySpider.getPersonView(new Viewpoint(post.getPoster()), 
					                                                     post.getPoster(),
					                                                     PersonViewExtra.PRIMARY_EMAIL);
			
			StringBuilder messageText = new StringBuilder();
			XmlBuilder messageHtml = new XmlBuilder();
			
			messageHtml.appendHtmlHead("");
			messageHtml.append("<body>\n");
			
			messageHtml.append("<div style=\"width:500px\">\n");
			messageHtml.append("  <div style=\"border:1px solid black;min-height:100px;\"><!-- bubble div -->\n");
			
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
					redirectUrl.append(StringUtils.urlEncode(url));
					redirectUrl.append("&postId=");
					redirectUrl.append(post.getId()); // no need to encode, we know it is OK

					if (invitation != null) {
						redirectUrl.append("&inviteKey=");
						redirectUrl.append(invitation.getAuthKey());
					}
					
					messageText.append(url);
					messageText.append("\n");
					
			        String format = "<div style=\"margin:0.3em;\">\n" 
			        + "<a style=\"font-weight:bold;font-size:150%%;\" title=\"%s\" href=\"%s\">%s</a>\n"
			        + "</div>\n";

			        // FIXME we repeat the post title for every link, if we ever really support multiple links 
			        messageHtml.append(String.format(format, XmlBuilder.escape(url),
			        		XmlBuilder.escape(redirectUrl.toString()), XmlBuilder.escape(title)));
				}
			}

			if (title == null) {
				// uhhhh....
				title = "Untitled Post";
			}
			
			messageText.append("\n");
			
			// TEXT: append post text
			messageText.append(post.getText());
			
			// HTML: append post text
			messageHtml.append("<div style=\"font-size:120%;margin:0.5em;\">\n");
			messageHtml.appendTextAsHtml(post.getText(), null);
			messageHtml.append("</div>\n");

			messageHtml.append("  </div><!-- close bubble div -->\n");
			
			// TEXT: "link shared by"
			
			messageText.append("  (Link shared by " + posterViewedBySelf.getName() + ")");
			
			String viewPersonPageId = posterViewedBySelf.getViewPersonPageId();
			String posterPublicPageUrl = null;
			if (viewPersonPageId != null)
				posterPublicPageUrl = baseurl + "/viewperson?personId=" + viewPersonPageId;
						
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
			if (recipientInviteUrl != null) {
				messageText.append("      " + posterViewedBySelf.getName()
						+ " created an invitation for you: " + recipientInviteUrl + "\n");
			}
			if (recipientStopUrl != null) {
				messageText.append("      To stop getting these mails, go to " + recipientStopUrl + "\n");
			}
			
			// HTML: append footer
			
			if (recipientInviteUrl != null) {
				format = "<div style=\"text-align:center;margin-top:1em;font-size:9pt;\">\n"
					+ "<a href=\"%s\">%s</a> created an open "
					+ "<a href=\"%s\">invitation for you</a> to use <a href=\"%s\">Dumb Hippo</a>\n"
					+ "</div>\n";
				messageHtml.append(String.format(format, 
						// FIXME handle null public page url
						XmlBuilder.escape(posterPublicPageUrl),
						XmlBuilder.escape(posterViewedBySelf.getName()),
						XmlBuilder.escape(recipientInviteUrl),
						XmlBuilder.escape(baseurl)));
			}
			
			String stopLink;
			if (recipientStopUrl != null)
				stopLink = String.format("| <a style=\"font-size:8pt;\" href=\"%s\">Stop Getting These Mails</a>",
						XmlBuilder.escape(recipientStopUrl));
			else
				stopLink = "";
			
			format = "<div style=\"text-align:center;margin-top:1em;font-size:8pt;\">\n" 
				+ "<a style=\"font-size:8pt;\" href=\"%s\">What's DumbHippo?</a> %s\n"
				+ "</div>\n";
			messageHtml.append(String.format(format,
					recipientInviteUrl != null ? XmlBuilder.escape(recipientInviteUrl) : XmlBuilder.escape(baseurl),
					stopLink));
 			
			messageHtml.append("</div>\n");
			messageHtml.append("</body>\n</html>\n");
					
			MimeMessage msg = mailer.createMessage(post.getPoster(), recipient.getEmail());
			
			mailer.setMessageContent(msg, title, messageText.toString(), messageHtml.toString());
			
			logger.debug("Sending mail to " + recipient.toString());
			mailer.sendMessage(msg);
		}
	}
	
	public MessageSenderBean() {
		this.emailSender = new EmailSender();
		this.xmppSender = new XMPPSender();
	}
	
	public void sendPostNotification(Resource recipient, Post post) {
		if (recipient instanceof Account) {
			Account account = (Account)recipient;
			xmppSender.sendPostNotification(account.getOwner(), post, null);
		} else if (recipient instanceof EmailResource) {
			emailSender.sendPostNotification((EmailResource)recipient, post);
		} else {
			throw new IllegalStateException("Don't know how to send a notification to resource: " + recipient);
		}
	}

	public void sendPostClickedNotification(Post post, List<User> viewers, User clicker) {
		boolean seenPoster = false;
		
		for (Resource recipientResource : post.getExpandedRecipients()) {
			User recipient = identitySpider.getUser(recipientResource);
			if (recipient != null) {
				if (!recipient.equals(clicker))
					xmppSender.sendPostNotification(recipient, post, viewers);
				if (recipient.equals(post.getPoster()))
					seenPoster = true;
			}
		}
		
		// Send to the poster, but not if they were in expandedRecipients
		if (!seenPoster)
			xmppSender.sendPostNotification(post.getPoster(), post, viewers);
	}
}
