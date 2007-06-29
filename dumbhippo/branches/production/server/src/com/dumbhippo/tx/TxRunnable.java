package com.dumbhippo.tx;

/**
 * Like the standard Runnable interface, but the run() method
 * is allowed to throw {@link RetryException}. 
 * 
 * @author otaylor
 */
public interface TxRunnable {
	public void run() throws RetryException;
}
