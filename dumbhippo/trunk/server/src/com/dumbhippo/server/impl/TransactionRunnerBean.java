package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class TransactionRunnerBean implements TransactionRunner {

	static private final Log logger = GlobalSetup.getLog(TransactionRunnerBean.class);
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	public <T> T runTaskInNewTransaction(Callable<T> callable) throws Exception {
		TransactionRunner proxy = EJBUtil.contextLookup(ejbContext, TransactionRunner.class);
		return proxy.internalRunTaskInNewTransaction(callable);
	}
	
	public <T> T runTaskRetryingOnConstraintViolation(Callable<T> callable) throws Exception {
		int retries = 1;
		
		while (true) {
			try {
				return runTaskInNewTransaction(callable);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Constraint violation race condition detected, retrying: " + e.getClass().getName(), e);
					retries--;
				} else {
					logger.error("Fatal error running task: " + e.getClass().getName(), e);
					throw e;
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <T> T internalRunTaskInNewTransaction(Callable<T> callable) throws Exception {
		return callable.call();
	}
}
