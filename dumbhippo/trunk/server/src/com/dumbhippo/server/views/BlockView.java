package com.dumbhippo.server.views;

import com.dumbhippo.DateUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.UserBlockData;

public abstract class BlockView implements ObjectView {
	private Block block;
	private UserBlockData userBlockData;

	public BlockView(Block block, UserBlockData ubd) {
		this.block = block;
		this.userBlockData = ubd;
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
	
	public String getTimeAgo() {
		return DateUtils.formatTimeAgo(block.getTimestamp());
	}
	
	public String getDescriptionHtml() { return ""; };
	
	public String getBlockId() {
		// TODO: decide what would be a suitable id or make this method abstract
	    return "";	
	}
	
	public abstract String getWebTitleType();
	
	public abstract String getWebTitle();
	
	public String getWebTitleLink() { return null; }
	
	public abstract String getIconName();
	
	public Guid getIdentifyingGuid() {
		return block.getGuid();
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
							"clickedTimestamp", Long.toString(userBlockData.getClickedTimestampAsLong()));
		
		writeDetailsToXmlBuilder(builder);
		
		builder.closeElement();
	}
	
	protected abstract void writeDetailsToXmlBuilder(XmlBuilder builder);
}
