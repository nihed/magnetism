package com.dumbhippo.server.blocks;

import com.dumbhippo.DateUtils;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public abstract class BlockView implements ObjectView {
	
	private Block block;
	private UserBlockData userBlockData;
	private GroupBlockData groupBlockData;
	private Viewpoint viewpoint;
	private boolean populated;
	private boolean participated; 

	public BlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		this.viewpoint = viewpoint;
		this.block = block;
		this.userBlockData = ubd;
		this.populated = false;
		this.participated = participated;
	}

	public BlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		this.viewpoint = viewpoint;
		this.block = block;
		this.groupBlockData = gbd;
		this.populated = false;
		this.participated = participated;
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
	
	public boolean isPublic() { 
		return getBlock().isPublicBlock(); 
	}
	
	public String getPrivacyTip() {
		throw new RuntimeException("No privacy tip specified for non-public block");
	}
	
	public StackReason getStackReason() {
		if (userBlockData != null)
			return participated ? userBlockData.getParticipatedReason() : userBlockData.getStackReason();
		else if (groupBlockData != null)
			return participated ? groupBlockData.getParticipatedReason() : groupBlockData.getStackReason();
		else
			return StackReason.NEW_BLOCK;
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		if (!isPopulated())
			throw new RuntimeException("Attempt to write blockview to xml without populating it: " + this);
		
		String clickedCountString;
		String significantClickedCountString;
		if (block.getBlockType().getClickedCountUseful()) {
			clickedCountString = Integer.toString(block.getClickedCount());
			significantClickedCountString = Integer.toString(getSignificantClickedCount());
		} else {
			// FIXME this is to be nice to older clients. We need to check that the client 
			// handles this attribute's absence, then phase this out to use null instead
			// (causing XmlBuilder to omit the attr entirely)
			clickedCountString = "-1"; // null;
			significantClickedCountString = "-1"; // null; 
		}
		
		builder.openElement("block",
							"id", block.getId(),
							"type", block.getBlockType().name(),
							"isPublic", Boolean.toString(isPublic()),							
							"timestamp", Long.toString(block.getTimestampAsLong()),
							"clickedCount", clickedCountString,
							"significantClickedCount", significantClickedCountString,
							"ignored", Boolean.toString(userBlockData.isIgnored()),
							"ignoredTimestamp", Long.toString(userBlockData.getIgnoredTimestampAsLong()),
							"clicked", Boolean.toString(userBlockData.isClicked()),
							"clickedTimestamp", Long.toString(userBlockData.getClickedTimestampAsLong()),
							"stackReason", getStackReason().name(),
							"icon", getIcon());
		
		writeDetailsToXmlBuilder(builder);
		
		builder.closeElement();
	}
	
	protected abstract void writeDetailsToXmlBuilder(XmlBuilder builder);	
	
	/** This is used by the flash embed, which is more "thin client" than the 
	 * web and windows/linux clients and thus needs a lot less info. In general the 
	 * flash embed does not know about specific block types, and we'd like to keep it 
	 * that way, so if you find yourself doing the equivalent of writeDetailsToXmlBuilder()
	 * in this method you are probably wrong.
	 * 
	 * @param builder builder to write to
	 */
	public void writeSummaryToXmlBuilder(XmlBuilder builder) {
		if (!isPopulated())
			throw new RuntimeException("Attempt to write blockview to xml without populating it: " + this);
		
		long sortTimestamp = block.getTimestampAsLong();
		if (userBlockData.getIgnoredTimestampAsLong() < sortTimestamp)
			sortTimestamp = userBlockData.getIgnoredTimestampAsLong();
		
		builder.appendEmptyNode("block",
				"id", block.getId(),
				"sortTimestamp", Long.toString(sortTimestamp),
				"timeAgo", getSummaryTimeAgo(),
				"heading", getSummaryHeading(),
				"link", getSummaryLink(),
				"linkText", getSummaryLinkText());
	}
	
	public final String getSummaryTimeAgo() {
		return DateUtils.formatTimeAgo(block.getTimestamp());
	}
	
	public abstract String getSummaryHeading();
	
	public abstract String getSummaryLink();
	
	public abstract String getSummaryLinkText();
	
	// utility function for use in implementations of writeDetailsToXmlBuilder
	protected void writeFeedEntryToXmlBuilder(XmlBuilder builder, FeedEntry entry) {
		builder.appendTextNode("feedEntry", entry.getDescription(),
				"title", entry.getTitle(),
				"href", entry.getLink().getUrl());
	}
	
	// utility function for use in implementations of writeDetailsToXmlBuilder
	protected void writeThumbnailsToXmlBuilder(XmlBuilder builder, ThumbnailsBlockView thumbnailsBlock) {
		Thumbnails thumbnails = thumbnailsBlock.getThumbnails();
		
		if (thumbnails == null)
			throw new IllegalStateException("ThumbnailsBlockView may not return null from getThumbnails()");
		
		builder.openElement("thumbnails", "count", Integer.toString(thumbnails.getThumbnailCount()),
				"maxWidth", Integer.toString(thumbnails.getThumbnailWidth()),
				"maxHeight", Integer.toString(thumbnails.getThumbnailHeight()), 
				"totalItems", Integer.toString(thumbnails.getTotalThumbnailItems()),
				"totalItemsString", thumbnails.getTotalThumbnailItemsString(),
				"moreTitle", thumbnailsBlock.getMoreThumbnailsTitle(),
				"moreLink", thumbnailsBlock.getMoreThumbnailsLink());
		for (Thumbnail t : thumbnails.getThumbnails()) {
			builder.appendEmptyNode("thumbnail",
					"title", t.getThumbnailTitle(), 
					"src", t.getThumbnailSrc(),
					"href", t.getThumbnailHref(), 
					"width", Integer.toString(t.getThumbnailWidth()),
					"height", Integer.toString(t.getThumbnailHeight()));
		}
		builder.closeElement();
	}
	
	@Override
	public String toString() {
		return "{view of " + block + "}";
	}
	
	static final int SIGNIFICANT_CLICKS[] = {
		5,10,25,100,500,1000
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
