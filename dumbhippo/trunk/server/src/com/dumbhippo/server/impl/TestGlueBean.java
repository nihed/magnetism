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
import org.apache.commons.logging.LogFactory;

import com.dumbhippo.FullName;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TestGlueRemote;

/**
 * @author hp
 * 
 */
@Stateless
public class TestGlueBean implements TestGlueRemote {

	static Log logger = LogFactory.getLog(TestGlueBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;

	@EJB
	private transient IdentitySpider identitySpider;

	@EJB
	private transient AccountSystem accountSystem;

	public TestGlueBean() {
		GlobalSetup.initialize();
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
		logger.info("getting active accounts spider = " + identitySpider);
		return identitySpider.getActiveAccounts();
	}

	public HippoAccount getAnAccount() {
		logger.info("getting a single account <--");
		
		Set<HippoAccount> accounts = getActiveAccounts();
		
		logger.info("here <--");
		
		HippoAccount copy = new HippoAccount(accounts.iterator().next());
		
		logger.info("copy = " + copy.toString() + "orig = " + accounts.iterator().next().toString());
		
		/* HippoAccount copy = new HippoAccount(new Person());
		logger.info("copy = " + copy.toString()); */
		
		return copy;
	}
}
