package com.dumbhippo.jive;

import java.io.File;

import org.jivesoftware.messenger.IQRouter;
import org.jivesoftware.messenger.SessionManager;
import org.jivesoftware.messenger.XMPPServer;
import org.jivesoftware.messenger.container.Plugin;
import org.jivesoftware.messenger.container.PluginManager;
import org.jivesoftware.messenger.handler.IQHandler;
import org.jivesoftware.util.Log;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		Log.debug("Initializing Hippo plugin");
		
		// this is a little broken since we have no connection tracking and thus
		// no clue when the server restarts and we need to call this again
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		glue.serverStartup(System.currentTimeMillis());
		
		IQHandler myHandler = new ClientMethodIQHandler();
		IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
		iqRouter.addHandler(myHandler);		
	
		SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
		sessionManager.registerListener(new PresenceMonitor());
				
		Log.debug("... done initializing Hippo plugin");
	}

	public void destroyPlugin() {
		Log.debug("Unloading Hippo plugin");

		Log.debug("... done unloading Hippo plugin");
	}
}
