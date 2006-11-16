package com.dumbhippo.mbean;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.util.EJBUtil;

public class YouTubeUpdaterPeriodicJob implements PeriodicJob {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(YouTubeUpdaterPeriodicJob.class);	
	
	public static final long YOUTUBE_POLL_FREQUENCY = 1000 * 60 * 10; // 10 minutes, matches feed system
	
	public long getFrequencyInMilliseconds() {
		return YOUTUBE_POLL_FREQUENCY;
	}

	private static class YouTubeUserUpdateTask implements Runnable {

		private String user;
		
		YouTubeUserUpdateTask(String user) {
			this.user = user;
		}
		
		public void run() {
			final YouTubeUpdater updater = EJBUtil.defaultLookup(YouTubeUpdater.class);
			updater.periodicUpdate(user);
		}
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		EJBUtil.assertNoTransaction();
		
		YouTubeUpdater updater = EJBUtil.defaultLookup(YouTubeUpdater.class);
		Set<String> usernames = updater.getActiveYouTubeUsers();
		
		logger.debug("YouTubeUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " + usernames.size() + " users to poll, iteration " + iteration);
		
		ExecutorService threadPool = ThreadUtils.newCachedThreadPool("YouTube updater " + generation + " " + iteration);

		for (final String username : usernames) {
			threadPool.execute(new YouTubeUserUpdateTask(username));
		}
		
		// tell thread pool to terminate once all tasks are run.
		threadPool.shutdown();
		
		// The idea is to avoid "piling up" (only have one thread pool
		// doing anything at a time). There's a timeout here 
		// though and if it expired we would indeed pile up.
		// This throws InterruptedException which would cause the periodic update thread to shut down.
		if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
			logger.warn("YouTube updater thread pool timed out; some updater thread is still live and we're continuing anyway");
		}		
	}

	// keep this short/one-wordish since it's in all the log messages
	public String getName() {
		return "YouTube";
	}
}
