/**
 * 
 */
package com.dumbhippo.server.impl;

import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TestGlue;
import com.dumbhippo.server.TestGlueRemote;

/**
 * @author hp
 * 
 */
@Stateless
public class TestGlueBean implements TestGlue, TestGlueRemote {

	static private final Log logger = GlobalSetup.getLog(TestGlueBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;

	@EJB
	private transient IdentitySpider identitySpider;

	@EJB
	private transient AccountSystem accountSystem;

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
			
			EmailResource resource = identitySpider.getEmail(e);
			if (resource == null) {
				System.err.println("EmailResource is null");
				throw new Error("null resource");
			}

			HippoAccount account = accountSystem.createAccountFromResource(resource);
			if (account == null) {
				System.err.println("HippoAccount is null");
				throw new Error("null account");
			}
			
			account.getOwner().setName(FullName.parseHumanString(n));
		}
		
		
	}

	public Set<HippoAccount> getActiveAccounts() {
		// the returned data here includes all the auth cookies...
		// so you probably shouldn't do this outside of test glue
		logger.debug("getting active accounts spider = " + identitySpider);
		return accountSystem.getActiveAccounts();
	}
	
	public String authorizeNewClient(long accountId, String name) {
		logger.debug("authorizing new client for account " + accountId + " name = " + name);
		// Replace account with one attached to persistence context
		HippoAccount persistedAccount = em.find(HippoAccount.class, accountId);
		logger.debug("persistedAccount = " + persistedAccount);
		Client client = accountSystem.authorizeNewClient(persistedAccount, name);
		logger.debug("added client authKey = " + client.getAuthKey() + " client works = " + persistedAccount.checkClientCookie(client.getAuthKey()));
		return client.getAuthKey();
	}

	public HippoAccount createAccountFromEmail(String email) {
		return accountSystem.createAccountFromEmail(email);
	}
}
