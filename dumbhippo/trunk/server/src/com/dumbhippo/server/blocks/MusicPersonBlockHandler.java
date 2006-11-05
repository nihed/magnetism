package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.UserCreationListener;

@Local
public interface MusicPersonBlockHandler
	extends BlockHandler, UserCreationListener {

	public BlockKey getKey(User user);
	public BlockKey getKey(Guid userId);
}
