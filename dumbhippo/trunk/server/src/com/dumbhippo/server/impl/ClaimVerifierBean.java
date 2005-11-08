package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.ClaimVerifierException;
import com.dumbhippo.server.IdentitySpider;

@Stateless
public class ClaimVerifierBean implements ClaimVerifier {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;
	
	public String getAuthKey(Person person, Resource resource) {
		// TODO Auto-generated method stub
		return null;
	}

	public void verify(Person user, String authKey, Resource resource) throws ClaimVerifierException {
		// TODO Auto-generated method stub

	}

}
