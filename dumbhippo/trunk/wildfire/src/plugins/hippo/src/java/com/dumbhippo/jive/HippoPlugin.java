package com.dumbhippo.jive;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQRouter;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.component.InternalComponentManager;
import org.jivesoftware.wildfire.container.Module;
import org.jivesoftware.wildfire.container.Plugin;
import org.jivesoftware.wildfire.container.PluginManager;
import org.jivesoftware.wildfire.handler.IQHandler;
import org.xmpp.component.ComponentException;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.jive.rooms.RoomHandler;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.util.EJBUtil;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {
	
	private RoomHandler roomHandler = new RoomHandler();
	private PresenceMonitor presenceMonitor = new PresenceMonitor();
	private MessageSender messageSenderProvider = new MessageSender();
	private CompatibilityNotifier compatibilityNotifier = new CompatibilityNotifier();
	private List<Module> internalModules = new ArrayList<Module>();
	
	private void addIQHandler(IQHandler handler) {
		IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
		internalModules.add(handler);
		handler.start();
		iqRouter.addHandler(handler);
	}
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		try {
			Log.debug("Initializing Hippo plugin");
			
			XmppMessageSender messageSender = EJBUtil.defaultLookup(XmppMessageSender.class);
			messageSender.setProvider(messageSenderProvider);
			messageSenderProvider.start();
			
			Log.debug("Adding PresenceMonitor");
			SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
			sessionManager.registerListener(presenceMonitor);
					
			try {
				InternalComponentManager.getInstance().addComponent("rooms", roomHandler);
			} catch (ComponentException e) {
				throw new RuntimeException("Error adding Rooms component", e);
			}
			
			compatibilityNotifier.start();
			
			addIQHandler(new ApplicationsIQHandler());
			addIQHandler(new ClientInfoIQHandler());
			addIQHandler(new MySpaceIQHandler());					
			addIQHandler(new MusicIQHandler(roomHandler));
			addIQHandler(new PrefsIQHandler());
			addIQHandler(new HotnessIQHandler());	
			addIQHandler(new RecentPostsIQHandler());
			addIQHandler(new PostControlsIQHandler());
			addIQHandler(new GroupIQHandler());			
			addIQHandler(new BlocksIQHandler());
			addIQHandler(new AccountQuestionIQHandler());
			addIQHandler(new SettingsIQHandler());
			addIQHandler(new WhereImIQHandler());
			
			Log.debug("... done initializing Hippo plugin");
		} catch (Exception e) {
			Log.debug("Failed to init hippo plugin: " + ExceptionUtils.getRootCause(e).getMessage(), e);
		}
	}

	public void destroyPlugin() {
		Log.debug("Unloading Hippo plugin");
	
		for (Module m : internalModules) {
			m.stop();
		}
		internalModules = null;
		
		compatibilityNotifier.stop();

		Log.debug("Removing rooms route");
		InternalComponentManager.getInstance().removeComponent("rooms");
		
		Log.debug("Shutting down presence monitor");
		presenceMonitor.shutdown();
		
		PresenceService.getInstance().clearLocalPresence();

		XmppMessageSender messageSender = EJBUtil.defaultLookup(XmppMessageSender.class);
		messageSender.setProvider(null);
		messageSenderProvider.shutdown();
		
		Log.debug("... done unloading Hippo plugin");
	}
}
