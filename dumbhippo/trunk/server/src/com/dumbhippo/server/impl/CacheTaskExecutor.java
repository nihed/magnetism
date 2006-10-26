package com.dumbhippo.server.impl;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.jgroups.ChannelException;
import org.jgroups.blocks.LockNotGrantedException;
import org.jgroups.blocks.LockNotReleasedException;

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
	static final int TIMEOUT = 60 * 1000; // 1 minute
	
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

			try {
				lockService.lock(lockName, TIMEOUT);
			} catch (LockNotGrantedException e) {
				// This probably means a timeout. All the other failures should only occur
				// if communication between the cluster is disrupted or something else
				// is seriously wrong
				throw new RuntimeException("Could not establish lock for " + lockName, e);
			} catch (ChannelException e) {
				throw new RuntimeException("Communication failure establishing lock for " + lockName, e);
			}
			
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
