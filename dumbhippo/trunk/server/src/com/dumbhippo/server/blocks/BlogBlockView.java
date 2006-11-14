package com.dumbhippo.server.blocks;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public class BlogBlockView extends AbstractPersonBlockView implements ExternalAccountBlockView {
	
	private FeedEntry entry;
	
	public BlogBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView, FeedEntry entry) {
		super(viewpoint, block, ubd);
		populate(userView, entry);
	}

	public BlogBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
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
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("blogPerson");
		writeFeedEntryToXmlBuilder(builder, entry);
		builder.closeElement();
	}
	
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.BLOG;
	}

	@Override
	public String getIcon() {
		return "/images3/blog_icon.png";
		//return entry.getFeed().getFavicon();
	}
}
