package com.dumbhippo.server.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AbstractLoginRequired;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.ShareLinkGlue;
import com.dumbhippo.server.UnknownPersonException;

@Stateful
public class ShareLinkGlueBean extends AbstractLoginRequired implements ShareLinkGlue, Serializable {

	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	@EJB
	private transient IdentitySpider identitySpider;
	
	@EJB
	private transient MessageSender xmpp;
	
	private transient Person cachedLoggedInUser;
	
	private Person getLoggedInUser() {
		if (cachedLoggedInUser == null && getLoggedInUserId() != null) {
			identitySpider.lookupPersonById(getLoggedInUserId());
		}	
		
		if (cachedLoggedInUser == null)
			throw new IllegalStateException("Trying to use ShareLinkGlueBean when not logged in");
		
		return cachedLoggedInUser; 
	}
	
	public List<String> freeformRecipientsToIds(List<String> userEnteredRecipients) throws UnknownPersonException {
		Person person = getLoggedInUser(); // double-checks that we're logged in as side effect
		
		// FIXME for now we can only parse email addresses
		List<String> recipients = new ArrayList<String>();
		PersonView personView = identitySpider.getViewpoint(null, person);
		for (String freeform : userEnteredRecipients) {
			Person recipient = identitySpider.lookupPersonByEmail(identitySpider.getEmail(freeform));
			if (recipient == null)
				throw new UnknownPersonException("Person '" + freeform + "' not known to " + personView.getHumanReadableName(), freeform);
			recipients.add(recipient.getId());
		}
		return recipients;
	}

	public void shareLink(String url, List<String> recipientIds, String description) {
		Person poster = getLoggedInUser(); // double-checks that we're logged in as side effect
		
		Set<Person> recipients = new HashSet<Person>();
		Set<Resource> shared = Collections.singleton((Resource)identitySpider.getLink(url));
		
		Post post = new Post(poster, null, description, recipients, shared);
		em.persist(post);
		
		for (Person r : recipients) {
			xmpp.sendShareLink(r.getId() + "@dumbhippo.com", url, description);
		}
	}

}
