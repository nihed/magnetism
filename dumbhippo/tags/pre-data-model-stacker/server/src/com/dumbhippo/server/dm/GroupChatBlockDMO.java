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

	@DMProperty(defaultInclude=true, defaultChildren="+")
	public GroupDMO getGroup() {
		return session.findUnchecked(GroupDMO.class, ((GroupChatBlockView)blockView).getGroupView().getGroup().getGuid());
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		return getGroup().getStoreKey();
	}
	
	// GroupMemberBlockView returns "" for the title link, and I'm hestitant to change
	// that for fear something is depending on it in the JSP's, so we just override
	// it here to a more sensible null.
	@Override
	public String getTitleLink() {
		return null;
	}
}
