package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.listeners.GroupCreationListener;

@Local
public interface GroupChatBlockHandler
	extends BlockHandler, GroupCreationListener {
	public BlockKey getKey(Group group);
	public BlockKey getKey(Guid groupId);
}
