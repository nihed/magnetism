package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.listeners.PostChatListener;
import com.dumbhippo.server.listeners.PostClickedListener;
import com.dumbhippo.server.listeners.PostListener;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface PostBlockHandler
	extends BlockHandler, PostListener, PostChatListener, PostClickedListener {

	public BlockKey getKey(Post post);
	public BlockKey getKey(Guid postId);
	
	public Block lookupBlock(Post post);
	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Post post) throws NotFoundException;
	public void setPostHushed(UserViewpoint viewpoint, Post post, boolean hushed);
	
}
