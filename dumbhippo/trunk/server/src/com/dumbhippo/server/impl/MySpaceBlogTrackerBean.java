package com.dumbhippo.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.EJB;
import javax.ejb.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MySpaceBlogTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.MySpaceScraper;

@Stateless
public class MySpaceBlogTrackerBean implements MySpaceBlogTracker {
	static private final Logger logger = GlobalSetup.getLogger(MySpaceBlogTrackerBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider identitySpider;

	private ExecutorService threadPool;
	
	@PostConstruct
	public void init() {
		threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("MySpaceBlogTracker");
				return t;
			}
		});
	}
	
	private static final String LOOKUP_COMMENT_BY_ID_QUERY =
		"SELECT cm FROM MySpaceBlogComment cm WHERE cm.owner = :owner and cm.commentId = :commentId";
	
	private MySpaceBlogComment lookupComment(User user, long commentId) {
		try {
			return (MySpaceBlogComment) em.createQuery(LOOKUP_COMMENT_BY_ID_QUERY)
			.setParameter("owner", user)
			.setParameter("commentId", commentId)
			.getSingleResult();
		} catch (EntityNotFoundException e) {
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
	
	public void setFriendId(Account acct, String friendId) {
		try {
			acct = identitySpider.lookupGuid(Account.class, acct.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		acct.setMySpaceFriendId(friendId);
	}

	public void updateFriendId(User user) {
		final Account acct = user.getAccount();
		assert(acct != null);		
		threadPool.execute(new Runnable() {
			public void run() {
				MySpaceBlogTracker tracker = EJBUtil.defaultLookup(MySpaceBlogTracker.class);
				String name = acct.getMySpaceName();
				assert(name != null);
				try {
					String friendId = MySpaceScraper.getFriendId(name);
					tracker.setFriendId(acct, friendId);
				} catch (IOException e) {
					logger.error("Failed to retrieve MySpace friend ID", e);
				}
			} 
		});
	}

}
