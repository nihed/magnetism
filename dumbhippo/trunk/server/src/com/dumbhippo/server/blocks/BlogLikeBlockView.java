package com.dumbhippo.server.blocks;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Abstract class for block views of external accounts which act 
 * like blogs - MySpace, LiveJournal, and the arbitrary blog feed.
 * 
 * @author walters
 */
public abstract class BlogLikeBlockView extends AbstractPersonBlockView implements ExternalAccountBlockView, SimpleTitleBlockView {
	
	protected FeedEntry entry;
	
	public BlogLikeBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public BlogLikeBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(PersonView userView, FeedEntry entry) {
		partiallyPopulate(userView);
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
					"userId", getUserView().getUser().getId());
		writeFeedEntryToXmlBuilder(builder, entry);
		builder.closeElement();
	}
	
	public String getDescriptionAsHtml() {
		XmlBuilder xml = new XmlBuilder();
		xml.appendTextAsHtml(getEntry().getDescription(), null);
		return xml.toString();
	}	
	
	public String getTitle() {
		return getEntry().getTitle();
	}

	public String getLink() {
		return getEntry().getLink().getUrl();
	}

	public String getTitleForHome() {
		return getTitle();
	}

	public @Override String getSummaryLink() {
		return entry.getLink().getUrl();
	}

	public @Override String getSummaryLinkText() {
		return entry.getTitle();
	}
}
