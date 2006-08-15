package com.dumbhippo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;

/**
 * The idea of this class is that for the function "ResultType func(KeyType key)"
 * we only need to run one concurrent instance of the function for each value of 
 * the key. This class is not a cache; it will happily run two identical calls 
 * in serial. It simply ensures that calls don't overlap (within a single class
 * loader, obviously it does not work across app server instances).
 * 
 */
public final class UniqueTaskExecutor<KeyType,ResultType> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(UniqueTaskExecutor.class);		
	
	private ExecutorService threadPool;
	private boolean shutdown;
	private Map<KeyType,UniqueFutureTask> inProgress;
	private String name;
	
	private synchronized ExecutorService getThreadPool() {
		if (shutdown)
			throw new RuntimeException("getThreadPool() called after shutdown");
		
		return threadPool;
	}
	
	public synchronized void shutdown() {
		shutdown = true;
			
		if (threadPool != null) {
			threadPool.shutdown();
			threadPool = null;
		}
	}
	
	public UniqueTaskExecutor(String name) {
		this.name = name;
		inProgress = new HashMap<KeyType,UniqueFutureTask>();
		threadPool = ThreadUtils.newCachedThreadPool(name);
	}
		
	private synchronized void removeTask(KeyType key) {
		inProgress.remove(key);
		// logger.debug("Removing task in {} for key {}", name, key);
	}
	
	public synchronized Future<ResultType> execute(KeyType key, Callable<ResultType> callable) {
		UniqueFutureTask existing = inProgress.get(key);
		if (existing != null) {
			logger.debug("Using existing task for {} key {}", name, key);
			return existing;
		}
		UniqueFutureTask task = new UniqueFutureTask(key, callable);
		inProgress.put(key, task);
		
		// logger.debug("Added task to {} for key {}", name, key);
		
		// warn whenever we cross a large number; this is intended to 
		// detect a bug where we don't remove tasks
		if (inProgress.size() == 50) {
			logger.warn("Large number of {} tasks in flight: {}", name, inProgress.size());
		}
		
		getThreadPool().execute(task);
		return task;
	}
	
	// note this is an inner class and has to be threadsafe in accessing the outer
	private class UniqueFutureTask extends FutureTask<ResultType> {
		private KeyType key;

		UniqueFutureTask(KeyType key, Callable<ResultType> callable) {
			super(callable);
			this.key = key;
		}
		
		@Override
		protected void done() {
			removeTask(key);
		}
	}
}
