package com.dumbhippo.server.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;

/**
 * Base class used for beans that implement a cached web service lookup.
 *
 */
public abstract class AbstractCacheBean {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractCacheBean.class);
	
	// how long to wait on the search API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	// 2 days, shared by yahoo-related subclasses
	static protected final int YAHOO_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 2;
	// hour timeout to retry on failure
	static protected final int FAILED_QUERY_TIMEOUT = 1000 * 60 * 60;

	
	private static ExecutorService threadPool;
	private static boolean shutdown = false;
	
	protected synchronized static ExecutorService getThreadPool() {
		if (shutdown)
			throw new RuntimeException("getThreadPool() called after shutdown");
			
		if (threadPool == null) {
			threadPool = ThreadUtils.newCachedThreadPool("ws cache pool");
		}
		
		return threadPool;
	}
	
	public static void shutdown() {
		synchronized (AbstractCacheBean.class) {
			shutdown = true;
			
			if (threadPool != null) {
				threadPool.shutdown();
				threadPool = null;
			}
		}	
	}

	static protected <T> T getFutureResult(Future<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logger.warn("thread pool worker thread interrupted {}", e.getMessage());
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			logger.warn("thread pool worker thread threw execution exception {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
