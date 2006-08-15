package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Token;
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
					} catch (NoResultException e) {
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

	public Client signIn(Token token, String clientName) throws HumanVisibleException {
		
		if (token.isExpired()) {
			logger.debug("Expired token when signing in: {}", token);
			throw new HumanVisibleException("The link you followed has expired; you'll need to request a new login link.");
		}
		
		Resource resource;
		if (token instanceof LoginToken) {
 		    resource = ((LoginToken)token).getResource();	
		} else if (token instanceof InvitationToken) {
			if (!((InvitationToken)token).isViewed()) {
				throw new RuntimeException("Tried to use an invitation token that was not previously viewed for signing in: " + token);				
			}
			resource = ((InvitationToken)token).getInvitee();
		} else {
			throw new RuntimeException("Unexpected token type for token " + token);
		}
		
		User user = spider.lookupUserByResource(SystemViewpoint.getInstance(), resource);
		Account account;
		
		if (user != null)
			account = accounts.lookupAccountByUser(user);
		else
			account = null;
		
		if (account == null) {
			logger.debug("No account for resource {}", resource.getHumanReadableString());
			throw new HumanVisibleException("We don't have an account associated with '" + resource.getHumanReadableString() + "'");
		}
		
		Client client = accounts.authorizeNewClient(account, clientName);

		logger.debug("Signin completed for client={} user={}", client, user);
		return client;				
	}

}
