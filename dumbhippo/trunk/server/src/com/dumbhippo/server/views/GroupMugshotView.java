package com.dumbhippo.server.views;

import java.util.List;

public class GroupMugshotView {

	private GroupView groupView;
	private List<BlockView> blocks;
	
	public GroupMugshotView(GroupView personView, List<BlockView> blocks) {
		this.groupView = personView;
		this.blocks = blocks;
	}

	public GroupView getGroupView() {
		return groupView;
	}
	
	public List<BlockView> getBlocks() {
		return blocks;
	}
}
