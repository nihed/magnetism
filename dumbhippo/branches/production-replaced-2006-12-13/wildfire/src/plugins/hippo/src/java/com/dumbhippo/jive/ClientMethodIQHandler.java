package com.dumbhippo.jive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.handler.IQHandler;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.XMPPMethods;
import com.dumbhippo.server.XMPPMethods.XMPPRemoted;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.SimpleAnnotatedInvoker;
import com.dumbhippo.server.util.SimpleAnnotatedInvoker.ArgumentInterceptor;

public class ClientMethodIQHandler extends IQHandler {
	private IQHandlerInfo info;
	
	private class PersonArgumentPrepender implements ArgumentInterceptor {
		public List<Object> interceptArgs(Method method, List<String> args, Object context) throws InvocationTargetException {
			Class[] paramTypes = method.getParameterTypes();
			List<Object> ret = new ArrayList<Object>();
			if (paramTypes.length > 0 
					&& Guid.class.isAssignableFrom(paramTypes[0])) {
				try {
					JID from = (JID) context;
					String guid  = from.getNode();
					if (guid != null)
						ret.add(new Guid(guid));
					else
						throw new RuntimeException("no from node");
				} catch (ParseException e) {
					throw new InvocationTargetException(e);
				}
			}
			ret.addAll(args);
			Log.debug("transformed args: " + Arrays.toString(ret.toArray()));
			return ret;
		}
	}
	
	public ClientMethodIQHandler() {
		super("Dumbhippo IQ Method Handler");
		Log.debug("creating ClientMethodIQHandler");
		info = new IQHandlerInfo("method", "http://dumbhippo.com/protocol/servermethod");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		Log.debug("handling IQ packet " + packet);
		IQ reply = IQ.createResultIQ(packet);
		Element iq = packet.getChildElement();
        String method = iq.attributeValue("name");
        List<String> args = new ArrayList<String>();
        
        // TODO Don't look this up each time 
        // We currently do this to avoid problems during development
        // from reloading Jive - later we probably want to move this to
        // constructor
        XMPPMethods methods = EJBUtil.defaultLookup(XMPPMethods.class);
        SimpleAnnotatedInvoker xmppInvoker 
        	= new SimpleAnnotatedInvoker(XMPPRemoted.class, methods, new PersonArgumentPrepender());
        
        for (Object argObj: iq.elements()) {
        	Node arg = (Node) argObj;
        	Log.debug("parsing expected arg node " + arg);        	
        	if (arg.getNodeType() == Node.ELEMENT_NODE) {
        		String argValue = arg.getText();
        		Log.debug("Adding arg value" + argValue);
        		args.add(argValue);
        	}
        }
        
        try {
        	Log.debug("invoking method " + method + " with (" + args.size() + ") args " + Arrays.toString(args.toArray()));
        	@SuppressWarnings("unused") 
        	String replyStr = xmppInvoker.invoke(method, args, packet.getFrom());
        	// Don't do anything with this yet
		} catch (Exception e) {
			Log.debug("Caught exception during client method invocation", e);
		}
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}

	@Override
	public void start() {
	}
}
