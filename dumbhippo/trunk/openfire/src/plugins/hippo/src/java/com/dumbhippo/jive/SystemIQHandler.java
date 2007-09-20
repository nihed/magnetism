package com.dumbhippo.jive;

import org.xmpp.packet.IQ;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;

@IQHandler(namespace=SystemIQHandler.SYSTEM_NAMESPACE)
public class SystemIQHandler extends AnnotatedIQHandler {
	static final String SYSTEM_NAMESPACE = "http://mugshot.org/p/system";
	
	protected SystemIQHandler() {
		super("Mugshot system IQ Handler");
	}

	@IQMethod(name="getResource", type=IQ.Type.get)
	@IQParams({ "resourceId" })
	public DMObject<?> getResource(DMObject<?> resource) throws IQException {
		return resource;
	}
}
