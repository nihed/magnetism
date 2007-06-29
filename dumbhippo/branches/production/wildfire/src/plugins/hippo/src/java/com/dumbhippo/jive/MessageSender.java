package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ChannelHandler;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.Session;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.dumbhippo.ThreadUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.XmppMessageSenderProvider;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.EntityView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/**
 * This class implements sending messages to a particular user or list of users;
 * one use for this class is for to act as the "message sender provider" for the
 * session beans. But it is also useful for sending messages from the plugin
 * code which runs inside Wildfire, because it provides non-blocking ordered
 * delivery, something which is harder to get using the Wildfire internal
 * methods (as the hoops we jump through here show).
 * 
 * Non-blocking: A call to sendMessage will never block: the message is just
 *   queued for future delivery. 
 * 
 * Ordering: If one call to sendMessage completes before another starts, and 
 *   they share a common recipient, then the messages will be delivered to the
 *   recipients in that order.
 *   
 * Reliability: If the user is online when sendMessage() is called and stays
 *   online for sufficiently long after, they will get the message
 */
public class MessageSender implements XmppMessageSenderProvider {
	ExecutorService pool = ThreadUtils.newCachedThreadPool("MessageSender-pool");
	private Map<JID, SessionMessageQueue> sessionQueues = new HashMap<JID, SessionMessageQueue>();
	static private MessageSender instance;
	
	private GroupSystem groupSystem;
	private PersonViewer personViewer;
	private PostingBoard postingBoard;

	public void start() {
		groupSystem = EJBUtil.defaultLookup(GroupSystem.class);
		personViewer = EJBUtil.defaultLookup(PersonViewer.class);
		postingBoard = EJBUtil.defaultLookup(PostingBoard.class);
		
		instance = this;
	}

	public void shutdown() {
		instance = null;
		pool.shutdown();
	}
	
	/**
	 * @return the static singleton instance of this class. May be null during startup
	 *  or shutdown, but otherwise will always exist.
	 */
	static public MessageSender getInstance() {
		return instance;
	}
	
	static class SessionMessageQueue implements Runnable {
		Queue<Packet> queue = new LinkedList<Packet>();
		JID resource;
		
		public SessionMessageQueue(JID resource) {
			this.resource = resource;
		}
		
		public synchronized void addMessage(ExecutorService pool, Packet template) {
			boolean wasEmpty = queue.isEmpty();
			queue.add(template);
			
			if (wasEmpty)
				pool.execute(this);
		}
		
		public void run() {
			List<Packet> toSend;
			
			synchronized(this) {
				toSend = new ArrayList<Packet>(queue);
				queue.clear(); 
			}
			
			Session session = SessionManager.getInstance().getSession(resource);
			if (session == null)
				return; // Just dump packets to not-connected resources

			for (Packet template : toSend) {
				Packet packet = template.createCopy();
				packet.setTo(resource);
				
				try {
					@SuppressWarnings("unchecked")
					ChannelHandler<Packet> handler = session;
					handler.process(packet);
				} catch (UnauthorizedException e) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * Check to see if a user is present on the system. This check will
	 * be done automatically when you call sendMessage(), but if you
	 * are sending to only one user and creating the message payload
	 * is significantly expensive, then it may be useful to check ahead
	 * of time.
	 */ 
	public boolean userIsPresent(Guid guid) {
		String node = guid.toJabberId(null);
		return (SessionManager.getInstance().getSessionCount(node) > 0);
	}

	private void queuePacket(JID address, Packet template) {
		SessionMessageQueue sessionQueue;

		synchronized (sessionQueues) {
			sessionQueue = sessionQueues.get(address);
			if (sessionQueue == null) {
				sessionQueue = new SessionMessageQueue(address);
				sessionQueues.put(address, sessionQueue);
			}
		}
		
		sessionQueue.addMessage(pool, template);
	}
	
	/**
	 * Send a message to a set of users
	 * 
	 * @param to the users to send the message to
	 * @param template template for the messages we want to send out. It will be
	 *     copied and the recipient filled in.
	 */
	public void sendPacketToUsers(Set<Guid> to, Packet template) {
		for (Guid guid : to) {
			String node = guid.toJabberId(null);
			
			for (ClientSession session: SessionManager.getInstance().getSessions(node)) {
				queuePacket(session.getAddress(), template);
			}
			
		}
	}

	/**
	 * Send a message to a set of resources
	 * 
	 * @param to the resources to send the message to
	 * @param template template for the messages we want to send out. It will be
	 *     copied and the recipient filled in.
	 */
	public void sendPacketToResources(Set<JID> to, Packet template) {
		for (JID address : to) {
			queuePacket(address, template);
		}
	}
	
	/**
	 * Send a message to a single resource
	 * 
	 * @param to the resource to send the message to
	 * @param template template for the messages we want to send out. It will be
	 *     copied and the recipient filled in.
	 */
	public void sendPacketToResource(JID to, Packet template) {
		queuePacket(to, template);
	}
	
	/**
	 * sendMessage variant that sends the message only to a single recipient
	 * 
	 * @param to the recipient
	 * @param template Message object to send
	 */
	public void sendMessage(Guid to, Message template) {
		sendPacketToUsers(Collections.singleton(to), template);
	}

	private Element elementFromXml(String xmlString) {
        Document document;
        try {
            document = DocumentHelper.parseText(xmlString);
        } catch (DocumentException e) {
            throw new RuntimeException("Couldn't parse payload as XML");
        }
        
        Element element = document.getRootElement();
        element.detach();
        
        return element;
	}
	
	// This is the sendMessage() variant used by the Session beans to avoid
	// having to access Wildfire XMPP types. 
	public void sendMessage(Set<Guid> to, String payload) {
        Message template = new Message();
        template.setType(Message.Type.headline);
        template.getElement().add(elementFromXml(payload));
        sendPacketToUsers(to, template);
	}

	private static final String NEW_POST_ELEMENT_NAME = "newPost";
	private static final String NEW_POST_NAMESPACE = "http://dumbhippo.com/protocol/post";
	
	private void addNewPostExtension(Message message, PostView postView, Set<EntityView> referencedEntities) {
		// Add the extra XML elements to the message that convey all our
		// structured information. Doing this by building an XML string then
		// parsing it is gross, but avoids larger code changes.
		
		XmlBuilder builder = new XmlBuilder();
		builder.openElement(NEW_POST_ELEMENT_NAME, "xmlns", NEW_POST_NAMESPACE);
		for (EntityView ev: referencedEntities) {
			ev.writeToXmlBuilderOld(builder);
		}
		postView.writeToXmlBuilderOld(builder);
		builder.closeElement();
		
        Document extensionDocument;
        try {
            extensionDocument = DocumentHelper.parseText(builder.toString());
        } catch (DocumentException e) {
            throw new RuntimeException("Couldn't parse payload as XML");
        }
        
        Element extensionElement = extensionDocument.getRootElement();
        extensionElement.detach();
        
        message.getElement().add(elementFromXml(builder.toString()));
	}
	
	public void sendNewPostMessage(User recipient, Post post) {
		// Short-circuit message creation
		if (!userIsPresent(recipient.getGuid()))
			return;
		
		String title = post.getTitle();
		String url = post.getUrl() != null ? post.getUrl().toExternalForm() : null;
		
		if (url == null) {
			// this particular jabber message protocol has no point without an url
			Log.debug("no url found on post, not sending xmpp message");
			return;
		}
		
		Viewpoint viewpoint = new UserViewpoint(recipient);

		PostView postView = postingBoard.getPostView(viewpoint, post);
		Set<EntityView> referenced = postingBoard.getReferencedEntities(viewpoint, post);
		
        Message message = new Message();
        message.setType(Message.Type.normal);
        message.setBody(String.format("%s\n%s", title, url));
        addNewPostExtension(message, postView, referenced);
        
        sendMessage(recipient.getGuid(), message);
	}
	
	private static final String MEMBERSHIP_CHANGE_ELEMENT_NAME = "membershipChange";
	private static final String MEMBERSHIP_CHANGE_NAMESPACE = "http://dumbhippo.com/protocol/group";
	
	private void addMembershipChangeExtension(Message message, GroupView groupView, GroupMember member, PersonView memberView) {
		XmlBuilder builder = new XmlBuilder();
		builder.openElement(MEMBERSHIP_CHANGE_ELEMENT_NAME, "xmlns", MEMBERSHIP_CHANGE_NAMESPACE, 
				            "membershipStatus", member.getStatus().toString(),
				            "groupId", groupView.getIdentifyingGuid().toString(), 
				            "userId", memberView.getIdentifyingGuid().toString());
		memberView.writeToXmlBuilderOld(builder);
		groupView.writeToXmlBuilderOld(builder);
		builder.closeElement();
		
        message.getElement().add(elementFromXml(builder.toString()));
	}
	
	public void sendGroupMembershipChange(User recipient, GroupMember groupMember) {
		// Short-circuit message creation
		if (!userIsPresent(recipient.getGuid()))
			return;
		
		Viewpoint viewpoint = new UserViewpoint(recipient);
		
		PersonView memberView = personViewer.getPersonView(viewpoint, groupMember.getMember());

		GroupView groupView = groupSystem.getGroupView(viewpoint, groupMember.getGroup());
		
        Message message = new Message();
        message.setType(Message.Type.normal);
        addMembershipChangeExtension(message, groupView, groupMember, memberView);
        
		sendMessage(recipient.getGuid(), message);
	}
}
