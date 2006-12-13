package com.dumbhippo.server.blocks;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;

public interface SingleBlockForFeedBlockHandler extends BlockHandler {
	public BlockKey getKey(User user);
	public BlockKey getKey(Guid userId);
}
