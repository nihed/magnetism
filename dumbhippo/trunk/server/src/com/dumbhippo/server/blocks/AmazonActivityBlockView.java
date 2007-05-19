package com.dumbhippo.server.blocks;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.AmazonActivityType;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonItemView;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonListView;
import com.dumbhippo.services.AmazonReviewView;

public class AmazonActivityBlockView extends AbstractPersonBlockView 
                                     implements ExternalAccountBlockView, TitleDescriptionBlockView {	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonActivityBlockView.class);	
	
	private AmazonActivityStatus activityStatus;
	private AmazonReviewView reviewView;
	private AmazonListView listView;
	private AmazonListItemView listItemView;
	private AmazonItemView itemView;
	
	public AmazonActivityBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public AmazonActivityBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(PersonView userView, AmazonActivityStatus activityStatus, AmazonReviewView reviewView, AmazonListView listView,
			      AmazonListItemView listItemView, AmazonItemView itemView, List<ChatMessageView> recentMessages, int messageCount) {
		partiallyPopulate(userView);
		setRecentMessages(recentMessages);
		setMessageCount(messageCount);
		this.activityStatus = activityStatus;
		this.reviewView = reviewView;
		this.listView = listView;
		this.listItemView = listItemView;
        this.itemView = itemView;
		setPopulated(true);
	}
	
	@Override
	public String getIcon() {
		return "/images3/" + ExternalAccountType.AMAZON.getIconName();
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		// product title and editorial review or user review are passed
		// as block title and description
		builder.openElement("amazonActivity",
				            "imageUrl", getImageUrl(),
				            "imageWidth", Long.toString(getImageWidth()),
				            "imageHeight", Long.toString(getImageHeight()),
					        "userId", getPersonSource().getUser().getId());	

		switch (activityStatus.getActivityType()) {
            case REVIEWED :
                builder.openElement("review", 
                		            "title", getReviewTitle(),             
                		            "rating", Long.toString(getReviewRating()));        
		        builder.closeElement();            	
		        break;
            case WISH_LISTED :    	
                builder.openElement("listItem",
                		            "listName", getListName(),
                		            "listLink", getListLink());	
                builder.appendTextNode("comment", getListItemComment());
		        builder.closeElement();
		        break;
		}
		
		builder.closeElement();
	}
	
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.AMAZON;
	}
	
	public String getTitle() {
    	// if we wanted to not display what item was reviewed/wish-listed
    	// if the review/listing is no longer there, we'd need to return
    	// some message saying that the item is no longer in the list 
    	// here and not return links/images of the item if reviewView or
    	// listItemView is null
    	if (itemView != null)
    		return itemView.getTitle();
    	else 
    		return "Information about the item is no longer available";
    }	
	
	public String getTitleForHome() {
		return getTitle();
	}
	 
	public String getLink() {
		// TODO: use our associate id for these links, figure out how exactly to add it to
		// the link, as this is not something returned by the web services
		switch (activityStatus.getActivityType()) {
            case REVIEWED :
		        return "http://www.amazon.com/o/ASIN/" + activityStatus.getItemId();
            case WISH_LISTED :
            	if (listView != null && listItemView != null) {
            		// if the item is added to the cart after following this link, it is added to
            		// be purchased for someone's wish list
            	    return "http://www.amazon.com/o/ASIN/" + activityStatus.getItemId() + 
            	           "/?coliid=" + listItemView.getListItemId() + "&colid=" + listView.getListId();
            	} else {
            		return "http://www.amazon.com/o/ASIN/" + activityStatus.getItemId();
            	}
		}

        throw new RuntimeException("need to support activity type for " + activityStatus + " in getLink()");	
	}
	
	@Override
	public String getSummaryLink() {
		return getLink();
	}

	@Override
	public String getSummaryLinkText() {
		return getTitle();
	}
	
	@Override
	public String getTypeTitle() {
		switch (activityStatus.getActivityType()) {
		    case REVIEWED :
		        return "Amazon review";
		    case WISH_LISTED:
			    return "Amazon item";
		   	// no default, it hides bugs 
	    }
		
		throw new RuntimeException("need to support activity type for " + activityStatus + " in getTypeTitle()");		
	}

	@Override
	public String getSummaryHeading() {
		switch (activityStatus.getActivityType()) {
	        case REVIEWED :
	            return "Reviewed";
	        case WISH_LISTED:
		        return "Wish-listed";
	      	// no default, it hides bugs 
        }
	
	    throw new RuntimeException("need to support activity type for " + activityStatus + " in getSummaryHeading()");		
	}
	
	public Date getSentDate() {
		// review date only contains information about a day, but no time of day
		// what we really want here is the original time the block was sent, so if
		// we find that this is needed on the user interface level, we should add
		// an originally sent timestamp to Block
		return null;
	}
	
	public String getSentTimeAgo() {
		return null;
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
		switch (activityStatus.getActivityType()) {
	        case REVIEWED :
	        	if (reviewView != null)
	                return StringUtils.ellipsizeText(reviewView.getContent());
	        	else 
	        		return "This review is no longer available";
	        case WISH_LISTED:
		        if (itemView != null) 
		        	return StringUtils.ellipsizeText(itemView.getEditorialReview());		     
		        else 			
		        	return "";
	     	// no default, it hides bugs 
        }
	
	    throw new RuntimeException("need to support activity type for " + activityStatus + " in getDescription()");		 
	}
	
	public String getReviewTitle() {
		if (activityStatus.getActivityType() != AmazonActivityType.REVIEWED) {
		    throw new RuntimeException("Should only be getting a review title for a REVIEWED activity type"); 	
		}
		
		if (reviewView != null)
	        return reviewView.getTitle();
		else 
			return "";
	}
	
	public int getReviewRating() {
		if (activityStatus.getActivityType() != AmazonActivityType.REVIEWED) {
		    throw new RuntimeException("Should only be getting a review rating for a REVIEWED activity type"); 	
		}
		
		if (reviewView != null)		
		    return reviewView.getRating();
		else
			return -1;
	}
	
	public String getListName() {
		if (activityStatus.getActivityType() != AmazonActivityType.WISH_LISTED) {
		    throw new RuntimeException("Should only be getting a list name for a WISH_LISTED activity type"); 	
		}
		
		if (listView != null)
			return listView.getListName();
		else 
			return "wish list";
	}
	
	public String getListLink() {
		if (activityStatus.getActivityType() != AmazonActivityType.WISH_LISTED) {
		    throw new RuntimeException("Should only be getting a list link for a WISH_LISTED activity type"); 	
		}
		
		return "http://www.amazon.com/gp/registry/" + activityStatus.getListId();
	}
	
	public String getListItemComment() {
		if (activityStatus.getActivityType() != AmazonActivityType.WISH_LISTED) {
		    throw new RuntimeException("Should only be getting a list item comment for a WISH_LISTED activity type"); 	
		}
		
		// Though this comment is typically short, the limit for it on Amazon is greater than our database column 
		// limit, so we should ellipsize.
		if (listItemView != null)
		    return StringUtils.ellipsizeText(listItemView.getComment());
		else
			return "";
	}
	
	public boolean isImageUrlAvailable() {
	    return 	(itemView != null && itemView.getImageUrl().length() > 0 
				&& itemView.getImageWidth() > 0 && itemView.getImageHeight() > 0);
	}
	
	public String getImageUrl() {
		if (isImageUrlAvailable())
			return itemView.getImageUrl();
		else
			return "/images3/amazon_no_image_medium.png";		
	}
	
	public int getImageWidth() {
		if (isImageUrlAvailable())
			return itemView.getImageWidth();
		else
			return 88;
	}

	public int getImageHeight() {
		if (isImageUrlAvailable())
			return itemView.getImageHeight();
		else
			return 104;
	}	
}
