package com.dumbhippo.jive.rooms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.XMPPServer;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import com.dumbhippo.identity20.Guid;
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
	
	private void sendErrorIQReply(Packet packet, PacketError.Condition condition, String error) {
		Log.debug("Sending error reply back to ");
		IQ reply = IQ.createResultIQ((IQ) packet);
		reply.setError(new PacketError(condition, 
				   PacketError.Type.modify, 
				   error));
		reply.setTo(packet.getFrom());
		XMPPServer.getInstance().getPacketRouter().route(reply);		
	}
	
	public void processPacket(Packet packet) {
		JID from = packet.getFrom();
		JID to = packet.getTo();
		
		Log.debug("RoomHandler: packet from " + from + " to " + to);

		assert(to.getDomain().equals(getServiceDomain()));
		
		// We only allow DumbHippo users to join our chatrooms; this allows
		// us to use the node name rather than the full JID as an identifier
		if (!from.getDomain().equals(XMPPServer.getInstance().getServerInfo().getName())) {
 			if (packet instanceof IQ) {
				Log.warn("Attempt to join room from unknown user at '" + from.getDomain() + "'");
				sendErrorIQReply(packet, PacketError.Condition.forbidden,
							"You can't join this room");
			}
			
			return;
		}
		
		Room room = getRoom(to.getNode(), from.getNode());
		if (room == null) {
			// the client needs an error here so it can display to the user 
			// if they click on an untrusted chat id and it turns out 
			// to be busted.
			if (packet instanceof IQ) {
				Log.debug("Attempt to join nonexistent room '" + to.getNode() + "'");
				sendErrorIQReply(packet, PacketError.Condition.item_not_found,
							"Can't find that chat room");
			}

			return;
		}
		
		room.processPacket(packet);			
	}
	
	public void roomChanged(Guid roomGuid) {
		Room room = peekRoom(roomGuid.toJabberId(null));
		if (room == null)
			return;
		else
			room.reloadCaches();
	}
	
	public Room peekRoom(String roomName) {
		return getRoom(roomName, null, false);
	}
	
	private Room getRoom(String roomName, String userId) {
		return getRoom(roomName, userId, true);
	}
	
	private Room getRoom(String roomName, String userId, boolean create) {
		Room room = rooms.get(roomName);
		if (room == null && create) {
			room = Room.loadFromServer(this, roomName);
			if (room == null) {
				Log.debug("  room doesn't seem to exist");
				return null;
			}
			rooms.put(roomName, room);			
		} else if (room == null) {
			return null;
		}
		if (userId == null)
			return room;
		if (room.checkUserCanJoin(userId)) {
			Log.debug("  sending packet to existing room " + roomName);
			return room;
		} else {
			Log.debug("  not authorized to send packet to room");
			return null;
		}
	}
	
	/**
	 * Returns all rooms the user is present in.
	 * @param userId an id for the user
	 * @return a list of rooms the user is present in
	 */
	public List<Room> getRoomsForUser(String userId) {
		List<Room> roomsForUser = new ArrayList<Room>();
		for (Room room : rooms.values()) {
			if (room.checkUserPresent(userId)) {
				roomsForUser.add(room);
			}			
		}
		return roomsForUser;
	}
}
