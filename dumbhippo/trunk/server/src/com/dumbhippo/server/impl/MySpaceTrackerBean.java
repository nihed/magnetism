package com.dumbhippo.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.services.MySpaceScraper;

@Stateless
public class MySpaceTrackerBean implements MySpaceTracker {
	static private final Logger logger = GlobalSetup.getLogger(MySpaceTrackerBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private MessageSender messageSender;

	@EJB
	private ExternalAccountSystem externalAccounts;
	
	private ExecutorService threadPool;
	
	@PostConstruct
	public void init() {
		threadPool = ThreadUtils.newCachedThreadPool("MySpaceBlogTracker");
	}
	
	private static final String LOOKUP_COMMENT_BY_ID_QUERY =
		"SELECT cm FROM MySpaceBlogComment cm WHERE cm.owner = :owner and cm.commentId = :commentId";
	
	private MySpaceBlogComment lookupComment(User user, long commentId) {
		try {
			return (MySpaceBlogComment) em.createQuery(LOOKUP_COMMENT_BY_ID_QUERY)
			.setParameter("owner", user)
			.setParameter("commentId", commentId)
			.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}	
	
	public void addMySpaceBlogComment(User user, long commentId, long posterId) {
		MySpaceBlogComment cm = lookupComment(user, commentId);
		if (cm != null)
			return;
		cm = new MySpaceBlogComment(user, commentId, posterId);
		em.persist(cm);
	}

	private static final String LOOKUP_COMMENTS_QUERY =
		"SELECT cm FROM MySpaceBlogComment cm WHERE cm.owner = :owner ORDER BY cm.discoveredDate DESC";
	
	public List<MySpaceBlogComment> getRecentComments(User user) {			
		List<MySpaceBlogComment> comments = new ArrayList<MySpaceBlogComment>();
		List results = em.createQuery(LOOKUP_COMMENTS_QUERY).setParameter("owner", user).getResultList();		
		for (Object o : results) {
			comments.add((MySpaceBlogComment) o);
		}
		return comments;	
	}
	
	public void setFriendId(User user, String friendId) {
		ExternalAccount external;
		try {
			external = externalAccounts.lookupExternalAccount(new UserViewpoint(user), user, ExternalAccountType.MYSPACE);
			external.setExtra(friendId);
		} catch (NotFoundException e) {
			// nothing to do here really... can legitimately happen in some race situations
		}
	}

	public void updateFriendId(final User user) {
		final String mySpaceName;
		try {
			mySpaceName = externalAccounts.getMySpaceName(SystemViewpoint.getInstance(), user);
		} catch (NotFoundException e) {
			setFriendId(user, null);
			return;
		}
		
		threadPool.execute(new Runnable() {
			public void run() {
				MySpaceTracker tracker = EJBUtil.defaultLookup(MySpaceTracker.class);
				try {
					String friendId = MySpaceScraper.getFriendId(mySpaceName);
					tracker.setFriendId(user, friendId);
				} catch (IOException e) {
					logger.warn("Failed to retrieve MySpace friend ID", e);
				}
			} 
		});
	}

	public void notifyNewContactComment(UserViewpoint viewpoint, String mySpaceContactName) {
		Set<User> mySpaceNameOwners = identitySpider.getUserContactsWithMySpaceName(viewpoint, mySpaceContactName);
		for (User u : mySpaceNameOwners) {
			messageSender.sendMySpaceContactCommentNotification(u);
		}
	}
}
