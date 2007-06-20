package com.dumbhippo.polling;

import java.util.Date;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.PollingTaskEntry;

public abstract class PollingTask implements Callable<PollingTaskExecutionResult> {
	
	private static final Logger logger = GlobalSetup.getLogger(PollingTask.class);	
	
	public static final int MAX_FAMILY_NAME_LENGTH = 20;
	public static final int MAX_TASK_ID_LENGTH = 128;

	private long taskDbId = -1;
	
	// Exponentially weighted average executation time
	private long executionAverage = -1;
	
	// Used for the default polling frequency algorithm
	private long recentChangeDecayFactor = -1;
	
	// Used to calculate periodicity
	private long lastChange = 0;
	
	// The following fields are recorded in the database in a separate
	// thread, so must be protected by synchronizing on the object 
	
	// Last time this task was executed
	private long lastExecuted = -1;
	
	// Exponentially weighted average time between changes
	private long periodicityAverage = -1; // 
	
	// Whether or not we have been executed since the last persist
	private boolean dirty;

	public PollingTask() {
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
	
	// Subclasses may override this to enforce e.g. a minimum polling
	// periodicity
	public long rescheduleSeconds(PollingTaskExecutionResult result, long suggested) { 
		return suggested; 
	};
	
	protected abstract PollResult execute() throws Exception;

	public final long getLastChange() {
		return lastChange;
	}

	public final PollingTaskExecutionResult call() throws Exception {
		logger.debug("Executing polling task {}-{}", getFamily(), getIdentifier());
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
			return new PollingTaskExecutionResult(this);				
		} catch (Exception e) {
			logger.warn("Execution of polling task failed for task: " + toString(), e);
			return new PollingTaskExecutionResult(this);				
		}
			
		return new PollingTaskExecutionResult(this, executionStart, executionEnd, executionChanged, obsolete);
	}

	public final long getExecutionAverage() {
		return executionAverage;
	}

	public final void setExecutionAverage(long executionAverage) {
		this.executionAverage = executionAverage;
	}

	public final synchronized long getPeriodicityAverage() {
		return periodicityAverage;
	}
	
	public final synchronized void setPeriodicityAverage(long periodicityAverage) {
		if (this.periodicityAverage != periodicityAverage) {
			this.periodicityAverage = periodicityAverage;
			this.dirty = true;
		}
	}

	public final void touchChanged() {
		this.lastChange = System.currentTimeMillis();
	}

	@Override
	public final String toString() {
		return getFamily().getName() + "-" + getIdentifier();
	}
	
	public synchronized void syncStateFromTaskEntry(PollingTaskEntry entry) {
		if (taskDbId == -1)
			taskDbId = entry.getId();
		else
			assert(taskDbId != entry.getId());
		
		if (entry.getLastExecuted() != null)
		    lastExecuted = entry.getLastExecuted().getTime();
		else 
			lastExecuted = -1;
		periodicityAverage = entry.getPeriodicityAverage();
		dirty = false;
	}

	public synchronized void syncStateToTaskEntry(PollingTaskEntry entry) {
		if (dirty) {
			Date lastExecutedDate = null;
			if (lastExecuted >= 0)
				lastExecutedDate = new Date(lastExecuted);
			entry.setLastExecuted(lastExecutedDate);
	
			// Fixup for a bug
			if (periodicityAverage < 0)
				periodicityAverage = getDefaultPeriodicitySeconds()*1000;
			
			entry.setPeriodicityAverage(periodicityAverage);
			
			dirty = false;
		}
	}

	public long getDbId() {
		return taskDbId;
	}

	public long getRecentChangeDecayFactor() {
		return recentChangeDecayFactor;
	}

	public void setRecentChangeDecayFactor(long recentChangeDecayFactor) {
		this.recentChangeDecayFactor = recentChangeDecayFactor;
	}
	
	// This is a workaround for not correctly persisting computed task periodicity;
	// (as of 2007-06-20) the per-family default periodicities are way too high if 
	// we start all 10,000+ tasks off with them, so we want to use something bigger. 
	// This provides a single place to tweak that bigger value.
	// 
	static final private long GLOBAL_DEFAULT_PERIODICITY_SECONDS = 3600; // 1 hour
	
	public long getDefaultPeriodicitySeconds() {
		// return getFamily().getDefaultPeriodicitySeconds();
		return GLOBAL_DEFAULT_PERIODICITY_SECONDS;
	}
}