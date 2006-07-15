package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;

@Stateless
public class LoginVerifierBean implements LoginVerifier {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LoginVerifier.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;
	
	@EJB
	private AccountSystem accounts;
	
	@EJB
	private TransactionRunner runner;
	
	public LoginToken getLoginToken(final Resource resource) throws HumanVisibleException {
		
		if (resource == null)
			throw new IllegalArgumentException("null resource");

		User user = spider.lookupUserByResource(SystemViewpoint.getInstance(), resource);
		if (user == null) {
			throw new HumanVisibleException("That address hasn't been added to any account");
		}

		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<LoginToken>() {

				public LoginToken call() {
					Query q;
					
					q = em.createQuery("from LoginToken t where t.resource = :resource");
					q.setParameter("resource", resource);
					
					LoginToken token;
					try {
						token = (LoginToken) q.getSingleResult();
						if (token.isExpired()) {
							em.remove(token);
							em.flush();
							throw new EntityNotFoundException("found expired token, making a new one");
						}
					} catch (EntityNotFoundException e) {
						token = new LoginToken(resource);
						em.persist(token);
					}
					
					return token;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	public Client signIn(LoginToken token, String clientName) throws HumanVisibleException {
		
		if (token.isExpired()) {
			logger.debug("Expired login token {}", token);
			throw new HumanVisibleException("The link you followed has expired; you'll need to start over.");
		}
		
		Resource resource = token.getResource();
		User user = spider.lookupUserByResource(SystemViewpoint.getInstance(), resource);
		Account account;
		
		if (user != null)
			account = accounts.lookupAccountByUser(user);
		else
			account = null;
		
		if (account == null) {
			logger.debug("No account for this login token {}", token);
			throw new HumanVisibleException("We don't have an account associated with '" + resource.getHumanReadableString() + "'");
		}
		
		Client client = accounts.authorizeNewClient(account, clientName);

		logger.debug("Signin completed for client={} user={}", client, user);
		return client;
	}
}
