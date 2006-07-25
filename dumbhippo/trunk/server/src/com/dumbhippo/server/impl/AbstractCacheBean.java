package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.UniqueTaskExecutor;

/**
 * Base class used for beans that implement a cached web service lookup.
 *
 */
public abstract class AbstractCacheBean<KeyType,ResultType> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractCacheBean.class);
	
	// how long to wait on the search API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	// 2 days, shared by yahoo-related subclasses
	static protected final int YAHOO_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 2;
	// hour timeout to retry on failure
	static protected final int FAILED_QUERY_TIMEOUT = 1000 * 60 * 60;

	protected enum Request {
		AMAZON_ALBUM,
		RHAPSODY_DOWNLOAD,
		YAHOO_ALBUM,
		YAHOO_ALBUM_SONGS,
		YAHOO_ARTIST_ALBUMS,
		YAHOO_ARTIST,
		YAHOO_ARTIST_BY_NAME,
		YAHOO_SONG,
		YAHOO_SONG_DOWNLOAD
	}
	
	private static EnumMap<Request,UniqueTaskExecutor> executors;
	private static boolean shutdown = false;
	
	private Request defaultRequest;
	
	private synchronized static UniqueTaskExecutor getExecutorInternal(Request request) {
		if (shutdown)
			throw new RuntimeException("getExecutor() called after shutdown");
		
		if (executors == null)
			executors = new EnumMap<Request,UniqueTaskExecutor>(Request.class);
		
		UniqueTaskExecutor executor = executors.get(request);
		if (executor == null) {
			executor = new UniqueTaskExecutor(request.name().toLowerCase() + " pool");
			executors.put(request, executor);
		}
		return executor;
	}
	
	@SuppressWarnings("unchecked")
	public static void shutdown() {
		synchronized (AbstractCacheBean.class) {
			shutdown = true;
			
			for (UniqueTaskExecutor executor : executors.values()) {
				executor.shutdown();
			}
			executors.clear();
		}
	}
	
	protected static <T> T getFutureResultNullOnException(Future<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logger.warn("future interrupted {}: {}", e.getClass().getName(), e.getMessage());
			return null;
		} catch (ExecutionException e) {
			logger.warn("future threw execution exception {}: {}", e.getClass().getName(), e.getMessage());
			return null;
		}
	}
	
	protected static <T> List<T> getFutureResultEmptyListOnException(Future<List<T>> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logger.warn("future interrupted {}: {}", e.getClass().getName(), e.getMessage());
			return Collections.emptyList();
		} catch (ExecutionException e) {
			logger.warn("future threw execution exception {}: {}", e.getClass().getName(), e.getMessage());
			return Collections.emptyList();
		}
	}
	
	protected AbstractCacheBean(Request defaultRequest) {
		this.defaultRequest = defaultRequest;
	}
	
	@SuppressWarnings("unchecked")
	protected UniqueTaskExecutor<KeyType,ResultType> getExecutor() {
		return getExecutorInternal(defaultRequest);
	}
	
	@SuppressWarnings("unchecked")
	protected UniqueTaskExecutor<KeyType,ResultType> getExecutor(Request request) {
		return getExecutorInternal(request);
	}
}
