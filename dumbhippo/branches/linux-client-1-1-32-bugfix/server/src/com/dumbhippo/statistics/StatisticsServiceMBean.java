package com.dumbhippo.statistics;

import org.jboss.system.ServiceMBean;

// To be a valid MBean, StatisticsService needs to implement
// a service with this name; but we don't actually need
// anything beyond what's in the base ServiceMBean interface.
public interface StatisticsServiceMBean extends ServiceMBean {
}
