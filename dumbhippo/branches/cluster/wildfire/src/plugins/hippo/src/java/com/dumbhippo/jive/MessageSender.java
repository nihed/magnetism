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
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.Message;

import com.dumbhippo.ThreadUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.EntityView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.XmppMessageSenderProvider;
import com.dumbhippo.server.util.EJBUtil;

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
	private Map<Guid, UserMessageQueue> userQueues = new HashMap<Guid, UserMessageQueue>();
	static private MessageSender instance;
	
	public void start() {
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
	
	static class UserMessageQueue implements Runnable {
		Queue<Message> queue = new LinkedList<Message>();
		String node;
		
		public UserMessageQueue(String node) {
			this.node = node;
		}
		
		public synchronized void addMessage(ExecutorService pool, Message template) {
			boolean wasEmpty = queue.isEmpty();
			queue.add(template);
			
			if (wasEmpty)
				pool.execute(this);
		}
		
		public void run() {
			List<Message> toSend;
			
			synchronized(this) {
				toSend = new ArrayList<Message>(queue);
				queue.clear(); 
			}
			
			for (Message template : toSend) {
				Message message = template.createCopy();
				try {
					SessionManager.getInstance().userBroadcast(node, message);
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
	
	/**
	 * Send a message to a set of users.
	 * 
	 * @param to the users to send the message to
	 * @param template template for the messages we want to send out. It will be
	 *     copied and the recipient filled in.
	 */
	public void sendMessage(Set<Guid> to, Message template) {
		for (Guid guid : to) {
			String node = guid.toJabberId(null);
			
			// We want to avoid queueing messages for users not on this server,
			// since that will frequently be the vast majority of the recipients
			// that we are given
			if (SessionManager.getInstance().getSessionCount(node) == 0)
				continue;

			UserMessageQueue userQueue;

			synchronized (userQueues) {
				userQueue = userQueues.get(guid);
				if (userQueue == null) {
					userQueue = new UserMessageQueue(node);
					userQueues.put(guid, userQueue);
				}
			}
			
			userQueue.addMessage(pool, template);
		}
	}

	/**
	 * sendMessage variant that sends the message only to a single recipient
	 * 
	 * @param to the recipient
	 * @param template Message object to send
	 */
	public void sendMessage(Guid to, Message template) {
		sendMessage(Collections.singleton(to), template);
	}

	// This is the sendMessage() variant used by the Session beans to avoid
	// having to access Wildfire XMPP types. 
	public void sendMessage(Set<Guid> to, String payload) {
        Document payloadDocument;
        try {
            payloadDocument = DocumentHelper.parseText(payload);
        } catch (DocumentException e) {
            throw new RuntimeException("Couldn't parse payload as XML");
        }
        
        Element payloadElement = payloadDocument.getRootElement();
        payloadElement.detach();
        Message template = new Message();
        template.setType(Message.Type.headline);
        template.getElement().add(payloadElement);
        sendMessage(to, template);
	}

	private static final String ELEMENT_NAME = "newPost";
	private static final String NAMESPACE = "http://dumbhippo.com/protocol/post";
	
	private void addNewPostExtension(Message message, PostView postView, Set<EntityView> referencedEntities) {
		// Add the extra XML elements to the message that convey all our
		// structured information. Doing this by building an XML string then
		// parsing it is gross, but avoids larger code changes.
		
		XmlBuilder builder = new XmlBuilder();
		builder.openElement(ELEMENT_NAME, "xmlns", NAMESPACE);
		for (EntityView ev: referencedEntities) {
			ev.writeToXmlBuilder(builder);
		}
		postView.writeToXmlBuilder(builder);
		builder.closeElement();
		
        Document extensionDocument;
        try {
            extensionDocument = DocumentHelper.parseText(builder.toString());
        } catch (DocumentException e) {
            throw new RuntimeException("Couldn't parse payload as XML");
        }
        
        Element extensionElement = extensionDocument.getRootElement();
        extensionElement.detach();
        
        message.getElement().add(extensionElement);
	}
	
	public void sendNewPostMessage(User recipient, Post post) {
		PostingBoard postingBoard = EJBUtil.defaultLookup(PostingBoard.class);
		
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
        message.setType(Message.Type.headline);
        message.setBody(String.format("%s\n%s", title, url));
        addNewPostExtension(message, postView, referenced);
        
        sendMessage(recipient.getGuid(), message);
	}
}
