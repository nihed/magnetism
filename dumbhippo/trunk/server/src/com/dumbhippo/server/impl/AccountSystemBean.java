package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;

@Stateless
public class AccountSystemBean implements AccountSystem {
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;

	public HippoAccount createAccountFromResource(Resource res) {
		
		// FIXME check whether resource already exists!
		
		Person person = new Person();
		em.persist(person);
		spider.addVerifiedOwnershipClaim(person, res);
		HippoAccount account = new HippoAccount(person);
		em.persist(account);
		return account;
	}

	public HippoAccount createAccountFromEmail(String email) {
		Resource res = spider.getEmail(email);
		return createAccountFromResource(res);
	}

	public Client authorizeNewClient(HippoAccount acct, String name) {
		Client c = new Client(name);
		em.persist(c);
		acct.authorizeNewClient(c);
		return c;
	}
	

	public HippoAccount lookupAccountByPerson(Person person) {
		HippoAccount ret;
		try {
			ret = (HippoAccount) em.createQuery("from HippoAccount a where a.owner = :person").setParameter("person", person).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public HippoAccount lookupAccountByPersonId(String personId) {
		HippoAccount ret;
		try {
			ret = (HippoAccount) em.createQuery("from HippoAccount a where a.owner.id = :id").setParameter("id", personId).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public long getNumberOfActiveAccounts() {
		long count = (Long) em.createQuery("SELECT SIZE(*) FROM HippoAccount a").getSingleResult();
		return count;
	}

	@SuppressWarnings("unchecked")
	public Set<HippoAccount> getActiveAccounts() {
		Query q = em.createQuery("FROM HippoAccount");
		
		// List is not a Set
		Set<HippoAccount> accounts = new HashSet<HippoAccount>(q.getResultList());
		
		return accounts;
	}
}
