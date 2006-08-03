package com.dumbhippo.statistics;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.mx.util.MBeanServerLocator;

/**
 * Statistics source for global values for the server, such as JVM statistics 
 * @author otaylor
 */
public class ServerStatistics implements StatisticsSource {
	private static ServerStatistics instance = new ServerStatistics();
//	@Column(id="heapSize",
//			name="Heap Size", 
//			units=ColumnUnit.BYTES, 
//			type=ColumnType.SNAPSHOT)
//	public long getHeapSize() {
//		return 0;
//	}
	
	static public ServerStatistics getInstance() {
		return instance;
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
}
