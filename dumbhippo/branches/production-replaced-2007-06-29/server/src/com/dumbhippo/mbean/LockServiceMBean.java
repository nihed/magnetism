package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBean;
import org.w3c.dom.Element;

public interface LockServiceMBean extends ServiceMBean  {
	void setClusterName(String name);
	String getClusterName();
	void setClusterConfig(Element element);
}
