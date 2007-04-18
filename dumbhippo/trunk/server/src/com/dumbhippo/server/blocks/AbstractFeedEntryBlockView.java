package com.dumbhippo.server.blocks;

import java.util.Date;
import java.util.List;

import com.dumbhippo.DateUtils;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Abstract class for block views of external accounts which have 
 * a feed associated and we have one feed entry for each block 
 * being viewed.
 * 
 * @author walters
 */
public abstract class AbstractFeedEntryBlockView extends AbstractPersonBlockView implements ExternalAccountBlockView, TitleDescriptionBlockView {
	
	protected FeedEntry entry;
	
	public AbstractFeedEntryBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public AbstractFeedEntryBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(PersonView userView, FeedEntry entry, List<ChatMessageView> recentMessages, int messageCount) {
		partiallyPopulate(userView);
		setRecentMessages(recentMessages);
		setMessageCount(messageCount);
		this.entry = entry;
		setPopulated(true);
	}
	
	public FeedEntry getEntry() {
	    return entry;	
	}
	
	public void setEntry(FeedEntry entry) {
		this.entry = entry;
	}
	
	protected abstract String getElementName();
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement(getElementName(),
					"userId", getPersonSource().getUser().getId());
		writeExtendedDetailsToXmlBuilder(builder);		
		builder.closeElement();
	}
	
	protected void writeExtendedDetailsToXmlBuilder(XmlBuilder builder) {
	}
	
	public String getDescriptionAsHtml() {
		String description = getDescription();
		if (description.trim().length() > 0) {
			XmlBuilder xml = new XmlBuilder();
			xml.appendTextAsHtml(description, null);
			return xml.toString();
		} else {
			return "";
		}
	}
	
	public String getDescription() {
		return StringUtils.ellipsizeText(getEntry().getDescription());
	}
	
	public String getTitle() {
		return getEntry().getTitle();
	}
	
	public Date getSentDate() {
		return getEntry().getDate();
	}
	
	public String getSentTimeAgo() {
		return DateUtils.formatTimeAgo(getSentDate());
	}

	public String getLink() {
		return getEntry().getLink().getUrl();
	}

	public String getTitleForHome() {
		return getTitle();
	}

	@Override 
	public String getSummaryLink() {
		return entry.getLink().getUrl();
	}

	 @Override
	public String getSummaryLinkText() {
		return entry.getTitle();
	}
}
