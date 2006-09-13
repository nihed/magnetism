package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.FacebookWebServices;

@Stateless
public class FacebookTrackerBean implements FacebookTracker {
	static private final Logger logger = GlobalSetup.getLogger(FacebookTrackerBean.class);
	
	// How old the facebook data can be before we refetch
	static final long FACEBOOK_UPDATE_TIME = 10 * 60 * 1000; // 10 minutes
	
	// Interval at which we check all threads for needing update. This is shorter 
	// than FACEBOOK_UPDATE_TIME so that we don't just miss an update and wait 
	// an entire additional cycle
	static final long UPDATE_THREAD_TIME = FACEBOOK_UPDATE_TIME / 2;
	
	// how long to wait on the Facebook API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
		
	@EJB
	private Configuration config;
	
	@EJB
	private Stacker stacker;
	
	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken) {
		ExternalAccount externalAccount = externalAccounts.getOrCreateExternalAccount(viewpoint, ExternalAccountType.FACEBOOK);
		
		FacebookAccount facebookAccount;
		if (externalAccount.getExtra() == null) {
		    facebookAccount = new FacebookAccount(externalAccount);
		    em.persist(facebookAccount);
		    externalAccount.setExtra(Long.toString(facebookAccount.getId()));
		    externalAccount.setSentiment(Sentiment.LOVE);
		} else {
			facebookAccount = em.find(FacebookAccount.class, Long.parseLong(externalAccount.getExtra()));
			if (facebookAccount == null)
				throw new RuntimeException("Invalid FacebookAccount id " + externalAccount.getExtra() + " is stored in externalAccount " + externalAccount);
		}
		
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		ws.updateSession(facebookAccount, facebookAuthToken);
		
		ws.updateMessageCount(facebookAccount);
		// we want to stack an update regardless of whether they have new messages, so that they know
		// they logged in successfully
		stacker.stackAccountUpdate(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
                ExternalAccountType.FACEBOOK, (new Date()).getTime());
	}
	
	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, User user) throws NotFoundException {
		if (!em.contains(user.getAccount()))
			throw new RuntimeException("detached account in lookupExternalAccount()");
	
		ExternalAccount externalAccount = externalAccounts.lookupExternalAccount(viewpoint, user, ExternalAccountType.FACEBOOK);

		FacebookAccount facebookAccount;
		if (externalAccount.getExtra() == null) {
			throw new NotFoundException("No facebook account details for user " + user);
		} else {
			facebookAccount = em.find(FacebookAccount.class, Long.parseLong(externalAccount.getExtra()));
			if (facebookAccount == null)
				throw new RuntimeException("Invalid FacebookAccount id " + externalAccount.getExtra() + " is stored in externalAccount " + externalAccount);
		}	
		
		return facebookAccount;
	}
	
	public List<FacebookAccount> getAccountsWithValidSession() {
		List list = em.createQuery("SELECT fa FROM FacebookAccount fa WHERE fa.sessionKeyValid = true")
				.getResultList();
		return TypeUtils.castList(FacebookAccount.class, list);
	}
	
	public void updateMessageCount(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		boolean hasNewMessages = ws.updateMessageCount(facebookAccount);
		if (hasNewMessages || !facebookAccount.isSessionKeyValid()) {
			stacker.stackAccountUpdate(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
					                   ExternalAccountType.FACEBOOK, (new Date()).getTime());
		}
	}
	
	public String getApiKey() {
    	String apiKey;
		try {
			apiKey = config.getPropertyNoDefault(HippoProperty.FACEBOOK_API_KEY).trim();
			if (apiKey.length() == 0)
				apiKey = null;	
		} catch (PropertyNotFoundException e) {
			apiKey = null;
		}
		
		if (apiKey == null)
			logger.warn("Facebook API key is not set, we can't use Facebook web services.");
		
		return apiKey;
	}
	
	public synchronized static void startup() {
		logger.info("Starting FacebookUpdater");
		FacebookUpdater.getInstance().start();
	}
	
	public synchronized static void shutdown() {	
		FacebookUpdater.getInstance().shutdown();
	}
	
	private static class FacebookUpdater extends Thread {
		private static FacebookUpdater instance;
		private int generation;
		
		static synchronized FacebookUpdater getInstance() {
			if (instance == null)
				instance = new FacebookUpdater();
			
			return instance;
		}
		
		public FacebookUpdater() {
			super("FacebookUpdater");
		}
		
		private boolean runOneGeneration() {
			int iteration = 0;
			generation += 1;
			try {
                // We start off by sleeping for our delay time to reduce the initial
				// server load on restart
				long lastUpdate = System.currentTimeMillis();
				
				while (true) {
					iteration += 1;
					try {
						long sleepTime = lastUpdate + UPDATE_THREAD_TIME - System.currentTimeMillis();
						if (sleepTime < 0)
							sleepTime = 0;
						Thread.sleep(sleepTime);
						
						// We intentionally iterate here rather than inside a session
						// bean method to get a separate transaction for each method on
						// FacebookTracker rather than holding a single transaction over the whole
						// process.
						final FacebookTracker facebookTracker = EJBUtil.defaultLookup(FacebookTracker.class);
						List<FacebookAccount> facebookAccounts = facebookTracker.getAccountsWithValidSession();
						
						logger.debug("FacebookUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " 
								     + facebookAccounts.size() + " facebook accounts to request info for, iteration " 
								     + iteration);
						
						ExecutorService threadPool = ThreadUtils.newCachedThreadPool("facebook fetcher " + generation + " " + iteration);
						for (final FacebookAccount facebookAccount : facebookAccounts) {
						    threadPool.execute(new Runnable() {
								public void run() {
									final FacebookTracker facebookTracker = EJBUtil.defaultLookup(FacebookTracker.class);
									facebookTracker.updateMessageCount(facebookAccount.getId());
								}
							});
						}
						// tell thread pool to terminate once all tasks are run.
						threadPool.shutdown();
						
						// The idea is to avoid "piling up" (only have one thread pool
						// doing anything at a time). There's a timeout here 
						// though and if it expired we would indeed pile up.
						if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
							logger.warn("FacebookUpdater thread pool timed out; some updater thread is still live and we're continuing anyway");
						}
						
						lastUpdate = System.currentTimeMillis();
					} catch (InterruptedException e) {
						break;
					}
				}
			} catch (RuntimeException e) {
				// not sure jboss will catch and print this since it's our own thread, so doing it here
				logger.error("Unexpected exception updating facebook accounts, generation " + generation + " exiting abnormally", e);
				return true; // do another generation
			}
			logger.debug("Cleanly shutting down facebook updater thread generation {}", generation);
			return false; // shut down
		}
		
		@Override
		public void run() {
			generation = 0;

			while (runOneGeneration()) {
				// sleep protects us from 100% CPU in catastrophic failure case
				logger.warn("FacebookUpdater thread sleeping and then restarting itself");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
			}
		}
		
		public void shutdown() {
			interrupt();
			
			try {
				join();
				logger.info("Successfully stopped FacebookUpdater thread");
			} catch (InterruptedException e) {
				// Shouldn't happen, just ignore
			}
	    }
	}
}
