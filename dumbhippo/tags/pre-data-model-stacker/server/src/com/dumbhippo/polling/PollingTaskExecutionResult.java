package com.dumbhippo.polling;

public class PollingTaskExecutionResult {
	private PollingTask task;
	// Post-execution temporary stats
	private long start = -1;
	private long end = -1;
	private boolean failed;
	private boolean changed;
	private boolean obsolete;
	
	public PollingTaskExecutionResult(PollingTask task) {
		this.task = task;
		this.failed = true;
	}
	
	public PollingTaskExecutionResult(PollingTask task, long executionStart, long executionEnd, boolean executionChanged, boolean obsolete) {
		this.task = task;
		this.start = executionStart;
		this.end = executionEnd;
		this.changed = executionChanged;
		this.obsolete = obsolete;
		this.failed = false;
	}
	
	public PollingTask getTask() {
		return task;
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