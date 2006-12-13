package com.dumbhippo.jive.jmx;

import org.jivesoftware.wildfire.XMPPServer;

public class XMPPService implements XMPPServiceMBean {
	String homeDirectory;
	
    public void start() {
    	new XMPPServer(homeDirectory); // Will be stored as a global singleton
    }
    
    public void stop()  {
    	XMPPServer.getInstance().stop();
    }
    
    public void setHomeDirectory(String directory) {
    	homeDirectory = directory;
    }
    
    public String getHomeDirectory() {
    	return homeDirectory;
    }    
}
