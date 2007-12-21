package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.persistence.BlockMessage;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/blockMessage")
public abstract class BlockMessageDMO extends ChatMessageDMO {
	protected BlockMessageDMO(ChatMessageKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		message = em.find(BlockMessage.class, getKey().getId());
		if (message == null)
			throw new NotFoundException("No such block message");
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		BlockDMO block = session.findUnchecked(BlockDMO.class, new BlockDMOKey(((BlockMessage)message).getBlock())); 
		return block.getStoreKey();
	}
}
