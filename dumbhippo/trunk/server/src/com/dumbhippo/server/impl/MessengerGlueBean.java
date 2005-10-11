package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.PersonView;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	static Log logger = LogFactory.getLog(MessengerGlueBean.class);
	
	/*
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	*/
	
	@EJB
	private transient IdentitySpider identitySpider;
		
	@EJB
	private transient AccountSystem accountSystem;
	
	private HippoAccount accountFromUsername(String username) throws JabberUserNotFoundException {
		HippoAccount account = accountSystem.lookupAccountByPersonId(username);
		if (account == null)
			throw new JabberUserNotFoundException("username does not exist");
		
		assert account.getOwner().getId().equals(username);
		
		return account;
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		HippoAccount account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			return false;
		}
		
		assert account != null;
		
		return account.checkClientCookie(token, digest);
	}
	

	public long getJabberUserCount() {
		return accountSystem.getNumberOfActiveAccounts();
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
