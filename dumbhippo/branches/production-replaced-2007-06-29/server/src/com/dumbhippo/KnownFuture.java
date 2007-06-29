package com.dumbhippo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class KnownFuture<T> implements Future<T> {

	private T value;
	
	public KnownFuture(T value) {
		this.value = value;
	}
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		// can't interrupt since we already completed
		return false;
	}

	public boolean isCancelled() {
		// we were not canceled before normal completion
		return false;
	}

	public boolean isDone() {
		return true;
	}

	public T get() throws InterruptedException, ExecutionException {
		return value;
	}

	public T get(long timeout, TimeUnit units) throws InterruptedException, ExecutionException, TimeoutException {
		return value;
	}
}
