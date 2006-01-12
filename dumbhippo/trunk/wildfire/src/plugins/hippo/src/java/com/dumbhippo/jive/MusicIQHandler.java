package com.dumbhippo.jive;

import javax.jms.ObjectMessage;

import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventMusicChanged;

public class MusicIQHandler extends IQHandler {

	private IQHandlerInfo info;
	private JmsProducer queue;
	
	public MusicIQHandler() {
		super("DumbHippo Music IQ Handler");
		
		info = new IQHandlerInfo("music", "http://dumbhippo.com/protocol/music");
		
		Log.debug("Opening JmsProducer for " + XmppEvent.QUEUE);
		queue = new JmsProducer(XmppEvent.QUEUE, false);
		Log.debug("Done constructing Music IQ handler");
	}

	private void makeError(IQ reply, String message) {
		Log.error(message);
		reply.setError(new PacketError(PacketError.Condition.bad_request, 
                					   PacketError.Type.modify, 
                					   message));
		return;
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
		
		if (!type.equals("musicChanged")) {
			makeError(reply, "Unknown music IQ type, known types are: musicChanged");
			return reply;
		}
		
		return processMusicChanged(from, iq, reply);
	}

	private IQ processMusicChanged(JID from, Element iq, IQ reply) {
		
		XmppEventMusicChanged event = new XmppEventMusicChanged(from.getNode());

		for (Object argObj : iq.elements()) {
        	Node node = (Node) argObj;
        	
        	Log.debug("parsing expected arg node " + node);
        	
        	if (node.getNodeType() == Node.ELEMENT_NODE) {
        		Element element = (Element) node;
        		
        		if (!element.getName().equals("prop")) {
        			makeError(reply, "Unknown node type: " + element.getName());
        			return reply;
        		}
        		
        		String key = element.attributeValue("key");
        		String value = element.getText();

        		Log.debug("Adding track property key='" + key + "' value='" + value + "'");
        	
        		event.setProperty(key, value);
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
