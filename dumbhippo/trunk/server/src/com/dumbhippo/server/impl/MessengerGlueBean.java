package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	static private final Logger logger = GlobalSetup.getLogger(MessengerGlueBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
		
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private PostingBoard postingBoard;
		
	private Account accountFromUsername(String username) throws JabberUserNotFoundException {
		Guid guid;
		try {
			guid = Guid.parseJabberId(username);
		} catch (ParseException e) {
			throw new JabberUserNotFoundException("corrupt username");
		}
		Account account = accountSystem.lookupAccountByPersonId(guid.toString());
		if (account == null)
			throw new JabberUserNotFoundException("username does not exist");
		
		assert account.getOwner().getId().equals(username);
		
		return account;
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		Account account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			return false;
		}
		
		assert account != null;
		
		return account.checkClientCookie(token, digest);
	}
	

	public long getJabberUserCount() {
		return accountSystem.getNumberOfActiveAccounts();
	}


	public void setName(String username, String name)
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


	public void setEmail(String username, String email) 
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public JabberUser loadUser(String username) throws JabberUserNotFoundException {
		
		Account account = accountFromUsername(username);
		
		PersonView view = identitySpider.getSystemView(account.getOwner(), PersonViewExtra.PRIMARY_EMAIL);
		
		JabberUser user = new JabberUser(username, account.getOwner().getNickname(), view.getEmail().getEmail());
	
		return user;
	}

	public void serverStartup(long timestamp) {
		logger.debug("Jabber server startup at " + new Date(timestamp));
	}
	
	public void onUserAvailable(String username) {
		logger.debug("Jabber user " + username + " now available");
		try {
			// account could be missing due to debug users or our own
			// send-notifications
			// user, i.e. any user on the jabber server that we don't know about
			Account account;
			try {
				account = accountFromUsername(username);
			} catch (JabberUserNotFoundException e) {
				logger.debug("username signed on that we don't know: " + username);
				return;
			}
			if (!account.getWasSentShareLinkTutorial()) {
				logger.debug("We have a new user!!!!! WOOOOOOOOOOOOHOOOOOOOOOOOOOOO send them tutorial!");
	
				postingBoard.doShareLinkTutorialPost(account.getOwner());
	
				account.setWasSentShareLinkTutorial(true);
			}
		} catch (RuntimeException e) {
			logger.error("Failed to do share link tutorial", e);
			throw e;
		}
	}

	public void onUserUnavailable(String username) {
		logger.debug("Jabber user " + username + " now unavailable");
	}
	
	private User getUserFromUsername(String username) {
		User user = null;
		try {
			user = identitySpider.lookupGuid(User.class, Guid.parseJabberId(username));
		} catch (NotFoundException e) {
		} catch (Guid.ParseException e) {
		}
		
		return user;
	}
	
	private Post getPostFromRoomName(User user, String roomName) {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Post post = null;
		try {
			post = postingBoard.loadRawPost(viewpoint, Guid.parseJabberId(roomName));
		} catch (Guid.ParseException e) {
		}
		
		return post;
	}
	
	public ChatRoomInfo getChatRoomInfo(String roomName, String initialUsername) {
		User initialUser = getUserFromUsername(initialUsername);
		if (initialUser == null)
			throw new RuntimeException("non-existant username: " + initialUsername);
				
		Post post = getPostFromRoomName(initialUser, roomName);
		if (post == null)
			return null;
		 
		List<ChatRoomUser> allowedUsers = new ArrayList<ChatRoomUser>();
		
		User poster = post.getPoster();
		allowedUsers.add(new ChatRoomUser(poster.getGuid().toJabberId(null), poster.getVersion(), poster.getNickname()));
		
		// FIXME: This isn't really right; it doesn't handle public posts and
		// posts
		// where people join a group that it was sent to after the post was
		// sent. Public posts will need to be handled with a separate flag
		// in ChatRoomInfo.
		for (Resource recipient : post.getExpandedRecipients()) {
			User user = identitySpider.getUser(recipient);
			if (user != null && !user.equals(poster))
				allowedUsers.add(new ChatRoomUser(user.getGuid().toJabberId(null), user.getVersion(), user.getNickname()));
		}
		
		List<PostMessage> messages = postingBoard.getPostMessages(post);

		List<ChatRoomMessage> history = new ArrayList<ChatRoomMessage>();
		for (PostMessage postMessage : messages) {
			String username = postMessage.getFromUser().getGuid().toJabberId(null);
			ChatRoomMessage message;
			message = new ChatRoomMessage(username, postMessage.getMessageText(), postMessage.getTimestamp()); 
			history.add(message);
		}
		
		return new ChatRoomInfo(roomName, post.getTitle(), allowedUsers, history);
	}
}
