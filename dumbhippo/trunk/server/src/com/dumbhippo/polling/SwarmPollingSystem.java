package com.dumbhippo.polling;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ScheduledExecutorCompletionService;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.ThreadUtils.DaemonRunnable;
import com.dumbhippo.persistence.PollingTaskFamilyType;
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
public class SwarmPollingSystem extends ServiceMBeanSupport implements SwarmPollingSystemMBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SwarmPollingSystem.class);
	
	private static final int MIN_TASK_PERIODICITY_SEC = 7 * 60; // 7 minutes
	private static final int MAX_TASK_PERIODICITY_SEC = 2 * 60 * 60; // 2 hours
	
	// How quickly to reschedule a task if it's changed
	private static final int TASK_CHANGE_RESCHEDULE_SEC = 4 * 60; // 4 minutes
	
	private static final int MAX_CONCURRENT_THREADS = 500;
	
	private static SwarmPollingSystem instance;
	
	public static SwarmPollingSystem getInstance() {
		return instance;
	}
	
	private Set<PollingTask> tasks = new HashSet<PollingTask>();
	private ScheduledExecutorService executor = ThreadUtils.newScheduledThreadPoolExecutor("polling task worker", MAX_CONCURRENT_THREADS);
	private ScheduledExecutorCompletionService<PollingTaskExecutionResult> taskCompletion = new ScheduledExecutorCompletionService<PollingTaskExecutionResult>(executor);
	
	private TaskCompletionWorker taskCompletionWorker;
	private Thread taskCompletionThread;
	
	private TaskPersistenceWorker taskPersistenceWorker;
	private Thread taskPersistenceThread;
	
	public SwarmPollingSystem() {
	}
	
	public void executeTaskNow(PollingTaskFamilyType family, String id) throws Exception {
		String expectedName = family.name();
		for (PollingTask task : tasks) {
			if (task.getFamily().getName().equals(expectedName) && 
					task.getIdentifier().equals(id)) {
				task.call();
			}
		}
	}

	private synchronized void obsoleteTasks(Set<PollingTask> obsoleteTasks) {
		tasks.removeAll(obsoleteTasks);
		taskPersistenceWorker.addObsoleteTasks(obsoleteTasks);
	}
	
	public int getExecutingTaskCount() {
		// Kind of a hack but it seems cleaner than declaring executor as a ThreadPoolExecutor
		// everywhere
		if (executor == null || !(executor instanceof ThreadPoolExecutor))
			return 0;
		return ((ThreadPoolExecutor) executor).getActiveCount();
	}
	
	public int getPendingTaskCount() {
		if (executor == null || !(executor instanceof ThreadPoolExecutor))
			return 0;
		return ((ThreadPoolExecutor) executor).getQueue().size();		
	}
	
	private class TaskCompletionWorker implements DaemonRunnable {
	
		public TaskCompletionWorker() {
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
				if (task.getPeriodicityAverage() == -1) {
					long defaultPeriodicity = task.getFamily().getDefaultPeriodicitySeconds();
					if (defaultPeriodicity != -1)
						task.setPeriodicityAverage(defaultPeriodicity*1000);
					else {
						// We initialize task periodicity to 30 minutes as a guess, but task families
						// should really be picking their own
						task.setPeriodicityAverage(30 * 60 * 1000);
					}
				} else if (task.getLastChange() > 0) {
					long periodicity = System.currentTimeMillis() - task.getLastChange();
					task.setPeriodicityAverage((long) exponentialAverage(task.getPeriodicityAverage(), 
							                                             periodicity, 
							                                             0.3));
				}
				task.touchChanged();
			}	
		}
		
		public void run() throws InterruptedException {		
			while (true) {
				Future<PollingTaskExecutionResult> futureResult = taskCompletion.take();
				PollingTask task;
				PollingTaskExecutionResult result;				
				try {
					result = futureResult.get();
					task = result.getTask();
				} catch (ExecutionException e) {
					// We catch Exception inside PollingTask.call, so if we get an exception
					// here something went seriously wrong
					throw new RuntimeException(e);
				} catch (CancellationException e) {
					// Ignore cancelled tasks
					continue;
				}
				if (result.isObsolete()) {
					obsoleteTasks(Collections.singleton(task));
				} else {
					recalculateTaskStats(task, result);
					long periodicityScheduleSecs;
					// Fallback wait is slightly faster than the calculated average periodicity
					periodicityScheduleSecs = (long) (0.75 * (task.getPeriodicityAverage()/1000));
					long scheduleSecs;					
					if (result.isChanged()) {
						// If we changed recently, start polling more frequently, decaying exponentially
						// to a frequency calculated from the periodicity.
						if (task.getRecentChangeDecayFactor() == -1)
							task.setRecentChangeDecayFactor(1);
						else
							task.setRecentChangeDecayFactor(task.getRecentChangeDecayFactor() + 1);
						scheduleSecs = TASK_CHANGE_RESCHEDULE_SEC * task.getRecentChangeDecayFactor();
					} else {
						scheduleSecs = periodicityScheduleSecs;
					}
					if (scheduleSecs > periodicityScheduleSecs)
						scheduleSecs = periodicityScheduleSecs;
					scheduleSecs = task.rescheduleSeconds(result, scheduleSecs);
					scheduleSecs = Math.max(scheduleSecs, MIN_TASK_PERIODICITY_SEC);
					scheduleSecs = Math.min(scheduleSecs, MAX_TASK_PERIODICITY_SEC);
					int scheduleSecsRange = (int) (scheduleSecs * 0.1); 
					int scheduleSecsOffset = new Random().nextInt(scheduleSecsRange * 2) - scheduleSecsRange;
					taskCompletion.schedule(task, scheduleSecs+scheduleSecsOffset, TimeUnit.SECONDS);
				}
			}
		}
	}
	
	private class TaskPersistenceWorker implements DaemonRunnable {
		
		// Load every 5 seconds, save every minute
		private static final long LOAD_PERIODICITY_MS = 5 * 1000;
		private static final long SAVE_PERIODICITY_COUNT = 12;
		
		private long lastSeenTaskDatabaseId = -1;
		private long loadCount = 0;
		
		private Set<PollingTask> pendingObsolete = new HashSet<PollingTask>();
		private Set<PollingTask> dirtyTasks = new HashSet<PollingTask>();
		
		public void addObsoleteTasks(Set<PollingTask> tasks) {
			synchronized (pendingObsolete) {
				pendingObsolete.addAll(tasks);
			}
		}
		
		public void addDirtyTasks(Set<PollingTask> tasks) {
			synchronized (dirtyTasks) {
				dirtyTasks.addAll(tasks);
			}
		}
		
		public void run() throws InterruptedException {
			
			logger.info("Waiting 1 minute to load tasks");
			// Wait a minute after startup to check for tasks
			Thread.sleep(1 * 60 * 1000);
			
			while (true) {
				Thread.sleep(LOAD_PERIODICITY_MS);		
				
				PollingTaskPersistence persister = EJBUtil.defaultLookup(PollingTaskPersistence.class);

				PollingTaskLoadResult loadResult = persister.loadNewTasks(lastSeenTaskDatabaseId);
				int i = 0;
				Random r = new Random();
				for (PollingTask task : loadResult.getTasks()) {
					tasks.add(task);
					long periodicitySecs = task.getFamily().getDefaultPeriodicitySeconds();
					if (periodicitySecs < 0)
						periodicitySecs = MAX_TASK_PERIODICITY_SEC;
					int offsetSecs = r.nextInt((int) periodicitySecs);
					taskCompletion.schedule(task, offsetSecs, TimeUnit.SECONDS);
					i++;
				}
				lastSeenTaskDatabaseId = loadResult.getLastDbId();
				long totalLoaded = loadResult.getTasks().size();
				
				loadCount++;
				if (loadCount == SAVE_PERIODICITY_COUNT) {
					loadCount = 0;
					int total = 0;
					synchronized (dirtyTasks) {
						persister.snapshot(dirtyTasks);
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
		logger.info("Starting SwarmPollingSystem singleton");

		taskPersistenceWorker = new TaskPersistenceWorker();
		taskPersistenceThread = ThreadUtils.newDaemonThread("dynamic task persister", taskPersistenceWorker);
		taskPersistenceThread.start();
		
		taskCompletionWorker = new TaskCompletionWorker();
		taskCompletionThread = ThreadUtils.newDaemonThread("dynamic task completion", taskCompletionWorker);
		taskCompletionThread.start();
		
		instance = this;
	}
	
	public synchronized void stopSingleton() {		
		logger.info("Stopping SwarmPollingSystem singleton");
		
		taskPersistenceThread.interrupt();
		try {
			taskPersistenceThread.join();
		} catch (InterruptedException e) {
			logger.warn("Interrupted trying to join thread {}", taskPersistenceThread.getName());			
		}
		taskPersistenceWorker = null;		
		
		instance = null;
		
		logger.debug("SwarmPollingSystem singleton stopped");
	}
}
