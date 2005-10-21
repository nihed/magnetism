package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PostingBoard;

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
	
	private Set<Person> guidSetToPerson(Set<Guid> guids) {
		Set<Person> recipients = new HashSet<Person>(guids.size());	
		for (Guid personId : guids) {
			Person r = identitySpider.lookupPersonById(personId);
			if (r != null) {
				recipients.add(r);
			} else {
				throw new IllegalArgumentException("Person " + personId + " is not known");
			}
		}
		return recipients;
	}
	
	public Post createURLPost(Person poster, String title, String text, String url, Set<String> recipientGuids) {
		Set<Resource> shared = (Collections.singleton((Resource) identitySpider.getLink(url)));
		Set<Person> recipients = guidSetToPerson(Guid.parseStrings(recipientGuids));
		// if this throws we shouldn't send out notifications
		Post post = createPost(poster, title, text, shared, recipients);
		
		logger.debug("Sending out jabber notifications...");
		for (Person r : recipients) {
			messageSender.sendShareLink(r, url, text);
		}
		return post;
	}	
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Post createPost(Person poster, String title, String text, Set<Resource> resources, Set<Person> recipients) {
		
		logger.debug("saving new Post");
		Post post = new Post(poster, title, text, recipients, resources);
		em.persist(post);
	
		return null;
	}

	public Set<Post> getPostedURLsFor(Person poster) {
		// TODO Auto-generated method stub
		return null;
	}

}
