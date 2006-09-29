package com.dumbhippo.server;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class GroupBlockView extends BlockView {
	private GroupView group;
	
	public GroupBlockView(Block block, UserBlockData ubd, GroupView group) {
		super(block, ubd);
		this.group = group;
	}

	public String getWebTitleType() {
		return "Mugshot";
	}
	
	public String getWebTitle() {
		return "Membership change";
	}
	
	public String getIconName() {
		return "mugshot_icon.png";
	}
	
	public GroupView getGroupView() {
		return this.group;
	}
}
