package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBean;

// To be a valid MBean, HippoService needs to implement
// a service with this name; but we don't actually need
// anything beyond what's in the base ServiceMBean interface.
public interface HippoServiceMBean extends ServiceMBean {
}
