package com.dumbhippo.server.impl;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.jgroups.ChannelException;
import org.jgroups.blocks.LockNotGrantedException;
import org.jgroups.blocks.LockNotReleasedException;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.UniqueTaskExecutor;
import com.dumbhippo.mbean.LockService;

/**
 * This class extends UniqueTaskExecutor so that when we execute a task, we first
 * get a cluster-wide lock. This allows us to avoid making multiple web requests for
 * the same data, and also prevents race conditions when multiple threads try
 * to store the same data into the database.  
 *
 * @author otaylor
 */
public class CacheTaskExecutor<KeyType, ResultType> extends UniqueTaskExecutor<KeyType, ResultType> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(CacheTaskExecutor.class);

	// This is the timeout before we give up trying to give a lock, but this
	// timeout is only about waiting for the other nodes to respond. If
	// the lock is held elsewhere, the other nodes respond instantly and 
	// we fail.
	static final int COMMUNICATION_TIMEOUT = 60 * 1000; // 1 minute
	
	// Time before we retry getting a lock
	static final int RETRY_TIME = 1000; // 1 second
	
	// Maximum number of retries
	static final int MAX_RETRIES = 60;
	
	public CacheTaskExecutor(String name) {
		super(name);
	}
	
	@Override
	public synchronized Future<ResultType> execute(KeyType key, Callable<ResultType> callable) {
		return super.execute(key, new CallableWrapper<ResultType>(key, callable));
	}
	
	private final static class LockName implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private String executorName;
		private Object key;

		public LockName(String executorName, Object key) {
			this.executorName = executorName;
			this.key = key;
		}
		
		@Override
		public int hashCode() {
			return (executorName.hashCode() * 31) ^ key.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof CacheTaskExecutor.LockName))
				return false;
			
			LockName otherName = (LockName)other;
			return otherName.executorName.equals(executorName) && otherName.key.equals(key);
		}
		
		@Override
		public String toString() {
			return "{LockName " + executorName + "/" + key + "}";
		}
	}
	
	private class CallableWrapper<T> implements Callable<T> {
		private KeyType key;
		private Callable<T> inner;

		public CallableWrapper(KeyType key, Callable<T> inner) {
			this.key = key;
			this.inner = inner;
		}
		
		public T call() throws Exception {
			LockService lockService = LockService.getInstance();
			LockName lockName = new LockName(getName(), key);
			int retries = 0;

			do {
				try {
					lockService.lock(lockName, COMMUNICATION_TIMEOUT);
					break;
				} catch (LockNotGrantedException e) {
					// Some other thread has a lock, try again
				} catch (ChannelException e) {
					throw new RuntimeException("Communication failure establishing lock for " + lockName, e);
				}
				
				retries++;				
				if (retries > MAX_RETRIES)
					throw new RuntimeException("Gave up on getting lock for " + lockName + " after " + MAX_RETRIES + " retries");
				
				logger.debug("Can't get lock for {}, waiting {}ms", lockName, RETRY_TIME);
				
				try {
					Thread.sleep(RETRY_TIME);
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while waiting for lock");
				}
				
			} while (true);
			
			try {
				return inner.call();
			} finally {
				try {
					lockService.unlock(lockName);
				} catch (LockNotReleasedException e) {
					throw new RuntimeException("Failed to release lock for " + lockName, e);
				} catch (ChannelException e) {
					throw new RuntimeException("Communication failure releasing lock for " + lockName, e);
				}
			}
		}
	}
}
