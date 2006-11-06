package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.listeners.PostChatListener;
import com.dumbhippo.server.listeners.PostClickedListener;
import com.dumbhippo.server.listeners.PostListener;

@Local
public interface PostBlockHandler
	extends BlockHandler, PostListener, PostChatListener, PostClickedListener {

	public BlockKey getKey(Post post);
	public BlockKey getKey(Guid postId);
	
}
