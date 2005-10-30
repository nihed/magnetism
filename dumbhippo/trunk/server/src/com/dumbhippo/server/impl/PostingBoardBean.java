package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Stateless
public class PostingBoardBean implements PostingBoard {

	private static final Log logger = GlobalSetup.getLog(PostingBoardBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;	
	
	@EJB
	private MessageSender messageSender;
		
	public Post createURLPost(Person poster, String title, String text, String url, Set<String> recipientGuids, PostVisibility visibility) throws ParseException, GuidNotFoundException {
		Set<Resource> shared = (Collections.singleton((Resource) identitySpider.getLink(url)));
		
		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(GuidPersistable.class, recipientGuids);

		// for each recipient, if it's a group we want to explode it into persons
		// (but also keep the group itself), if it's a person we just add it
		
		Set<Person> personRecipients = new HashSet<Person>();
		Set<Group> groupRecipients = new HashSet<Group>();
		Set<Person> expandedRecipients = new HashSet<Person>();
		
		// sort into persons and groups
		for (GuidPersistable r : recipients) {
			if (r instanceof Person) {
				personRecipients.add((Person) r);
			} else if (r instanceof Group) {
				groupRecipients.add((Group) r);
			} else {
				// wtf
				throw new GuidNotFoundException(r.getId());
			}
		}
		
		// build expanded recipients
		expandedRecipients.addAll(personRecipients);
		for (Group g : groupRecipients) {
			Set<Person> members = g.getMembers();
			expandedRecipients.addAll(members);
		}
		
		// if this throws we shouldn't send out notifications, so do it first
		Post post = createPost(poster, visibility, title, text, shared, personRecipients, groupRecipients, expandedRecipients);
		
		// FIXME I suspect this should be outside the transaction and asynchronous
		logger.debug("Sending out jabber/email notifications... (to Person only)");
		for (Person r : expandedRecipients) {
			messageSender.sendPostNotification(r, post);
		}
		return post;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Post createPost(Person poster, PostVisibility visibility, String title, String text, Set<Resource> resources, Set<Person> personRecipients, Set<Group> groupRecipients, Set<Person> expandedRecipients) {
		
		logger.debug("saving new Post");
		Post post = new Post(poster, visibility, title, text, personRecipients, groupRecipients, expandedRecipients, resources);
		em.persist(post);
	
		return post;
	}

	public List<Post> getPostsFor(Person poster, int max) {
		Query q;
		q = em.createQuery("select p from Post p, LinkResource link where p.poster = :personid and link.id in elements(p.resources) order by p.postDate desc");
		q.setParameter("personid", poster);
		if (max > 0)
			q.setMaxResults(max);

		@SuppressWarnings("unchecked")		
		List<Post> ret = q.getResultList();	
		return ret;
	}

	public List<PostInfo> getPostInfosFor(Person poster, Person viewer, int max) {
		List<PostInfo> result = new ArrayList<PostInfo>();
		
		List<Post> posts = getPostsFor(poster, max);
		for (Post p : posts) {
			PersonPostData ppd = getPersonPostData(viewer, p);				
			result.add(new PostInfo(identitySpider, viewer, p, ppd));
		}
		
		return result;
	}
	
	public List<PostInfo> getReceivedPostInfos(Person recipient, int max) {
		Query q;
		q = em.createQuery("select p from Post p where :recipient in elements(p.expandedRecipients) order by p.postDate desc");
		q.setParameter("recipient", recipient);
		if (max > 0) 
			q.setMaxResults(max);

		@SuppressWarnings("unchecked")		
		List<Post> posts = q.getResultList();
		
		List<PostInfo> results = new ArrayList<PostInfo>();
		for (Post p : posts) {
			PersonPostData ppd = getPersonPostData(recipient, p);			
			results.add(new PostInfo(identitySpider, recipient, p, ppd));
		}
		
		return results;
	}
	
	private PersonPostData getPersonPostData(Person viewer, Post post) {
		if (viewer == null)
			return null;
		
		Query q = em.createQuery("select ppd from PersonPostData ppd where ppd.post = :post and ppd.person = :viewer");
		q.setParameter("post", post);
		q.setParameter("viewer", viewer);
		try {
			return (PersonPostData) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	public List<PostInfo> getGroupPostInfos(Group recipient, Person viewer, int max) {
		Query q;
		q = em.createQuery("select p from Post p where :recipient in elements(p.groupRecipients) order by p.postDate desc");
		q.setParameter("recipient", recipient);
		if (max > 0)
			q.setMaxResults(max);

		@SuppressWarnings("unchecked")		
		List<Post> posts = q.getResultList();
		
		List<PostInfo> results = new ArrayList<PostInfo>();
		for (Post p : posts) {
			PersonPostData ppd = getPersonPostData(viewer, p);
			results.add(new PostInfo(identitySpider, viewer, p, ppd));
		}
		
		return results;
	}
	
	public Post loadPost(Guid guid) {
		return em.find(Post.class, guid.toString());
	}

	public void postClickedBy(Post post, Person clicker) {
		logger.debug("Post " + post + " clicked by " + clicker);
		messageSender.sendPostClickedNotification(post, clicker);
		// FIXME should be unique...
		PersonPostData postData = new PersonPostData(clicker, post);
		em.persist(postData);
	}
}
