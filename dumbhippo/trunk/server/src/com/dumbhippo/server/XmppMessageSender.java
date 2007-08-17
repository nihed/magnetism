package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;

/**
 * This interface is used to send out XMPP messages from the session beans.
 * In general, it's usually better to let the wildfire server plugin listen 
 * to LiveEvent messages and send out notifications from there, but there
 * are a few cases where this is needed.  
 * 
 * @author otaylor
 */
@Local
public interface XmppMessageSender {
	/**
	 * Send an XMPP message to a user on our system, if they are logged
	 * into the current server. Message delivery is non-blocking and
	 * if two calls to sendLocalMessage() are made in order delivery
	 * of the messages will occur in the same order.
	 * 
	 * The message will be of type 'headline' and be sent from 
	 * admin@dumbhippo.com. (Assuming that the Jive server is configured
	 * as dumbhippo.com)
	 * 
	 * @param guid user to deliver the message to
	 * @param payload contents of the 'message' element. This must
	 *   currently be a single element, so you can't send both
	 *   body text and something else. It's possible to lift that
	 *   restriction, but it might be better to send over a
	 *   dom4j object and get rid of the reparsing we do from
	 *   XML to string. 
	 */
	public void sendLocalMessage(Guid to, String payload);

	/**
	 * Variant of sendLocalMessage sending the message to a set of
	 * users. This may be more efficient than calling 
	 * the single-user variant once per each user, since it doesn't
	 * have to reparse the payload each time.  
	 * 
	 * @param guid user to deliver the message to
	 * @param payload contents of the 'message' element. This must
	 *   currently be a single element.
	 */
	public void sendLocalMessage(Set<Guid> to, String payload);
	
	/**
	 * Sends a notification of a new post to a particular user if they
	 * are logged in to the current server. Otherwise does nothing
	 * 
	 * @param recipient the recipentt of the
	 * @param post the post to send to the recipient
	 */
	public void sendNewPostMessage(User recipient, Post post);
	
	/**
	 * Set the singleton provider that does the work of sending out
	 * messages.
	 * 
	 * @param sender
	 */
	public void setProvider(XmppMessageSenderProvider sender);

	/**
	 * Debug function to send a message to an arbitrary external JID. The from is
	 * settable to allow testing sending a message from different server aliases.
	 * 
	 * @param to recipient of the message
	 * @param from sender of the message (must be a JID on this server domain or alias domain)
	 * @param body body of the message
	 */
	public void sendAdminMessage(String to, String from, String body);
	
	public void sendAdminFriendRequest(String to, String from);
}
