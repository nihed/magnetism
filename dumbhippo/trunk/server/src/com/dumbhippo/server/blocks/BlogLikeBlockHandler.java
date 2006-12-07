package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;

@Local
public interface BlogLikeBlockHandler extends BlockHandler {
	public BlockKey getKey(User user);
	public BlockKey getKey(Guid userId);
}
