package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.LoginVerifierException;

@Stateless
public class LoginVerifierBean implements LoginVerifier {
	
	@EJB
	private IdentitySpider spider;
	
	@EJB
	private AccountSystem accounts;
	
	public String getAuthKey(Resource resource) {
		// TODO Auto-generated method stub
		return null;
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
