package com.dumbhippo.jive;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.server.util.EJBUtil;

/**
 * This class provides a base class for writing Wildfire IQ Handlers for use within
 * the Dumbhippo server in a more declarative way than a raw derivation from IQHandler.
 * 
 * A subclass declares the namespace it handles using the @IQHandler annotation and
 * then marks specific methods which handle different elements within that namespace
 * with the @IQMethod annotation.
 * 
 * In addition to the support for method annotations, this class also supports 
 * an IQException checked exception for returning errors from the IQ, traps RuntimeException,
 * adds an (optionally disableable) transaction around the entire IQ, for IQ
 * methods that take a UserViewpoint as the first parameter, determines and verifies
 * the user that is calling the method, and injects Session beans into fields annotated
 * with the @EJB annotation.
 * 
 * You could extend this class in the style of HttpMethodsBean, but for right now, all parsing of the IQ 
 * request and construction of the IQ reply needs to be done manually.
 *  
 * @author otaylor
 */
public class AnnotatedIQHandler extends org.jivesoftware.openfire.handler.IQHandler {
	private IQHandlerInfo info;
	private Map<String, AnnotatedIQMethod> getMethods = new HashMap<String, AnnotatedIQMethod>();
	private Map<String, AnnotatedIQMethod> setMethods = new HashMap<String, AnnotatedIQMethod>();
	
	private void resolveHandlerInfo() {
		IQHandler annotation = getClass().getAnnotation(IQHandler.class);
		if (annotation == null)
			throw new RuntimeException("IQHandler annotation not found for AnnotatedIQHandler subclass");

		// The name field of IQHandlerInfo is ignored by Jive. In our usage,
		// each IQHandler handles all IQ's for a single namespace
		info = new IQHandlerInfo(null, annotation.namespace());
	}
	
	private void resolveMethods() {
		for (Method method : getClass().getMethods()) {
			AnnotatedIQMethod iqMethod = AnnotatedIQMethod.getForMethod(this, method);
			if (iqMethod == null)
				continue;
			
			Map<String, AnnotatedIQMethod> methods;
			switch (iqMethod.getType()) {
			case get:
				methods = getMethods;
				break;
			case set:
				methods = setMethods;
				break;
			default:
				throw new RuntimeException("Unexpected IQ type " + iqMethod.getType().name() + " for method " + method.getName());
			}
			
			if (methods.containsKey(iqMethod.getName()))
				throw new RuntimeException("Duplicate handler methods for " + iqMethod.getName());
			
			methods.put(iqMethod.getName(), iqMethod);
			
			Log.debug(info.getNamespace() + ": Found IQ " + iqMethod.getName() + "/" + iqMethod.getType().name() + " => " + method.getName());
		}
	}

	private void setField(Object o, Field f, Object value) {
		try {
			// Like EJB3, we support private-field injection
			f.setAccessible(true);
			f.set(o, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error injecting object", e);
		}
	}
	
	private void injectEjbs() {
		Class clazz = getClass();
		for (Class<?> c = clazz; c.getPackage() == clazz.getPackage(); c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				EJB annotation = field.getAnnotation(EJB.class);
				if (annotation == null)
					continue;
				
				// For right now, we don't support any of the EJB annotation
				// elements, though some of them would make sense
				
				Object bean = EJBUtil.defaultLookup(field.getType());
				setField(this, field, bean);
			}
		}
	}

	protected AnnotatedIQHandler(String moduleName) {
		super(moduleName);
		
		resolveHandlerInfo();
		resolveMethods();
	}
	
	@Override
	public void start() {
		super.start();
		injectEjbs();
	}

	@Override
	public void process(Packet packet) {
		IQ request = (IQ)packet;
		
		Log.debug("handling IQ packet " + request);
		
		IQ reply = null;
		
		try {
			Map<String, AnnotatedIQMethod> methods;
			switch (request.getType()) {
			case get:
				methods = getMethods;
				break;
			case set:
				methods = setMethods;
				break;
			default:
				throw IQException.createBadRequest("Unexpected IQ type " + request.getType().name());
			}
			
			Element child = request.getChildElement();
			if (child == null)
				throw IQException.createBadRequest("No child element");
			
			AnnotatedIQMethod iqMethod = methods.get(child.getName());
			if (iqMethod == null)
				throw IQException.createBadRequest("Unknown IQ");
			
			iqMethod.runIQ(request);

		} catch (IQException e) {
			// Create a new reply to avoid anything already written to the reply
			reply = IQ.createResultIQ(request);
			reply.setError(new PacketError(e.getCondition(), e.getType(), e.getMessage()));
		} catch (RuntimeException e) {
			Log.error("Internal server error handling IQ", e);
			reply = IQ.createResultIQ(request);
			reply.setError(new PacketError(PacketError.Condition.internal_server_error,
					       				   PacketError.Type.wait,
					       				   "Internal server error"));
		}
		
		if (reply != null) {
			try {
				deliverer.deliver(reply);
			} catch (UnauthorizedException e) {
				Log.error("Got UnauthorizedException sending error reply, giving up", e);
			}
		}
	}
	
	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// We override process, so we don't need to implement this 
		throw new UnsupportedOperationException();
	}
}
