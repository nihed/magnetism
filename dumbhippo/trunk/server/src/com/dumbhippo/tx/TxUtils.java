package com.dumbhippo.tx;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;

public class TxUtils {
	protected static final Logger logger = GlobalSetup.getLogger(TxUtils.class);

	private static ExecutorService postTransactionExecutor;

	private static boolean shutdown = false;

	private static synchronized ExecutorService getPostTransactionExecutor() {
		if (shutdown)
			throw new RuntimeException(
					"getPostTransactionExecutor() called after shutdown");

		if (postTransactionExecutor == null)
			postTransactionExecutor = ThreadUtils
					.newCachedThreadPool("post-transaction tasks");

		return postTransactionExecutor;
	}

	public synchronized static void shutdown() {
		shutdown = true;

		if (postTransactionExecutor != null) {
			ThreadUtils.shutdownAndAwaitTermination(postTransactionExecutor);
			postTransactionExecutor = null;
		}
		logger.debug("TxUtils shut down");
	}

	private static TransactionManager getTransactionManager() {
		try {
			Context context = new InitialContext();
			return (TransactionManager) context
					.lookup("java:/TransactionManager");
		} catch (NamingException e) {
			throw new RuntimeException("Can't get transaction manager", e);
		}
	}

	/**
	 * Get the currently active transaction
	 * 
	 * @return the current Transaction; throws a RuntimeException if none
	 */
	public static Transaction getCurrentTransaction() {
		TransactionManager tm = TxUtils.getTransactionManager();
		Transaction transaction;
		
		try {
			transaction = tm.getTransaction();
		} catch (SystemException e) {
			throw new RuntimeException("Error getting current transaction", e);
		}
		if (transaction == null)
			throw new IllegalStateException("No current transaction");
		
		return transaction;
	}
	
	private static void runInSingleTransaction(TransactionManager tm, TxRunnable runnable) throws RetryException {

		try {
			tm.begin();
		} catch (Exception e) {
			throw new RuntimeException("Error starting transaction", e);
		}

		try {
			runnable.run();
		} catch (RuntimeException e) {
			try {
				tm.setRollbackOnly();
			} catch (Exception e2) {
				logger.error("Error marking transaction rollback only", e2);
			}
			throw new RuntimeException("Error in transaction", e);
		} finally {
			try {
				tm.getTransaction().commit();
			} catch (RollbackException e) {
				// Presumably because we set rollback-only above
			} catch (Exception e) {
				throw new RuntimeException("Error committing transaction", e);
			}
		}
	}

	private static final int RETRY_COUNT = 2;

	/**
	 * Run a task inside a new transaction. If the task throws RetryException,
	 * then the task will be retried (up to a fixed limit of times). No
	 * transaction can be currently active.
	 * 
	 * @param runnable the task to run
	 */
	public static void runInTransaction(TxRunnable runnable) {
		TransactionManager tm = getTransactionManager();
		assertNoTransaction(tm);

		int retries = RETRY_COUNT;
		while (true) {
			try {
				runInSingleTransaction(tm, runnable);
				return;
			} catch (RetryException e) {
				if (retries == 0) {
					Throwable cause = e.getCause();
					if (cause == null)
						cause = e;
					
					logger.error("Giving up after " + RETRY_COUNT	+ " retries", cause);
					ExceptionUtils.throwAsRuntimeException(cause);
				} else {
					logger.info("Retrying on constraint violation: {}", e.getMessage());
					retries--;
				}
			}
		}
	}

	/**
	 * Run a task inside a new transaction. No transaction can be currently active.
	 * 
	 * @param runnable the task to run
	 */
	public static void runInTransaction(Runnable runnable) {
		TransactionManager tm = getTransactionManager();
		assertNoTransaction(tm);

		try {
			tm.begin();
		} catch (Exception e) {
			throw new RuntimeException("Error starting transaction", e);
		}

		try {
			runnable.run();
		} catch (RuntimeException e) {
			try {
				tm.setRollbackOnly();
			} catch (Exception e2) {
				logger.error("Error marking transaction rollback only", e2);
			}
			throw new RuntimeException("Error in transaction", e);
		} finally {
			try {
				tm.getTransaction().commit();
			} catch (RollbackException e) {
				// Presumably because we set rollback-only above
			} catch (Exception e) {
				throw new RuntimeException("Error committing transaction", e);
			}
		}
	}

	private static <T> T runInSingleTransaction(TransactionManager tm, Callable<T> callable) throws Exception {
		try {
			tm.begin();
		} catch (Exception e) {
			throw new RuntimeException("Error starting transaction", e);
		}

		try {
			return callable.call();
		} catch (Exception e) {
			try {
				tm.setRollbackOnly();
			} catch (Exception e2) {
				logger.error("Error marking transaction rollback only", e2);
			}
			throw e;
		} finally {
			try {
				tm.getTransaction().commit();
			} catch (RollbackException e) {
				// Presumably because we set rollback-only above
			} catch (Exception e) {
				throw new RuntimeException("Error committing transaction", e);
			}
		}
	}

	/**
	 * Run a task inside a new transaction. If the task throws RetryException,
	 * then the task will be retried (up to a fixed limit of times). No
	 * transaction can be currently active.
	 * 
	 * @param callable the task to run
	 */
	public static <T> T runInTransaction(Callable<T> callable) throws Exception {
		TransactionManager tm = getTransactionManager();
		assertNoTransaction(tm);

		int retries = RETRY_COUNT;
		while (true) {
			try {
				return runInSingleTransaction(tm, callable);
			} catch (RetryException e) {
				if (retries == 0) {
					Throwable cause = e.getCause();
					if (cause == null)
						cause = e;

					logger.error("Giving up after " + RETRY_COUNT	+ " retries", cause);
					ExceptionUtils.throwAsRuntimeException(cause);
				} else {
					logger.info("Retrying on constraint violation: {}", e.getMessage());
					retries--;
				}
			}
		}
	}

	/**
	 * Run a task that might generate a database constraint and need to be
	 * retried for that reason. If a database constraint violation occurs,
	 * RetryException is thrown. (The retry is not actually done.)
	 * 
	 * @param callable the task to run
	 */
	public static <T> T runNeedsRetry(TxCallable<T> callable) throws RetryException {
		try {
			T t = callable.call();
			// Flush here, so that any constraint violations will be triggered at this
			// point, not at transaction commit
			DataService.flush();
			return t;
		} catch (RuntimeException e) {
			if (ExceptionUtils.hasCause(e, EJBUtil.CONSTRAINT_VIOLATION_EXCEPTIONS)) {
				throw new RetryException(e);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Run a task that might generate a database constraint and need to be
	 * retried for that reason. If a database constraint violation occurs,
	 * RetryException is thrown. (The retry is not actually done.)
	 * 
	 * @param runnnable the task to run
	 */
	public static void runNeedsRetry(TxRunnable runnable) throws RetryException {
		try {
			runnable.run();
			// Flush here, so that any constraint violations will be triggered at this
			// point, not at transaction commit
			DataService.flush();
		} catch (RuntimeException e) {
			if (ExceptionUtils.hasCause(e, EJBUtil.CONSTRAINT_VIOLATION_EXCEPTIONS)) {
				throw new RetryException(e);
			} else {
				throw e;
			}
		}
	}

	public static String transactionStatusString(int status) {
		switch (status) {
		case Status.STATUS_ACTIVE:
			return "ACTIVE-" + status;
		case Status.STATUS_COMMITTED:
			return "COMMITTED-" + status;
		case Status.STATUS_MARKED_ROLLBACK:
			return "MARKED_ROLLBACK-" + status;
		case Status.STATUS_NO_TRANSACTION:
			return "NO_TRANSACTION-" + status;
		case Status.STATUS_PREPARED:
			return "PREPARED-" + status;
		case Status.STATUS_PREPARING:
			return "PREPARING-" + status;
		case Status.STATUS_ROLLEDBACK:
			return "ROLLEDBACK-" + status;
		case Status.STATUS_ROLLING_BACK:
			return "ROLLING_BACK-" + status;
		case Status.STATUS_UNKNOWN:
			return "UNKNOWN-" + status;
		default:
			return "NOT_HANDLED-" + status;
		}
	}

	private static int getTransactionStatus(TransactionManager tm) {
		int txStatus;
		try {
			txStatus = tm.getStatus();
		} catch (SystemException e) {
			throw new RuntimeException("failed to get tx status", e);
		}
		return txStatus;
	}

	/**
	 * Check if a transaction is currently running.
	 * 
	 * @return true if a transaction is currently running
	 */
	public static boolean isTransactionActive() {
		return getTransactionStatus(getTransactionManager()) == Status.STATUS_ACTIVE;
	}

	private static void assertTransactionStatus(TransactionManager tm, int desired) {
		int txStatus = getTransactionStatus(tm);

		if (txStatus != desired) {
			throw new IllegalStateException("Unexpected tx status "
					+ transactionStatusString(txStatus) + " expecting "
					+ transactionStatusString(desired));
		}
	}

	private static void assertNoTransaction(TransactionManager tm) {
		assertTransactionStatus(tm, Status.STATUS_NO_TRANSACTION);
	}

	/**
	 * Assert that no transaction is currently running
	 * 
	 * @throws RuntimeException if a transaction is in progress
	 */
	public static void assertNoTransaction() {
		assertNoTransaction(getTransactionManager());
	}

	private static void assertHaveTransaction(TransactionManager tm) {
		assertTransactionStatus(tm, Status.STATUS_ACTIVE);
	}

	/**
	 * Assert that a transaction is currently running
	 * 
	 * @throws RuntimeException if no transaction is in progress
	 */
	public static void assertHaveTransaction() {
		assertHaveTransaction(getTransactionManager());
	}

	private static class PostTransactionTask implements Runnable,
			Synchronization {
		private Runnable runnable;

		private TxRunnable txRunnable;

		private boolean inTransaction;

		public PostTransactionTask(Runnable runnable, boolean inTransaction) {
			this.runnable = runnable;
			this.inTransaction = inTransaction;
		}

		public PostTransactionTask(TxRunnable txRunnable) {
			this.txRunnable = txRunnable;
			this.inTransaction = true;
		}

		public void run() {
			if (inTransaction) {
				if (runnable != null)
					runInTransaction(runnable);
				else
					runInTransaction(txRunnable);
			} else {
				runnable.run();
			}
		}

		public void afterCompletion(int status) {
			// logger.debug("Post-commit task after completion status is {} {}",
			// transactionStatusString(status),
			// runnable.getClass().getName());

			if (status == Status.STATUS_COMMITTED) {
				getPostTransactionExecutor().execute(this);
				// logger.debug("Post-commit task submitted");
			} else {
				logger.debug("Not running post-commit task since status={}",
						TxUtils.transactionStatusString(status));
			}
		}

		public void beforeCompletion() {
		}

	}

	private static void runOnCommit(PostTransactionTask task) {
		try {
			getCurrentTransaction().registerSynchronization(task);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		} catch (RollbackException e) {
			throw new RuntimeException(e);
		} catch (SystemException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Run a task (without a transaction) when the current transaction commits 
	 * succesfully. No action will be taken if the transaction is rolled back.
	 * The task is actually run asynchronously at some point in time after
	 * the commit. 
	 * 
	 * @param runnable the task to run
	 */
	public static void runOnCommit(Runnable runnable) {
		runOnCommit(new PostTransactionTask(runnable, false));
	}

	/**
	 * Run a task inside a transaction when the current transaction commits 
	 * succesfully. No action will be taken if the transaction is rolled back.
	 * The task is actually run asynchronously at some point in time after
	 * the commit. If the task throws RetryException, then it will be retried
	 * (up to a fixed limit of times). 
	 * 
	 * @param runnable the task to run
	 */
	public static void runInTransactionOnCommit(TxRunnable runnable) {
		runOnCommit(new PostTransactionTask(runnable));
	}

	/**
	 * Run a task inside a transaction when the current transaction commits 
	 * succesfully. No action will be taken if the transaction is rolled back.
	 * The task is actually run asynchronously at some point in time after
	 * the commit. 
	 * 
	 * @param runnable the task to run
	 */
	public static void runInTransactionOnCommit(Runnable runnable) {
		runOnCommit(new PostTransactionTask(runnable, true));
	}
}
