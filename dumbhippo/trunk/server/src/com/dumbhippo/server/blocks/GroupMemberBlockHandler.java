package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.User;

@Local
public interface GroupMemberBlockHandler extends BlockHandler {
	public BlockKey getKey(GroupMember member);
	public BlockKey getKey(Group group, User user);
	public BlockKey getKey(Guid groupId, Guid userId);
}
