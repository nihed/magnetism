package com.dumbhippo.polling;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ScheduledExecutorCompletionService;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.ThreadUtils.DaemonRunnable;
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
public class SwarmPollingSystem extends ServiceMBeanSupport implements SwarmPollingSystemMBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SwarmPollingSystem.class);
	
	private static final int MAX_CONCURRENT_THREADS = 1000;
	
	private static SwarmPollingSystem instance;
	
	public static SwarmPollingSystem getInstance() {
		return instance;
	}
	
	private ScheduledExecutorService executor = ThreadUtils.newScheduledThreadPoolExecutor("polling task worker", MAX_CONCURRENT_THREADS);
	private ScheduledExecutorCompletionService<PollingTaskExecutionResult> taskCompletion = new ScheduledExecutorCompletionService<PollingTaskExecutionResult>(executor);
	
	private TaskCompletionWorker taskCompletionWorker;
	private Thread taskCompletionThread;
	
	private TaskPersistenceWorker taskPersistenceWorker;
	private Thread taskPersistenceThread;
	
	public SwarmPollingSystem() {
	}
	
	public void pokeTask(int family, String id) {
	}

	private synchronized void obsoleteTasks(Set<PollingTask> tasks) {
		taskPersistenceWorker.addObsoleteTasks(tasks);
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
					long defaultPeriodicity = task.getFamily().getDefaultPeriodicity();
					if (defaultPeriodicity != -1)
						task.setPeriodicityAverage(defaultPeriodicity);
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
				Future<PollingTaskExecutionResult> result = taskCompletion.take();
				
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
				for (PollingTask task : loadResult.getTasks()) {
					
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
		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		logger.info("Starting DynamicPollingSystem singleton");

		taskPersistenceWorker = new TaskPersistenceWorker();
		taskPersistenceThread = ThreadUtils.newDaemonThread("dynamic task set persister", taskPersistenceWorker);
		taskPersistenceThread.start();
		
		instance = this;
	}
	
	public synchronized void stopSingleton() {		
		logger.info("Stopping DynamicPollingSystem singleton");
		
		taskPersistenceThread.interrupt();
		try {
			taskPersistenceThread.join();
		} catch (InterruptedException e) {
			logger.warn("Interrupted trying to join thread {}", taskPersistenceThread.getName());			
		}
		taskPersistenceWorker = null;		
		
		instance = null;
		
		logger.debug("DynamicPollingSystem singleton stopped");
	}
}
