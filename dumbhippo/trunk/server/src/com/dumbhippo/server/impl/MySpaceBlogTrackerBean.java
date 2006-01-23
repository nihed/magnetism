package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MySpaceBlogTracker;

@Stateless
public class MySpaceBlogTrackerBean implements MySpaceBlogTracker {
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
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

}
