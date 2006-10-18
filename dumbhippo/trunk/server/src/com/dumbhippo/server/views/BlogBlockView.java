package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.UserBlockData;

public class BlogBlockView extends BlockView {
	
	private PersonView userView;
	private FeedEntry entry;
	
	public BlogBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView, FeedEntry entry) {
		super(viewpoint, block, ubd);
		this.userView = userView;
		this.entry = entry;
		setPopulated(true);
	}

	public BlogBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	@Override
	public String getWebTitleType() {
		return "Blog";
	}

	@Override	
	public String getWebTitle() {
		return entry.getTitle();
	}
	
	@Override	
	public String getIconName() {
		return ExternalAccountType.BLOG.getIconName();
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	public void setUserView(PersonView userView) {
		this.userView = userView;
	}
	
	public FeedEntry getEntry() {
	    return entry;	
	}
	
	public void setEntry(FeedEntry entry) {
		this.entry = entry;
	}
	
	@Override
	public PersonView getPersonSource() {
	    return userView;	
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("updateItem",
				                "updateTitle", entry.getTitle(),
				                "updateLink", entry.getLink().getUrl(),
				                "updateText", entry.getDescription());
	}
	
	public List<Object> getReferencedObjects() {
		return Collections.singletonList((Object)userView);
	}
}
