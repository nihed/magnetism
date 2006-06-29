package com.dumbhippo.jive;

import java.io.File;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQRouter;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.component.InternalComponentManager;
import org.jivesoftware.wildfire.container.Plugin;
import org.jivesoftware.wildfire.container.PluginManager;
import org.xmpp.component.ComponentException;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jive.rooms.RoomHandler;
import com.dumbhippo.jms.JmsConsumer;
import com.dumbhippo.live.GroupEvent;
import com.dumbhippo.live.LiveEvent;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {
	
	private RoomHandler roomHandler;
	private PresenceMonitor presenceMonitor;
	
	private JmsConsumer incomingQueue;
	
	private class MessageQueueConsumer implements Runnable {
		private boolean shutdown;
		public synchronized void shutdown() {
			shutdown = true;
		}
		public void run() {
			while (true) {
				Message msg = incomingQueue.receive();
				if (shutdown)
					break;
				if (msg instanceof ObjectMessage) {
					
					ObjectMessage objectMsg = (ObjectMessage) msg;
					Object obj;
					
					try {
						 obj = objectMsg.getObject();
					} catch (JMSException e) {
						e.printStackTrace();
						continue; // not much else to do...
					}
					
					Log.debug("Message contained object: " + obj.getClass().getCanonicalName());
					
					if (obj instanceof GroupEvent) {
						GroupEvent groupEvent = (GroupEvent) obj;
						Guid groupId = groupEvent.getGroupId();
						roomHandler.roomChanged(groupId);
					}
				}				
			}
		}		
	}
	
	private MessageQueueConsumer queueConsumer;
	private Thread queueConsumerThread;
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		try {
			Log.debug("Initializing Hippo plugin");
			
			Log.debug("Adding PresenceMonitor");
			presenceMonitor = new PresenceMonitor();
			SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
			sessionManager.registerListener(presenceMonitor);
					
			roomHandler = new RoomHandler(presenceMonitor);			
			try {
				InternalComponentManager.getInstance().addComponent("rooms", roomHandler);
			} catch (ComponentException e) {
				throw new RuntimeException("Error adding Rooms component", e);
			}
			
			IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
			iqRouter.addHandler(new ClientMethodIQHandler());		
			iqRouter.addHandler(new ClientInfoIQHandler());
			iqRouter.addHandler(new MySpaceIQHandler());					
			iqRouter.addHandler(new MusicIQHandler(roomHandler));
			iqRouter.addHandler(new PrefsIQHandler());
			iqRouter.addHandler(new HotnessIQHandler());	
			iqRouter.addHandler(new RecentPostsIQHandler());
			iqRouter.addHandler(new PostControlsIQHandler());			
			
			incomingQueue = new JmsConsumer(LiveEvent.XMPP_QUEUE);
			queueConsumer = new MessageQueueConsumer();
			queueConsumerThread = new Thread(queueConsumer);
			queueConsumerThread.start();
			
			Log.debug("... done initializing Hippo plugin");
		} catch (Exception e) {
			Log.debug("Failed to init hippo plugin: " + ExceptionUtils.getRootCause(e).getMessage(), e);
		}
	}

	public void destroyPlugin() {
		Log.debug("Unloading Hippo plugin");
		
		Log.debug("Removing rooms route");
		InternalComponentManager.getInstance().removeComponent("rooms");
		
		Log.debug("Shutting down presence monitor");
		presenceMonitor.shutdown();
		
		Log.debug("Shutting down queue consumer");		
		queueConsumer.shutdown();
		queueConsumerThread.interrupt();
		incomingQueue.close();

		Log.debug("... done unloading Hippo plugin");
	}
}
