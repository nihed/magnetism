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
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
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
	
	public LoginToken getLoginToken(Resource resource) throws HumanVisibleException {
		
		if (resource == null)
			throw new IllegalArgumentException("null resource");

		User user = spider.lookupUserByResource(resource);
		if (user == null) {
			throw new HumanVisibleException("That address hasn't been added to any account");
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
	
	public Pair<Client,Person> signIn(LoginToken token, String clientName) throws HumanVisibleException {
		
		if (token.isExpired())
			throw new HumanVisibleException("The link you followed has expired; you'll need to start over.");
		
		Resource resource = token.getResource();
		User user = spider.lookupUserByResource(resource);
		Account account;
		
		if (user != null)
			account = accounts.lookupAccountByUser(user);
		else
			account = null;
		
		if (account == null)
			throw new HumanVisibleException("We don't have an account associated with '" + resource.getHumanReadableString() + "'");
		
		Client client = accounts.authorizeNewClient(account, clientName);
		
		return new Pair<Client,Person>(client, user);
	}
}
