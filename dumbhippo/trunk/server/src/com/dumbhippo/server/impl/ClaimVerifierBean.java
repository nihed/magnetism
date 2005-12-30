package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class ClaimVerifierBean implements ClaimVerifier {

	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(ClaimVerifier.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private TransactionRunner runner;
	
	public String getAuthKey(final User user, final Resource resource) {
		
		if (user == null && resource == null)
			throw new IllegalArgumentException("one of user/resource has to be non-null");
		
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<String>() {
				
				public String call() {
					Query q;
					
					q = em.createQuery("from ResourceClaimToken t where t.user = :user and t.resource = :resource");
					q.setParameter("user", user);
					q.setParameter("resource", resource);
					
					ResourceClaimToken token;
					try {
						token = (ResourceClaimToken) q.getSingleResult();
						if (token.isExpired()) {
							em.remove(token);
							throw new EntityNotFoundException("found expired token, making a new one");
						}
					} catch (EntityNotFoundException e) {
						token = new ResourceClaimToken(user, resource);
						em.persist(token);
					}
					
					return token.getAuthKey();
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	public void verify(User user, ResourceClaimToken token, Resource resource) throws HumanVisibleException {
		if (user != null) {
			if (!user.equals(token.getUser())) {
				Viewpoint viewpoint = new Viewpoint(user);
				PersonView self = identitySpider.getPersonView(viewpoint, user);
				PersonView other = identitySpider.getPersonView(viewpoint, token.getUser());
				throw new HumanVisibleException("You are signed in as " + self.getName() 
						+ " but trying to change the account " + other.getName());
			}
		} else {
			user = token.getUser();
		}

		assert user != null;
		
		if (resource != null) {
			Resource tokenResource = token.getResource();
			if (tokenResource != null && !tokenResource.equals(resource)) {
				throw new HumanVisibleException(tokenResource.getHumanReadableString() + " should match " + resource.getHumanReadableString());
			}
		} else {
			resource = token.getResource();
		}
		
		if (resource == null) {
			// this should be a RuntimeException I guess since for a given resource type, we should guarantee
			// that we either have a trusted sender or that we recorded the resource prior to sending
			throw new HumanVisibleException("Something went wrong; could not figure out what address you are trying to add");
		}
		
		identitySpider.addVerifiedOwnershipClaim(user, resource);
	}
}
