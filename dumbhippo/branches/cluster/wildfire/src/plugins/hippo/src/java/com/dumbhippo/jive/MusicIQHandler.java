package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jive.rooms.RoomHandler;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.util.EJBUtil;

public class MusicIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public MusicIQHandler(RoomHandler roomHandler) {
		super("DumbHippo Music IQ Handler");
		
		Log.debug("creaing Music handler");
		info = new IQHandlerInfo("music", "http://dumbhippo.com/protocol/music");
			
		Log.debug("Done constructing Music IQ handler");
	}
	
	@Override
	public void destroy() {
		super.destroy();
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
		Map<String,String> properties;
		try {
			properties = parseTrackNode(iq);
		} catch (ParseException e) {
		    Log.debug(e);
			makeError(reply, e.getMessage());
			return reply;
		}
		
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		glue.handleMusicChanged(Guid.parseTrustedJabberId(from.getNode()), properties);
		
		return reply;
	}

	private IQ processPrimingTracks(JID from, Element iq, IQ reply) {	
		List<Map<String,String>> primingTracks = new ArrayList<Map<String,String>>();		
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
        		primingTracks.add(properties);
        	}
        }
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		glue.handleMusicPriming(Guid.parseTrustedJabberId(from.getNode()), primingTracks);		
        
		return reply;
	}
	
	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
