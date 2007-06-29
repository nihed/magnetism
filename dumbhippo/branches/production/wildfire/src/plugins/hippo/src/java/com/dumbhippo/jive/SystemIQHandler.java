package com.dumbhippo.jive;

import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.ReadOnlySession;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.DataService;

@IQHandler(namespace=SystemIQHandler.SYSTEM_NAMESPACE)
public class SystemIQHandler extends AnnotatedIQHandler {
	static final String SYSTEM_NAMESPACE = "http://mugshot.org/p/system";
	
	protected SystemIQHandler() {
		super("Mugshot system IQ Handler");
	}

	@IQMethod(name="getResource", type=IQ.Type.get)
	@IQParams({ "resourceId" })
	public DMObject<?> getResource(String resourceId) throws IQException {
		ReadOnlySession session = DataService.currentSessionRO();
		try {
			return session.find(resourceId);
		} catch (NotFoundException e) {
			throw new IQException(PacketError.Condition.item_not_found, PacketError.Type.cancel, e.getMessage());
		}
	}
}
