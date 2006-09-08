package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ObjectMessage;

import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.jive.rooms.Room;
import com.dumbhippo.jive.rooms.RoomHandler;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventMusicChanged;
import com.dumbhippo.xmppcom.XmppEventPrimingTracks;

public class MusicIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	private JmsProducer queue;
	
	// we need it to know which rooms to send a message about a music change to
	private RoomHandler roomHandler;
	
	public MusicIQHandler(RoomHandler roomHandler) {
		super("DumbHippo Music IQ Handler");
		
		Log.debug("creaing Music handler");
		info = new IQHandlerInfo("music", "http://dumbhippo.com/protocol/music");
				
		Log.debug("Opening JmsProducer for " + XmppEvent.QUEUE);
		queue = new JmsProducer(XmppEvent.QUEUE, true);
		
		this.roomHandler = roomHandler;
		Log.debug("Done constructing Music IQ handler");
	}
	
	@Override
	public void destroy() {
		super.destroy();
		queue.close();
		queue = null;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		Element iq = packet.getChildElement();

		String type = iq.attributeValue("type");
		if (type == null) {
			makeError(reply, "No type attribute on the root iq element for music message");
			return reply;
		}
		
		if (type.equals("musicChanged")) {
			return processMusicChanged(from, iq, reply);	
		} else if (type.equals("primingTracks")) {
			return processPrimingTracks(from, iq, reply);
		} else {
			
			makeError(reply, "Unknown music IQ type, known types are: musicChanged, primingTracks");
			return reply;
		}
	}

	static class ParseException extends Exception {
		private static final long serialVersionUID = 1L;

		ParseException(String message) {
			super(message);
		}
	}
	
	private Map<String,String> parseTrackNode(Element track) throws ParseException {
		Map<String,String> properties = new HashMap<String,String>();
		for (Object argObj : track.elements()) {
        	Node node = (Node) argObj;
        	
        	if (node.getNodeType() == Node.ELEMENT_NODE) {
        		Element element = (Element) node;
        		
        		if (!element.getName().equals("prop")) {
        			throw new ParseException("Unknown node type: " + element.getName());
        		}
        		
        		String key = element.attributeValue("key");
        		String value = element.getText();

        		// Log.debug("Adding track property key='" + key + "' value='" + value + "'");
        	
        		properties.put(key, value);
        	}
        }
    	return properties;
	}
	
	private IQ processMusicChanged(JID from, Element iq, IQ reply) {
		XmppEventMusicChanged event = new XmppEventMusicChanged(from.getNode());
		Map<String,String> properties;
		try {
			properties = parseTrackNode(iq);
		} catch (ParseException e) {
		    Log.debug(e);
			makeError(reply, e.getMessage());
			return reply;
		}
		
		event.addProperties(properties);
		
        ObjectMessage message = queue.createObjectMessage(event);
        queue.send(message);
        
		// Let's make sure this is a DumbHippo user, so we can use the node name as an
        // identifier
		if (from.getDomain().equals(XMPPServer.getInstance().getServerInfo().getName())) {
            String username = from.getNode();
            for (Room room : roomHandler.getRoomsForUser(username))	{
    			// createCopy creates a deep copy of the element. 
    			// The new element is detached from its parent, 
    			// and getParent() on the clone will return null, so we can have
    			// it have a new parent!
            	room.processMusicChange(iq.createCopy(), properties, username);
            }
		}
		return reply;
	}

	private IQ processPrimingTracks(JID from, Element iq, IQ reply) {	
		XmppEventPrimingTracks event = new XmppEventPrimingTracks(from.getNode());

		// Log.debug("priming tracks xml: " + iq.asXML());
		
		for (Object argObj : iq.elements()) {
        	Node node = (Node) argObj;
        	
        	if (node.getNodeType() == Node.ELEMENT_NODE) {
        		Element element = (Element) node;
        		
        		if (!element.getName().equals("track")) {
        			makeError(reply, "Unknown node type: " + element.getName());
        			return reply;
        		}
        		
        		Map<String,String> properties;
        		try {
					 properties = parseTrackNode(element);
				} catch (ParseException e) {
					Log.debug(e);
					makeError(reply, e.getMessage());
					return reply;
				}
        		
				event.addTrack(properties);
        	}
        }
        
        ObjectMessage message = queue.createObjectMessage(event);
        queue.send(message);
		
		return reply;
	}
	
	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
