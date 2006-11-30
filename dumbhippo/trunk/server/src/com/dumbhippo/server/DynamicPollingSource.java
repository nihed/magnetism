package com.dumbhippo.server;

import java.util.Set;

import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTaskFamily;

public interface DynamicPollingSource {
	public Set<PollingTask> getTasks();

	public PollingTaskFamily getTaskFamily();
}
