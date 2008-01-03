package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/groupMessage")
public abstract class GroupMessageDMO extends ChatMessageDMO {
	protected GroupMessageDMO(ChatMessageKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		message = em.find(GroupMessage.class, getKey().getId());
		if (message == null)
			throw new NotFoundException("No such group message");
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		GroupDMO group = session.findUnchecked(GroupDMO.class, ((GroupMessage)message).getGroup().getGuid()); 
		return group.getStoreKey();
	}
}
