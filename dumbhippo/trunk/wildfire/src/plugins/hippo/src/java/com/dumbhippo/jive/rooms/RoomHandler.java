package com.dumbhippo.jive.rooms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.XMPPServer;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import com.dumbhippo.jive.PresenceMonitor;

public class RoomHandler implements Component {
	private PresenceMonitor presenceMonitor;
	private JID address;
	private Map<String, Room> rooms;
	
	public String getDescription() {
		return "Handler for DumbHippo chat rooms";
	}
	
	public String getName() {
		return "HippoRoomComponent";
	}
	
	public void initialize(JID jid, ComponentManager manager) {
	}
	
	public void start() {
	}
	
	public void stop() {
	}
	
	public void shutdown() {
	}
	
	private String getServiceDomain() {
		return "rooms." + XMPPServer.getInstance().getServerInfo().getName();
	}

	public RoomHandler(PresenceMonitor presenceMonitor) {
		this.presenceMonitor = presenceMonitor;
		address = new JID(null, getServiceDomain(), null);
		rooms = new HashMap<String, Room>();
	}
	
	public PresenceMonitor getPresenceMonitor() {
		return presenceMonitor;
	}
	
	public JID getAddress() {
		return address;
	}
	
	public void processPacket(Packet packet) {
		JID from = packet.getFrom();
		JID to = packet.getTo();
		
		Log.debug("RoomHandler: packet from " + from + " to " + to);

		assert(to.getDomain().equals(getServiceDomain()));
		
		// We only allow DumbHippo users to join our chatrooms; this allows
		// us to use the node name rather than the full JID as an identifier
		if (!from.getDomain().equals(XMPPServer.getInstance().getServerInfo().getName()))
			return; // Ignore
		
		Room room = getRoom(to.getNode(), from.getNode());
		if (room == null)
			return; // Ignore
		
		room.processPacket(packet);			
	}
	
	private Room getRoom(String roomName, String userId) {
		Room room = rooms.get(roomName);
		if (room != null) {
			if (room.userCanJoin(userId)) {
				Log.debug("  sending packet to existing room " + roomName);
				return room;
			} else {
				Log.debug("  not authorized to send packet to room");
				return null;
			}
		}
		
		room = Room.loadFromServer(this, roomName, userId);
		if (room == null) {
			Log.debug("  room doesn't seem to exist");
			return null;
		}
		rooms.put(roomName, room);
		
		return room;
	}
	
	/**
	 * Returns all rooms the user is present in.
	 * @param userId an id for the user
	 * @return a list of rooms the user is present in
	 */
	public List<Room> getRoomsForUser(String userId) {
		List<Room> roomsForUser = new ArrayList<Room>();
		for (Room room : rooms.values()) {
			if (room.userPresent(userId)) {
				roomsForUser.add(room);
			}			
		}
		return roomsForUser;
	}
}	
	