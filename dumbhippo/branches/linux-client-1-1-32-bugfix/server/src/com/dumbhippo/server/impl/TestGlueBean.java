/**
 * 
 */
package com.dumbhippo.server.impl;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TestGlue;
import com.dumbhippo.server.TestGlueRemote;

/**
 * @author hp
 */
@Stateless
public class TestGlueBean implements TestGlue, TestGlueRemote {

	static private final Logger logger = GlobalSetup.getLogger(TestGlueBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;

	@EJB
	private AccountSystem accountSystem;
	
	public TestGlueBean() {
	}
	 
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dumbhippo.server.TestGlueRemote#loadTestData()
	 */
	public void loadTestData() {

		logger.info("Loading test data");
		
		if (em == null)
			throw new Error("EntityManager was not injected");
		if (identitySpider == null)
			throw new Error("IdentitySpider was not injected");
		if (accountSystem == null)
			throw new Error("AccountSystem was not injected");
		
		// using example.com in case we start actually sending mail ;-)
		String[] emails = { "person1@1.example.com", "person2@2.example.com", "person3@3.example.net",
				"person4@4.example.info", "person5@5.example.com" };
		String[] fullNames = { "Person The First", "Person The Second", "Person The Third", "Person Fourth", "Fifth" };

		for (int i = 0; i < emails.length; i++) {
			String e = emails[i];
			String n = fullNames[i];
			
			EmailResource resource;
			try {
				resource = identitySpider.getEmail(e);
			} catch (ValidationException e1) {
				resource = null;
			}
			if (resource == null) {
				System.err.println("EmailResource is null");
				throw new Error("null resource");
			}

			Account account = accountSystem.createAccountFromResource(resource);
			if (account == null) {
				System.err.println("Account is null");
				throw new Error("null account");
			}
			
			account.getOwner().setNickname(n);
		}
		
		
	}

	public Set<Account> getActiveAccounts() {
		// the returned data here includes all the auth cookies...
		// so you probably shouldn't do this outside of test glue
		logger.debug("getting active accounts spider = " + identitySpider);
		return accountSystem.getActiveAccounts();
	}
	
	public String authorizeNewClient(String accountId, String name) {
		logger.debug("authorizing new client for account " + accountId + " name = " + name);
		// Replace account with one attached to persistence context
		Account persistedAccount = em.find(Account.class, accountId);
		logger.debug("persistedAccount = " + persistedAccount);
		Client client = accountSystem.authorizeNewClient(persistedAccount, name);
		logger.debug("added client authKey = " + client.getAuthKey() + " client works = " + persistedAccount.checkClientCookie(client.getAuthKey()));
		return client.getAuthKey();
	}

	public void setInvitations(String userId, int invites) throws ParseException, NotFoundException {
			User user = identitySpider.lookupGuidString(User.class, userId);
			Account acct = user.getAccount();
			acct.setInvitations(invites);
	}
}
