package com.dumbhippo.server;

import java.util.concurrent.Callable;

import javax.ejb.Local;

@Local
public interface TransactionRunner {
	public <T> T runTaskInNewTransaction(Callable<T> callable);
	public <T> T runTaskRetryingOnConstraintViolation(Callable<T> callable);
	
	// internal, way to get TransactionAttribute, do not use
	public <T> T internalRunTaskInNewTransaction(Callable<T> callable);
}
