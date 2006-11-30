package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;

@Entity
@javax.persistence.Table(name="CachedPollingTaskStats",
		uniqueConstraints = {
			@UniqueConstraint(columnNames={"family","taskId"})
		})
public class CachedPollingTaskStats extends DBUnique {
	
	private static final long serialVersionUID = 1L;
	
	private String family;
	private String taskId;
	private long lastExecuted = -1;
	private long periodicityAverage = -1;
	
	public CachedPollingTaskStats(String family, String id) {
		this.family = family;
		this.taskId = id;
	}
	
	@Column(nullable=false, length=PollingTask.MAX_FAMILY_NAME_LENGTH)
	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}
	
	@Column(nullable=false, length=PollingTask.MAX_TASK_ID_LENGTH)
	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String id) {
		this.taskId = id;
	}

	@Column(nullable=true)
	public Date getLastExecuted() {
		if (lastExecuted != -1)
			return new Date(lastExecuted);
		return null;
	}

	public void setLastExecuted(Date lastExecuted) {
		if (lastExecuted != null)
			this.lastExecuted = lastExecuted.getTime();
		else
			this.lastExecuted = -1;
	}

	@Column(nullable=false)	
	public long getPeriodicityAverage() {
		return periodicityAverage;
	}

	public void setPeriodicityAverage(long periodicityAverage) {
		this.periodicityAverage = periodicityAverage;
	}	
}
