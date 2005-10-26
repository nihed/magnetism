package com.dumbhippo.jive;

import java.io.File;

import org.jivesoftware.messenger.IQRouter;
import org.jivesoftware.messenger.XMPPServer;
import org.jivesoftware.messenger.container.Plugin;
import org.jivesoftware.messenger.container.PluginManager;
import org.jivesoftware.messenger.handler.IQHandler;
import org.jivesoftware.util.Log;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		Log.debug("Initializing Hippo plugin");
		IQHandler myHandler = new ClientMethodIQHandler();
		IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
		iqRouter.addHandler(myHandler);		
		
		Log.debug("... done initializing Hippo plugin");
	}

	public void destroyPlugin() {
		Log.debug("Unloading Hippo plugin");

		Log.debug("... done unloading Hippo plugin");
	}
}
