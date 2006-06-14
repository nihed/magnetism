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
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.UnauthorizedException;
import com.dumbhippo.server.util.EJBUtil;

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
		// Important to add the account first here so other code
		// can just assume all users have accounts. Specifically
		// the GroupMember canonicalization code assumes this for example.
		spider.addVerifiedOwnershipClaim(user, account);		
		spider.addVerifiedOwnershipClaim(user, res);
		return account;
	}

	public Account createAccountFromEmail(String email) throws ValidationException {
		Resource res = spider.getEmail(email);
		return createAccountFromResource(res);
	}

	public Client authorizeNewClient(Account acct, String name) {
		Client c = new Client(acct, name);
		em.persist(c);
		acct.authorizeNewClient(c);
		return c;
	}
	
	public Account checkClientCookie(Guid guid, String authKey) throws NotFoundException, UnauthorizedException {
		Account account = lookupAccountByOwnerId(guid);
		if (!account.checkClientCookie(authKey))
			throw new UnauthorizedException("Invalid authorization cookie");
			
		return account;
	}
	
	public Client getExistingClient(Guid userId, long clientId) throws NotFoundException {
		Client client = em.find(Client.class, clientId);
		if (client == null)
			throw new NotFoundException("Client not found");
		if (!client.getAccount().getOwner().getGuid().equals(userId))
			throw new RuntimeException("Client doesn't match user");
		return client;
	}

	public Account lookupAccountByUser(User user) {
		if (!em.contains(user)) {
			try {
				user = EJBUtil.lookupGuid(em, User.class, user.getGuid());
			} catch (NotFoundException e) {
				throw new RuntimeException("Failed to look up user", e);
			}
		}
		
		return user.getAccount();
	}

	public Account lookupAccountByOwnerId(Guid ownerId) throws NotFoundException {
		User user = EJBUtil.lookupGuid(em, User.class, ownerId);
		return user.getAccount();
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
		try {
			Account acct = lookupAccountByOwnerId(userId);
			acct.setLastLoginDate(new Date());
		} catch (NotFoundException e) {
			throw new RuntimeException("User doesn't exist");
		}
	}
}
