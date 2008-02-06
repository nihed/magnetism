package com.dumbhippo.polling;

import java.util.Collection;

import org.jboss.system.ServiceMBean;

import com.dumbhippo.persistence.PollingTaskFamilyType;

/** MBean that lives on one node in the cluster and handles polling
 */
public interface SwarmPollingSystemMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	public void pokeTask(PollingTaskFamilyType family, String id);
	public void pokeTask(String taskId);
	
	public void runExternalTasks(Collection<String> taskIds);
	
	public void resyncAllExternalTasks() throws Exception;
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
