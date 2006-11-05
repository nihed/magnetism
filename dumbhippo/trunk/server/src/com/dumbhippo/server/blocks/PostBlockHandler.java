package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Post;

@Local
public interface PostBlockHandler extends BlockHandler {

	public BlockKey getKey(Post post);
	public BlockKey getKey(Guid postId);
}
