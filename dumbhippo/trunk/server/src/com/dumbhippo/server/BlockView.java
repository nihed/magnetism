package com.dumbhippo.server;

import com.dumbhippo.DateUtils;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public abstract class BlockView {
	private Block block;
	private UserBlockData userBlockData;

	public BlockView(Block block, UserBlockData ubd) {
		this.block = block;
		this.userBlockData = ubd;
	}

	public Block getBlock() {
		return block;
	}
	
	public UserBlockData getUserBlockData() {
		return userBlockData;
	}	
	
	public String getTimeAgo() {
		return DateUtils.formatTimeAgo(block.getTimestamp());
	}
	
	public abstract String getWebTitleType();
	
	public abstract String getWebTitle();
	
	public String getWebTitleLink() { return null; }
	
	public abstract String getIconName();

}
