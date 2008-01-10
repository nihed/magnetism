package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/postMessage")
public abstract class PostMessageDMO extends ChatMessageDMO {
	protected PostMessageDMO(ChatMessageKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		message = em.find(PostMessage.class, getKey().getId());
		if (message == null)
			throw new NotFoundException("No such post message");
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		PostDMO post = session.findUnchecked(PostDMO.class, ((PostMessage)message).getPost().getGuid()); 
		return post.getStoreKey();
	}
}
