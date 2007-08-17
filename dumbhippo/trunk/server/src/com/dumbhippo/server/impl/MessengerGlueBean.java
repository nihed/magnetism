package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.ServerStatus;
import com.dumbhippo.server.blocks.PostBlockHandler;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

@Stateless
public class MessengerGlueBean implements MessengerGlue {
	
	static private final Logger logger = GlobalSetup.getLogger(MessengerGlueBean.class);
	
	@EJB
	private ClaimVerifier claimVerifier;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
		
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private PostBlockHandler postBlockHandler;

	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private ServerStatus serverStatus;
	
	static final private long EXECUTION_WARN_MILLISECONDS = 5000;
	
	static private long tooBusyCount;
	
	static final private int MAX_ACTIVE_REQUESTS = 7; 
	
	static private int activeRequestCount;
	
	static private int maxActiveRequestCount;
	
	static private synchronized void changeActiveRequestCount(int delta) {
		activeRequestCount += delta;
		if (activeRequestCount > maxActiveRequestCount)
			maxActiveRequestCount = activeRequestCount;
	}
	
	static public synchronized int getActiveRequestCount() {
		return activeRequestCount;
	}

	static public synchronized int getMaxActiveRequestCount() {
		return maxActiveRequestCount;
	}
	
	static private synchronized void incrementTooBusyCount() {
		tooBusyCount += 1;
	}
	
	static public synchronized long getTooBusyCount() {
		return tooBusyCount;
	}
	
	@AroundInvoke
	public Object timeAndCatchRuntimeExceptions(InvocationContext ctx) throws Exception {
		try {
			changeActiveRequestCount(1);
			long start = System.currentTimeMillis();
			Object result = ctx.proceed();
			long end = System.currentTimeMillis();
			if (end - start > EXECUTION_WARN_MILLISECONDS) {
				logger.warn("Execution of MessengerGlueBean.{} took {} milliseconds", 
				  	        ctx.getMethod().getName(), end - start);
			}
			return result;
		} finally {
			changeActiveRequestCount(-1);
		}
	}
	
	private Account accountFromUsername(String username) throws JabberUserNotFoundException {
		try {
			Guid guid = Guid.parseJabberId(username);
			return accountFromUserId(guid);
		} catch (ParseException e) {
			throw new JabberUserNotFoundException("corrupt username");
		}
	}
	
	private Account accountFromUserId(Guid userId) throws JabberUserNotFoundException {
		try {
			Account account = accountSystem.lookupAccountByOwnerId(userId);
			
			assert account.getOwner().getId().equals(userId);
			
			return account;
		} catch (NotFoundException e) {
			throw new JabberUserNotFoundException("username does not exist");
		}
	}
	
	@SuppressWarnings("unused")
	private User userFromTrustedUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted username doesn't appear to exist: " + username);
		}
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		Account account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			return false;
		}
		
		assert account != null;
		
		return !account.isAdminDisabled() && account.checkClientCookie(token, digest);
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
		
		PersonView view = personViewer.getSystemView(account.getOwner());
		
		String email = null;
		if (view.getEmail() != null)
			email = view.getEmail().getEmail();
		
		JabberUser user = new JabberUser(username, account.getOwner().getNickname(), email);
	
		return user;
	}
	
	private void doShareLinkTutorial(UserViewpoint newUser) throws RetryException {
		logger.debug("We have a new user!!!!! WOOOOOOOOOOOOHOOOOOOOOOOOOOOO send them tutorial!");

		Account account = newUser.getViewer().getAccount();
		
		InvitationToken invite = invitationSystem.getCreatingInvitation(account);
		
		// see what feature the user was sold on originally, and share the right thing 
		// with them accordingly
		
		User owner = newUser.getViewer();
		if (invite != null && invite.getPromotionCode() == PromotionCode.MUSIC_INVITE_PAGE_200602)
			postingBoard.doNowPlayingTutorialPost(newUser, owner);
		else {
			Set<Group> invitedToGroups = groupSystem.findRawGroups(newUser, owner, MembershipStatus.INVITED);
			Set<Group> invitedToFollowGroups = groupSystem.findRawGroups(newUser, owner, MembershipStatus.INVITED_TO_FOLLOW);
			invitedToGroups.addAll(invitedToFollowGroups);
			if (invitedToGroups.size() == 0) {
				postingBoard.doShareLinkTutorialPost(newUser, owner);
			} else {
				for (Group group : invitedToGroups) {
					postingBoard.doGroupInvitationPost(newUser, owner, group);
				}
			}
		}

		account.setWasSentShareLinkTutorial(true);
	}

	public void updateLoginDate(Guid userId, Date timestamp) {
		// account could be missing due to debug users or our own
		// send-notifications user. In fact any user on the jabber server 
		// that we don't know about
		Account account;
		try {
			account = accountFromUserId(userId);
		} catch (JabberUserNotFoundException e) {
			logger.warn("username logged in that we don't know: {}", userId);
			return;
		}
		
		account.setNeedsDownload(false);
		account.setLastLoginDate(timestamp);
	}	

	public void updateLogoutDate(Guid userId, Date timestamp) {
		Account account;
		try {
			account = accountFromUserId(userId);
		} catch (JabberUserNotFoundException e) {
			logger.warn("username logged out that we don't know: {}", userId);
			return;
		}
		
		account.setLastLogoutDate(timestamp);
	}
	
	public void sendConnectedResourceNotifications(Guid userId, boolean wasAlreadyConnected) throws RetryException {
		Account account;
		try {
			account = accountFromUserId(userId);
		} catch (JabberUserNotFoundException e) {
			logger.warn("username signed on that we don't know: {}", userId);
			return;
		}

		UserViewpoint viewpoint = new UserViewpoint(account.getOwner(), Site.XMPP);
		
		if (!account.getWasSentShareLinkTutorial()) {
			doShareLinkTutorial(viewpoint);
		}
	}
	
	private User getUserFromGuid(Guid guid) {
		try {
			return identitySpider.lookupGuid(User.class, guid);
		} catch (NotFoundException e) {
			throw new RuntimeException("User does not exist: " + guid, e);
		}
	}
	
	public Map<String, String> getCurrentMusicInfo(String username) {
		Map<String,String> musicInfo = new HashMap<String,String>();
		
		// would through an exception if the user does not exist
		User user = getUserFromGuid(Guid.parseTrustedJabberId(username));
		try {
			// because we are asking only for one recent track, we do not need to
			// pass a viewpoint
		    TrackView trackView = musicSystem.getCurrentTrackView(null, user);
		    musicInfo.put("name", trackView.getName());
		    musicInfo.put("artist", trackView.getArtist());
		    musicInfo.put("musicPlaying", Boolean.toString(trackView.isNowPlaying()));
		} catch (NotFoundException e) {
			// user does not have a music history
			return null;
		}
		
		return musicInfo;
	}

	public Map<String,String> getPrefs(String username) {
		Map<String,String> prefs = new HashMap<String,String>();
		
		Account account;
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			logger.warn("Returning empty prefs for user we've never heard of");
			return prefs;
		}
		
		return accountSystem.getPrefs(account);
	}

	static final String RECENT_POSTS_ELEMENT_NAME = "recentPosts";
	static final String RECENT_POSTS_NAMESPACE = "http://dumbhippo.com/protocol/post";
	
	public void setPostIgnored(Guid userId, Guid postId, boolean ignore) throws NotFoundException, ParseException {
		User user = getUserFromGuid(userId);
		UserViewpoint viewpoint = new UserViewpoint(user, Site.XMPP);
		Post post = postingBoard.loadRawPost(viewpoint, postId);
		
		postBlockHandler.setPostHushed(viewpoint, post, ignore);
	}

	public void addGroupMember(Guid userId, Guid groupId, Guid inviteeId) throws NotFoundException {
		User user = getUserFromGuid(userId);
		UserViewpoint viewpoint = new UserViewpoint(user, Site.XMPP);
		GroupView groupView = groupSystem.loadGroup(viewpoint, groupId);
		User invitee = getUserFromGuid(inviteeId);		
		groupSystem.addMember(user, groupView.getGroup(), invitee);
	}

	public boolean isServerTooBusy() {
		if (activeRequestCount >= MAX_ACTIVE_REQUESTS || serverStatus.throttleXmppConnections()) {
			incrementTooBusyCount();
			return true;
		} else {
			return false;
		}
	}
	
	public void handleMusicChanged(UserViewpoint viewpoint, Map<String, String> properties) throws RetryException {
		musicSystem.setCurrentTrack(viewpoint.getViewer(), properties, true);
	}

	public void handleMusicPriming(UserViewpoint viewpoint, List<Map<String, String>> tracks) throws RetryException {
		User user = viewpoint.getViewer();
		if (identitySpider.getMusicSharingPrimed(user)) {
			// at log .info, since it isn't really a problem, but if it happened a lot we'd 
			// want to investigate why
			logger.info("Ignoring priming data for music sharing, already primed");
			return;
		}
		// the tracks are in order from most to least highly-ranked, we want to 
		// timestamp the most highly-ranked one as most recent, so do this backward
		tracks = new ArrayList<Map<String,String>>(tracks);
		Collections.reverse(tracks);
		for (Map<String,String> properties : tracks) {
			musicSystem.addHistoricalTrack(user, properties, true);
		}
		// don't do this again
		identitySpider.setMusicSharingPrimed(user, true);
		logger.debug("Primed user with {} tracks", tracks.size());	
	}

	public void sendQueuedXmppMessages(String to, String from) {
		logger.debug("{} has now added us to their roster, sending queued messages from {}" , from , to);
		XmppResource fromResource;
		try {
			fromResource = identitySpider.lookupXmpp(from);
		} catch (NotFoundException e) {
			logger.debug("Couldn't find XmppResource for {}, ignoring", from);
			return; // Ignore
		}
		
		claimVerifier.sendQueuedXmppLinks(to, fromResource);
	}
}
