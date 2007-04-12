package com.dumbhippo.statistics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jboss.cache.TreeCacheMBean;
import org.jboss.cache.eviction.EvictionPolicy;
import org.jboss.cache.eviction.EvictionQueue;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;

import com.dumbhippo.logging.CountingAppender;
import com.dumbhippo.polling.SwarmPollingSystem;
import com.dumbhippo.server.impl.MessengerGlueBean;

/**
 * Statistics source for global values for the server, such as JVM statistics 
 * @author otaylor
 */
public class ServerStatistics implements StatisticsSource {
	private static ServerStatistics instance = new ServerStatistics();
	
	static public ServerStatistics getInstance() {
		return instance;
	}
  	
	@Column(id="heapUsed",
			name="Heap Used", 
			units=ColumnUnit.BYTES, 
			type=ColumnType.SNAPSHOT)
	public long getHeapSize() {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		return memoryBean.getHeapMemoryUsage().getUsed();
	}
	
	@Column(id="collectionTime",
			name="Collection Time", 
			units=ColumnUnit.MILLISECONDS, 
			type=ColumnType.CUMULATIVE)
	public long getCollectionTime() {
		long time = 0;
		
		for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
			time += gcBean.getCollectionTime();
		}
		
		return time;
	}
	
	private Object getDBAttribute(String attr) {
		MBeanServer jboss = MBeanServerLocator.locateJBoss();
		try {
			return jboss.getAttribute(new ObjectName("jboss.jca:service=ManagedConnectionPool,name=DumbHippoDS"), attr);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get database attribute '" + attr + "'", e);
		}		
	}
	
	private long getDBNumericAttribute(String attr) {
		return ((Number) getDBAttribute(attr)).longValue();
	}
	
	@Column(id="dbMaxConnectionCount",
			name="DB Max Connection Count", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getDatabaseMaxConnectionCount() {
		return getDBNumericAttribute("MaxSize");
	}		
	
	@Column(id="dbConnectionCount",
			name="DB Connection Count", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getDatabaseConnectionCount() {
		return getDBNumericAttribute("InUseConnectionCount");
	}
	
	@Column(id="xmppActiveMethods",
			name="Active MessengerGlue Methods", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getXmppActiveMethods() {
		return MessengerGlueBean.getActiveRequestCount();
	}
	
	@Column(id="xmppMaxActiveMethods",
			name="Max Active MessengerGlue Methods", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getXmppMaxActiveMethods() {
		return MessengerGlueBean.getMaxActiveRequestCount();
	}
	
	@Column(id="xmppTooBusyCount",
			name="MessengerGlue Too Busy Responses", 
			units=ColumnUnit.EVENTS, 
			type=ColumnType.CUMULATIVE)
	public long getXmppTooBusyCount() {
		return MessengerGlueBean.getTooBusyCount();
	}
	
	@Column(id="entityCacheNodes",
			name="Entity Cache Nodes", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getEntityCacheNodes() {
		// We use the eviction queue as a cheap way of finding out the total
		// number of nodes in the tree; it's tracked there because you can
		// evict when the count exceeds a limit (though we don't). 
		// TreeCache.getNumberOfNodes() is considerly more expensive since
		// it walks over the entire tree.
		
		MBeanServer server = MBeanServerLocator.locateJBoss();
		TreeCacheMBean mbean;		
		try {
			mbean = (TreeCacheMBean) MBeanProxyExt.create(TreeCacheMBean.class, "jboss.cache:service=EJB3EntityTreeCache", server);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
		
		TreeCache cache = mbean.getInstance();
		EvictionPolicy policy = cache.getEvictionRegionManager().getRegion(new Fqn()).getEvictionPolicy();
		EvictionQueue queue = policy.getEvictionAlgorithm().getEvictionQueue();
		
		return queue.getNumberOfNodes();
	}
	
	@Column(id="warnCount",
	        name="Warning Count",
	        units=ColumnUnit.COUNT,
	        type=ColumnType.CUMULATIVE)
	public long getWarnCount() {
		return CountingAppender.getWarnCount();
	}
		
	@Column(id="errorCount",
	        name="Error Count",
	        units=ColumnUnit.COUNT,
	        type=ColumnType.CUMULATIVE)
	public long getErrorCount() {
		return CountingAppender.getErrorCount();
	}
	
	@Column(id="executingTaskCount",
			name="Executing task count",
			units=ColumnUnit.COUNT,
			type=ColumnType.SNAPSHOT)
	public long getExecutingTaskCount() {
		SwarmPollingSystem swarm = SwarmPollingSystem.getInstance();
		if (swarm != null)
			return swarm.getExecutingTaskCount();
		return 0;
	}
	
	@Column(id="pendingTaskCount",
			name="Pending task count",
			units=ColumnUnit.COUNT,
			type=ColumnType.SNAPSHOT)
	public long getPendgingTaskCount() {
		SwarmPollingSystem swarm = SwarmPollingSystem.getInstance();
		if (swarm != null)
			return swarm.getPendingTaskCount();
		return 0;
	}	
}
