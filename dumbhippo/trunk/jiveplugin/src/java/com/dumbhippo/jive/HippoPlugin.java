package com.dumbhippo.jive;

import java.io.File;

import org.jivesoftware.messenger.container.Plugin;
import org.jivesoftware.messenger.container.PluginManager;
import org.jivesoftware.util.Log;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {

	private static final String linkReflectorDomain = "link-reflector";
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		Log.debug("Initializing Hippo plugin");
		
		
		ComponentManager componentManager = ComponentManagerFactory.getComponentManager();
		
		try {
			componentManager.addComponent(linkReflectorDomain, new LinkReflectorComponent());
		} catch (ComponentException e) {
			Log.error(e);
		}
		
		Log.debug("... done initializing Hippo plugin");
	}

	public void destroyPlugin() {
		Log.debug("Unloading Hippo plugin");
		
		ComponentManager componentManager = ComponentManagerFactory.getComponentManager();
		
		try {
			componentManager.removeComponent(linkReflectorDomain);
		} catch (ComponentException e) {
			Log.error(e);
		}
		
		Log.debug("... done unloading Hippo plugin");
	}
}
