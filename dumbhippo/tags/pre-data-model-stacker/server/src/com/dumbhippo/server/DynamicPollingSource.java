package com.dumbhippo.server;

import java.util.Set;

import com.dumbhippo.polling.PollingTask;
import com.dumbhippo.polling.PollingTaskFamily;

public interface DynamicPollingSource {
	
	public Set<PollingTask> getTasks();

	public PollingTaskFamily getTaskFamily();
}
