package com.dumbhippo.server.impl;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.FeedUpdater;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class FeedUpdaterBean implements FeedUpdater {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FeedUpdaterBean.class);
	
	// How old the feed data can be before we refetch
	static final long FEED_UPDATE_TIME = 10 * 60 * 1000; // 10 minutes
	
	// Interval at which we check all threads for needing update. This is shorter 
	// than FEED_UPDATE_TIME so that we don't just miss an update and wait 
	// an entire additional cycle
	static final long UPDATE_THREAD_TIME = FEED_UPDATE_TIME / 2;
	
	public synchronized static void startup() {
		logger.info("Starting FeedUpdater");
		FeedUpdater.getInstance().start();
	}
	
	public synchronized static void shutdown() {
		FeedUpdater.getInstance().shutdown();
	}

	private static class FeedUpdater extends Thread {
		private static FeedUpdater instance;
		private int generation;
		
		static synchronized FeedUpdater getInstance() {
			if (instance == null)
				instance = new FeedUpdater();
			
			return instance;
		}
		
		public FeedUpdater() {
			super("FeedUpdater");
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
								threadPool.execute(new Runnable() {
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
											logger.info("Couldn't update feed {}: {}", feed, e.getCodeString() + ": " + e.getMessage());
											feedSystem.markFeedFailedLastUpdate(feed);											
										}
									}
								});
							}
						}
						
						logger.debug("Started {} feed fetching threads for iteration {}", count, iteration);
						
						// tell thread pool to terminate once all tasks are run.
						threadPool.shutdown();
						
						// The idea is to avoid "piling up" (only have one thread pool
						// doing anything at a time). There's a timeout here 
						// though and if it expired we would indeed pile up.
						if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
							logger.warn("FeedUpdater thread pool timed out; some updater thread is still live and we're continuing anyway");
						}
						
						lastUpdate = System.currentTimeMillis();
					} catch (InterruptedException e) {
						break;
					}
				}
			} catch (RuntimeException e) {
				// not sure jboss will catch and print this since it's our own thread, so doing it here
				logger.error("Unexpected exception updating feeds, generation " + generation + " exiting abnormally", e);
				return true; // do another generation
			}
			logger.debug("Cleanly shutting down feed updater thread generation {}", generation);
			return false; // shut down
		}
		
		@Override
		public void run() {
			generation = 0;

			while (runOneGeneration()) {
				// sleep protects us from 100% CPU in catastrophic failure case
				logger.warn("FeedUpdater thread sleeping and then restarting itself");
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
				logger.info("Successfully stopped FeedUpdater thread");
			} catch (InterruptedException e) {
				// Shouldn't happen, just ignore
			}
		}
	}
}
