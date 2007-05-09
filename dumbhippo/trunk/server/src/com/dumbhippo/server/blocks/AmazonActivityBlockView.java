package com.dumbhippo.server.blocks;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonItemView;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonReviewView;

public class AmazonActivityBlockView extends AbstractPersonBlockView 
                                     implements ExternalAccountBlockView, TitleDescriptionBlockView {	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonActivityBlockView.class);	
	
	private AmazonActivityStatus activityStatus;
	private AmazonReviewView reviewView;
	private AmazonListItemView listItemView;
	private AmazonItemView itemView;
	
	public AmazonActivityBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public AmazonActivityBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(PersonView userView, AmazonActivityStatus activityStatus, AmazonReviewView reviewView, 
			      AmazonListItemView listItemView, AmazonItemView itemView, List<ChatMessageView> recentMessages, int messageCount) {
		partiallyPopulate(userView);
		setRecentMessages(recentMessages);
		setMessageCount(messageCount);
		this.activityStatus = activityStatus;
		this.reviewView = reviewView;
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
		builder.openElement("amazonActivity",
					"userId", getPersonSource().getUser().getId());	
		builder.closeElement();
	}
	
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.AMAZON;
	}
	
	public String getTitle() {
		switch (activityStatus.getActivityType()) {
            case REVIEWED :
            	// reviewView might be null if the review was deleted
            	if (reviewView != null)
                    return reviewView.getTitle();
            	else
            		return activityStatus.getItemId();
            case WISH_LISTED:
            	if (listItemView != null && itemView != null)
	                return listItemView.getQuantityDesired() + " of " + itemView.getTitle();
            	else if (itemView != null)
            		return itemView.getTitle();
            	else 
            		return activityStatus.getItemId();
     	    // no default, it hides bugs 
        }    

        throw new RuntimeException("need to support activity type for " + activityStatus + " in getTitle()");		 
	}	
	
	public String getTitleForHome() {
		return getTitle();
	}
	 
	public String getLink() {
		// TODO: use our associate id for these links, figure out how exactly to add it to
		// the link, as this is not something returned by the web services
		// TODO: include somewhere a link to the wish list for wish list items
		switch (activityStatus.getActivityType()) {
            case REVIEWED :
		        return "http://www.amazon.com/o/ASIN/" + activityStatus.getItemId();
            case WISH_LISTED :
            	return "http://www.amazon.com/gp/registry/" + activityStatus.getListId();
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
			    return "Amazon wish list item";
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
		        if (listItemView != null && (listItemView.getComment().trim().length() > 0) && itemView != null)
		        	return StringUtils.ellipsizeText(listItemView.getComment() + "\n\n" + itemView.getEditorialReview());
		        else if (itemView != null) 
		        	return StringUtils.ellipsizeText(itemView.getEditorialReview());
		        else if	(listItemView != null)
		        	return StringUtils.ellipsizeText(listItemView.getComment());
		        else 			
		        	return "This item is no longer on the list";
	     	// no default, it hides bugs 
        }
	
	    throw new RuntimeException("need to support activity type for " + activityStatus + " in getDescription()");		 
	}

}
