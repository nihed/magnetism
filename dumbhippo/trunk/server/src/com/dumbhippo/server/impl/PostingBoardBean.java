package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@SuppressWarnings("serial")
@Stateless
public class PostingBoardBean implements PostingBoard {

	private static final Log logger = GlobalSetup.getLog(PostingBoardBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;	
	
	@EJB
	private MessageSender messageSender;
		
	public Post createURLPost(Person poster, String title, String text, String url, Set<String> recipientGuids) throws ParseException, GuidNotFoundException {
		Set<Resource> shared = (Collections.singleton((Resource) identitySpider.getLink(url)));
		
		// this is what can throw ParseException
		Set<Person> recipients = identitySpider.lookupGuidStrings(Person.class, recipientGuids);

		// if this throws we shouldn't send out notifications
		Post post = createPost(poster, title, text, shared, recipients);
		
		logger.debug("Sending out jabber notifications...");
		for (Person r : recipients) {
			messageSender.sendShareLink(r, post.getGuid(), url, text);
		}
		return post;
	}	
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Post createPost(Person poster, String title, String text, Set<Resource> resources, Set<Person> recipients) {
		
		logger.debug("saving new Post");
		Post post = new Post(poster, title, text, recipients, resources);
		em.persist(post);
	
		return post;
	}

	public List<Post> getPostsFor(Person poster, int max) {
		Query q;
		q = em.createQuery("select p from Post p, LinkResource link where p.poster = :personid and link.id in elements(p.resources) order by p.postDate");
		q.setParameter("personid", poster);
		if (max > 0) {
			q.setMaxResults(max);
		}
		@SuppressWarnings("unchecked")		
		List<Post> ret = q.getResultList();	
		return ret;
	}

	public List<PostInfo> getPostInfosFor(Person poster, Person viewer, int max) {
		List<PostInfo> result = new ArrayList<PostInfo>();
		
		List<Post> posts = getPostsFor(poster, max);
		for (Post p : posts) {
			result.add(new PostInfo(identitySpider, viewer, p));
		}
		
		return result;
	}
}
