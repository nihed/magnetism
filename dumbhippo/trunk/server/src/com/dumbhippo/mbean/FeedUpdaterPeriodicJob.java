package com.dumbhippo.mbean;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;

public final class FeedUpdaterPeriodicJob implements PeriodicJob {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FeedUpdaterPeriodicJob.class);
	
	// How old the feed data can be before we refetch in milliseconds
	public static final long FEED_UPDATE_TIME = 1000 * 60 * 10; // 10 minutes
	
	// How long to wait if we failed to update last time in milliseconds
	public static final long BAD_FEED_UPDATE_TIME = 1000 * 60 * 60 * 7; // 7 hours
	
	// Interval at which we check all feeds for needing update. This is shorter 
	// than FEED_UPDATE_TIME so that we don't just miss an update and wait 
	// an entire additional cycle
	public static final long UPDATE_THREAD_TIME = FEED_UPDATE_TIME / 2;

	public FeedUpdaterPeriodicJob() {
		
	}
	
	public long getFrequencyInMilliseconds() {
		return UPDATE_THREAD_TIME;
	}

	private static class FeedChecker implements Runnable {
		private Feed feed;
		
		FeedChecker(Feed feed) {
			this.feed = feed;
		}
		
		public void run() {
			final FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);
			try {
                final Object o = feedSystem.updateFeedFetchFeed(feed);
                if (o != null) {
                        final TransactionRunner runner = EJBUtil.defaultLookup(TransactionRunner.class);
                        runner.runTaskRetryingOnDuplicateEntry(new Runnable() {
                                public void run() {
                                        try {
                                            feedSystem.updateFeedStoreFeed(o);
                                        } catch (final XmlMethodException e) {
                                                logger.warn("Couldn't store feed update results {}: {}", feed, e.getCodeString() + ": " + e.getMessage());
                                                // XmlMethodException here should be some kind of feed parse error, not a database error.
                                                // we don't want to mark a feed failed due to a db error anyway since the expected db
                                                // error is that another thread successfully saved the feed first.
                                                feedSystem.markFeedFailedLastUpdate(feed);
                                        }
                                }
                        });
                }
			} catch (XmlMethodException e) {
				// this should have been logged already deeper in the system where more detail was known,
				// do a debug log here in case it somehow was not.
				logger.debug("Couldn't update feed {}: {}", feed, e.getCodeString() + ": " + e.getMessage());
				feedSystem.markFeedFailedLastUpdate(feed);											
			}
		}
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		// We intentionally iterate here rather than inside a session
		// bean method to get a separate transaction for each method on
		// feedSystem rather than holding a single transaction over the whole
		// process.
		FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);
		List<Feed> feeds = feedSystem.getInUseFeeds();
		
		logger.debug("FeedUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " + feeds.size() + " feeds in use, iteration " + iteration);
		
		ExecutorService threadPool = ThreadUtils.newCachedThreadPool("feed fetcher " + generation + " " + iteration);

		int count = 0;
		for (final Feed feed : feeds) {
			if (feedSystem.updateFeedNeedsFetch(feed)) {
				++count;
				threadPool.execute(new FeedChecker(feed));
			}
		}
		
		logger.debug("Started {} feed fetching threads for iteration {}", count, iteration);
		
		// tell thread pool to terminate once all tasks are run.
		threadPool.shutdown();
		
		// The idea is to avoid "piling up" (only have one thread pool
		// doing anything at a time). There's a timeout here 
		// though and if it expired we would indeed pile up.
		// This throws InterruptedException which would cause the periodic update thread to shut down.
		if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
			logger.warn("FeedUpdater thread pool timed out; some updater thread is still live and we're continuing anyway");
		}
	}

	// keep this short/one-wordish since it's in all the log messages
	public String getName() {
		return "feeds";
	}
}
