package com.dumbhippo.server.views;

import java.util.List;

import com.dumbhippo.server.blocks.BlockView;

public class GroupMugshotView {

	protected GroupView groupView;
	protected List<BlockView> blocks;
	
	protected GroupMugshotView() {
	}
	
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
