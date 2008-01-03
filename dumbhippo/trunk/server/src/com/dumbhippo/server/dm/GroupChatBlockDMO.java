package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.blocks.GroupChatBlockView;

@DMO(classId="http://mugshot.org/p/o/groupChatBlock")
public abstract class GroupChatBlockDMO extends BlockDMO {
	protected GroupChatBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public String getChatId() {
		return ((GroupChatBlockView)blockView).getGroupView().getGroup().getId();
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public GroupDMO getGroup() {
		return session.findUnchecked(GroupDMO.class, ((GroupChatBlockView)blockView).getGroupView().getGroup().getGuid());
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		return getGroup().getStoreKey();
	}
}
