package com.dumbhippo.jive.jmx;

public interface XMPPServiceMBean {
    public void start() throws Exception;
    public void stop() throws Exception;
    public void setHomeDirectory(String directory);
    public String getHomeDirectory();
}

