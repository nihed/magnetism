package com.dumbhippo.jive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;

import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.dm.ChangeNotificationSet;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.ReadWriteSession;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxUtils;

public abstract class AnnotatedIQMethod {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(AnnotatedIQMethod.class);	
	
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

	// We split IQ processing into two phases
	//
	//  - Phase1 makes updates and computes what data model objects to return
	//  - Phase2 fetches data to return to the user
	//
	// In the case of an update (set) method, we run the phases in separate
	// transactions, and we generate any change notifications from the update
	// before phase2.
	//
	// The downside of generating the change notifications synchronously
	// is that we are generating them for *all* users on the local server, 
	// not just the notification we need (for this user). With a bit more
	// coding we could split things so that we do the resolution of the
	// change set for all users synchronously, but only do the notification
	// fetch synchronously for *this* user and do the notifications for other
	// users asynchronously. The speed of returning from the IQ may determine 
	// the quality of the user interaction on the client.
	
	public abstract Object doIQPhase1(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException, RetryException;
	public abstract void doIQPhase2(UserViewpoint viewpoint, IQ request, IQ reply, Object resultObject) throws IQException, RetryException;
	
	protected Object invokeMethod(Object... params) throws IQException, RetryException {
		try {
			return method.invoke(handler, params);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error invoking IQ method", e);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException(); 
			if (targetException instanceof IQException)
				throw (IQException)targetException;
			else if (targetException instanceof RetryException)
				throw (RetryException)targetException;
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
		logger.debug("Starting IQ request run");
		long iqStartTime = new Date().getTime();
		try {
			final DataModel model = DataService.getModel();
			final XmppClient client = XmppClientManager.getInstance().getClient(request.getFrom());
			boolean readWrite = (annotation.type() == IQ.Type.set || annotation.forceReadWrite());
			final IQ reply = IQ.createResultIQ(request);
			boolean success = false;
			
			if (readWrite && annotation.needsTransaction()) {
				final ChangeNotificationSet[] notifications = new ChangeNotificationSet[1];
				
				final Object resultObject = TxUtils.runInTransaction(new Callable<Object>() {
					public Object call() throws IQException, RetryException {
						ReadWriteSession session = model.initializeReadWriteSession(client);
						
						// We want to order things so that the notifications get sent
						// back *before* our response to the update IQ
						notifications[0] = session.getNotifications();
						notifications[0].setAutoNotify(false);

						// Passing in the request to "phase 1" is for GenericIQMethod
						// wherethe IQ might return "plain old data", not fetched
						// from the data model
						return doIQPhase1((UserViewpoint)session.getViewpoint(), request, reply);
					}
				});
				
				model.sendNotifications(notifications[0]);
				
				long serial = client.getStoreClient().allocateSerial();
				
				try {
					if (needsFetchTransaction()) {
						TxUtils.runInTransaction(new Callable<Boolean>() {
							public Boolean call() throws IQException, RetryException {
								DataModel model = DataService.getModel();
								DMSession session = model.initializeReadOnlySession(client);
								doIQPhase2((UserViewpoint)session.getViewpoint(), request, reply, resultObject);
								return true;
							}
						});
					}
					
					success = true;
				} finally {
					if (success)
						client.queuePacket(reply, serial);
					else
						client.nullNotification(serial);
				}
				
			} else {
				long serial = client.getStoreClient().allocateSerial();
				
				try {
					if (annotation.needsTransaction()) {
						TxUtils.runInTransaction(new Callable<Boolean>() {
							public Boolean call() throws IQException, RetryException {
								DMSession session;
									
								session = model.initializeReadOnlySession(client);
								
								Object resultObject = doIQPhase1((UserViewpoint)session.getViewpoint(), request, reply);
								doIQPhase2((UserViewpoint)session.getViewpoint(), request, reply, resultObject);
								
								return true;
							}
						});
					} else {
						UserViewpoint viewpoint = new UserViewpoint(getUserId(request), Site.XMPP);
						Object resultObject = doIQPhase1(viewpoint, request, reply);
						doIQPhase2(viewpoint, request, reply, resultObject);
					}
					
					success = true;
				} finally {
					if (success)
						client.queuePacket(reply, serial);
					else
						client.nullNotification(serial);
				}
			}
		} catch (IQException e) {
			logger.error("Failure during IQ request run {}", e.getMessage());			
			throw e;
		} catch (Exception e) {
			logger.error("General failure during IQ request run " + e.getStackTrace());			
			throw new RuntimeException("Unexpected exception running IQ method in transaction", e);
		}
		logger.debug("Completed IQ request run in {}s", (new Date().getTime() - iqStartTime)/1000);		
	}

	/**
	 * We don't want to fetch data from the data model within a ReadWrite transaction
	 * because:
	 * 
	 *  - Caching is disabled within a ReadWrite transaction
	 *  - If the transaction is rolled back on commit, then we'll have an incorrect
	 *    view of what data the client has received
	 *    
	 * So in order to return a result from an IQ method, we first do the update in
	 * a read-write transaction, and then use a separate read-only transaction to
	 * fetch the requested data.
	 * 
	 * @return whether there should be separate transactions for update and fetch 
	 */
	protected boolean needsFetchTransaction() {
		return false;
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
		} else if (method.getReturnType().equals(void.class) &&
				   annotation.type() == IQ.Type.set) {
			return new VoidIQMethod(handler, method, annotation);
		} else {
			throw new RuntimeException(method + ": Unexpected signature for IQ handler method");
		}
	}
}
