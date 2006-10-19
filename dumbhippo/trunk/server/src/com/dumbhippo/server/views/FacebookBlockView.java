package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.UserBlockData;

public class FacebookBlockView extends ExternalAccountBlockView {
	private PersonView userView;
	private List<FacebookEvent> facebookEvents;
	
	public FacebookBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView, List<FacebookEvent> facebookEvents) {
		super(viewpoint, block, ubd);
		this.userView = userView;
		this.facebookEvents = facebookEvents;
		setPopulated(true);
	}

	public FacebookBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	public void setUserView(PersonView userView) {
		this.userView = userView;
	}
	
	public List<FacebookEvent> getFacebookEvents() {
	    return facebookEvents;	
	}
	
	public void setFacebookEvents(List<FacebookEvent> facebookEvents) {
		this.facebookEvents = facebookEvents;
	}
	
	@Override
	public PersonView getPersonSource() {
	    return userView;	
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

	@Override
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.FACEBOOK;
	}
}