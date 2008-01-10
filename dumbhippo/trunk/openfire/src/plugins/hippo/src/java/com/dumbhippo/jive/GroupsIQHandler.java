package com.dumbhippo.jive;

import javax.ejb.EJB;

import org.xmpp.packet.IQ;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=GroupsIQHandler.GROUPS_NAMESPACE)
public class GroupsIQHandler extends AnnotatedIQHandler {
	static final String GROUPS_NAMESPACE = "http://mugshot.org/p/groups"; 
	
	@EJB
	private MessengerGlue glue;
	
	public GroupsIQHandler() {
		super("Mugshot Groups IQ Handler");
	}
	
	@IQMethod(name="inviteUser", type=IQ.Type.set)
	@IQParams({ "groupId", "userId" })
	public void inviteUser(UserViewpoint viewpoint, Guid groupId, Guid userId) throws IQException {
        try {
			glue.addGroupMember(viewpoint.getViewer().getGuid(), groupId, userId);
		} catch (NotFoundException e) {
			throw IQException.createBadRequest("bad groupId or userId parameter");
		}
	}
}
