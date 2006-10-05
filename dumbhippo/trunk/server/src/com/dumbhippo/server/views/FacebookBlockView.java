package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.UserBlockData;

public class FacebookBlockView extends BlockView {
	private PersonView userView;
	private List<FacebookEvent> facebookEvents;
	
	public FacebookBlockView(Block block, UserBlockData ubd, PersonView userView, List<FacebookEvent> facebookEvents) {
		super(block, ubd);
		this.userView = userView;
		this.facebookEvents = facebookEvents;
	}

	public String getWebTitleType() {
		return "Facebook";
	}

	public String getWebTitle() {
		return "new updates";
	}
	
	public String getIconName() {
		return ExternalAccountType.FACEBOOK.getIconName();
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	public List<FacebookEvent> getFacebookEvents() {
	    return facebookEvents;	
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("updateItem",
				                "updateTitle", "new updates",
				                "updateLink", "http:://www.facebook.com",
				                "updateText", "");
	}
	
	public List<Object> getReferencedObjects() {
		return Collections.singletonList((Object)userView);
	}
}