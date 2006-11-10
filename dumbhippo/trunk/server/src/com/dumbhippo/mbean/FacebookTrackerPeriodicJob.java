package com.dumbhippo.mbean;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.persistence.FacebookAccount;
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
					// FIXME: we might end up calling web services multiple times with an expired session
					// key, perhaps have the functions should return a boolean on whether the session key
					// is still fine or unite them in one function
					facebookTracker.updateMessageCount(facebookAccount.getId());
					facebookTracker.updateTaggedPhotos(facebookAccount.getId());
					facebookTracker.updateAlbums(facebookAccount.getId());
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
	}

	public String getName() {
		return "facebook";
	}	
}
