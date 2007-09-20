package com.dumbhippo.jive;

import java.lang.reflect.Method;

import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

/**
 * Used for data-model update IQ methods (which always have no return).
 */
public class UpdateIQMethod extends QueryIQMethod {
	public UpdateIQMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		super(handler, method, annotation);
	}
	
	@Override
	public void doIQ(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException, RetryException {
		invokeMethod(getParams(viewpoint, request));
	}
}
