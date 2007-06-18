package com.dumbhippo.tx;

import java.util.concurrent.Callable;

/**
 * Similar to the standard Callable interface, but the call() method
 * is <i>only</i> allowed to throw {@link RetryException}. 
 * 
 * @author otaylor
 */
public interface TxCallable<T> extends Callable<T> {
	public T call() throws RetryException;
}
