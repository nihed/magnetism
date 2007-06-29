package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.BasicThumbnails;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.FacebookPhotoData;
import com.dumbhippo.services.FacebookPhotoDataView;

public class FacebookBlockView extends AbstractPersonBlockView 
       implements ThumbnailsBlockView, ExternalAccountBlockView, TitleBlockView {
	
	private FacebookEvent facebookEvent;
	private String link;
	private List<FacebookPhotoDataView> photoData; 
	
	public FacebookBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
		this.photoData = new ArrayList<FacebookPhotoDataView>();
	}
	
	public FacebookBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
		this.photoData = new ArrayList<FacebookPhotoDataView>();
	}
	
	void populate(PersonView userView, FacebookEvent facebookEvent, String link) {
		partiallyPopulate(userView);
		this.facebookEvent = facebookEvent;
		this.link = link;
		setPopulated(true);		
	}
	
	public void setPhotoData(List<FacebookPhotoDataView> photoData) {
	    this.photoData = photoData;	
	}
	
	public FacebookEvent getFacebookEvent() {
	    return facebookEvent;	
	}	
	
	public Thumbnails getThumbnails() {
		return new BasicThumbnails(photoData, photoData.size(), 
				                   FacebookPhotoData.FACEBOOK_THUMB_SIZE, FacebookPhotoData.FACEBOOK_THUMB_SIZE);
	}

	public String getMoreThumbnailsLink() {
		return getLink();
	}

	public String getMoreThumbnailsTitle() {
		switch (getFacebookEvent().getEventType()) {
		case NEW_TAGGED_PHOTOS_EVENT :
			return "All photos tagged with " + getPersonSource().getName();
		case NEW_ALBUM_EVENT :
		case MODIFIED_ALBUM_EVENT :
			return "All photos in '" + getFacebookEvent().getAlbum().getName() + "'";
   	    default:
   	        // for sending the info to the client, we pretend that all facebook
   	    	// events could have thumbnails
   	    	return "More photos";
		}
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		// we could send the link separately, but since getMoreThumbnailsLink()
		// above returns getLink(), we'll just use that on the client for now;
		// the titles for the block and for the thumbnails more link are not the same 
		builder.openElement("facebookEvent",
				"userId", getPersonSource().getUser().getId(),
				"title", getTitleForHome());
		// we could omit this if the event doesn't have the thumbnails, but
		// we rely on it for the moreThumbnailsLink for now 
		writeThumbnailsToXmlBuilder(builder, this);
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
	
	@Override
	public String getPrivacyTip() {
		return "Private: This Facebook update can only be seen by you.";
	}

	public String getTitleForHome() {
		return getTitle();
	}

	private String getTextForSelf() {
        FacebookEvent event = getFacebookEvent();
				
		String pluralChar = "";
		
		if (event.getCount() != 1) {
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
				return "You were tagged in " + event.getCount() + " photo" + pluralChar;
			case NEW_ALBUM_EVENT :
				return "You created a new album '" + event.getAlbum().getName() + "'";
			case MODIFIED_ALBUM_EVENT :
				return "You modified an album '" + event.getAlbum().getName() + "'";
			case LOGIN_STATUS_EVENT :
				if (event.getCount() == 0) 
					return "Log in to receive updates";
				else if (event.getCount() == 1) 
					return "You are now logged in and will receive updates";
				else if (event.getCount() == -1) 
				    return "New! Save your Facebook login permanently";
	   	    // no default, it hides bugs
		}
		
		throw new RuntimeException("need to support event type for " + event + " in getTextForSelf()");			
	}
	
	private String getGenericText() {
		FacebookEvent event = getFacebookEvent();
		String pluralChar = "";
		
		if (event.getCount() != 1) {
			pluralChar = "s";
		}
		
		switch (event.getEventType()) {
			case NEW_WALL_MESSAGES_EVENT :
				return event.getCount() + " new wall message" + pluralChar;
			case NEW_TAGGED_PHOTOS_EVENT :
				return getPersonSource().getName() + " was tagged in " +  event.getCount() + " photo" + pluralChar;
			case NEW_ALBUM_EVENT :
				return getPersonSource().getName() + " has created a new album '" + event.getAlbum().getName() + "'";
			case MODIFIED_ALBUM_EVENT :
				return getPersonSource().getName() + " has modified an album '" + event.getAlbum().getName() + "'";
			case LOGIN_STATUS_EVENT:
			case UNREAD_MESSAGES_UPDATE:
			case UNSEEN_POKES_UPDATE:
				throw new RuntimeException("need to support event type for " + event.getEventType() + " in getGenericText()");
				
	   	    // no default, it hides bugs
		}
		
		throw new RuntimeException("need to support event type for " + event.getEventType() + " in getTextForOthers()");	
	}

	public String getTitle() {
		if (getViewpoint().isOfUser(getPersonSource().getUser()))
			return getTextForSelf();
		else 		
			return getGenericText();
	}

	@Override
	public String getTypeTitle() {
		return "Facebook";
	}

	// FIXME doesn't fit into the way summary blocks look / are worded 
	public @Override String getSummaryHeading() {
		return "Facebook";
	}

	//	 FIXME doesn't fit into the way summary blocks look / are worded
	public @Override String getSummaryLink() {
		return getLink();
	}
	
	//	 FIXME doesn't fit into the way summary blocks look / are worded
	public @Override String getSummaryLinkText() {
		return getTitle();
	}
}
