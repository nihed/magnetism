package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.LoginVerifierException;

@Stateless
public class LoginVerifierBean implements LoginVerifier {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;
	
	public String getAuthKey(Resource resource) {
		// TODO Auto-generated method stub
		return null;
	}

	public Person verify(String authKey) throws LoginVerifierException {
		// TODO Auto-generated method stub
		return null;
	}
}
