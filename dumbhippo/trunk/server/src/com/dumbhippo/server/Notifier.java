package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.AccountStatusListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.server.listeners.GroupChatListener;
import com.dumbhippo.server.listeners.GroupCreationListener;
import com.dumbhippo.server.listeners.GroupMembershipListener;
import com.dumbhippo.server.listeners.MusicListener;
import com.dumbhippo.server.listeners.PostChatListener;
import com.dumbhippo.server.listeners.PostListener;
import com.dumbhippo.server.listeners.UserCreationListener;

/** 
 * The notifier bean implements synchronous notifications to interested 
 * session beans. These are "synchronous hooks" (contrast with an "event
 * queue").
 * 
 * If you call a method on Notifier, the same method will be called on
 * all other session beans that implement it.
 * 
 * The way it works is simple. For each listener interface,
 * we build a list of session beans that implement it.
 * The notifier session bean implements *all* the listener interfaces.
 * If any session bean wants to send out a notification, it calls the 
 * listener method on the Notifier bean, and the Notifier bean then 
 * invokes said method on all other beans that implement the method.
 *  
 * The implementation is a bit strange, since there's no way to 
 * just list all interfaces in a package or class loader. Instead, 
 * to get the set of listener interfaces NotifierBean just scans
 * what it implements itself. And to get the set of session bean 
 * interfaces, it queries the JNDI context.  
 * 
 * @author Havoc Pennington
 */
@Local
public interface Notifier 
	extends AccountStatusListener, UserCreationListener, GroupCreationListener,
	PostListener, ExternalAccountsListener, GroupMembershipListener,
	MusicListener, GroupChatListener, PostChatListener {
	// Nothing in Notifier itself; Notifier just "aggregates"
	// the listener interfaces. If you do add something here
	// then remember that NotifierBean has a non-default transaction 
	// attribute of SUPPORTS for methods
}
