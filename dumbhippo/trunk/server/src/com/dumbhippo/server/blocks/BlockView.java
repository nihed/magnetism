package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.DateUtils;
import com.dumbhippo.StringUtils;
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
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.Viewpoint;

public abstract class BlockView implements ObjectView {
	public static final int RECENT_MESSAGE_COUNT = 5;
		
	private Block block;
	private UserBlockData userBlockData;
	private GroupBlockData groupBlockData;
	private Viewpoint viewpoint;
	private List<ChatMessageView> recentMessages;
	private int messageCount = -1;
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
	
	public List<ChatMessageView> getRecentMessages() {
		if (recentMessages != null)
			return recentMessages;
		else
			return Collections.emptyList();
	}

	public void setRecentMessages(List<ChatMessageView> recentMessages) {
		this.recentMessages = recentMessages;
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}
	
	public ChatMessageView getLastMessage() {
		if (recentMessages == null)
			return null;
		
		try {
			return recentMessages.get(0);
		} catch (IndexOutOfBoundsException e) {
			return null;
		} 
	}
	
	public Guid getIdentifyingGuid() {
		return block.getGuid();
	}
	
	public boolean isPublic() { 
		return getBlock().isPublicBlock(); 
	}
	
	public String getPrivacyTip() {
		throw new RuntimeException("No privacy tip specified for non-public block: " + block);
	}
	
	public StackReason getStackReason() {
		if (userBlockData != null)
			return participated ? userBlockData.getParticipatedReason() : userBlockData.getStackReason();
		else if (groupBlockData != null)
			return participated ? groupBlockData.getParticipatedReason() : groupBlockData.getStackReason();
		else
			return StackReason.NEW_BLOCK;
	}
	
	// Kind of ugly to have this here, but it seems cleaner to have all the logic
	// for filtering in one place
	public boolean isFeed() {
		return false;
	}
	
	public boolean isMine() {
		return block.getBlockType().getBlockOwnership() == BlockType.BlockOwnership.DIRECT_DATA1 ||
        	   block.getBlockType().getBlockOwnership() == BlockType.BlockOwnership.DIRECT_DATA2;
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

		int messageCount = getMessageCount();
		String messageCountString = messageCount >= 0 ? Integer.toString(messageCount) : null;
		
		// not a shining example of OO design, but simpler than the alternatives...
		// (this is essentially a workaround for lack of "mixins" or something)
		boolean isTitle = this instanceof TitleBlockView;
		boolean isTitleDescription = this instanceof TitleDescriptionBlockView;
		boolean hasSource = this instanceof EntitySourceBlockView;
		boolean hasThumbnails = this instanceof ThumbnailsBlockView;
		
		// the "generic type" of a block allows the client to use a fallback 
		// means of displaying the block
		StringBuilder sb = new StringBuilder();
		if (isTitleDescription)
			sb.append("TITLE_DESCRIPTION");
		if (isTitle) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append("TITLE");
		}
		if (hasSource) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append("ENTITY_SOURCE");
		}
		if (hasThumbnails) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append("THUMBNAILS");
		}
		
		String genericTypes = sb.length() > 0 ? sb.toString() : null;
		
		builder.openElement("block",
							"id", block.getId(),
							"type", block.getBlockType().name(),
							"genericTypes", genericTypes, // if genericTypes is null this is omitted
							"isPublic", Boolean.toString(isPublic()),							
							"timestamp", Long.toString(block.getTimestampAsLong()),
							"clickedCount", clickedCountString,
							"chatId", getChatId(),
							"messageCount", messageCountString,
							"significantClickedCount", significantClickedCountString,
							"ignored", Boolean.toString(userBlockData.isIgnored()),
							"ignoredTimestamp", Long.toString(userBlockData.getIgnoredTimestampAsLong()),
							"clicked", Boolean.toString(userBlockData.isClicked()),
							"clickedTimestamp", Long.toString(userBlockData.getClickedTimestampAsLong()),
							"stackReason", getStackReason().name(),
							"filterFlags", isFeed() ? "FEED" : "",
							"isFeed", "" + isFeed(),
							"isMine", "" + isMine(),
							"icon", getIcon());

		if (hasSource)
			builder.appendEmptyNode("source", "id", ((EntitySourceBlockView) this).getEntitySource().getIdentifyingGuid().toString());
		
		if (isTitle) {
			String title = ((TitleBlockView) this).getTitle();
			String link = ((TitleBlockView) this).getLink();
			builder.appendTextNode("title", title, "link", link);
		}
		
		if (isTitleDescription)
			builder.appendTextNode("description", ((TitleDescriptionBlockView) this).getDescription());
		
		if (hasThumbnails)
			writeThumbnailsToXmlBuilder(builder, ((ThumbnailsBlockView) this));
		
		if (block.getBlockType().isDirectlyChattable()) {
			builder.openElement("recentMessages");
			for (ChatMessageView message : getRecentMessages()) {
				message.writeToXmlBuilder(builder);
			}
			builder.closeElement();
		}
		
		writeDetailsToXmlBuilder(builder);
		
		builder.closeElement();
	}
	
	protected abstract void writeDetailsToXmlBuilder(XmlBuilder builder);	
	
	/** This is used by the flash badge, which is more "thin client" than the 
	 * web and windows/linux clients and thus needs a lot less info. In general the 
	 * flash badge does not know about specific block types, and we'd like to keep it 
	 * that way, so if you find yourself doing the equivalent of writeDetailsToXmlBuilder()
	 * in this method you are probably wrong.
	 * 
	 * A flash badge item looks for example like:
	 * 
	 * Dugg (1 hour ago)
	 * &lt;a href="http://storylink"&gt;Story Title&lt;/a&gt;
	 * 
	 * where "Dugg" = summaryHeading, "http://storylink" = summaryLink, 
	 * "Story Title" = summaryLinkText, "1 hour ago" = summaryTimeAgo 
	 * 
	 * The "summary" properties are also used by the Google Gadget, though 
	 * not via this XML.
	 * 
	 * @param builder builder to write to
	 */
	public void writeSummaryToXmlBuilder(XmlBuilder builder) {
		if (!isPopulated())
			throw new RuntimeException("Attempt to write blockview to xml without populating it: " + this);
		
		long sortTimestamp = block.getTimestampAsLong();
		if (userBlockData.isIgnored() && userBlockData.getIgnoredTimestampAsLong() < sortTimestamp)
			sortTimestamp = userBlockData.getIgnoredTimestampAsLong();
		
		builder.appendEmptyNode("block",
				"id", block.getId(),
				"sortTimestamp", Long.toString(sortTimestamp),
				"timeAgo", getSummaryTimeAgo(),
				"heading", getSummaryHeading(),
				"link", getSummaryLink(),
				"linkText", getSummaryLinkText());
	}
	
	/** See writeSummaryToXmlBuilder(), this provides the time ago shown in the
	 * flash badge which has short summary versions of blocks 
	 */
	public final String getSummaryTimeAgo() {
		return DateUtils.formatTimeAgo(block.getTimestamp());
	}
	
	/** See writeSummaryToXmlBuilder(), this provides a short heading shown in the
	 * flash badge which has short summary versions of blocks. The style is like 
	 * "Dugg", "Posted", etc. see other existing examples.
	 */
	public abstract String getSummaryHeading();
	
	/** See writeSummaryToXmlBuilder(), this provides the href for the link shown in the
	 * flash badge which has short summary versions of blocks.
	 */
	public abstract String getSummaryLink();
	
	/** See writeSummaryToXmlBuilder(), this provides the text for the link shown in the flash 
	 * badge which has short summary versions of blocks.
	 */
	public abstract String getSummaryLinkText();
	
	// utility function for use in implementations of writeDetailsToXmlBuilder
	protected void writeFeedEntryToXmlBuilder(XmlBuilder builder, FeedEntry entry) {
		builder.appendTextNode("feedEntry", StringUtils.ellipsizeText(entry.getDescription()),
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
	
	/**
	 * Gets the ID of the chat room that should be used for chatting about
	 * this block.
	 * 
	 * @return the ID of the chat room, or null if the block isn't chattable.
	 */
	public String getChatId() {
		if (block.getBlockType().isDirectlyChattable())
			return block.getId();
		else
			return null;
	}
}
