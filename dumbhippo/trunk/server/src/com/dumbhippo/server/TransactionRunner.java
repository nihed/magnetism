package com.dumbhippo.server;

import java.util.Set;
import java.util.concurrent.Callable;

import javax.ejb.Local;

@Local
public interface TransactionRunner {
	public <T> T runTaskInNewTransaction(Callable<T> callable) throws Exception;
	public void runTaskInNewTransaction(Runnable runnable);
	
	// FIXME: This marks places that need to be fixed by adding post-transaction
	// queueing of non-database operations.
	public <T> T runTaskNotInNewTransaction(Callable<T> callable) throws Exception;
	
	public <T> T runTaskRetryingOnTransactionException(Callable<T> callable, Set<Class<?>> retryExceptions) throws Exception;
	public <T> T runTaskRetryingOnConstraintViolation(Callable<T> callable) throws Exception;
	public <T> T runTaskRetryingOnDuplicateEntry(Callable<T> callable) throws Exception;
	
	public void runTaskRetryingOnTransactionException(Runnable runnable, Set<Class<?>> retryExceptions);
	public void runTaskRetryingOnConstraintViolation(Runnable runnable);
	public void runTaskRetryingOnDuplicateEntry(Runnable runnable);
	
	public <T> T runTaskThrowingConstraintViolation(Callable<T> callable) throws Exception;
	
	/**
	 * Asynchronously execute a runnable after the current transaction has completed successfully.
	 * 
	 * @param runnable executed after transaction completion
	 */
	public void runTaskOnTransactionCommit(Runnable runnable);
	
	// internal, way to get TransactionAttribute, do not use
	public <T> T internalRunTaskInNewTransaction(Callable<T> callable) throws Exception;
	public void internalRunTaskInNewTransaction(Runnable runnable);
}
