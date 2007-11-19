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
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.NotFoundException;

/** 
 * ContactDMO is not quite a straight wrapper of a Contact in the database, because 
 * we go ahead and merge the info from the User a contact refers to. This is done 
 * on the server side because if we had a web view of contacts, we'd want it to 
 * be consistent with the client.
 * 
 * There are several downsides to this approach, though; the change notification 
 * is busted right now, i.e. changing the User won't update the Contact. In addition 
 * this approach means sending some info to the client twice, once in the User object
 * and once as part of the Contact.
 *
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
		String nick = contact.getNickname();
		if (nick == null) {
			UserDMO user = getUser();
			if (user != null)
				return user.getName();
			else
				return null;
		} else {
			return nick;
		}
	}
	
	private UserDMO getUserDMOFromAccount(Account account) {
		return session.findUnchecked(UserDMO.class, account.getOwner().getGuid());
	}
	
	@DMProperty(defaultInclude=true)
	public UserDMO getOwner() {
		return getUserDMOFromAccount(contact.getAccount());
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
		
		// merge in any email from the User if we can see them
		UserDMO user = getUser();
		if (user != null) {
			// user.getEmails() should be filtered to our viewpoint already
			results.addAll(user.getEmails());
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
		
		// merge in any screen names from the User if we can see them
		UserDMO user = getUser();
		if (user != null) {
			// user.getAims() should be filtered to our viewpoint already
			results.addAll(user.getAims());
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
		
		// merge in any xmpps from the User if we can see them
		UserDMO user = getUser();
		if (user != null) {
			// user.getXmpps() should be filtered to our viewpoint already
			results.addAll(user.getXmpps());
		}
		
		return results;
	}		
	
	@DMProperty(defaultInclude=true)
	public UserDMO getUser() {
		for (ContactClaim cc : contact.getResources()) {
			Resource r = cc.getResource();
			if (r instanceof Account)
				return getUserDMOFromAccount((Account)r);
		}
		
		return null;
	}
}
