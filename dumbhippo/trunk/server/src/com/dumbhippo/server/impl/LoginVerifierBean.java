package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.LoginVerifierException;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class LoginVerifierBean implements LoginVerifier {
	
	static private final Log logger = GlobalSetup.getLog(LoginVerifier.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;
	
	@EJB
	private AccountSystem accounts;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	public LoginToken getLoginToken(Resource resource) throws LoginVerifierException {
		
		if (resource == null)
			throw new IllegalArgumentException("null resource");

		User user = spider.lookupPersonByResource(resource);
		if (user == null) {
			throw new LoginVerifierException("That address hasn't been added to any account");
		}
		
		LoginVerifier proxy = (LoginVerifier) ejbContext.lookup(LoginVerifier.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateLoginToken(resource);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating LoginToken, retrying");
					retries--;
				} else {
					throw new RuntimeException("Unexpected error creating LoginToken", e);
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public LoginToken findOrCreateLoginToken(Resource resource) {
		Query q;
		
		q = em.createQuery("from LoginToken t where t.resource = :resource");
		q.setParameter("resource", resource);
		
		LoginToken token;
		try {
			token = (LoginToken) q.getSingleResult();
			if (token.isExpired()) {
				em.remove(token);
				throw new EntityNotFoundException("found expired token, making a new one");
			}
		} catch (EntityNotFoundException e) {
			token = new LoginToken(resource);
			em.persist(token);
		}
		
		return token;	
	}
	
	public Pair<Client,Person> signIn(LoginToken token, String clientName) throws LoginVerifierException {
		
		if (token.isExpired())
			throw new LoginVerifierException("The link you followed has expired; you'll need to start over.");
		
		Resource resource = token.getResource();
		Person person = spider.lookupPersonByResource(resource);
		Account account;
		
		if (person != null)
			account = accounts.lookupAccountByPerson(person);
		else
			account = null;
		
		if (account == null)
			throw new LoginVerifierException("We don't have an account associated with '" + resource.getHumanReadableString() + "'");
		
		Client client = accounts.authorizeNewClient(account, clientName);
		
		return new Pair<Client,Person>(client, person);
	}
}
