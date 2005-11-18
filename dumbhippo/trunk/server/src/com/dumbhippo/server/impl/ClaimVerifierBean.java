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
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.ClaimVerifierException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class ClaimVerifierBean implements ClaimVerifier {

	static private final Log logger = GlobalSetup.getLog(ClaimVerifier.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	public String getAuthKey(User user, Resource resource) {
		
		if (user == null && resource == null)
			throw new IllegalArgumentException("one of user/resource has to be non-null");
		
		ClaimVerifier proxy = (ClaimVerifier) ejbContext.lookup(ClaimVerifier.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateAuthKey(user, resource);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating ResourceClaimToken, retrying");
					retries--;
				} else {
					throw new RuntimeException("Unexpected error creating ResourceClaimToken", e);
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public String findOrCreateAuthKey(User user, Resource resource) {
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
	
	public void verify(User user, ResourceClaimToken token, Resource resource) throws ClaimVerifierException {
		if (user != null) {
			if (!user.equals(token.getUser())) {
				Viewpoint viewpoint = new Viewpoint(user);
				PersonView self = identitySpider.getPersonView(viewpoint, user);
				PersonView other = identitySpider.getPersonView(viewpoint, token.getUser());
				throw new ClaimVerifierException("You are signed in as " + self.getName() 
						+ " but trying to change the account " + other.getName());
			}
		} else {
			user = token.getUser();
		}

		assert user != null;
		
		if (resource != null) {
			Resource tokenResource = token.getResource();
			if (tokenResource != null && !tokenResource.equals(resource)) {
				throw new ClaimVerifierException(tokenResource.getHumanReadableString() + " should match " + resource.getHumanReadableString());
			}
		} else {
			resource = token.getResource();
		}
		
		if (resource == null) {
			// this should be a RuntimeException I guess since for a given resource type, we should guarantee
			// that we either have a trusted sender or that we recorded the resource prior to sending
			throw new ClaimVerifierException("Something went wrong; could not figure out what address you are trying to add");
		}
		
		identitySpider.addVerifiedOwnershipClaim(user, resource);
	}
}
