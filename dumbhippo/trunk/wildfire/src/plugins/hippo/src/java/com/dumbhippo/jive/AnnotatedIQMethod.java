package com.dumbhippo.jive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.XMPPServer;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;

public abstract class AnnotatedIQMethod {
	protected IQMethod annotation;
	protected AnnotatedIQHandler handler;
	protected Method method;

	public AnnotatedIQMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		this.handler = handler;
		this.annotation = annotation;
		this.method = method;
	}
	
	public IQ.Type getType() {
		return annotation.type();
	}
	
	public String getName() {
		return annotation.name();
	}
	
	public abstract void doIQ(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException;
	
	protected Object invokeMethod(Object... params) throws IQException {
		try {
			return method.invoke(handler, params);
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

	private Guid getUserId(IQ request) throws IQException {
		JID from = request.getFrom();
		if (!from.getDomain().equals(XMPPServer.getInstance().getServerInfo().getName()))
			throw IQException.createForbidden();
		
		try {
			 return Guid.parseJabberId(from.getNode());
		} catch (ParseException e) {
			throw IQException.createForbidden();
		}
	}
	
	public void runIQ(final IQ request) throws IQException {
		Log.debug("handling IQ packet " + request);

		try {
			final XmppClient client = XmppClientManager.getInstance().getClient(request.getFrom());
			long serial = client.getStoreClient().allocateSerial();
			final IQ reply = IQ.createResultIQ(request);
			
			if (annotation.needsTransaction()) {
				handler.runTaskInNewTransaction(new Callable<Boolean>() {
					public Boolean call() throws IQException {
						DataModel model = DataService.getModel();
						DMSession session;
							
						if (annotation.type() == IQ.Type.get)
							session = model.initializeReadOnlySession(client);
						else
							session = model.initializeReadWriteSession(client);
						
						doIQ((UserViewpoint)session.getViewpoint(), request, reply);
						return true;
					}
				});
			} else {
				doIQ(new UserViewpoint(getUserId(request)), request, reply);
			}

			client.queuePacket(reply, serial);
				
		} catch (IQException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception running IQ method in transaction", e);
		}
	}
	
	public static AnnotatedIQMethod getForMethod(AnnotatedIQHandler handler, Method method) {
		IQMethod annotation = method.getAnnotation(IQMethod.class);
		if (annotation == null)
			return null;
		
		Class<?>[] parameterTypes = method.getParameterTypes();
		
		if (method.getReturnType().equals(void.class) &&
			parameterTypes.length == 2 && 
			parameterTypes[0].equals(IQ.class) &&
			parameterTypes[1].equals(IQ.class)) {
			return new GenericIQMethod(handler, method, annotation, false);
		} else if (method.getReturnType().equals(void.class) &&
				   parameterTypes.length == 3 && 
			       parameterTypes[0].equals(UserViewpoint.class) &&
			       parameterTypes[1].equals(IQ.class) &&
			       parameterTypes[2].equals(IQ.class)) {
			return new GenericIQMethod(handler, method, annotation, true);
		} else if (DMObject.class.isAssignableFrom(method.getReturnType())) {
			return new SingleQueryIQMethod(handler, method, annotation);
		} else if (Collection.class.isAssignableFrom(method.getReturnType())) {
			return MultiQueryIQMethod.getForMethod(handler, method, annotation);
		} else {
			throw new RuntimeException(method + ": Unexpected signature for IQ handler method");
		}
	}
}
