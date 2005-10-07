package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;

@Stateless
public class AccountSystemBean implements AccountSystem {
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	@EJB
	private transient IdentitySpider spider;

	public HippoAccount createAccountFromResource(Resource res) {
		Client initialClient = new Client();
		em.persist(initialClient);
		Person person = new Person();
		spider.addVerifiedOwnershipClaim(person, res);
		HippoAccount account = new HippoAccount("unknown", initialClient);
		em.persist(account);
		return account;
	}
}
