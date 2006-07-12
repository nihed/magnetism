package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class TransactionRunnerBean implements TransactionRunner {

	static private final Logger logger = GlobalSetup.getLogger(TransactionRunnerBean.class);
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	public <T> T runTaskInNewTransaction(Callable<T> callable) throws Exception {
		TransactionRunner proxy = EJBUtil.contextLookup(ejbContext, TransactionRunner.class);
		return proxy.internalRunTaskInNewTransaction(callable);
	}
	
	public void runTaskInNewTransaction(Runnable runnable) throws Exception {
		TransactionRunner proxy = EJBUtil.contextLookup(ejbContext, TransactionRunner.class);
		proxy.internalRunTaskInNewTransaction(runnable);
	}
	
	public <T> T runTaskNotInNewTransaction(Callable<T> callable) throws Exception {
		return callable.call();
	}
	
	public <T> T runTaskRetryingOnConstraintViolation(Callable<T> callable) throws Exception {
		int retries = 1;
		
		while (true) {
			try {
				return runTaskInNewTransaction(callable);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					// log at .info so we can get a sense of whether/how-often this happens
					logger.info("Constraint violation race condition detected, retrying. {}: {}", e.getClass().getName(), e.getMessage());
					retries--;
				} else {
					logger.error("Fatal error running task: {} {}", e.getClass().getName(), e.getMessage());
					throw e;
				}
			}
		}
	}

	public <T> T runTaskThrowingConstraintViolation(Callable<T> callable) throws Exception {
		return callable.call();
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <T> T internalRunTaskInNewTransaction(Callable<T> callable) throws Exception {
		return callable.call();
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void internalRunTaskInNewTransaction(Runnable runnable) throws Exception {
		runnable.run(); 
	}
}
