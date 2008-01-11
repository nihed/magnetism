package com.dumbhippo.jive;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.RoutingTable;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.component.InternalComponentManager;
import org.jivesoftware.openfire.container.Module;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.util.Log;
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
	private XmppClientManager clientManager = new XmppClientManager();
	private MessageSender messageSenderProvider = new MessageSender();
	private CompatibilityNotifier compatibilityNotifier = new CompatibilityNotifier();
	private AdminHandler adminHandler = new AdminHandler();
	private PacketInterceptor securityInterceptor = new HippoSecurityInterceptor();
	private List<Module> internalModules = new ArrayList<Module>();
	
	private void addIQHandler(IQHandler handler) {
		Log.debug("Adding IQ handler " + handler.getClass().getName() + " " + handler.getInfo().getNamespace());
		
		internalModules.add(handler);
		handler.start();
		
		/**
		 * Because we've set up adminHandler to get all packets sent to admin@[ourdomain]
		 * we need to actually handle our IQ's there, and adding the handler to the 
		 * global iqRouter doesn't do any good. We do it anyways, since it's not clear
		 * what's supposed to be happening... the fact that IQ's end up in our
		 * handler is somewhat coincidental.  
		 */
		adminHandler.addIQHandler(handler);

		IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
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
			sessionManager.registerListener(clientManager);
			
			InterceptorManager.getInstance().addInterceptor(securityInterceptor);
			
			XMPPServer.getInstance().getRoutingTable().addRoute(adminHandler.getAddress(), adminHandler);
					
			try {
				InternalComponentManager.getInstance().addComponent("rooms", roomHandler);
			} catch (ComponentException e) {
				throw new RuntimeException("Error adding Rooms component", e);
			}
			
			compatibilityNotifier.start();
			
			addIQHandler(new SystemIQHandler());
			addIQHandler(new ApplicationsIQHandler());
			addIQHandler(new LegacyApplicationsIQHandler());
			addIQHandler(new ChatMessagesIQHandler());
			addIQHandler(new ClientInfoIQHandler());
			addIQHandler(new MySpaceIQHandler());					
			addIQHandler(new MusicIQHandler());
			addIQHandler(new PrefsIQHandler());
			addIQHandler(new HotnessIQHandler());	
			addIQHandler(new RecentPostsIQHandler());
			addIQHandler(new PostControlsIQHandler());
			addIQHandler(new LegacyGroupIQHandler());	
			addIQHandler(new GroupsIQHandler());	
			addIQHandler(new LegacyBlocksIQHandler());
			addIQHandler(new BlocksIQHandler());
			addIQHandler(new AccountQuestionIQHandler());
			addIQHandler(new SettingsIQHandler());			
			addIQHandler(new LegacySettingsIQHandler());
			addIQHandler(new WhereImIQHandler());
			addIQHandler(new LegacyContactsIQHandler());
			addIQHandler(new ContactsIQHandler());
			addIQHandler(new EntityIQHandler());			
			
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
		clientManager.shutdown();
		
		PresenceService.getInstance().clearLocalPresence();

		RoutingTable routingTable = XMPPServer.getInstance().getRoutingTable();
		if (routingTable != null)
			routingTable.removeRoute(adminHandler.getAddress());

		InterceptorManager.getInstance().removeInterceptor(securityInterceptor);
		
		XmppMessageSender messageSender = EJBUtil.defaultLookup(XmppMessageSender.class);
		messageSender.setProvider(null);
		messageSenderProvider.shutdown();
		
		Log.debug("... done unloading Hippo plugin");
	}
}
