package com.dumbhippo.live;

import java.util.Set;

import com.dumbhippo.identity20.Guid;

public class BlockEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	Guid blockId;
	long stackTimestamp;
	Set<Guid> affectedUsers;

	public BlockEvent(Guid blockId, long stackTimestamp, Set<Guid> affectedUsers) {
		this.blockId = blockId;
		this.stackTimestamp = stackTimestamp;
		this.affectedUsers = affectedUsers;
	}
	
	public Guid getBlockId() {
		return blockId;
	}
	
	public long getStackTimestamp() {
		return stackTimestamp;
	}
	
	public Set<Guid> getAffectedUsers() {
		return affectedUsers;
	}

	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return null;
	}
}
