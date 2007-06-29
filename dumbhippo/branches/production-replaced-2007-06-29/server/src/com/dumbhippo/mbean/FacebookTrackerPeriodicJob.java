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
import com.dumbhippo.server.util.EJBUtil;

public class FacebookTrackerPeriodicJob implements PeriodicJob {
	static private final Logger logger = GlobalSetup.getLogger(FacebookTrackerPeriodicJob.class);
	
	// How often to poll facebook
	static final long FACEBOOK_UPDATE_TIME = 1000 * 60 * 11; // 11 minutes

	public long getFrequencyInMilliseconds() {
		return FACEBOOK_UPDATE_TIME;
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		// We intentionally iterate here rather than inside a session
		// bean method to get a separate transaction for each method on
		// FacebookTracker rather than holding a single transaction over the whole
		// process.
		final FacebookSystem facebookSystem = EJBUtil.defaultLookup(FacebookSystem.class);
		List<FacebookAccount> facebookAccounts = facebookSystem.getAllAccounts();
		
		logger.debug("FacebookUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " 
				     + facebookAccounts.size() + " facebook accounts to check, iteration " 
				     + iteration);
		
		ExecutorService threadPool = ThreadUtils.newFixedThreadPool("facebook fetcher " + generation + " " + iteration, 10);
		for (final FacebookAccount facebookAccount : facebookAccounts) {
		    threadPool.execute(new Runnable() {
				public void run() {
					final FacebookTracker facebookTracker = EJBUtil.defaultLookup(FacebookTracker.class);
					if (facebookAccount.isSessionKeyValid()) {
						// even if we find out that the session key is expired in updateMessageCount,
						// we still want to call updateTaggedPhotos, because it removes the cached
						// photos that we can't keep anymore when the session key is expired
						facebookTracker.updateMessageCount(facebookAccount.getId());
						facebookTracker.updateTaggedPhotos(facebookAccount.getId());
						// don't do anything about albums for now, since we aren't showing
						// facebook updates to anyone but the album owner, so it's not interesting,
						// and we don't have a system for clearing the cached facebook info about albums
						// facebookTracker.updateAlbums(facebookAccount.getId());
					}
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
