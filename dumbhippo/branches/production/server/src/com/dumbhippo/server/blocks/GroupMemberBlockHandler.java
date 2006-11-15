package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.GroupMembershipListener;

@Local
public interface GroupMemberBlockHandler
	extends BlockHandler, GroupMembershipListener {
	public BlockKey getKey(Group group, User user);
	public BlockKey getKey(Guid groupId, Guid userId);
}
