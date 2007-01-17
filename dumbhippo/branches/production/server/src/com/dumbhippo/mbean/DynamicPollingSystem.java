package com.dumbhippo.mbean;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.mbean.DynamicPollingSystem.TestPollingTaskFamily.FamilyType;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoadResult;
import com.dumbhippo.server.util.EJBUtil;

/** 
 *  This polling system executes polling tasks, optimizing the polling
 *  period depending on the task's rate of change.
 *  
 *  TODO: some random improvement thoughts
 *    - Seems probable that a large number of tasks will end up clumped in slower
 *      task sets.  Might want to add support for having multiple staggered instances of 
 *      the longer sets.
 *    - Perhaps increase polling on all accounts a user has specified
 *      based on their overall activity
 *    - Have the client ping us when it recognizes a URL like youtube.com/upload or the
 *      like
 *    - Add statistics to track things like how often tasks are bouncing, task deviation
 *      from expected, etc.
 */
public class DynamicPollingSystem extends ServiceMBeanSupport implements DynamicPollingSystemMBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DynamicPollingSystem.class);
	
	public interface PollingTaskFamily {
		public long getDefaultPeriodicity();
		
		public long getMaxOutstanding();
		
		public long getMaxPerSecond();
		
		public String getName();
	}
	
	private static class PollingTaskExecutionResult {
		// Post-execution temporary stats
		private long start = -1;
		private long end = -1;
		private boolean failed;
		private boolean changed;
		private boolean obsolete;
		
		public PollingTaskExecutionResult() {
			this.failed = true;
		}
		
		public PollingTaskExecutionResult(long executionStart, long executionEnd, boolean executionChanged, boolean obsolete) {
			this.start = executionStart;
			this.end = executionEnd;
			this.changed = executionChanged;
			this.obsolete = obsolete;
			this.failed = false;
		}

		public boolean isChanged() {
			return changed;
		}

		public long getEnd() {
			return end;
		}

		public long getStart() {
			return start;
		}

		public boolean isObsolete() {
			return obsolete;
		}

		public boolean isFailed() {
			return failed;
		}			
	}
	
	/**
	 * This exception should be thrown for executions which result in some
	 * sort of (potentially) transient problem.  For example, an error opening a TCP
	 * connection to a web service.  It should not be used to wrap just any exception
	 * from the code because exceptions wrapped in this way will not have their stack
	 * traces printed to the log.  Using this exception requires the task invoke
	 * other methods or code which provides for this distinction.
	 * 
	 * This class intentionally omits the Exception(String) constructor because it
	 * is assumed that it will be used solely to wrap an existing throwable from
	 * an error condition as opposed to being a primary thrown exception.
	 * 
	 * @author walters
	 */
	public static class PollingTaskNormalExecutionException extends Exception {
		private static final long serialVersionUID = 1L;

		public PollingTaskNormalExecutionException(String message, Throwable cause) {
			super(message, cause);
		}

		public PollingTaskNormalExecutionException(Throwable cause) {
			super(cause);
		}
	}
	
	public static abstract class PollingTask implements Callable<PollingTaskExecutionResult> {
		public static final int MAX_FAMILY_NAME_LENGTH = 20;
		public static final int MAX_TASK_ID_LENGTH = 128;

		private PollingTaskFamilyExecutionState executionState;
		
		// Exponential weighted averages, recalculated after each execution
		private long executionAverage = -1;
		private long periodicityAverage = -1;
		
		// Used to calculate periodicity
		private long lastChange = 0;
		
		// Recorded in the database
		private long lastExecuted = -1;
		
		// Whether or not we have been executed since the last persist
		private boolean dirty;

		public PollingTask() {
		}
		
		private final void setExecutionState(PollingTaskFamilyExecutionState state) {
			this.executionState = state;
		}
		
		public abstract PollingTaskFamily getFamily();		
		public abstract String getIdentifier();

		@Override
		public final boolean equals(Object obj) {
			if (!(obj instanceof PollingTask))
				return false;
			PollingTask task = (PollingTask) obj;
			return task.getFamily() == getFamily() && task.getIdentifier().equals(getIdentifier());
		}

		@Override
		public final int hashCode() {
			return getIdentifier().hashCode();
		}
		
		protected static class PollResult {
			public boolean executionChanged;
			public boolean obsolete;
			public PollResult(boolean executionChanged, boolean obsolete) {
				this.executionChanged = executionChanged;
				this.obsolete = obsolete;
			}
		}
		
		protected abstract PollResult execute() throws Exception;

		public final long getLastChange() {
			return lastChange;
		}

		public final PollingTaskExecutionResult call() throws Exception {
			executionState.awaitElgibility();
			long executionStart = System.currentTimeMillis();
			// Update stuff saved to database
			synchronized (this) {
				this.lastExecuted = executionStart;
				this.dirty = true;
			}
			boolean executionChanged;
			boolean obsolete;
			long executionEnd;
			try {
				PollResult result = execute();
				executionChanged = result.executionChanged;
				obsolete = result.obsolete;
				executionEnd = System.currentTimeMillis();
			} catch (PollingTaskNormalExecutionException e) {
				logger.info("Transient exception: {}", e.getMessage());
				return new PollingTaskExecutionResult();				
			} catch (Exception e) {
				logger.warn("Execution of polling task failed", e);
				return new PollingTaskExecutionResult();				
			} finally {
				executionState.notifyComplete();
			}
				
			return new PollingTaskExecutionResult(executionStart, executionEnd, executionChanged, obsolete);
		}

		public final long getExecutionAverage() {
			return executionAverage;
		}

		public final void setExecutionAverage(long executionAverage) {
			this.executionAverage = executionAverage;
		}

		public final long getPeriodicityAverage() {
			return periodicityAverage;
		}
		
		public final void setPeriodicityAverage(long periodicityAverage) {
			this.periodicityAverage = periodicityAverage;
		}

		public final void touchChanged() {
			this.lastChange = System.currentTimeMillis();
		}

		public long getLastExecuted() {
			return lastExecuted;
		}
		
		@Override
		public final String toString() {
			return getFamily().getName() + "-" + getIdentifier();
		}
		
		// Should be invoked while synchronized		
		public void flagClean() {
			dirty = false;
		}

		// Should be invoked while synchronized
		public boolean isDirty() {
			return dirty;
		}
	}
	
	// Kind of arbitrary but...hey.  It feels good to me.  These are prime numbers
	// closest to durations in seconds.
	private static final long[] pollingSetTimeSeconds = { 
		7,                // 7 seconds
		23,               // 23 seconds
		61,               // ~1 minute 
		307,              // ~5 minutes
	    907,              // ~15 minutes
	    1801,			  // ~30 minutes
		3607              // ~1 hour
		// Presently we really don't have anything we want to poll with
		// periodicity this long
		// 28807,            // ~8 hours
		// 86413,            // ~1 day
		// 259201            // ~3 days
	};
	private static final int DEFAULT_POLLING_SET_INDEX = 2;
	private static final int MAX_WORKER_COUNT = 300;
	
	private static class PollingTaskFamilyExecutionState {
		private PollingTaskFamily family;
		private long currentTaskCount;
		
		public PollingTaskFamilyExecutionState(PollingTaskFamily family) {
			this.family = family;
			currentTaskCount = 0;
		}
		
		public PollingTaskFamily getFamily() {
			return family;
		}

		// Invoked from task worker threads
		public void awaitElgibility() throws InterruptedException {
			synchronized (this) {
				while ((family.getMaxOutstanding() > 0 &&
					    currentTaskCount >= family.getMaxOutstanding()))
					wait();
				currentTaskCount++;		
			}
		}
		
		// Invoked from task worker threads
		private void notifyComplete() {
			synchronized (this) {
		 		currentTaskCount--;
				notifyAll();			
			}
		}		
	}
	
	private static Map<PollingTaskFamily, PollingTaskFamilyExecutionState> taskFamilies = new HashMap<PollingTaskFamily, PollingTaskFamilyExecutionState>();
	private static ExecutorService threadPool = ThreadUtils.newFixedThreadPool("polling task worker", MAX_WORKER_COUNT);
	private static Set<PollingTask> globalTasks;
	
	private static TaskSet[] taskSetWorkers;
	private static Thread[] taskSetThreads;
	private static TaskPersistenceWorker taskPersistenceWorker;
	private static Thread taskPersistenceThread;
	
	public DynamicPollingSystem() {
	}
	
	public synchronized void pokeTaskSet(int index) {
		TaskSet set = taskSetWorkers[index];
		synchronized (set) {
			boolean interrupt = set.poke();
			if (interrupt)
				taskSetThreads[index].interrupt();
		}
	}	
	
	private PollingTaskFamilyExecutionState getExecution(PollingTaskFamily family) {
		PollingTaskFamilyExecutionState execution = taskFamilies.get(family);
		if (execution == null) {
			execution = new PollingTaskFamilyExecutionState(family);
			taskFamilies.put(family, execution);
		}
		return execution;
	}
	
	public synchronized void pushTasks(Set<PollingTask> newTasks) {
		if (globalTasks == null)
			return;
		newTasks.removeAll(globalTasks); // Filter out ones we already know about
		globalTasks.addAll(newTasks);
		for (PollingTask task : newTasks) {
			logger.debug("adding task {}", task.toString());
			task.setExecutionState(getExecution(task.getFamily()));
			if (task.getPeriodicityAverage() == -1)
				taskSetWorkers[DEFAULT_POLLING_SET_INDEX].addTasks(Collections.singleton(task));
			else {
				for (int i = 0; i < pollingSetTimeSeconds.length; i++) {
					long periodicity = pollingSetTimeSeconds[i] * 1000;
					if (i == pollingSetTimeSeconds.length - 1
						|| (task.getPeriodicityAverage() > (periodicity * 1.2)
							&& task.getPeriodicityAverage() < pollingSetTimeSeconds[i+1] * 1000)) {
						taskSetWorkers[i].addTasks(Collections.singleton(task));					
					}
				}
			}
		}
	}
	
	// Invoked from TaskSet thread
	private synchronized List<Future<PollingTaskExecutionResult>> executeTaskSet(Set<PollingTask> tasks, long timeoutMs) throws InterruptedException {
		return ThreadUtils.invokeAll(threadPool, tasks, timeoutMs, TimeUnit.MILLISECONDS);
	}
	
	private synchronized Set<PollingTask> bumpTasks(TaskSet currentSet, Set<PollingTask> tasks, boolean slower) {
		for (int i = 0; i < taskSetWorkers.length; i++) {
			if (taskSetWorkers[i] == currentSet) {
				taskSetWorkers[i + (slower ? 1 : -1)].addTasks(tasks);
			}
		}
		// Currently we assume each set has no limit - later we want to allow a task to accept only a subset
		return tasks;
	}
	
	private synchronized void obsoleteTasks(Set<PollingTask> tasks) {
		globalTasks.removeAll(tasks);
		taskPersistenceWorker.addObsoleteTasks(tasks);
	}
	
	private class TaskSet implements Runnable {
		private static final long BUCKET_SPACING_SECONDS = 60 * 4; // 4 minutes
		
		private long timeout;
		private boolean hasFasterSet;
		private boolean hasSlowerSet;
		private Set<PollingTask> pendingTaskAdditions = new HashSet<PollingTask>();
		private Map<PollingTask,Integer> tasks;
		private boolean poked;
		private boolean sleeping;
		private int bucketCount;
		private int currentBucket;
		
		public TaskSet(long timeoutSeconds, boolean hasFasterSet, boolean hasSlowerSet) {
			this.timeout = timeoutSeconds * 1000;
			this.hasFasterSet = hasFasterSet;
			this.hasSlowerSet = hasSlowerSet;
			tasks = new HashMap<PollingTask, Integer>();
			this.bucketCount = (int) (timeoutSeconds / BUCKET_SPACING_SECONDS);
			if (this.bucketCount <= 0)
				this.bucketCount = 1;
			currentBucket = 0;
		}
		
		// Must be called while synchronized
		public boolean poke() {
			poked = true;
			return sleeping;
		}
		
		private double exponentialAverage(double prevAvg, double current, double alpha) {
			return prevAvg + (alpha * (current - prevAvg)); 
		}
		
		private void recalculateTaskStats(PollingTask task, PollingTaskExecutionResult result) {
			long lastExecDuration = result.getEnd() - result.getStart();
			boolean changed = result.isChanged();
			if (task.getExecutionAverage() == -1)
				task.setExecutionAverage(lastExecDuration);
			else
				task.setExecutionAverage((long) exponentialAverage(task.getExecutionAverage(), lastExecDuration, 0.1));
			if (changed) {
				task.touchChanged();
				if (task.getPeriodicityAverage() == -1) {
					long defaultPeriodicity = task.getFamily().getDefaultPeriodicity();
					if (defaultPeriodicity != -1)
						task.setPeriodicityAverage(defaultPeriodicity);
					else {
						// We initialize task periodicity to 1.5 * set timeout at first as a random guess
						task.setPeriodicityAverage((long) (timeout * 1.5));
					}
				} else {
					task.setPeriodicityAverage((long) exponentialAverage(task.getPeriodicityAverage(), timeout, 0.3));
				}
			}
		}
		
		private synchronized boolean executeTasks(int bucket) {	
			// Suck in any tasks that were added, randomly assigning
			// them to buckets
			Random r = new Random();
			synchronized (pendingTaskAdditions) {
				int bucketNum = r.nextInt(bucketCount);
				for (PollingTask task : pendingTaskAdditions) {
					tasks.put(task, bucketNum);
				}
				pendingTaskAdditions.clear();
			}
			
			Set<PollingTask> currentTasks = new HashSet<PollingTask>();
			for (Map.Entry<PollingTask, Integer> entry : tasks.entrySet()) {
				if (entry.getValue() == bucket) {
					currentTasks.add(entry.getKey());
				}
			}
			
			// Basically just avoid the log message and some overhead
			if (currentTasks.isEmpty())
				return true;

			List<Future<PollingTaskExecutionResult>> results;
			// Execute the tasks using the common thread pool.  Do not let execution of any
			// of them exceed our task set timeout.
			try {
				 results = executeTaskSet(currentTasks, timeout);
			} catch (InterruptedException e) {
				logger.debug("task set execution interrupted");					
				return false;
			}

			// Tasks that took too long to execute - this could be a variety of reasons
			// such as a rate limit on how many of those tasks we can execute simultaneously
			// (e.g. web service limits), load on our server/bandwidth, or simply a bug
			// in the task.  Regardless, we push them all up to higher sets.
			Set<PollingTask> execTimeout = new HashSet<PollingTask>();
			// Tasks that change too slowly to be in this set
			Set<PollingTask> slowerCandidates = new HashSet<PollingTask>();
			// Tasks that are changing every time we poll, so they're good
			// candidates to go in a faster set if possible
			Set<PollingTask> fasterCandidates = new HashSet<PollingTask>();
			// Tasks whose implementation says they are no longer in use
			Set<PollingTask> obsolete = new HashSet<PollingTask>();
		
			Iterator<Future<PollingTaskExecutionResult>> resultsIterator = results.iterator();
			Iterator<PollingTask> tasksIterator = currentTasks.iterator();
			
			int failureCount = 0;
			int changedCount = 0;
			
			while (resultsIterator.hasNext()) {
				Future<PollingTaskExecutionResult> resultFuture = resultsIterator.next();
				PollingTask task = tasksIterator.next();
							
				if (resultFuture.isCancelled()) {
					execTimeout.add(task);
				} else {
					PollingTaskExecutionResult result;
					try {
						result = resultFuture.get();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
					if (result.isFailed()) {
						failureCount++;
					} else if (result.isObsolete()) {
						obsolete.add(task);
					} else {				
						recalculateTaskStats(task, result);
					}
					
					if (result.isChanged())
						changedCount++;

					// Both successful and failed tasks can be bumped to different sets, but we 
					// just delete obsolete ones.
					if (!result.isObsolete()) {
						if (result.isChanged() && task.getPeriodicityAverage() != -1 && task.getPeriodicityAverage() < (timeout * 1.1)) {
							// Tasks which changed this round, and with a periodicity average 
							// within 10% of this task set get bumped to a faster set if possible
							fasterCandidates.add(task);
						} else if ((System.currentTimeMillis() - task.getLastChange()) > timeout * 2.1) {
							// Tasks who have not changed in more than two iterations get bumped 
							// to a slower set if possible 
							slowerCandidates.add(task);
						}
					}
				}
			}
			logger.debug("bucket: " + bucket + " executed: " + tasks.size() + " changed: " + changedCount + " timeout: " + execTimeout.size() + " slower: " + slowerCandidates.size() + " faster: " + fasterCandidates.size() + " obsolete: " + obsolete.size() + " failed: " + failureCount);
			if (hasSlowerSet) {
				for (PollingTask task : bumpTasks(this, execTimeout, true)) {
					tasks.remove(task);
				}
			}
			if (hasSlowerSet) {
				for (PollingTask task : bumpTasks(this, slowerCandidates, true)) {
					tasks.remove(task);
				}
			}
			if (hasFasterSet) {
				for (PollingTask task : bumpTasks(this, fasterCandidates, false)) {
					tasks.remove(task);
				}
			}
			obsoleteTasks(obsolete);
			for (PollingTask task : obsolete) {
				tasks.remove(task);
			}
			return true;
		}
		
		public void run() {
			// Wait 1 minute after server startup before beginning dynamic polling
			long currentTimeout = 1 * 60 * 1000;
			
			while (true) {
				try {
					synchronized (this) {
						sleeping = true;
					}
					Thread.sleep(currentTimeout);
				} catch (InterruptedException e) {
					synchronized (this) {
						if (poked) {
							poked = false;
						} else {
							break;
						}
					}
				} finally {
					synchronized (this) {
						sleeping = false;
					}
				}
				
				if (!executeTasks(currentBucket))
					break;
				
				currentTimeout = Math.min(timeout, BUCKET_SPACING_SECONDS*1000);
				currentBucket = (currentBucket + 1) % bucketCount;
			}
		}
		
		public void addTasks(Set<PollingTask> task) {
			synchronized (pendingTaskAdditions) {
				pendingTaskAdditions.addAll(task);
			}
		}
		
		// Should only be called when synchronized on this object
		public Set<PollingTask> getTasks() {
			return tasks.keySet();
		}
	}
	
	private class TaskPersistenceWorker implements Runnable {
		
		// Load every 5 seconds, save every minute
		private static final long LOAD_PERIODICITY_MS = 5 * 1000;
		private static final long SAVE_PERIODICITY_COUNT = 12;
		
		private long lastSeenTaskDatabaseId = -1;
		private long loadCount = 0;
		
		private Set<PollingTask> pendingObsolete = new HashSet<PollingTask>();
		
		public void addObsoleteTasks(Set<PollingTask> tasks) {
			synchronized (pendingObsolete) {
				pendingObsolete.addAll(tasks);
			}
		}
		
		public void run() {
			while (true) {
				try {
					Thread.sleep(LOAD_PERIODICITY_MS);
				} catch (InterruptedException e) {
					break;
				}			
				
				PollingTaskPersistence persister = EJBUtil.defaultLookup(PollingTaskPersistence.class);

				PollingTaskLoadResult loadResult = persister.loadNewTasks(lastSeenTaskDatabaseId);
				pushTasks(loadResult.getTasks());
				lastSeenTaskDatabaseId = loadResult.getLastDbId();
				long totalLoaded = loadResult.getTasks().size();
				
				loadCount++;
				if (loadCount == SAVE_PERIODICITY_COUNT) {
					loadCount = 0;
					int total = 0;
					for (int i = 0; i < taskSetWorkers.length; i++) {
						TaskSet worker = taskSetWorkers[i];
						synchronized (worker) {
							Set<PollingTask> tasks = worker.getTasks();	
							total += tasks.size();
							persister.snapshot(tasks);
						}
					}
					Set<PollingTask> obsolete = new HashSet<PollingTask>();				
					synchronized (pendingObsolete) {
						obsolete.addAll(pendingObsolete);
						pendingObsolete.clear();
					}				
					persister.clean(obsolete);
					logger.debug("new: " + totalLoaded  + " saved: " + total + " obsoleted: " + obsolete.size());
				} else {
					if (totalLoaded > 0)
						logger.debug("new: " + totalLoaded);
				}
			}
		}		
	}
	
	public synchronized void startSingleton() {
		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		if (!config.isFeatureEnabled("pollingTask")) {
			logger.info("Dynamic polling not enabled; ignoring startup");
			return;
		}
		logger.info("Starting DynamicPollingSystem singleton");
		globalTasks = new HashSet<PollingTask>();
		taskSetThreads = new Thread[pollingSetTimeSeconds.length];
		taskSetWorkers = new TaskSet[pollingSetTimeSeconds.length];
		for (int i = 0; i < pollingSetTimeSeconds.length; i++) {
			long time = pollingSetTimeSeconds[i];
			
			TaskSet taskSet = new TaskSet(time, i > 0, i < pollingSetTimeSeconds.length - 2);
			Thread t = ThreadUtils.newDaemonThread("dynamic task set (" + time + ")",  taskSet);

			taskSetWorkers[i] = taskSet;			
			taskSetThreads[i] = t;
		}
		
		for (Thread t : taskSetThreads) {
			t.start();
		}
		taskPersistenceWorker = new TaskPersistenceWorker();
		taskPersistenceThread = ThreadUtils.newDaemonThread("dynamic task set persister", taskPersistenceWorker);
		taskPersistenceThread.start();
		
		if (config.isFeatureEnabled("pollingTaskDebug")) {
			logger.warn("pollingTask debug enabled; injecting test tasks");			
			TestPollingTaskFamily[] families = new TestPollingTaskFamily[TestPollingTaskFamily.FamilyType.values().length];
			for (int i = 0; i < families.length; i++) {
				families[i] = new TestPollingTaskFamily(FamilyType.values()[i]);
			}
			Set<PollingTask> testTasks = new HashSet<PollingTask>();
			Random r = new Random();
			for (int i = 0; i < 20; i++) {
				testTasks.add(new TestPollingTask(families[r.nextInt(families.length)]));
			}
			pushTasks(testTasks);
		}
	}
	
	public synchronized void stopSingleton() {
		Configuration config = EJBUtil.defaultLookup(Configuration.class);		
		if (!config.isFeatureEnabled("pollingTask")) {
			logger.info("Dynamic polling not enabled; ignoring shutdown");
			return;
		}		
		logger.info("Stopping DynamicPollingSystem singleton");
		globalTasks = null;
		
		taskPersistenceThread.interrupt();
		try {
			taskPersistenceThread.join();
		} catch (InterruptedException e) {
			logger.warn("Interrupted trying to join thread {}", taskPersistenceThread.getName());			
		}
		taskPersistenceWorker = null;		
		for (Thread t : taskSetThreads) {
			logger.debug(" interrupting {}", t.getName());
			t.interrupt();
		}
		for (Thread t : taskSetThreads) {
			try {
				logger.debug(" joining {}", t.getName());
				t.join();
			} catch (InterruptedException e) {
				logger.warn("Interrupted trying to join thread {}", t.getName());
			}
		}
		threadPool.shutdownNow();
		taskSetThreads = null;
		taskSetWorkers = null;
		taskFamilies.clear();
		logger.debug("DynamicPollingSystem singleton stopped");
	}
	
	protected static class TestPollingTaskFamily implements PollingTaskFamily {

		public enum FamilyType {
			SLOW(6000, 0.03),
			MEDIUM(1000, 0.03),
			FAST(150, 0.03);
		
			private long executionAverage;
			private double obsoletionFrequency;
			
			FamilyType(long avg, double obsoletionFrequency) {
				this.executionAverage = avg;
				this.obsoletionFrequency = obsoletionFrequency;
			}

			public double getObsoletionFrequency() {
				return obsoletionFrequency;
			}

			public long getExecutionAverage() {
				return executionAverage;
			}			
		}
		
		private FamilyType family;
		
		public TestPollingTaskFamily(FamilyType type) {
			this.family = type;
		}
		
		public long getDefaultPeriodicity() {
			return -1;
		}

		public long getMaxOutstanding() {
			return 3;
		}

		public long getMaxPerSecond() {
			return 5;
		}

		public String getName() {
			return "TestFamily-" + family.name();
		}
		
		public long getTestExecutionAverage() {
			return family.getExecutionAverage();
		}
		
		public double getTestObsoletionFrequency() {
			return family.getObsoletionFrequency();
		}
	}
	
	protected static class TestPollingTask extends PollingTask {
		public enum TaskType {
			VERY_SLOW(0.02),
			SLOW(0.5),
			MEDIUM(0.1),
			FAST(0.3),
			VERY_FAST(0.6);
			
			private double frequency;
			
			TaskType(double freq) {
				this.frequency = freq;
			}

			public double getFrequency() {
				return frequency;
			}
		}
		
		private TaskType type;
		private String id;
		private PollingTaskFamily family;
		
		public TestPollingTask(PollingTaskFamily family) {
			this(family, TaskType.values()[new Random().nextInt(TaskType.values().length)]);
		}
		
		public TestPollingTask(PollingTaskFamily family, TaskType type) {
			this.family = family;
			this.type = type;
			this.id = Guid.createNew().toString();
		}
		
		@Override
		protected PollResult execute() throws InterruptedException {
			TestPollingTaskFamily family = (TestPollingTaskFamily) getFamily();
			long execAverage = family.getTestExecutionAverage();
			double variance = 0.5;
			Random r = new Random();
			Thread.sleep(execAverage + (long) (execAverage * variance * (r.nextBoolean() ? -1 : 1)));
			return new PollResult(r.nextDouble() < type.getFrequency(), r.nextDouble() < family.getTestObsoletionFrequency());
		}

		@Override
		public String getIdentifier() {
			return id;
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}
	}
}
