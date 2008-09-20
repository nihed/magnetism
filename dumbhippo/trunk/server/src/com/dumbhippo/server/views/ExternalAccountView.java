package com.dumbhippo.server.views;

import java.util.List;
import java.util.Set;

import com.dumbhippo.BasicThumbnails;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.OnlineAccountType;
import com.dumbhippo.persistence.Sentiment;

public class ExternalAccountView {
    private ExternalAccount externalAccount;
    private ExternalAccountType externalAccountType;
    private OnlineAccountType onlineAccountType;
    private String link;
    private Thumbnails thumbnails;
    
	private class ExternalAccountThumbnails extends BasicThumbnails {
		ExternalAccountThumbnails(List<? extends Thumbnail> thumbnails, int thumbnailTotalItems, int thumbnailWidth, int thumbnailHeight) {
			super(thumbnails, thumbnailTotalItems, thumbnailWidth, thumbnailHeight);
		}
		
		@Override
		public String getTotalThumbnailItemsString() {
			if (externalAccount.getAccountType() != null) {
			    return externalAccount.getAccountType().formatThumbnailCount(getTotalThumbnailItems());
			} else if (getTotalThumbnailItems() == 1) {
			    return getTotalThumbnailItems() + " item";
			} else {
				return getTotalThumbnailItems() + " items";
			}
		}
	}
    
    public ExternalAccountView(ExternalAccount externalAccount) {
    	this.externalAccount = externalAccount;
    	this.externalAccountType = externalAccount.getAccountType();
    	this.onlineAccountType = externalAccount.getOnlineAccountType();
    }
    
    public ExternalAccountView(ExternalAccount externalAccount, String link) {
    	this(externalAccount);
    	this.link = link;
    }

    /** FIXME I think this would be better eliminated, anywhere that this "not associated with 
     * any specific external account" ExternalAccountView is allowed, we should be using 
     * ExternalAccountType, not ExternalAccountView.
     * @param externalAccountType
     */
    public ExternalAccountView(OnlineAccountType onlineAccountType) {
    	this.onlineAccountType = onlineAccountType;
    	this.externalAccountType = onlineAccountType.getAccountType();
    }
    
	public ExternalAccount getExternalAccount() {
		return externalAccount;
	}
	
	public ExternalAccountType getExternalAccountType() {
		return externalAccountType;
	}
	
	public OnlineAccountType getOnlineAccountType() {
		return onlineAccountType;
	}

	public String getSiteName() {
		return onlineAccountType.getSiteName();
	}
	
	public String getDomNodeIdName() {	
		String name = onlineAccountType.getName();
		String idName = "";
		int startIndex = 0;
		
		while (startIndex < name.length() && name.indexOf("_", startIndex) >= 0) {
			int underscoreIndex = name.indexOf("_", startIndex);
			if (startIndex < underscoreIndex)
			    idName = idName + name.substring(startIndex, startIndex+1).toUpperCase() + name.substring(startIndex+1, underscoreIndex);
			startIndex = underscoreIndex + 1;
		}
		if (startIndex+1 == name.length())
		    idName = idName + name.substring(startIndex, startIndex+1).toUpperCase();
        else if (startIndex+1 < name.length())
        	idName = idName + name.substring(startIndex, startIndex+1).toUpperCase() + name.substring(startIndex+1);
		    
		if (externalAccount != null)
		    return idName + externalAccount.getId();
		else
			return idName;
	}
	
	public String getId() {
		if (externalAccount != null)
		    return String.valueOf(externalAccount.getId());
		else
			return "";		
	}

	public String getHateQuip() {
		if (externalAccount != null)
		    return externalAccount.getQuip();
		else
			return "";		
	}
	
	public boolean isMugshotEnabled() {
	    return (externalAccount != null) && externalAccount.isMugshotEnabled();
	}
	
	public String getUserInfoType() {
		return onlineAccountType.getUserInfoType();
	}
	
	public String getSentiment() {
		if (externalAccount != null)
		     return externalAccount.getSentiment().name().toLowerCase();
	
		return Sentiment.INDIFFERENT.name().toLowerCase();
	}
	
	public String getUsername() {
		if (externalAccount != null && externalAccount.hasAccountInfo() && externalAccount.getUsername() != null)
			return externalAccount.getUsername();
		
		return "";
	}
	
	public String getIconName() {
		if (externalAccountType != null)
		    return externalAccountType.getIconName();
		else
			return "";
	}
	
	public String getLink() {
		if (link != null)
		    return link;
		if (!externalAccount.isLovedAndEnabled())
			return null;
		
		return externalAccount.getLink();
	}
	
	public String getLinkText() {
		if (!externalAccount.isLovedAndEnabled())
			return null;
		return getExternalAccount().getLinkText();		
	}
	
	public boolean getHasThumbnails() {
		return thumbnails != null;
	}
	
	public Thumbnails getThumbnails() {
		return thumbnails;
	}
	
	/* Don't abuse this; the thumbnails on ExternalAccountView should always be per-account, like "all my flickr photos",
	 * not per-something-else like a particular photo album. If the thumbnails are not per-account they belong elsewhere,
	 * such as in a BlockView or whatever. If you have a tag that needs an ExternalAccountView to render thumbnails,
	 * change it to take a Thumbnails object instead. 
	 */ 
	public void setThumbnailsData(List<Thumbnail> thumbnails, int thumbnailTotalItems, int thumbnailWidth, int thumbnailHeight) {
		this.thumbnails = new ExternalAccountThumbnails(thumbnails, thumbnailTotalItems, thumbnailWidth, thumbnailHeight);
	}
	
	/* Don't abuse this; the thumbnails on ExternalAccountView should always be per-account, like "all my flickr photos",
	 * not per-something-else like a particular photo album. If the thumbnails are not per-account they belong elsewhere,
	 * such as in a BlockView or whatever. If you have a tag that needs an ExternalAccountView to render thumbnails,
	 * change it to take a Thumbnails object instead. 
	 */ 	
	public void setThumbnails(Thumbnails thumbnails) {
		this.thumbnails = thumbnails;
	}
	
	public Set<Feed> getFeeds() {
		return getExternalAccount().getFeeds();
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("externalAccount",
				"type", getOnlineAccountType().getName(),
				"sentiment", getSentiment(),
				"icon", "/images3/" + getExternalAccount().getIconName(),
				// The following will not be added unless the account is loved and enabled
				"link", getLink(),
				"linkText", getLinkText());
		if (getFeeds() != null) {
			builder.openElement("feeds");
			for (Feed f : getFeeds()) {
				builder.appendEmptyNode("feed", "src", f.getSource().getUrl());
			}
			builder.closeElement();
		}
		if (getHasThumbnails()) {
			Thumbnails thumbnails = getThumbnails();
			builder.openElement("thumbnails", "width", ""+thumbnails.getThumbnailWidth(), 
					            "height", ""+thumbnails.getThumbnailHeight());
			for (Thumbnail thumbnail : thumbnails.getThumbnails()) {
				builder.appendEmptyNode("thumbnail", 
						                "src", thumbnail.getThumbnailSrc(),
						                "title", thumbnail.getThumbnailTitle(),
						                "href", thumbnail.getThumbnailHref(),
						                "width", ""+thumbnail.getThumbnailWidth(),
						                "height", ""+thumbnail.getThumbnailHeight());
			}
			builder.closeElement();
		}
		builder.closeElement();
	}
}
