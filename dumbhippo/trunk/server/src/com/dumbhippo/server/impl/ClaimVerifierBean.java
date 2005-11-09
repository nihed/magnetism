package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.ClaimVerifierException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class ClaimVerifierBean implements ClaimVerifier {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	public String getAuthKey(Person person, Resource resource) {
		// TODO Auto-generated method stub
		return null;
	}

	public void verify(Person user, ResourceClaimToken token, Resource resource) throws ClaimVerifierException {
		if (user != null) {
			if (!user.equals(token.getPerson())) {
				Viewpoint viewpoint = new Viewpoint(user);
				PersonView self = identitySpider.getPersonView(viewpoint, user);
				PersonView other = identitySpider.getPersonView(viewpoint, token.getPerson());
				throw new ClaimVerifierException("You are signed in as " + self.getHumanReadableName() 
						+ " but trying to change the account " + other.getHumanReadableName());
			}
		} else {
			user = token.getPerson();
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
