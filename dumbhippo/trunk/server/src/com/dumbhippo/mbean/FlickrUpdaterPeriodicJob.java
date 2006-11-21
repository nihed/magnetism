package com.dumbhippo.mbean;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.FlickrUpdater;
import com.dumbhippo.server.util.EJBUtil;

public class FlickrUpdaterPeriodicJob implements PeriodicJob {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FlickrUpdaterPeriodicJob.class);	
	
	public static final long FLICKR_POLL_FREQUENCY = 1000 * 60 * 13; // 13 minutes, prime number not used elsewhere
	
	public long getFrequencyInMilliseconds() {
		return FLICKR_POLL_FREQUENCY;
	}

	private static class FlickrUserUpdateTask implements Runnable {

		private String flickrId;
		
		FlickrUserUpdateTask(String flickrId) {
			this.flickrId = flickrId;
		}
		
		public void run() {
			final FlickrUpdater updater = EJBUtil.defaultLookup(FlickrUpdater.class);
			updater.periodicUpdate(flickrId);
		}
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		// We intentionally iterate here rather than inside a session
		// bean method to get a separate transaction for each method on
		// the updater bean rather than holding a single transaction over the whole
		// process.
		EJBUtil.assertNoTransaction();
		
		FlickrUpdater updater = EJBUtil.defaultLookup(FlickrUpdater.class);
		Set<String> flickrUserIds = updater.getActiveFlickrUserIds();
		
		logger.debug("FlickrUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " + flickrUserIds.size() + " users to poll, iteration " + iteration);
		
		ExecutorService threadPool = ThreadUtils.newFixedThreadPool("flickr updater " + generation + " " + iteration, 10);

		for (final String flickrId : flickrUserIds) {
			threadPool.execute(new FlickrUserUpdateTask(flickrId));
		}
		
		// tell thread pool to terminate once all tasks are run.
		threadPool.shutdown();
		
		logger.debug("Waiting for FlickrUpdater to terminate");
		
		// The idea is to avoid "piling up" (only have one thread pool
		// doing anything at a time). There's a timeout here 
		// though and if it expired we would indeed pile up.
		// This throws InterruptedException which would cause the periodic update thread to shut down.
		if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
			logger.warn("flickr updater thread pool timed out; some updater thread is still live and we're continuing anyway");
		}
		
		logger.debug("FlickrUpdater complete");
	}

	// keep this short/one-wordish since it's in all the log messages
	public String getName() {
		return "flickr";
	}
}
