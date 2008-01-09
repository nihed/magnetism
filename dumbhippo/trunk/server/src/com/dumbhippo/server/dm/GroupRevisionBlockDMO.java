package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.blocks.GroupRevisionBlockView;

@DMO(classId="http://mugshot.org/p/o/groupRevisionBlock")
public abstract class GroupRevisionBlockDMO extends BlockDMO {
	protected GroupRevisionBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@DMProperty(defaultInclude=true, defaultChildren="+")
	public GroupDMO getGroup() {
		return session.findUnchecked(GroupDMO.class, ((GroupRevisionBlockView)blockView).getGroupView().getGroup().getGuid());
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public UserDMO getRevisor() {
		return session.findUnchecked(UserDMO.class, ((GroupRevisionBlockView)blockView).getRevisorView().getUser().getGuid());
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		return getGroup().getStoreKey();
	}
}
