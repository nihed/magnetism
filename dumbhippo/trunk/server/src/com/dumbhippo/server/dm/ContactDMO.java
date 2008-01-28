package com.dumbhippo.server.dm;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.NotFoundException;

/** 
 * Currently ContactDMO is pretty much a straight wrapper of Contact and doesn't
 * try to pull in information from the user, if the Contact can be resolved to
 * a user. This has two advantages: first, it facilitates creating editing
 * interfaces for a Contact since the editing interface can distinguish editable
 * information from not-editable information. It also simplifies our work in
 * creating the appropriate notifications and cache invalidations.
 * 
 * The disadvantage is that every user who wants to display a contact with
 * information merged in has to do the merge themselves. If we didn't mind
 * doing the notification/invalidation work, we could duplicate properties
 * with both merged and unmerged versions.
 */
@DMO(classId="http://online.gnome.org/p/o/contact", resourceBase="/o/contact")
@DMFilter("viewer.canSeeContact(this)")
public abstract class ContactDMO extends DMObject<Guid> {
	
	private Contact contact;
	
	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;

	protected ContactDMO(Guid key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		contact = em.find(Contact.class, getKey().toString());
		if (contact == null)
			throw new NotFoundException("No such contact " + getKey().toString());
	}

	@DMProperty(defaultInclude=true)
	public String getName() {
		return contact.getNickname();
	}
	
	@DMProperty(defaultInclude=true)
	public UserDMO getOwner() {
		return session.findUnchecked(UserDMO.class, contact.getAccount().getOwner().getGuid());
	}
	
	@DMProperty(defaultInclude=true)
	public int getStatus() {
		return contact.getStatus().ordinal();
	}
	
	@DMProperty(defaultInclude=true)
	public Set<String> getEmails() {
		Set<String> results = new HashSet<String>();
		
		for (ContactClaim cc : contact.getResources()) {
			Resource r = cc.getResource();
			if (r instanceof EmailResource)
				results.add(((EmailResource)r).getEmail());
		}
		
		return results;
	}

	@DMProperty(defaultInclude=true)
	public Set<String> getAims() {
		Set<String> results = new HashSet<String>();
		
		for (ContactClaim cc : contact.getResources()) {
			Resource r = cc.getResource();
			if (r instanceof AimResource)
				results.add(((AimResource)r).getScreenName());
		}
		
		return results;
	}	
	
	@DMProperty(defaultInclude=true)
	public Set<String> getXmpps() {
		Set<String> results = new HashSet<String>();
		
		for (ContactClaim cc : contact.getResources()) {
			Resource r = cc.getResource();
			if (r instanceof XmppResource)
				results.add(((XmppResource)r).getJid());
		}
		
		return results;
	}		
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public UserDMO getUser() {
		User bestUser = null;
		
		for (ContactClaim cc : contact.getResources()) {
			Resource r = cc.getResource();
			AccountClaim ac = r.getAccountClaim();
			
			if (ac != null) {
				// Always prefer claims directly on Account resources
				if (r instanceof Account || bestUser == null);
					bestUser = ac.getOwner();
			}
		}
		
		if (bestUser != null)
			return session.findUnchecked(UserDMO.class, bestUser.getGuid());
		else
			return null;
	}
}
