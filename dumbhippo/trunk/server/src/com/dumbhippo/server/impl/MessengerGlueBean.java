package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.PersonView;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	private transient IdentitySpider identitySpider;
	
	/** 
	 * Used by the app server to provide us with an IdentitySpider
	 * @param identitySpider the spider
	 */
	@EJB
	protected void setIdentitySpider(IdentitySpider identitySpider) {
		this.identitySpider = identitySpider;
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		
		// TODO do some stuff here and maybe return true
		
		return false;
	}
	

	public long getJabberUserCount() {
		// TODO Auto-generated method stub
		return 0;
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
		
		HippoAccount account = identitySpider.lookupAccountByUsername(username);
		if (account == null)
			throw new JabberUserNotFoundException();
		
		assert account.getUsername().equals(username);
		
		PersonView view = identitySpider.getSystemViewpoint(account.getOwner());
		
		JabberUser user = new JabberUser(username, account.getOwner().getName().getFullName(), view.getEmail().getEmail());
	
		return user;
	}
}
