package com.dumbhippo.server.blocks;

import com.dumbhippo.DateUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.FeedEntry;
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
	
	/** Gets icon to be displayed next to the title */
	public abstract String getIcon();	
	
	/** Gets a string like "Flickr photoset" or "Web swarm" indicating the kind of block for 
	 * display to the user 
	 */
	public abstract String getTypeTitle();
	
	public boolean isPopulated() {
		return populated;
	}
	
	/** called by a default-visibility populate() method on each view */
	protected void setPopulated(boolean populated) {
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
		// FIXME: We really shouldn't include clickedCount/significantClickedCount
		//   to all blocks; they only make sense for posts, but the XmlBuilder
		//   API makes that painful.
		builder.openElement("block",
							"id", block.getId(),
							"type", block.getBlockType().name(),
							"timestamp", Long.toString(block.getTimestampAsLong()),
							"clickedCount", Integer.toString(block.getClickedCount()),
							"significantClickedCount", Integer.toString(getSignificantClickedCount()),
							"ignored", Boolean.toString(userBlockData.isIgnored()),
							"ignoredTimestamp", Long.toString(userBlockData.getIgnoredTimestampAsLong()),
							"clicked", Boolean.toString(userBlockData.isClicked()),
							"clickedTimestamp", Long.toString(userBlockData.getClickedTimestampAsLong()),
							"stackReason", userBlockData.getStackReason().name(),
							"icon", getIcon());
		
		writeDetailsToXmlBuilder(builder);
		
		builder.closeElement();
	}
	
	protected abstract void writeDetailsToXmlBuilder(XmlBuilder builder);
	
	// a child multiple block types might have
	protected void writeFeedEntryToXmlBuilder(XmlBuilder builder, FeedEntry entry) {
		builder.appendTextNode("feedEntry", entry.getDescription(),
				"title", entry.getTitle(),
				"href", entry.getLink().getUrl());
	}
	
	@Override
	public String toString() {
		return "{view of " + block + "}";
	}
	
	static final int SIGNIFICANT_CLICKS[] = {
		1,5,10,25,100,500,1000
	};
	
	public static boolean clickedCountIsSignificant(int count) {
		for (int i = 0; i < SIGNIFICANT_CLICKS.length; i++) {
			if (SIGNIFICANT_CLICKS[i] == count)
				return true;
		}
		
		return false;
	}
	
	public int getSignificantClickedCount() {
		int clickCount = block.getClickedCount();
		int result = 0;
		for (int i = 0; i < SIGNIFICANT_CLICKS.length; i++) {
			if (clickCount >= SIGNIFICANT_CLICKS[i]) {
				result = SIGNIFICANT_CLICKS[i];
			}
		}
		
		return result;
	}
}
