package com.dumbhippo.mbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.ThreadUtils.DaemonRunnable;
import com.dumbhippo.server.applications.ApplicationUpdatePeriodicJob;

/** The "cron job" mbean - runs on one node of the cluster.
 *  To add a "cron job" create a subclass of PeriodicJob and put the 
 *  class in the static list below. Each job has its own thread that 
 *  sleeps when the job is not active.
 */
public class PeriodicJobRunner extends ServiceMBeanSupport implements PeriodicJobRunnerMBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(PeriodicJobRunner.class);
	
	private static final Class<?>[] jobClasses = {
		FacebookTrackerPeriodicJob.class,
		ApplicationUpdatePeriodicJob.class
	};
	
	private List<Thread> threads;
	private Map<Class<?>, PeriodicJobRunnable> jobs;
	
	public PeriodicJobRunner() {
		threads = new ArrayList<Thread>();
		jobs = new HashMap<Class<?>, PeriodicJobRunnable>();
	}
	
	public synchronized void startSingleton() {
		
		// CAUTION this runs before our entire application is loaded - some session beans 
		// might not be available yet for example. Right now, we rely on all the jobs 
		// to have an interval longer than the time until we're fully started up, which 
		// is pretty sketchy.
		
		logger.info("Starting PeriodicUpdater singleton");
		
		for (Class<?> jobClass : jobClasses) {
			PeriodicJob job;
			try {
				job = (PeriodicJob) jobClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			PeriodicJobRunnable runnable = new PeriodicJobRunnable(job);
			Thread t = ThreadUtils.newDaemonThread("periodic " + job.getName(), runnable);
			threads.add(t);
			jobs.put(jobClass, runnable);
		}
		
		for (Thread t : threads) {
			t.start();
		}
	}
	
	public synchronized void stopSingleton() {
		logger.info("Stopping PeriodicUpdater singleton");
		jobs.clear();
		for (Thread t : threads) {
			logger.debug(" interrupting {}", t.getName());
			t.interrupt();
		}
		for (Thread t : threads) {
			try {
				logger.debug(" joining {}", t.getName());
				t.join();
			} catch (InterruptedException e) {
				logger.warn("Interrupted trying to join thread {}", t.getName());
			}
		}
		logger.debug("PeriodicUpdater singleton stopped");
	}
	
	public synchronized void pokeTask(Class<?> klass) {
		logger.debug("Prodding periodic task for class {}", klass);
		jobs.get(klass).poke();
	}

	private static class PeriodicJobRunnable implements DaemonRunnable {
		private int generation;
		private boolean poked;
		private PeriodicJob job;
		
		PeriodicJobRunnable(PeriodicJob job) {
			this.generation = 0;
			this.job = job;
			this.poked = false;
		}
		
		public synchronized void poke() {
			this.poked = true;
		}

		private boolean runOneGeneration() throws InterruptedException {
			int iteration = 0;
			generation += 1;
			boolean first = true;
			try {
				// We start off by sleeping for 1-3 minutes to reduce initial server load
				Thread.sleep(new Random().nextInt(2 * 60 * 1000) + 60 * 1000);
				
				long lastUpdate = System.currentTimeMillis();
				
				while (true) {
					iteration += 1;
					long sleepTime = lastUpdate + job.getFrequencyInMilliseconds() - System.currentTimeMillis();
					if (sleepTime < 0)
						sleepTime = 0;
					if (first) {
						first = false;
						sleepTime = 0;
					}
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						if (!poked) {
							// exit the loop - we're shutting down
							break;
						} else {
							logger.debug("Got poke in thread {}", job.getName());							
							// We were poked into running early - reset poked flag
							poked = false;
						}
					}

					job.doIt(sleepTime, generation, iteration);
					
					lastUpdate = System.currentTimeMillis();					
				}
			} catch (RuntimeException e) {
				logger.error("Unexpected exception in periodic job, generation " + generation + " exiting abnormally", e);
				return true; // do another generation
			}
			logger.debug("Cleanly shutting down periodic job thread generation {}", generation);
			return false; // shut down
		}
		
		public void run() throws InterruptedException {
			logger.info("Starting periodic update thread for '{}' with interval {} minutes",
					job.getName(), job.getFrequencyInMilliseconds() / 1000.0 / 60.0);
			while (runOneGeneration()) {
				// sleep protects us from 100% CPU in catastrophic failure case
				logger.warn("Periodic job thread sleeping and then restarting itself");
				Thread.sleep(10000);
			}
		}	
	}
}
