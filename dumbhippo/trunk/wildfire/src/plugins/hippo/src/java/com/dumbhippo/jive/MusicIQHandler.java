package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

@IQHandler(namespace=MusicIQHandler.MUSIC_NAMESPACE)
public class MusicIQHandler extends AnnotatedIQHandler {
	static final String MUSIC_NAMESPACE = "http://dumbhippo.com/protocol/music";

	public MusicIQHandler() {
		super("DumbHippo Music IQ Handler");
	}
	
	@Override
	public void destroy() {
		super.destroy();
	}
	
	@IQMethod(name="music", type=IQ.Type.set)
	public void doMusic(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException, RetryException {		
		Log.debug("handling IQ packet " + request);
		Element iq = request.getChildElement();

		String type = iq.attributeValue("type");
		if (type == null) {
			throw IQException.createBadRequest("No type attribute on the root iq element for music message");
		}
		
		if (type.equals("musicChanged")) {
		    processMusicChanged(viewpoint, iq, reply);	
		} else if (type.equals("primingTracks")) {
			processPrimingTracks(viewpoint, iq, reply);
		} else {
			throw IQException.createBadRequest("Unknown music IQ type, known types are: musicChanged, primingTracks");
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
	
	private void processMusicChanged(UserViewpoint viewpoint, Element iq, IQ reply) throws IQException, RetryException {
		Map<String,String> properties;
		try {
			properties = parseTrackNode(iq);
		} catch (ParseException e) {
		    Log.debug(e);
		    throw IQException.createBadRequest(e.getMessage());
		}
		
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		glue.handleMusicChanged(viewpoint, properties);
	}

	private void processPrimingTracks(UserViewpoint viewpoint, Element iq, IQ reply) throws IQException, RetryException {	
		List<Map<String,String>> primingTracks = new ArrayList<Map<String,String>>();		
		for (Object argObj : iq.elements()) {
        	Node node = (Node) argObj;
        	
        	if (node.getNodeType() == Node.ELEMENT_NODE) {
        		Element element = (Element) node;
        		
        		if (!element.getName().equals("track")) {
        			throw IQException.createBadRequest("Unknown node type: " + element.getName());
        		}
        		
        		Map<String,String> properties;
        		try {
					 properties = parseTrackNode(element);
				} catch (ParseException e) {
					Log.debug(e);
					throw IQException.createBadRequest(e.getMessage());
				}
        		primingTracks.add(properties);
        	}
        }
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		glue.handleMusicPriming(viewpoint, primingTracks);		
	}
}
