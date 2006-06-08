package com.dumbhippo.jive;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError.Condition;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;

public class PostControlsIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public PostControlsIQHandler() {
		super("DumbHippo Post Controls IQ Handler");
		
		Log.debug("creating PostControls handler");
		info = new IQHandlerInfo("postControl", "http://dumbhippo.com/protocol/postControl");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		
		Element element = packet.getChildElement();
		String type = element.attributeValue("type");		
		String id = element.attributeValue("id");
		
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		if (type.equals("ignore")) {
			try {
				glue.setPostIgnored(Guid.parseTrustedJabberId(from.getNode()), 
								    new Guid(id), true);
			} catch (NotFoundException e) {
				Log.error(e);
				reply.setError(Condition.internal_server_error);
			} catch (ParseException e) {
				Log.error(e);
				reply.setError(Condition.internal_server_error);
			}
		} else {
			reply.setError(Condition.item_not_found);
		}
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
