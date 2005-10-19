package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AbstractLoginRequired;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlueHttp;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PersonView;

@Stateful
public class AjaxGlueHttpBean extends AbstractLoginRequired implements AjaxGlueHttp, Serializable {
	
	private static final Log logger = GlobalSetup.getLog(AjaxGlueHttpBean.class);
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private MessageSender messageSender;
		
	/* (non-Javadoc)
	 * @see com.dumbhippo.server.AjaxGlueHttp#getFriendCompletions(java.io.OutputStream, java.lang.String, java.lang.String)
	 */
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, String entryContents) throws IOException {

		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		XmlBuilder xml = new XmlBuilder();

		xml.appendStandaloneFragmentHeader();
		
		xml.append("<people>");
		
		if (entryContents != null) {
			Set<HippoAccount> accounts = accountSystem.getActiveAccounts();
			for (HippoAccount a : accounts) {
				// FIXME get from viewpoint of personId

				// it's important that empty string returns all completions, otherwise
				// the arrow on the combobox doesn't drop down anything when it's empty
				
				String completion = null;

				PersonView view = identitySpider.getSystemViewpoint(a.getOwner());
				String humanReadable = view.getHumanReadableName();
				EmailResource email = view.getEmail();
				if (humanReadable.startsWith(entryContents)) {
					completion = humanReadable;
				} else if (email.getEmail().startsWith(entryContents)) {
					completion = email.getEmail();
				} else if (a.getOwner().getId().startsWith(entryContents)) {
					completion = a.getOwner().getId();
				}

				if (completion != null) {
					xml.appendElement("person", null, "id", a.getOwner().getId(), "display", humanReadable, "completion", completion);
				}
			}
		}

		xml.append("</people>");
		
		out.write(xml.toString().getBytes());
	}
	
	public void doShareLink(String url, String recipientIds, String description) {
		Person poster = getLoggedInUser(identitySpider); // double-checks that we're logged in as side effect
		
		Set<Person> recipients = new HashSet<Person>();
		Set<Resource> shared = Collections.singleton((Resource)identitySpider.getLink(url));
		
		String[] splitIds = recipientIds.split(","); 
		
		for (String personId : splitIds) {
			Person r = identitySpider.lookupPersonById(personId);
			if (r != null) {
				recipients.add(r);
			} else {
				// should not happen really...
				logger.error("Recipient " + personId + " is not known, trimming from recipient list");
			}
		}
		
		logger.debug("saving new Post");
		Post post = new Post(poster, null, description, recipients, shared);
		em.persist(post);
		
		logger.debug("Sending out jabber notifications...");
		for (Person r : recipients) {
			messageSender.sendShareLink(r.getId() + "@dumbhippo.com", url, description);
		}
	}
}
