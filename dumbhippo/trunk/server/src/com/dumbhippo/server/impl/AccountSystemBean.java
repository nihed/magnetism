package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserClientInfo;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.UnauthorizedException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxCallable;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class AccountSystemBean implements AccountSystem {
	private static final int WEB_LOGIN_UPDATE_SEC = 60*60; // Throttle updates to avoid
	                                                       // extra database writes

	static private final Logger logger = GlobalSetup.getLogger(AccountSystem.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;
		
	@EJB
	private Notifier notifier;
	
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
		
		notifier.onUserCreated(user);
		
		return account;
	}

	public Client authorizeNewClient(Account acct, String name) {
		acct.prepareToAuthorizeClient();
		
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
		} catch (NoResultException e) {
			throw new RuntimeException("Failed to count number of accounts", e);
		}
	}

	public Set<Account> getActiveAccounts() {
		Query q = em.createQuery("SELECT a FROM Account a WHERE a.wasSentShareLinkTutorial = TRUE");
		
		Set<Account> accounts = new HashSet<Account>();
		List<?> list = q.getResultList();
		
		for (Object o : list) {
			accounts.add((Account) o);
		}
		
		return accounts;
	}

	public List<Account> getRecentlyActiveAccounts() {	
		Query q = em.createQuery("SELECT a FROM Account a WHERE a.lastLoginDate >= :weekAgo");
		q.setParameter("weekAgo",  new Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)));
		return TypeUtils.castList(Account.class, q.getResultList());
	}

	public User getCharacter(final Character whichOne) throws RetryException {
		return TxUtils.runNeedsRetry(new TxCallable<User>() {
			public User call() throws RetryException {
				EmailResource email;
				try {
					email = spider.getEmail(whichOne.getEmail());
				} catch (ValidationException e) {
					throw new RuntimeException("Character has invalid email address!");
				}
				User user = spider.getUser(email);
				if (user == null) {
					// don't add any special handling in here - it should be OK if 
					// someone just creates the character accounts manually without running
					// this code. We don't want to start doing "if (character) ; else ;" all
					// over the place.
					logger.info("Creating special user " + whichOne);
					Account account = createAccountFromResource(email);
					user = account.getOwner();
					user.setNickname(whichOne.getDefaultNickname());
				}
				return user;
			}
		});
	}
	
	public User lookupCharacter(Character whichOne) throws NotFoundException {
		EmailResource email = spider.lookupEmail(whichOne.getEmail());
		User user = spider.getUser(email);
		if (user == null)
			throw new NotFoundException("No user for character");
		
		return user;
	}
	
	public User getMugshotCharacter() {
		try {
			return getCharacter(Character.MUGSHOT);
		} catch (RetryException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, String> getPrefs(Account account) {
		Map<String,String> prefs = new HashMap<String, String>();
		// account.isMusicSharingEnabled() could return null, so we should use getMusicSharingEnabled()
		// method in identitySpider to get the right default
		prefs.put("musicSharingEnabled", Boolean.toString(spider.getMusicSharingEnabled(account.getOwner(),
																			Enabled.AND_ACCOUNT_IS_ACTIVE)));

		// not strictly a "pref" but this is a convenient place to send this to the client
		prefs.put("musicSharingPrimed", Boolean.toString(account.isMusicSharingPrimed()));
		
		prefs.put("applicationUsageEnabled", Boolean.toString(spider.getApplicationUsageEnabled(account.getOwner())));
		
		return prefs;
	}

	public void updateWebActivity(User user) {
		Date current = new Date();
		try {
			user = spider.lookupGuid(User.class, user.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Account account = user.getAccount();
		if (account.getLastWebActivityDate() == null || current.getTime() - account.getLastWebActivityDate().getTime() > (WEB_LOGIN_UPDATE_SEC*1000)) {
			account.setLastWebActivityDate(current);	
		}
	}

	public void updateClientInfo(final UserViewpoint viewpoint, final String platform, String distribution, String version, String architecture) throws RetryException {
		// NULL doesn't work well as a unique key, so use "" when no 
		// distribution/version/architecture is specified
		final String dbDistribution = distribution != null ? distribution : ""; 
		final String dbVersion = version != null ? version : ""; 
		final String dbArchitecture = architecture != null ? architecture : ""; 

		TxUtils.runNeedsRetry(new TxRunnable() {
			public void run() {
				Query q = em.createQuery("SELECT uci FROM UserClientInfo uci " +
						                 " WHERE uci.user = :user " +
						                 "   AND uci.platform = :platform " +
						                 "   AND uci.distribution = :distribution" +
						                 "   AND uci.version = :version" +
						                 "   AND uci.architecture = :architecture")
		            .setParameter("user", viewpoint.getViewer())
					.setParameter("platform", platform)
					.setParameter("distribution", dbDistribution)
					.setParameter("version", dbVersion)
					.setParameter("architecture", dbArchitecture);
				
				UserClientInfo uci;
				try {
					uci = (UserClientInfo)q.getSingleResult();
					uci.setLastChecked(new Date());
				} catch (NoResultException e) {
					uci = new UserClientInfo(viewpoint.getViewer(), platform, dbDistribution, dbVersion, dbArchitecture);
					uci.setLastChecked(new Date());
					em.persist(uci);
				}
			}
		});
	}
}
