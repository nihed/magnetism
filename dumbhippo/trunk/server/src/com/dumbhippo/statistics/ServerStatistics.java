package com.dumbhippo.statistics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.mx.util.MBeanServerLocator;

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
}
