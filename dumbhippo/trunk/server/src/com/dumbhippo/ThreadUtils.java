package com.dumbhippo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;

public class ThreadUtils {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ThreadUtils.class);
	
	/**
	 * Like Executors.newCachedThreadPool, but you can specify the name of 
	 * the threads that will be created
	 * 
	 * @param baseName identifier of the thread pool. The threads in the
	 *   thread pool will be named "[baseName] 1", "[baseName] 2", and
	 *   so forth.
	 * @return a newly created ExecutorService. You should call shutdown()
	 *   on this service when you are finished using it.
	 */
	public static ExecutorService newCachedThreadPool(final String baseName) {
		return Executors.newCachedThreadPool(new ThreadFactory() {
			private int nextThreadId = 0;
			
			public synchronized Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName(baseName + " " + nextThreadId);
				nextThreadId += 1;
				return t;
			}
		});		
	}
	
	/**
	 * Like Executors.newSingleThreadExecutor, but you can specify the name of 
	 * the thread that will be created
	 * 
	 * @param name name of the thread
	 * @return a newly created ExecutorService. You should call shutdown()
	 *   on this service when you are finished using it.
	 */
	public static ExecutorService newSingleThreadExecutor(final String name) {
		return Executors.newSingleThreadExecutor(new ThreadFactory() {
			public synchronized Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName(name);
				return t;
			}
		});
	}
}
