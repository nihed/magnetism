package com.dumbhippo.server;

import java.util.concurrent.Callable;

import javax.ejb.Local;

@Local
public interface TransactionRunner {
	public <T> T runTaskInNewTransaction(Callable<T> callable) throws Exception;
	public void runTaskInNewTransaction(Runnable runnable) throws Exception;
	
	// FIXME: This marks places that need to be fixed by adding post-transaction
	// queueing of non-database operations.
	public <T> T runTaskNotInNewTransaction(Callable<T> callable) throws Exception;
	public <T> T runTaskRetryingOnConstraintViolation(Callable<T> callable) throws Exception;
	
	public <T> T runTaskThrowingConstraintViolation(Callable<T> callable) throws Exception;
	
	// internal, way to get TransactionAttribute, do not use
	public <T> T internalRunTaskInNewTransaction(Callable<T> callable) throws Exception;
	public void internalRunTaskInNewTransaction(Runnable runnable) throws Exception;
}
