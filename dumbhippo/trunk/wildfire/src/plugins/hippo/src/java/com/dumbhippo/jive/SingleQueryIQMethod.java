package com.dumbhippo.jive;

import java.lang.reflect.Method;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

public class SingleQueryIQMethod extends QueryIQMethod {
	public SingleQueryIQMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		super(handler, method, annotation);
	}
	
	@SuppressWarnings("unchecked")
	private static void fetchAndVisit(DMSession session, DMObject<?> resultObject, FetchNode fetchNode, XmppFetchVisitor visitor) {
		DMClassHolder classHolder = resultObject.getClassHolder(); 
		Fetch fetch = fetchNode.bind(classHolder);
		
		fetch.visit(session, classHolder, resultObject, visitor);
	}
	
	@Override
	public void doIQ(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException, RetryException {
		DMSession session = DataService.currentSessionRO();
		Element root = reply.setChildElement(annotation.name(), handler.getInfo().getNamespace());
		
		FetchNode fetchNode = getFetchNode(request);
		
		DMObject<?> resultObject = (DMObject<?>)invokeMethod(getParams(viewpoint, request));
		
		resultObject.getClassHolder();
		
		XmppFetchVisitor visitor = new XmppFetchVisitor(root, session.getModel());
		fetchAndVisit(session, resultObject, fetchNode, visitor);
	}
}
