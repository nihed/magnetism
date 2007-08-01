package com.dumbhippo.jive;

import java.util.PriorityQueue;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.jivesoftware.openfire.ChannelHandler;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.Log;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMClient;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.StoreClient;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.views.UserViewpoint;

public class XmppClient implements DMClient {
	private XmppClientManager clientManager;
	private DataModel model;
	private ClientSession clientSession;
	private StoreClient storeClient;
	private Guid userId;
	private long lastSentSerial = -1;
	private PriorityQueue<QueuedPacket> packets = new PriorityQueue<QueuedPacket>();
	private boolean havePacketSender = false;
	private boolean closed = false;
	
	static long MAX_WAIT_TIME = 5 * 60 * 1000; // 5 minutes

	XmppClient(XmppClientManager clientManager, DataModel model, ClientSession clientSession, Guid userId) {
		this.clientManager = clientManager;
		this.model = model;
		this.clientSession = clientSession;
		this.userId = userId;
		this.storeClient = model.getStore().openClient(this);
	}
	
	public StoreClient getStoreClient() {
		return storeClient;
	}

	public void close() {
		model.getStore().closeClient(storeClient);
		synchronized(packets) {
			closed = true;
		}
	}
	
	public void queuePacket(Packet packet, long serial) {
		boolean needSender;
		
		synchronized(packets) {
			if (closed)
				return;
			
			needSender = !havePacketSender;
			packets.add(new QueuedPacket(packet, serial));
			havePacketSender = true;
			
			if (!needSender)
				packets.notify();
		}
		
		if (needSender)
			clientManager.addClientSender(this);
	}

	public void queuePacket(Packet packet) {
		queuePacket(packet, storeClient.allocateSerial());
	}
	
	public void processPackets() {
		while(true) {
			QueuedPacket packet;
			
			synchronized(packets) {
				while (true) {
					if (closed) {
						havePacketSender = false;
						return;
					}
					
					packet = packets.peek();
					if (packet == null) {
						havePacketSender = false;
						return;
					}

					if (packet.serial == lastSentSerial + 1) {
						packets.poll();
						lastSentSerial = packet.serial;
						break;
					} else {
						long waitStart = System.currentTimeMillis();
						long now = waitStart;
						
						while (true) {
							long waitTime = waitStart + MAX_WAIT_TIME - now;
							
							if (waitTime < 0) {
								Log.error("Giving up waiting for packet after " + MAX_WAIT_TIME + " ms, a missing nullNotify?");
								packet = packets.peek();
								lastSentSerial = packet.serial - 1;
								break;
							}

							try {
								packets.wait(waitTime);
							} catch (InterruptedException e) { // Shut down
								havePacketSender = false;
								return;
							}
						
							packet = packets.peek();
							if (packet.serial == lastSentSerial + 1)
								break;
							
							now = System.currentTimeMillis();
						}
					}
				}
			}
			
			if (packet.packet == null) // from nullNotification
				continue;
			
			@SuppressWarnings("unchecked")
			ChannelHandler<Packet> handler = clientSession;
			try {
				handler.process(packet.packet);
			} catch (UnauthorizedException e) {
				Log.error("Got UnauthorizedException when delivering packet to client session", e);
			} catch (RuntimeException e) {
				Log.error("Got exception when delivering packet to client session", e);
			}
		}
	}

	public DMViewpoint createViewpoint() {
		return new UserViewpoint(userId, Site.XMPP);
	}

	QName NOTIFICATION_QNAME = QName.get("notify", Namespace.get("http://mugshot.org/p/system")); 
	
	public FetchVisitor beginNotification() {
		Element element = DocumentFactory.getInstance().createElement(NOTIFICATION_QNAME);
		return new XmppFetchVisitor(element, model);
	}

	public void endNotification(FetchVisitor visitor, long serial) {
		XmppFetchVisitor xmppVisitor = (XmppFetchVisitor)visitor;
		
        Message message = new Message();
        message.setType(Message.Type.headline);
        message.getElement().add(xmppVisitor.getRootElement());
        
        queuePacket(message, serial);
	}

	public <K, T extends DMObject<K>> void notifyEviction(DMClassHolder<K, T> classHolder, K key, long serial) {
		// TODO implement this
		nullNotification(serial);
	}

	public void nullNotification(long serial) {
		queuePacket(null, serial);
	}
	
	private static class QueuedPacket implements Comparable<QueuedPacket> {
		private Packet packet;
		private long serial;

		public QueuedPacket(Packet packet, long serial) {
			this.packet = packet;
			this.serial = serial;
		}

		public int compareTo(QueuedPacket other) {
			return serial < other.serial ? -1 : (serial == other.serial ? 0 : 1);
		}
	}
}
