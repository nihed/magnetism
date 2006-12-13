package com.dumbhippo.jive;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;

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
public class AnnotatedIQHandler extends org.jivesoftware.wildfire.handler.IQHandler {
	private static class MethodInfo {
		private Method method;
		private IQMethod annotation;
		private boolean needsViewpoint;
		
		public MethodInfo(Method method, IQMethod annotation, boolean needsViewpoint) {
			this.method = method;
			this.annotation = annotation;
			this.needsViewpoint = needsViewpoint;
		}

		public IQMethod getAnnotation() {
			return annotation;
		}

		public Method getMethod() {
			return method;
		}

		public boolean getNeedsViewpoint() {
			return needsViewpoint;
		}
	}
	
	private IQHandlerInfo info;
	private Map<String, MethodInfo> getMethods = new HashMap<String, MethodInfo>();
	private Map<String, MethodInfo> setMethods = new HashMap<String, MethodInfo>();
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private TransactionRunner transactionRunner;
	
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
			IQMethod annotation = method.getAnnotation(IQMethod.class);
			if (annotation == null)
				continue;
			
			Map<String, MethodInfo> methods;
			switch (annotation.type()) {
			case get:
				methods = getMethods;
				break;
			case set:
				methods = setMethods;
				break;
			default:
				throw new RuntimeException("Unexpected IQ type " + annotation.type().name() + " for method " + method.getName());
			}
			
			if (methods.containsKey(annotation.name()))
				throw new RuntimeException("Duplicate handler methods for " + annotation.name());
			
			Class<?>[] parameterTypes = method.getParameterTypes();
			
			boolean needsViewpoint;
			
			if (parameterTypes.length == 2 && 
				parameterTypes[0].equals(IQ.class) &&
				parameterTypes[1].equals(IQ.class)) {
				needsViewpoint = false;
			} else if (parameterTypes.length == 3 && 
				parameterTypes[0].equals(UserViewpoint.class) &&
				parameterTypes[1].equals(IQ.class) &&
				parameterTypes[2].equals(IQ.class)) {
				needsViewpoint = true;
			} else {
				throw new RuntimeException("Unexpected signature for IQ handler method " + method.getName());
			}
				
			if (!method.getReturnType().equals(void.class))
				throw new RuntimeException("IQ Handler method " + method.getName() + "can't have a return value");
			
			Log.debug(info.getNamespace() + ": Found IQ " + annotation.name() + "/" + annotation.type().name() + " => " + method.getName());
			
			methods.put(annotation.name(), new MethodInfo(method, annotation, needsViewpoint));
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

	private Object[] getParams(MethodInfo methodInfo, IQ request, IQ reply) throws IQException {
		if (methodInfo.getNeedsViewpoint()) {
			JID from = request.getFrom();
			if (!from.getDomain().equals(XMPPServer.getInstance().getServerInfo().getName()))
				throw IQException.createForbidden();
			
			Guid guid;
			try {
				 guid = Guid.parseJabberId(from.getNode());
			} catch (ParseException e) {
				throw IQException.createForbidden();
			}
			
			User user = identitySpider.lookupUser(guid);
			if (user == null)
				throw IQException.createForbidden();
			
			return new Object[] { new UserViewpoint(user), request, reply };

		} else {
			return new Object[] { request, reply };
		}
	}

	private void runIQ(MethodInfo methodInfo, IQ request, IQ reply) throws IQException {
		try {
			methodInfo.getMethod().invoke(this, getParams(methodInfo, request, reply));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error invoking IQ method", e);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException(); 
			if (targetException instanceof IQException)
				throw (IQException)targetException;
			else
				throw new RuntimeException("Unexpected exception invoking IQ method", targetException);
		}
	}

	private void runIQWithTx(final MethodInfo methodInfo, final IQ request, final IQ reply) throws IQException {
		try {
			transactionRunner.runTaskInNewTransaction(new Callable<Boolean>() {
				public Boolean call() throws IQException {
					runIQ(methodInfo, request, reply);
					return true;
				}
			});
		} catch (IQException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception running IQ method in transaction", e);
		}
	}
	
	@Override
	public IQ handleIQ(IQ request) throws UnauthorizedException {
		Log.debug("handling IQ packet " + request);
		
		IQ reply = IQ.createResultIQ(request);
		
		try {
			Map<String, MethodInfo> methods;
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
			
			MethodInfo methodInfo = methods.get(child.getName());
			if (methodInfo == null)
				throw IQException.createBadRequest("Unknown IQ");
			
			if (methodInfo.getAnnotation().needsTransaction())
				runIQWithTx(methodInfo, request, reply);
			else
				runIQ(methodInfo, request, reply);

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
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
