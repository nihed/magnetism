package com.dumbhippo.server.blocks;

import com.dumbhippo.DateUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public abstract class BlockView implements ObjectView {
	
	private Block block;
	private UserBlockData userBlockData;
	private Viewpoint viewpoint;
	private boolean populated;

	public BlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		this.viewpoint = viewpoint;
		this.block = block;
		this.userBlockData = ubd;
		this.populated = false;
	}

	public Block getBlock() {
		return block;
	}
	
	public BlockType getBlockType() {
		return getBlock().getBlockType();
	}
	
	public UserBlockData getUserBlockData() {
		return userBlockData;
	}	
	
	public Viewpoint getViewpoint() {
		return viewpoint;
	}
	
	public abstract String getIcon();
	
	public boolean isPopulated() {
		return populated;
	}
	
	public void setPopulated(boolean populated) {
		this.populated = populated;
	}
	
	public String getTimeAgo() {
		return DateUtils.formatTimeAgo(block.getTimestamp());
	}
	
	public Guid getIdentifyingGuid() {
		return block.getGuid();
	}
	
	// web swarms or group chats do not have a person source, while music and external account updates do
	public PersonView getPersonSource() {
		return null;
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("block",
							"id", block.getId(),
							"type", block.getBlockType().name(),
							"timestamp", Long.toString(block.getTimestampAsLong()),
							"clickedCount", Integer.toString(block.getClickedCount()),
							"ignored", Boolean.toString(userBlockData.isIgnored()),
							"ignoredTimestamp", Long.toString(userBlockData.getIgnoredTimestampAsLong()),
							"clicked", Boolean.toString(userBlockData.isClicked()),
							"clickedTimestamp", Long.toString(userBlockData.getClickedTimestampAsLong()),
							"icon", getIcon());
		
		writeDetailsToXmlBuilder(builder);
		
		builder.closeElement();
	}
	
	protected abstract void writeDetailsToXmlBuilder(XmlBuilder builder);
}
