package com.dumbhippo.live;

import org.jboss.system.ServiceMBean;
import org.w3c.dom.Element;

public interface PresenceServiceMBean extends ServiceMBean  {
	void setClusterName(String name);
	String getClusterName();
	void setClusterConfig(Element element);
}
