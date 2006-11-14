package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.ListBean;

public class FacebookBlockView extends AbstractPersonBlockView implements ExternalAccountBlockView {
	private List<FacebookEvent> facebookEvents;
	private String link;
	
	public FacebookBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView, List<FacebookEvent> facebookEvents, String link) {
		super(viewpoint, block, ubd);
		populate(userView, facebookEvents, link);
	}

	public FacebookBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	void populate(PersonView userView, List<FacebookEvent> facebookEvents, String link) {
		partiallyPopulate(userView);
		this.facebookEvents = facebookEvents;
		this.link = link;
		setPopulated(true);		
	}
		
	public List<FacebookEvent> getFacebookEvents() {
	    return facebookEvents;	
	}
	
	public FacebookEvent getFacebookEvent() {
		if (facebookEvents.isEmpty())
		    throw new RuntimeException("FacebookEvents list is empty in the FacebookBlockView");
		
	    return facebookEvents.get(0);	
	}	
	
	// this can later be a part of PhotoSetView interface implementation
	public ListBean<FacebookPhotoData> getPhotos() {
		List<FacebookPhotoData> photos = new ArrayList<FacebookPhotoData>();
		if (getFacebookEvent().getPhotos().size() > 0) {
			photos.addAll(getFacebookEvent().getPhotos());	
		} else if (getFacebookEvent().getAlbum() != null){
			photos.add(getFacebookEvent().getAlbum().getCoverPhoto());
		}
		return new ListBean<FacebookPhotoData>(photos);
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("facebookPerson");
		
		for (FacebookEvent event : getFacebookEvents()) {
			builder.appendEmptyNode("event", "type", event.getEventType().name(),
					"count", Integer.toString(event.getCount()),
					"timestamp",  Long.toString(event.getEventTimestampAsLong()));					
		}
		
		builder.closeElement();
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.FACEBOOK;
	}

	@Override
	public String getIcon() {
		return "/images3/" + ExternalAccountType.FACEBOOK.getIconName();
	}

	public String getLink() {
		return link;
	}

	public String getTextForHome() {		
		if (getViewpoint().isOfUser(getUserView().getUser()))
			return getTextForSelf();
		else 		
			return getGenericText();
	}

	public String getTextForSelf() {
        FacebookEvent event = getFacebookEvent();
				
		String pluralChar = "";
		
		if (event.getEventType() == FacebookEventType.NEW_TAGGED_PHOTOS_EVENT) {
		    if (event.getPhotos().size() != 1) 
		        pluralChar = "s";
		} else if (event.getCount() != 1) {
			pluralChar = "s";
		}
		
		switch (event.getEventType()) {
			case UNREAD_MESSAGES_UPDATE :
				return event.getCount() + " unread message" + pluralChar;
			case NEW_WALL_MESSAGES_EVENT :
				return event.getCount() + " new wall message" + pluralChar;
			case UNSEEN_POKES_UPDATE :
				return event.getCount() + " unseen poke" + pluralChar;
			case NEW_TAGGED_PHOTOS_EVENT :
				return "You were tagged in " + event.getPhotos().size() + " photo" + pluralChar;
			case NEW_ALBUM_EVENT :
				return "You created a new album \"" + event.getAlbum().getName() + "\"";
			case MODIFIED_ALBUM_EVENT :
				return "You modified an album \"" + event.getAlbum().getName() + "\"";
			case LOGIN_STATUS_EVENT :
				if (event.getCount() == 0) 
					return "Log in to receive updates";
				else 
					return "You are now logged in and will receive updates";
	   	    // no default, it hides bugs
		}
		
		throw new RuntimeException("need to support event type for " + event + " in getTextForSelf()");			
	}
	
	public String getGenericText() {
		FacebookEvent event = getFacebookEvent();
		String pluralChar = "";
		
		if (event.getEventType() == FacebookEventType.NEW_TAGGED_PHOTOS_EVENT) {
		    if (event.getPhotos().size() != 1) 
		        pluralChar = "s";
		} else if (event.getCount() != 1) {
			pluralChar = "s";
		}
		
		switch (event.getEventType()) {
			case NEW_WALL_MESSAGES_EVENT :
				return event.getCount() + " new wall message" + pluralChar;
			case NEW_TAGGED_PHOTOS_EVENT :
				return getUserView().getName() + " was tagged in " + event.getPhotos().size() + " photo" + pluralChar;
			case NEW_ALBUM_EVENT :
				return getUserView().getName() + " has created a new album \"" + event.getAlbum().getName() + "\"";
			case MODIFIED_ALBUM_EVENT :
				return getUserView().getName() + " has modified an album \"" + event.getAlbum().getName() + "\"";
	   	    // no default, it hides bugs
		}
		
		throw new RuntimeException("need to support event type for " + event + " in getTextForOthers()");	
	}
}
