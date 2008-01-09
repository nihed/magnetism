package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.blocks.GroupMemberBlockView;

@DMO(classId="http://mugshot.org/p/o/groupMemberBlock")
public abstract class GroupMemberBlockDMO extends BlockDMO {
	protected GroupMemberBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@DMProperty(defaultInclude=true, defaultChildren="+")
	public GroupDMO getGroup() {
		return session.findUnchecked(GroupDMO.class, ((GroupMemberBlockView)blockView).getGroupView().getGroup().getGuid());
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public UserDMO getMember() {
		return session.findUnchecked(UserDMO.class, ((GroupMemberBlockView)blockView).getMemberView().getUser().getGuid());
	}
	
	@DMProperty(defaultInclude=true)
	public String getStatus() {
		return ((GroupMemberBlockView)blockView).getStatus().name();
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		return getGroup().getStoreKey();
	}
}
