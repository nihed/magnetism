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
	
	public BlogBlockView(Block block, UserBlockData ubd, PersonView userView, FeedEntry entry) {
		super(block, ubd);
		this.userView = userView;
		this.entry = entry;
	}

	public String getWebTitleType() {
		return "Blog";
	}

	public String getWebTitle() {
		return entry.getTitle();
	}
	
	public String getIconName() {
		return ExternalAccountType.BLOG.getIconName();
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	public FeedEntry getEntry() {
	    return entry;	
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
