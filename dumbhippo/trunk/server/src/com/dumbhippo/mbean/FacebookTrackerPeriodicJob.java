package com.dumbhippo.mbean;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.tx.TxUtils;

public class FacebookTrackerPeriodicJob implements PeriodicJob {
	static private final Logger logger = GlobalSetup.getLogger(FacebookTrackerPeriodicJob.class);
	
	// How often to poll Facebook for active accounts
	static final long FACEBOOK_UPDATE_TIME = 1000 * 60 * 11; // 11 minutes
	// How often to poll Facebook for all accounts
    static final long FACEBOOK_UPDATE_ALL_FREQUENCY = 11; // every 11 iterations, or 121 minutes, or roughly 2 hours
	
	public long getFrequencyInMilliseconds() {
		return FACEBOOK_UPDATE_TIME;
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		// We intentionally iterate here rather than inside a session
		// bean method to get a separate transaction for each method on
		// FacebookTracker rather than holding a single transaction over the whole
		// process.
		final FacebookSystem facebookSystem = EJBUtil.defaultLookup(FacebookSystem.class);
		
		// on most iterations, we want to check only accounts of people who were recently
		// logged in with the client or on the web site, but sometimes we want to bring
		// all accounts with a valid session key up to date
		boolean applyLoginConstraints = true;
		if (iteration % FACEBOOK_UPDATE_ALL_FREQUENCY == 0) {
		    applyLoginConstraints = false;	
		}
		List<FacebookAccount> facebookAccounts = facebookSystem.getValidAccounts(applyLoginConstraints);
		
		logger.debug("FacebookUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " 
				     + facebookAccounts.size() + " facebook accounts to check, iteration " 
				     + iteration);
		
		ExecutorService threadPool = ThreadUtils.newFixedThreadPool("facebook fetcher " + generation + " " + iteration, 10);
		for (final FacebookAccount facebookAccount : facebookAccounts) {
		    threadPool.execute(new Runnable() {
				public void run() {
					final FacebookTracker facebookTracker = EJBUtil.defaultLookup(FacebookTracker.class);

					// updateMessageCount expects to run in a transaction, but updateTaggedPhotos
					// is TransactionAttributeType.NEVER, so we need to handle them differently.. 
					
					TxUtils.runInTransaction(new Runnable() {
						public void run() {
							DataService.getModel().initializeReadWriteSession(SystemViewpoint.getInstance());
							
							// even if we find out that the session key is expired in updateMessageCount,
							// we still want to call updateTaggedPhotos, because it removes the cached
							// photos that we can't keep anymore when the session key is expired
							facebookTracker.updateMessageCount(facebookAccount.getId());

							// don't do anything about albums for now, since we aren't showing
							// facebook updates to anyone but the album owner, so it's not interesting,
							// and we don't have a system for clearing the cached facebook info about albums
							
							// facebookTracker.updateAlbums(facebookAccount.getId());
						}
					});
					
					facebookTracker.updateTaggedPhotos(facebookAccount.getId());
				}
			});
		}

		// tell thread pool to terminate once all tasks are run.
		threadPool.shutdown();
		
		logger.debug("Waiting for FacebookUpdater to terminate");
		
		// The idea is to avoid "piling up" (only have one thread pool
		// doing anything at a time). There's a timeout here 
		// though and if it expired we would indeed pile up.
		if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
			logger.warn("FacebookUpdater thread pool timed out; some updater thread is still live and we're continuing anyway");
		}
		
		logger.debug("FacebookUpdater complete");
	}

	public String getName() {
		return "facebook";
	}	
}
