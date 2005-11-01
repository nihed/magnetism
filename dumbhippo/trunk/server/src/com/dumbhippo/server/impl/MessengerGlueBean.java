package com.dumbhippo.server.impl;

import java.util.Date;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.PersonView;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	static private final Log logger = GlobalSetup.getLog(MessengerGlueBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
		
	@EJB
	private AccountSystem accountSystem;

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

	public void serverStartup(long timestamp) {
		logger.debug("Jabber server startup at " + new Date(timestamp));
	}
	
	public void onUserAvailable(String username) {
		logger.debug("Jabber user " + username + " now available");
	}

	public void onUserUnavailable(String username) {
		logger.debug("Jabber user " + username + " now unavailable");
	}
}
