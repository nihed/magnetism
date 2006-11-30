package com.dumbhippo.mbean;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.util.EJBUtil;

/**
 * Abstract class for handling periodic updates of external account types
 * whose implementations also subclass the generic CachedExternalUpdater.
 * 
 * @author walters
 *
 */
public abstract class ExternalAccountUpdaterPeriodicJob implements PeriodicJob {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ExternalAccountUpdaterPeriodicJob.class);	
	
	protected abstract Class<? extends CachedExternalUpdater<?>> getUpdater();
	
	protected class UpdateTask implements Runnable {
		private String handle;
		
		UpdateTask(String handle) {
			this.handle = handle;
		}

		public String getHandle() {
			return handle;
		}

		public void run() {
			EJBUtil.defaultLookup(getUpdater()).periodicUpdate(handle);
		}
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		EJBUtil.assertNoTransaction();
		
		CachedExternalUpdater<?> updater = EJBUtil.defaultLookup(getUpdater());
		Set<String> usernames = updater.getActiveUsers();
		
		logger.debug(getName() + " updater slept " + sleepTime / 1000.0 + " seconds, and now has " + usernames.size() + " users to poll, iteration " + iteration);
		
		ExecutorService threadPool = ThreadUtils.newFixedThreadPool(getName() + " updater " + generation + " " + iteration, 10);

		for (final String username : usernames) {
			threadPool.execute(new UpdateTask(username));
		}
		
		// tell thread pool to terminate once all tasks are run.
		threadPool.shutdown();
		
		logger.debug("Waiting for " + getName() + " updater to terminate");
		
		// The idea is to avoid "piling up" (only have one thread pool
		// doing anything at a time). There's a timeout here 
		// though and if it expired we would indeed pile up.
		// This throws InterruptedException which would cause the periodic update thread to shut down.
		if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
			logger.warn(getName() + " updater thread pool timed out; some updater thread is still live and we're continuing anyway");
		}
		
		logger.debug(getName() + " updater complete");
	}

	// keep this short/one-wordish since it's in all the log messages
	public abstract String getName();
}
