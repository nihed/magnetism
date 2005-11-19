package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Account;
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

	public Account createAccountFromResource(Resource res) {
		User user = new User();
		user.setNickname(res.getDerivedNickname());
		em.persist(user);
		spider.addVerifiedOwnershipClaim(user, res);
		Account account = new Account(user);
		em.persist(account);
		spider.addVerifiedOwnershipClaim(user, account);
		return account;
	}

	public Account createAccountFromEmail(String email) {
		Resource res = spider.getEmail(email);
		return createAccountFromResource(res);
	}

	public Client authorizeNewClient(Account acct, String name) {
		Client c = new Client(acct, name);
		em.persist(c);
		acct.authorizeNewClient(c);
		return c;
	}
	
	public boolean checkClientCookie(Person user, String authKey) {
		Account account = lookupAccountByPerson(user);
		return account.checkClientCookie(authKey);
	}

	public Account lookupAccountByPerson(Person person) {
		Account ret;
		try {
			ret = (Account) em.createQuery("from Account a where a.owner = :person").setParameter("person", person).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public Account lookupAccountByPersonId(String personId) {
		Account ret;
		try {
			ret = (Account) em.createQuery("from Account a where a.owner.id = :id").setParameter("id", personId).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public long getNumberOfActiveAccounts() {
		long count = (Long) em.createQuery("SELECT SIZE(*) FROM Account a").getSingleResult();
		return count;
	}

	public Set<Account> getActiveAccounts() {
		Query q = em.createQuery("FROM Account");
		
		Set<Account> accounts = new HashSet<Account>();
		List list = q.getResultList();
		
		for (Object o : list) {
			accounts.add((Account) o);
		}
		
		return accounts;
	}
}
