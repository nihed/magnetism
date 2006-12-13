package com.dumbhippo.server.impl;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class TransactionRunnerBean implements TransactionRunner {

	static private final Logger logger = GlobalSetup.getLogger(TransactionRunnerBean.class);
	
	@javax.annotation.Resource
	private EJBContext ejbContext;

	private static ExecutorService postTransactionExecutor;
	private static boolean shutdown = false;
	
	private static synchronized ExecutorService getPostTransactionExecutor() {
		if (shutdown)
			throw new RuntimeException("getPostTransactionExecutor() called after shutdown");
		
		if (postTransactionExecutor == null)
			postTransactionExecutor = ThreadUtils.newCachedThreadPool("post-transaction tasks");
		
		return postTransactionExecutor;
	}
	
	public synchronized static void shutdown() {
		shutdown = true;
		
		if (postTransactionExecutor != null) {
			ThreadUtils.shutdownAndAwaitTermination(postTransactionExecutor);
			postTransactionExecutor = null;
		}
		logger.debug("TransactionRunner shut down");
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public <T> T runTaskInNewTransaction(Callable<T> callable) throws Exception {
		TransactionRunner proxy = EJBUtil.contextLookup(ejbContext, TransactionRunner.class);
		return proxy.internalRunTaskInNewTransaction(callable);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public void runTaskInNewTransaction(Runnable runnable) {
		TransactionRunner proxy = EJBUtil.contextLookup(ejbContext, TransactionRunner.class);
		proxy.internalRunTaskInNewTransaction(runnable);
	}
	
	public <T> T runTaskNotInNewTransaction(Callable<T> callable) throws Exception {
		return callable.call();
	}
	
	
	public <T> T runTaskRetryingOnTransactionException(Callable<T> callable,  Set<Class<?>> retryExceptions) throws Exception {
		int retries = 1;
		
		while (true) {
			try {
				return runTaskInNewTransaction(callable);
			} catch (Exception e) {
				if (retries > 0 && ExceptionUtils.hasCause(e, retryExceptions)) {
					// log at .info so we can get a sense of whether/how-often this happens
					logger.info("Caught an exception inside a transaction, retrying. {}: {}", e.getClass().getName(), e.getMessage());
					retries--;
				} else {
					logger.error("Fatal error running task: {} {}", e.getClass().getName(), e.getMessage());
					throw e;
				}
			}
		}
	}
	
	public <T> T runTaskRetryingOnConstraintViolation(Callable<T> callable) throws Exception {	
		return runTaskRetryingOnTransactionException(callable, EJBUtil.getConstraintViolationExceptions());
	}
	
	public <T> T runTaskRetryingOnDuplicateEntry(Callable<T> callable) throws Exception {
		return runTaskRetryingOnTransactionException(callable, EJBUtil.getDuplicateEntryExceptions());
	}

	public void runTaskRetryingOnTransactionException(Runnable runnable, Set<Class<?>> retryExceptions) {
		int retries = 1;
		
		while (true) {
			try {
				runTaskInNewTransaction(runnable);
				return;
			} catch (RuntimeException e) {
				if (retries > 0 && ExceptionUtils.hasCause(e, retryExceptions)) {
					// log at .info so we can get a sense of whether/how-often this happens
					logger.info("Caught an exception inside a transaction, retrying. {}: {}", e.getClass().getName(), e.getMessage());
					retries--;
				} else {
					logger.error("Fatal error running task: {} {}", e.getClass().getName(), e.getMessage());
					throw e;
				}
			}
		}
	}

	public void runTaskRetryingOnConstraintViolation(Runnable runnable) {
		runTaskRetryingOnTransactionException(runnable, EJBUtil.getConstraintViolationExceptions());
	}

	public void runTaskRetryingOnDuplicateEntry(Runnable runnable) {
		runTaskRetryingOnTransactionException(runnable, EJBUtil.getDuplicateEntryExceptions());
	}

	public <T> T runTaskThrowingConstraintViolation(Callable<T> callable) throws Exception {
		return callable.call();
	}
	
	public void runTaskOnTransactionCommit(final Runnable runnable) {
		runTaskOnTransactionComplete(runnable, true);
	}

	public void runTaskOnTransactionCommitOrRollback(final Runnable runnable) {
		runTaskOnTransactionComplete(runnable, false);
	}

	static private String transactionStatusString(int status) {
		switch (status) {
		case Status.STATUS_ACTIVE:
			return "ACTIVE - " + status;
		case Status.STATUS_COMMITTED:
			return "COMMITTED - " + status;
		case Status.STATUS_MARKED_ROLLBACK:
			return "MARKED_ROLLBACK - " + status;
		case Status.STATUS_NO_TRANSACTION:
			return "NO_TRANSACTION - " + status;
		case Status.STATUS_PREPARED:
			return "PREPARED - " + status;
		case Status.STATUS_PREPARING:
			return "PREPARING - " + status;
		case Status.STATUS_ROLLEDBACK:
			return "ROLLEDBACK - " + status;
		case Status.STATUS_ROLLING_BACK:
			return "ROLLING_BACK - " + status;
		case Status.STATUS_UNKNOWN:
			return "UNKNOWN - " + status;
		default:
			return "NOT HANDLED - " + status;
		}
	}
	
	private void runTaskOnTransactionComplete(final Runnable runnable, final boolean checkCommited) {
		TransactionManager tm;
		try {
			tm = (TransactionManager) (new InitialContext()).lookup("java:/TransactionManager");
			tm.getTransaction().registerSynchronization(new Synchronization() {
				public void beforeCompletion() {
					// logger.debug("Post-commit task before completion {}", runnable.getClass().getName());
				}

				public void afterCompletion(int status) {
					//logger.debug("Post-commit task after completion status is {} {}", transactionStatusString(status),
					//		runnable.getClass().getName());
					
					if ((!checkCommited) || (status == Status.STATUS_COMMITTED)) {
						getPostTransactionExecutor().execute(runnable);
						//logger.debug("Post-commit task submitted");
					} else {
						logger.debug("Not running post-commit task since checkCommited={} and status={}",
								checkCommited, transactionStatusString(status));
					}
				}
			});
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		} catch (RollbackException e) {
			throw new RuntimeException(e);
		} catch (SystemException e) {
			throw new RuntimeException(e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <T> T internalRunTaskInNewTransaction(Callable<T> callable) throws Exception {
		return callable.call();
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void internalRunTaskInNewTransaction(Runnable runnable) {
		runnable.run();
	}
}
