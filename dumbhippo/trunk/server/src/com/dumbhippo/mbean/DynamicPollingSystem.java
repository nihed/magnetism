package com.dumbhippo.mbean;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.DynamicPollingSource;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.util.EJBUtil;

/** 
 *  This polling system executes polling tasks, optimizing the polling
 *  period depending on the task's rate of change.
 *  
 *  TODO: some random improvement thoughts
 *    - Seems probable that a large number of tasks will end up clumped in the 1 and 3 day
 *      task sets.  Might want to add support for having multiple staggered instances of 
 *      the longer sets.
 *    - Add statistics to track things like how often tasks are bouncing, task deviation
 *      from expected, etc.
 */
public class DynamicPollingSystem extends ServiceMBeanSupport implements DynamicPollingSystemMBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DynamicPollingSystem.class);
	
	public interface PollingTaskFamily {
		public long getDefaultPeriodicity();
		
		public long getDefaultMaxWait();
		
		public long getMaxOutstanding();
		
		public long getMaxPerSecond();
		
		public String getName();
	}
	
	public static abstract class PollingTask implements Callable<PollingTask> {
		public static final int MAX_FAMILY_NAME_LENGTH = 20;
		public static final int MAX_TASK_ID_LENGTH = 128;

		private PollingTaskFamilyExecutionState executionState;
		
		// Exponential weighted averages, recalculated after each execution
		private long executionAverage = -1;
		private long periodicityAverage = -1;
		
		// Used to calculate periodicity
		private long lastChange = 0;
		
		// Post-execution temporary stats
		private long executionStart = -1;
		private long executionEnd = -1;
		private boolean executionChanged;		

		public PollingTask() {
		}
		
		public final void setExecutionState(PollingTaskFamilyExecutionState state) {
			this.executionState = state;
		}
		
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
		
		protected abstract boolean execute();

		public final boolean isExecutionChanged() {
			return executionChanged;
		}

		public final long getExecutionEnd() {
			return executionEnd;
		}

		public final long getExecutionStart() {
			return executionStart;
		}

		public final long getLastChange() {
			return lastChange;
		}

		public final PollingTask call() throws InterruptedException {
			executionState.awaitElgibility();
			executionStart = System.currentTimeMillis();
			try {
				executionChanged = execute();
				executionEnd = System.currentTimeMillis();
			} finally {
				executionState.notifyComplete();
			}
				
			return this;
		}
		
		public final PollingTaskFamily getFamily() {
			return executionState.getFamily();
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
			return executionStart;
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
		3607,             // ~1 hour
		28807,            // ~8 hours
		86413,            // ~1 day
		259201            // ~3 days
	};
	private static final int DEFAULT_POLLING_SET_INDEX = 2;
	
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
	private static Set<PollingTask> globalTasks;
	private static TaskSet[] taskSetWorkers;
	private static Thread[] taskSetThreads;
	private static ExecutorService threadPool = ThreadUtils.newCachedThreadPool("polling task worker");
	private static Thread taskPersistenceWorker;
	private static Thread taskInitializationThread;
	
	public DynamicPollingSystem() {
	}
	
	public synchronized void registerFamily(PollingTaskFamily family) {
		taskFamilies.put(family, new PollingTaskFamilyExecutionState(family));
	}	
	
	public synchronized void pushTasks(Set<PollingTask> newTasks) {
		if (globalTasks == null)
			return;
		newTasks.removeAll(globalTasks); // Filter out ones we already know about
		globalTasks.addAll(newTasks);
		PollingTaskPersistence persister = EJBUtil.defaultLookup(PollingTaskPersistence.class);
		persister.initializeTasks(newTasks);
		for (PollingTask task : newTasks) {
			task.setExecutionState(taskFamilies.get(task.getFamily()));
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
	
	// The invokeAll signature really should have been ? extends Callable (i think)		
	@SuppressWarnings("unchecked")
	private Set<Callable<PollingTask>> castPollingTaskSet(Set<PollingTask> tasks) {	
		return (Set) tasks;
	}
	
	// Invoked from TaskSet thread
	private synchronized List<Future<PollingTask>> executeTaskSet(Set<PollingTask> tasks, long timeoutMs) throws InterruptedException {
		return threadPool.invokeAll(castPollingTaskSet(tasks), timeoutMs, TimeUnit.MILLISECONDS);
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
	
	private class TaskSet implements Runnable {
		private long timeout;
		private boolean hasFasterSet;
		private boolean hasSlowerSet;
		private Set<PollingTask> pendingTaskAdditions = new HashSet<PollingTask>();
		private Set<PollingTask> tasks;
		
		public TaskSet(long timeoutSeconds, boolean hasLowerSet, boolean hasHigherSet) {
			this.timeout = timeoutSeconds * 1000;
			this.hasFasterSet = hasLowerSet;
			this.hasSlowerSet = hasHigherSet;
			tasks = new HashSet<PollingTask>(); 			
		}
		
		private double exponentialAverage(double prevAvg, double current, double alpha) {
			return prevAvg - (alpha * (current - prevAvg)); 
		}
		
		private void recalculateTaskStats(PollingTask task) {
			long lastExecDuration = task.getExecutionEnd() - task.getExecutionStart();
			boolean changed = task.isExecutionChanged();
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
		
		private synchronized boolean executeTasks() {	
			// Suck in any tasks that were added
			synchronized (pendingTaskAdditions) {
				tasks.addAll(pendingTaskAdditions);
				pendingTaskAdditions.clear();
			}

			List<Future<PollingTask>> results;
			// Execute the tasks using the common thread pool.  Do not let execution of any
			// of them exceed our task set timeout.
			try {
				 results = executeTaskSet(tasks, timeout);
			} catch (InterruptedException e) {
				logger.debug("task set execution interrupted");					
				return false;
			}

			// Tasks that took too long to execute - this could be a variety of reasons
			// such as a rate limit on how many of those tasks we can execute simultaneously
			// (e.g. web service limits), load on our server/bandwidth, or simply a bug
			// in the task.  Regardless, we push them all up to higher sets.
			Set<PollingTask> tooSlow = new HashSet<PollingTask>();
			// Tasks that change too slowly to be in this set
			Set<PollingTask> slowerCandidates = new HashSet<PollingTask>();
			// Tasks that are changing every time we poll, so they're good
			// candidates to go in a faster set if possible
			Set<PollingTask> fasterCandidates = new HashSet<PollingTask>();				
			
			for (Future<PollingTask> result : results) {
				PollingTask task;
			
				try {
					task = result.get(); 
					// These exceptions shouldn't happen because all results should be done
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);							
				}					
				if (result.isCancelled()) {
					tooSlow.add(task);
				} else {
					recalculateTaskStats(task);

					if (hasFasterSet && task.getPeriodicityAverage() < (timeout * 1.1)) {
						// Tasks with a periodicity average within 10% of this task set get
						// bumped to a faster set if possible
						fasterCandidates.add(task);
					} else if (hasSlowerSet && (System.currentTimeMillis() - task.getLastChange()) > timeout * 2.1) {
						// Tasks who have not changed in more than two iterations get bumped 
						// to a slower set if possible 
						slowerCandidates.add(task);
					}
				}
			}
			tasks.removeAll(bumpTasks(this, tooSlow, true));
			tasks.removeAll(bumpTasks(this, slowerCandidates, true));
			tasks.removeAll(bumpTasks(this, fasterCandidates, false));
			return true;
		}
		
		public void run() {
			// Wait 1 minute after server startup before beginning dynamic polling
			long currentTimeout = 1 * 60 * 1000;
			
			while (true) {
				try {
					Thread.sleep(currentTimeout);
				} catch (InterruptedException e) {
					break;
				}
				
				if (!executeTasks())
					break;
				
				currentTimeout = timeout;
			}
		}
		
		public void addTasks(Set<PollingTask> task) {
			synchronized (pendingTaskAdditions) {
				pendingTaskAdditions.addAll(task);
			}
		}
		
		// Should only be called when synchronized on this object
		public Set<PollingTask> getTasks() {
			return tasks;
		}
	}
	
	private class TaskPersistenceWorker implements Runnable {
		private static final long PERSISTENCE_PERIODICITY_MS = 60 * 1000; // 1 minute
		private static final long PERSISTENCE_CLEAN_PERIDODICITY_GENERATIONS = 8 * 60; // 8 hours
		
		private long generation = 0;
		
		public void run() {
			while (true) {
				try {
					Thread.sleep(PERSISTENCE_PERIODICITY_MS);
				} catch (InterruptedException e) {
					break;
				}
				
				PollingTaskPersistence persister = EJBUtil.defaultLookup(PollingTaskPersistence.class);
				for (int i = 0; i < taskSetWorkers.length; i++) {
					TaskSet worker = taskSetWorkers[i];
					synchronized (worker) {
						persister.snapshot(worker.getTasks());
					}
				}
				generation++;
				if (generation % PERSISTENCE_CLEAN_PERIDODICITY_GENERATIONS == 0) {
					persister.clean();
				}
			}
		}		
	}
	
	// We wait 1 minute in the hopes everything is booted by then
	private class TaskInitializationWorker implements Runnable {
		private static final long TASK_INITIALIZATION_WAIT_MS = 60 * 1000; // 1 minute

		public void run() {
			try {
				Thread.sleep(TASK_INITIALIZATION_WAIT_MS);
			} catch (InterruptedException e) {
				return;
			}
						
			Collection<String> beanNames;
			try {
				beanNames = EJBUtil.listLocalBeanNames();
			} catch (NamingException e) {
				logger.warn("Failed to get bean names", e);
				beanNames = Collections.emptySet();
			}
			for (String beanName : beanNames) {
				Class<?> beanIface = EJBUtil.loadLocalBeanInterface(DynamicPollingSystem.class.getClassLoader(), beanName);
				if (DynamicPollingSource.class.isAssignableFrom(beanIface)) {			
					DynamicPollingSource source = (DynamicPollingSource) EJBUtil.defaultLookup(beanIface);
					registerFamily(source.getTaskFamily());
					pushTasks(source.getTasks());
				}
			}			
		}
	}
	
	public synchronized void startSingleton() {
		logger.info("Starting DynamicPollingSystem singleton");
		globalTasks = new HashSet<PollingTask>();
		taskSetThreads = new Thread[pollingSetTimeSeconds.length];
		taskSetWorkers = new TaskSet[pollingSetTimeSeconds.length];
		for (int i = 0; i < pollingSetTimeSeconds.length; i++) {
			long time = pollingSetTimeSeconds[i];
			
			TaskSet taskSet = new TaskSet(time, i > 0, i < pollingSetTimeSeconds.length - 1);
			Thread t = ThreadUtils.newDaemonThread("dynamic task set (" + time + ")",  taskSet);

			taskSetWorkers[i] = taskSet;			
			taskSetThreads[i] = t;
		}
		
		for (Thread t : taskSetThreads) {
			t.start();
		}
		taskPersistenceWorker = ThreadUtils.newDaemonThread("dynamic task set persister", new TaskPersistenceWorker());
		taskPersistenceWorker.start();
		
		taskInitializationThread = ThreadUtils.newDaemonThread("dynamic task set initializer", new TaskInitializationWorker());
		taskInitializationThread.start();		
	}
	
	public synchronized void stopSingleton() {
		logger.info("Stopping DynamicPollingSystem singleton");
		globalTasks = null;
		
		taskInitializationThread.interrupt();
		try {
			taskInitializationThread.join();
		} catch (InterruptedException e) {
			logger.warn("Interrupted trying to join thread {}", taskInitializationThread.getName());			
		}		
		
		taskPersistenceWorker.interrupt();
		try {
			taskPersistenceWorker.join();
		} catch (InterruptedException e) {
			logger.warn("Interrupted trying to join thread {}", taskPersistenceWorker.getName());			
		}
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
}
