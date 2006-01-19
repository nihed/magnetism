package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.MySpaceResource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MySpaceBlogTracker;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.TransactionRunner;

@Stateless
public class MySpaceBlogTrackerBean implements MySpaceBlogTracker {
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private IdentitySpider identitySpider;
	
	private static final String LOOKUP_COMMENT_BY_ID_QUERY =
		"SELECT cm FROM MySpaceBlogComment cm WHERE cm.blog = :blog and cm.commentId = :commentId";
	
	private MySpaceBlogComment lookupComment(MySpaceResource name, long commentId) {
		try {
			return (MySpaceBlogComment) em.createQuery(LOOKUP_COMMENT_BY_ID_QUERY)
			.setParameter("blog", name)
			.setParameter("commentId", commentId)
			.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}	
	
	public void addMySpaceBlogComment(User user, long commentId, long posterId) {
		PersonView pv = identitySpider.getSystemView(user, PersonViewExtra.MYSPACE_NAME);
		MySpaceResource myIdentity = pv.getMySpaceName();
		MySpaceBlogComment cm = lookupComment(myIdentity, commentId);
		if (cm != null)
			return;
		cm = new MySpaceBlogComment(myIdentity, commentId, posterId);
		em.persist(cm);
	}

	private static final String LOOKUP_COMMENTS_QUERY =
		"SELECT cm FROM MySpaceBlogComment cm WHERE cm.blog = :blog ORDER BY cm.discoveredDate DESC";
	
	public List<MySpaceBlogComment> getRecentComments(User user) {
		PersonView pv = identitySpider.getSystemView(user, PersonViewExtra.MYSPACE_NAME);
		MySpaceResource myIdentity = pv.getMySpaceName();				
		List<MySpaceBlogComment> comments = new ArrayList<MySpaceBlogComment>();
		List results = em.createQuery(LOOKUP_COMMENTS_QUERY).setParameter("blog", myIdentity).getResultList();		
		for (Object o : results) {
			comments.add((MySpaceBlogComment) o);
		}
		return comments;	
	}

}
