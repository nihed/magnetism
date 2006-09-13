package com.dumbhippo;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

public class ThreadUtils {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ThreadUtils.class);
		
	static private int nextGlobalThreadId = 0;
	
	static private UncaughtExceptionHandler exceptionHandler = new EmergencyExceptionHandler();
	
	// note that there's also a Thread.setDefaultUncaughtExceptionHandler which sets the 
	// global handler for all threads, but since our class will get unloaded without 
	// exiting the jvm, setting that would be evil.
	
	// Something to be careful of is that if you do ExecutorService.submit(), it returns 
	// a future and any exception from your runnable is set on the future as an ExecutionException,
	// rather than thrown out to the thread's root run(). This means that you need to 
	// actually call get() on the Future so you get that exception. If you don't want the 
	// future, be sure you use ExecutorService.execute(), not submit().
	
	static private class EmergencyExceptionHandler implements UncaughtExceptionHandler {
		@SuppressWarnings("unused")
		static private final Logger logger = GlobalSetup.getLogger(EmergencyExceptionHandler.class);

		public void uncaughtException(Thread thread, Throwable throwable) {
			logger.error("Uncaught exception terminated thread {}: {}", thread.getName(),
					ExceptionUtils.getRootCause(throwable).getMessage());
			logger.error("Exception killing thread", throwable);
		}
	}
	
	public static void shutdownAndAwaitTermination(ExecutorService service) {
		service.shutdown();
		try {
			service.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Timed out while waiting to shut down executor service");
		}
	}
	
	/**
	 * Like Executors.newCachedThreadPool, but you can specify the name of 
	 * the threads that will be created and the threads are daemon threads.
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
				t.setUncaughtExceptionHandler(exceptionHandler);
				return t;
			}
		});
	}

	/**
	 * Like Executors.newFixedThreadPool, but you can specify the name of 
	 * the thread that will be created and the threads are daemon threads.
	 */
	public static ExecutorService newFixedThreadPool(final String baseName, int numThreads) {
		return Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
			private int nextThreadId = 0;
			
			public synchronized Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName(baseName + " " + nextThreadId);
				nextThreadId += 1;
				t.setUncaughtExceptionHandler(exceptionHandler);
				return t;
			}
		});
	}

	/**
	 * Like Executors.newSingleThreadExecutor, but you can specify the name of 
	 * the thread that will be created and the threads are daemon threads.
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
				t.setUncaughtExceptionHandler(exceptionHandler);
				return t;
			}
		});
	}
	
	public static Thread newDaemonThread(String name, Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		synchronized (ThreadUtils.class) {
			t.setName(name + " " + nextGlobalThreadId);
			nextGlobalThreadId += 1;
		}
		t.setUncaughtExceptionHandler(exceptionHandler);
		return t;
	}
	
	private static void logException(Exception e, boolean fullLog) {
		if (e instanceof InterruptedException) {
			if (!fullLog)
				logger.warn("future interrupted {}: {}", e.getClass().getName(), e.getMessage());
			else
				logger.warn("future interrupted", e);
		} else if (e instanceof ExecutionException) {
			if (!fullLog)
				logger.warn("future threw execution exception {}: {}", e.getClass().getName(), e.getMessage());
			else
				logger.warn("future threw execution exception", e);
			Throwable cause = e.getCause();
			if (cause != null && cause != e) {
				logger.warn("cause of execution exception was {}: {}", cause.getClass().getName(), cause.getMessage());
			}
			Throwable root = ExceptionUtils.getRootCause(e);
			if (root != null && root != e && root != cause) {
				logger.warn("root cause of execution exception was {}: {}", root.getClass().getName(), root.getMessage());
			}
		} else {
			if (!fullLog)
				logger.warn("future got unexpected exception {}: {}", e.getClass().getName(), e.getMessage());
			else
				logger.warn("future got unexpected exception", e);
		}
	}
	
	public static <T> T getFutureResult(Future<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logException(e, false);
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			logException(e, false);
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T getFutureResultNullOnException(Future<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logException(e, true);
			return null;
		} catch (ExecutionException e) {
			logException(e, true);
			return null;
		}
	}
	
	public static <T> List<T> getFutureResultEmptyListOnException(Future<List<T>> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logException(e, true);
			return Collections.emptyList();
		} catch (ExecutionException e) {
			logException(e, true);
			return Collections.emptyList();
		}
	}
}
