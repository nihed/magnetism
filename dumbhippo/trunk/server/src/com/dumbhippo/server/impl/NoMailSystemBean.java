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
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.NoMail;
import com.dumbhippo.persistence.ToggleNoMailToken;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.TransactionRunner;

@Stateless
public class NoMailSystemBean implements NoMailSystem {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(NoMailSystem.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private Configuration config;
	
	public boolean getMailEnabled(EmailResource email) {
		Query q;
		
		q = em.createQuery("from NoMail n where n.email = :email");
		q.setParameter("email", email);
		try {
			NoMail noMail = (NoMail) q.getSingleResult();
			return noMail.getMailEnabled();
		} catch (EntityNotFoundException e) {
			return true; // enabled if not disabled
		}
	}
	
	public void processAction(final EmailResource email, Action action) {
		NoMail noMail;
		try {
			noMail = runner.runTaskThrowingConstraintViolation(new Callable<NoMail>() {

				public NoMail call() {
					Query q;
					
					q = em.createQuery("from NoMail t where t.email = :email");
					q.setParameter("email", email);
					
					NoMail result;
					try {
						result = (NoMail) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						result = new NoMail(email);
						em.persist(result);
					}
					
					return result;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return; // not reached
		}
		
		if (action == Action.TOGGLE_MAIL)
			noMail.setMailEnabled(!noMail.getMailEnabled());
		else if (action == Action.NO_MAIL_PLEASE)
			noMail.setMailEnabled(false);
		else
			noMail.setMailEnabled(true);
	}


	public String getNoMailUrl(String address, Action action) throws ValidationException {
		EmailResource email = identitySpider.getEmail(address);
		return getNoMailUrl(email, action);
	}
	
	public String getNoMailUrl(final EmailResource email, Action action) {
		ToggleNoMailToken token;
		try {
			token = runner.runTaskThrowingConstraintViolation(new Callable<ToggleNoMailToken>() {

				public ToggleNoMailToken call() {
					Query q;
					
					q = em.createQuery("from ToggleNoMailToken t where t.email = :email");
					q.setParameter("email", email);
					
					ToggleNoMailToken token;
					try {
						token = (ToggleNoMailToken) q.getSingleResult();
						if (token.isExpired()) {
							em.remove(token);
							em.flush(); // Sync to database before creating a new token
							throw new EntityNotFoundException("found expired nomail token, making a new one");
						}
					} catch (EntityNotFoundException e) {
						token = new ToggleNoMailToken(email);
						em.persist(token);
					}
					
					return token;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
		return getNoMailUrl(token, action);
	}

	public String getNoMailUrl(ToggleNoMailToken token, Action action) {
		String url = token.getAuthURL(config.getPropertyFatalIfUnset(HippoProperty.BASEURL));
		if (action == Action.NO_MAIL_PLEASE)
			return url + "&action=disable";
		else if (action == Action.WANTS_MAIL)
			return url + "&action=enable";
		else
			return url;
	}
}
