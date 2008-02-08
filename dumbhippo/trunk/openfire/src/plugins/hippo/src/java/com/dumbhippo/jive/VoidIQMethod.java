package com.dumbhippo.jive;

import java.lang.reflect.Method;

import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

/**
 * Used for data-model update IQ methods with no return. (Query methods always have
 * a return.)
 */
public class VoidIQMethod extends QueryIQMethod {
	public VoidIQMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		super(handler, method, annotation);
	}

	@Override
	public boolean needsFetchTransaction() {
		return false;
	}
	
	@Override
	public void doIQPhase2(UserViewpoint viewpoint, IQ request, IQ reply, Object resultObject) throws IQException, RetryException {
	}
}
