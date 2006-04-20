package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
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
		Account account = new Account(user);
		em.persist(user);
		em.persist(account);
		spider.addVerifiedOwnershipClaim(user, res);
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
	
	public boolean checkClientCookie(User user, String authKey) {
		Account account = lookupAccountByUser(user);
		return account.checkClientCookie(authKey);
	}

	public Account lookupAccountByUser(User user) {
		Account ret;
		try {
			ret = (Account) em.createQuery("from Account a where a.owner = :person").setParameter("person", user).getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new RuntimeException("User has no account!", e);
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
		try {
			Number num = (Number) em.createQuery("SELECT COUNT(a) FROM Account a").getSingleResult();
			return num.longValue();
		} catch (EntityNotFoundException e) {
			throw new RuntimeException("Failed to count number of accounts", e);
		}
	}

	public Set<Account> getActiveAccounts() {
		Query q = em.createQuery("FROM Account WHERE a.lastLoginTime ");
		
		Set<Account> accounts = new HashSet<Account>();
		List list = q.getResultList();
		
		for (Object o : list) {
			accounts.add((Account) o);
		}
		
		return accounts;
	}

	public List<Account> getRecentlyActiveAccounts() {		
		Query q = em.createQuery("FROM Account where (lastLoginDate - current_timestamp()) < :weekSecs")
			.setParameter("weekSecs",  7 * 24 * 60 * 60);
		return TypeUtils.castList(Account.class, q.getResultList());		
	}

	public void touchLoginDate(Guid userId) {
		Account acct = lookupAccountByPersonId(userId.toString());
		acct.setLastLoginDate(new Date());
	}
}
