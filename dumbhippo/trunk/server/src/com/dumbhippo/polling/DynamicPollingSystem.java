package com.dumbhippo.polling;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.ThreadUtils.DaemonRunnable;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.polling.DynamicPollingSystem.TestPollingTaskFamily.FamilyType;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoadResult;
import com.dumbhippo.server.util.EJBUtil;

/** 
 *  This polling system executes polling tasks, optimizing the polling
 *  period depending on the task's rate of change.
 *  
 *  TODO: some random improvement thoughts
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
	
	private static DynamicPollingSystem instance;
	
	public static DynamicPollingSystem getInstance() {
		return instance;
	}
	
	// Kind of arbitrary but...hey.  It feels good to me.  These are prime numbers
	// closest to durations in seconds.
	// Note: when updating this, ServerStatistics should be updated to add/remove a column.
	private static final long[] pollingSetTimeSeconds = { 
		181,              // ~3 minutes 
		307,              // ~5 minutes
	    907,              // ~15 minutes
	    1801			  // ~30 minutes
		// Presently we really don't have anything we want to poll with
		// periodicity this long	    
		// 3607              // ~1 hour
		// 28807,            // ~8 hours
		// 86413,            // ~1 day
		// 259201            // ~3 days
	};
	// Used for tasks with unknown periodicity
	private static final int DEFAULT_POLLING_SET_TIME = 307;
	private static final int MAX_WORKER_COUNT = 300;
	
	// Computed from DEFAULT_POLLING_SET_TIME
	private int defaultSetIndex;
	
	// Not used anymore
	static class PollingTaskFamilyExecutionState {
	}
	
	private Map<PollingTaskFamily, PollingTaskFamilyExecutionState> taskFamilies = new HashMap<PollingTaskFamily, PollingTaskFamilyExecutionState>();
	private ExecutorService threadPool = ThreadUtils.newFixedThreadPool("polling task worker", MAX_WORKER_COUNT);
	private Set<PollingTask> globalTasks;
	
	private TaskSet[] taskSetWorkers;
	private Thread[] taskSetThreads;
	private TaskPersistenceWorker taskPersistenceWorker;
	private Thread taskPersistenceThread;
	
	public DynamicPollingSystem() {
		for (int i = 0; i < pollingSetTimeSeconds.length; i++) {
			if (pollingSetTimeSeconds[i] == DEFAULT_POLLING_SET_TIME) {
				defaultSetIndex = i;
				return;
			}
		}		
		throw new RuntimeException("Default polling set time " + DEFAULT_POLLING_SET_TIME + " is not in pollingSetTimeSeconds");
	}
	
	public void pokeTaskSet(int index) {
		taskSetWorkers[index].poke();
	}
	
	public synchronized void pushTasks(Set<PollingTask> newTasks) {
		if (globalTasks == null)
			return;
		newTasks.removeAll(globalTasks); // Filter out ones we already know about
		globalTasks.addAll(newTasks);
		for (PollingTask task : newTasks) {
			logger.debug("adding task {}", task.toString());
			if (task.getPeriodicityAverage() == -1)
				taskSetWorkers[defaultSetIndex].addTasks(Collections.singleton(task));
			else {
				// Add to the taskset closest to the average change periodicity of this item
				for (int i = 0; i < pollingSetTimeSeconds.length; i++) {
					boolean addToSet = false;
					
					if (i == pollingSetTimeSeconds.length - 1)
						addToSet = true;
					else {
						long threshold = 1000 * (pollingSetTimeSeconds[i] + pollingSetTimeSeconds[i + 1]) / 2;
						if (task.getPeriodicityAverage() < threshold)
							addToSet = true;
					}
					
					if (addToSet) {
						taskSetWorkers[i].addTasks(Collections.singleton(task));
						break;
					}
				}
			}
		}
	}
	
	// Invoked from TaskSet thread
	private List<Future<PollingTaskExecutionResult>> executeTaskSet(Set<PollingTask> tasks, long timeoutMs) throws InterruptedException {
		return ThreadUtils.invokeAll(threadPool, tasks, timeoutMs, TimeUnit.MILLISECONDS);
	}
	
	private synchronized Set<PollingTask> bumpTasks(TaskSet currentSet, Set<PollingTask> tasks, boolean slower) {
		for (int i = 0; i < taskSetWorkers.length; i++) {
			if (taskSetWorkers[i] == currentSet) {
				taskSetWorkers[slower ? i+1 : 0].addTasks(tasks);
				return tasks;
			}
		}
		throw new RuntimeException("Task set not in taskSetWorkers?");
	}
	
	private synchronized void obsoleteTasks(Set<PollingTask> tasks) {
		globalTasks.removeAll(tasks);
		taskPersistenceWorker.addObsoleteTasks(tasks);
	}
	
	private class TaskSet implements DaemonRunnable {
		private static final long BUCKET_SPACING_SECONDS = 60 * 1; // 1 minute
		
		// These fields are immutable
		private long timeout;
		private boolean hasFasterSet;
		private boolean hasSlowerSet;
		private int bucketCount;

		// Synchronize on the object to read or modify these fields
		private boolean sleeping;
		private boolean poked;
		
		// This is only ever modified from our worker thread, so the synchronization
		// guarantees of ConcurrentHashMap are sufficient
		private Map<PollingTask,Integer> tasks = new ConcurrentHashMap<PollingTask, Integer>();

		// This is synchronized on separately
		private Set<PollingTask> pendingTaskAdditions = new HashSet<PollingTask>();

		public TaskSet(long timeoutSeconds, boolean hasFasterSet, boolean hasSlowerSet) {
			this.timeout = timeoutSeconds * 1000;
			this.hasFasterSet = hasFasterSet;
			this.hasSlowerSet = hasSlowerSet;
			this.bucketCount = (int) (timeoutSeconds / BUCKET_SPACING_SECONDS);
			if (this.bucketCount <= 0)
				this.bucketCount = 1;
		}
		
		public synchronized void poke() {
			if (sleeping && !poked) {
				poked = true;
				notify();
			}
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
			if (changed)
				task.touchChanged();
			
			// If we simply looked at changed, then we'd never update the periodicity average of
			// an item that never changed. That would mean that on server restart, we'd again
			// add it to the default polling bin, which might be quite fast. So what we do
			// is that once an item reaches the slowest polling set, we update the periodicity
			// even if the item isn't changing.
			if (changed || !hasSlowerSet) {
				if (task.getPeriodicityAverage() == -1) {
					long defaultPeriodicity = task.getFamily().getDefaultPeriodicitySeconds();
					if (defaultPeriodicity != -1)
						task.setPeriodicityAverage(defaultPeriodicity*1000);
					else {
						// We initialize task periodicity to 1.5 * set timeout at first as a random guess
						task.setPeriodicityAverage((long) (timeout * 1.5));
					}
				} else {
					task.setPeriodicityAverage((long) exponentialAverage(task.getPeriodicityAverage(), timeout, 0.3));
				}
			}
		}
		
		private boolean executeTasks(int bucket) {	
			// Suck in any tasks that were added, randomly assigning
			// them to buckets
			int newTaskCount = 0;
			Random r = new Random();
			synchronized (pendingTaskAdditions) {
				for (PollingTask task : pendingTaskAdditions) {
					tasks.put(task, r.nextInt(bucketCount));
					newTaskCount++;
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
						// There used to be additional logic here similar to:
						// && task.getPeriodicityAverage() != -1 && task.getPeriodicityAverage() < (timeout * 1.1)
						// which meant tasks with a periodicity average within 10% of this task set
						// got bumped, but we decided it makes more sense for most tasks just to bump them
						// to the very fastest set if they change; tasks which change in bursts will be
						// caught better by this, and tasks which go from rarely to frequently changing
						// will more quickly adjust.
						if (result.isChanged()) {
							fasterCandidates.add(task);
						} else if ((System.currentTimeMillis() - task.getLastChange()) > timeout * 2.1) {
							// Tasks who have not changed in more than two iterations get bumped 
							// to a slower set if possible 
							slowerCandidates.add(task);
						}
					}
				}
			}
			logger.debug("bucket: " + bucket + " new: " + newTaskCount + " executed: " + currentTasks.size() + " changed: " + changedCount + " timeout: " + execTimeout.size() + " slower: " + slowerCandidates.size() + " faster: " + fasterCandidates.size() + " obsolete: " + obsolete.size() + " failed: " + failureCount);
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
		
		public void run() throws InterruptedException {
			// Wait 1 minute after server startup before beginning dynamic polling
			long currentTimeout = 1 * 60 * 1000;
			int currentBucket = 0;
			
			while (true) {
				synchronized (this) {
					sleeping = true;
					poked = false;
					
					wait(currentTimeout);
					
					sleeping = false;
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
		
		public int getTaskCount() {
			return getTasks().size();
		}
		
		public Set<PollingTask> getTasks() {
			return tasks.keySet();
		}
	}
	
	private class TaskPersistenceWorker implements DaemonRunnable {
		
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
		
		public void run() throws InterruptedException {
			
			// Wait a minute after startup to check for tasks
			Thread.sleep(1 * 60 * 1000);
			
			while (true) {
				Thread.sleep(LOAD_PERIODICITY_MS);		
				
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

						Set<PollingTask> tasks = worker.getTasks();	
						total += tasks.size();
						persister.snapshot(tasks);
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
		taskPersistenceWorker = new TaskPersistenceWorker();
		taskPersistenceThread = ThreadUtils.newDaemonThread("dynamic task set persister", taskPersistenceWorker);
		taskPersistenceThread.start();
		
		instance = this;		
		
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
	
	public int getTaskCount(int i) {
		return taskSetWorkers[i].getTaskCount();
	}	
	
	public synchronized void stopSingleton() {		
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
		
		instance = null;
		
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
		
		public long getDefaultPeriodicitySeconds() {
			return 30 * 60; // 30 minutes
		}
		
		public long rescheduleSeconds(long secs) {
			return secs;
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
