package com.dumbhippo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Like ExecutorCompletionService, but has schedule() instead of submit().
public class ScheduledExecutorCompletionService <V> {
	private ScheduledExecutorService executor;
	private BlockingQueue<FutureTask<V>> queue;
	
	private class EnqueuingFutureTask extends FutureTask<V> {

		public EnqueuingFutureTask(Callable<V> callable) {
			super(callable);
		}

		@Override
		protected void done() {
			queue.add(this);
		}		
	}
	
	public ScheduledExecutorCompletionService(ScheduledExecutorService executor) {
		this.executor = executor;
	}
	
	// TODO - note here there is no timeout.  There is also not one in the
	// implementation of ScheduledExectutorService.
	public void schedule(Callable<V> callable, long delay, TimeUnit unit) {
		executor.schedule(new EnqueuingFutureTask(callable), delay, unit);
	}
	
	public Future<V> take() throws InterruptedException {
		return queue.take();
	}
}
