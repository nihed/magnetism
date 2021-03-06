package com.dumbhippo.jive;

import java.lang.reflect.Method;

import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

public class GenericIQMethod extends AnnotatedIQMethod {
	private boolean needsViewpoint;

	public GenericIQMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation, boolean needsViewpoint) {
		super(handler, method, annotation);
		
		this.needsViewpoint = needsViewpoint;
	}

	@Override
	public Object doIQPhase1(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException, RetryException {
		if (needsViewpoint)
			invokeMethod(viewpoint, request, reply);
		else
			invokeMethod(request, reply);
		
		return null;
	}
	
	@Override
	public void doIQPhase2(UserViewpoint viewpoint, IQ request, IQ reply, Object resultObject) throws IQException, RetryException {
	}
}
