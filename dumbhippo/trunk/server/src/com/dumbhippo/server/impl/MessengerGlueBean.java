package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.PersonView;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	@EJB
	private transient IdentitySpider identitySpider;
	
	private HippoAccount accountFromUsername(String username) throws JabberUserNotFoundException {
		Guid guid;
		try {
			guid = new Guid(username);
		} catch (IllegalArgumentException e) {
			throw new JabberUserNotFoundException("username was not a valid GUID", e);
		}
		
		// note that this person isn't persisted, it's just a temporary token
		Person person = new Person(guid);
		
		HippoAccount account = identitySpider.lookupAccountByPerson(person);
		if (account == null)
			throw new JabberUserNotFoundException();
		
		assert account.getOwner().getId().equals(username);
		
		return account;
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		HippoAccount account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			// FIXME temporary hack
			return "admin".equals(username);
		}

		assert account != null;
		
		return true;
	}
	

	public long getJabberUserCount() {
		return identitySpider.getNumberOfActiveAccounts();
	}


	public void setName(String username, String name)
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


	public void setEmail(String username, String email) 
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public JabberUser loadUser(String username) throws JabberUserNotFoundException {
		
		HippoAccount account = accountFromUsername(username);
		
		PersonView view = identitySpider.getSystemViewpoint(account.getOwner());
		
		JabberUser user = new JabberUser(username, account.getOwner().getName().getFullName(), view.getEmail().getEmail());
	
		return user;
	}
}
