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
import com.dumbhippo.persistence.Sentiment;

public class ExternalAccountView {
    private ExternalAccount externalAccount;
    private ExternalAccountType externalAccountType;
    private String link;
    private Thumbnails thumbnails;
    
	private class ExternalAccountThumbnails extends BasicThumbnails {
		ExternalAccountThumbnails(List<? extends Thumbnail> thumbnails, int thumbnailTotalItems, int thumbnailWidth, int thumbnailHeight) {
			super(thumbnails, thumbnailTotalItems, thumbnailWidth, thumbnailHeight);
		}
		
		@Override
		public String getTotalThumbnailItemsString() {
			return externalAccount.getAccountType().formatThumbnailCount(getTotalThumbnailItems());
		}
	}
    
    public ExternalAccountView(ExternalAccount externalAccount) {
    	this.externalAccount = externalAccount;
    	this.externalAccountType = externalAccount.getAccountType();
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
    public ExternalAccountView(ExternalAccountType externalAccountType) {
    	this.externalAccountType = externalAccountType;
    }
    
	public ExternalAccount getExternalAccount() {
		return externalAccount;
	}
	
	public ExternalAccountType getExternalAccountType() {
		return externalAccountType;
	}

	public String getSiteName() {
		return externalAccountType.getSiteName();
	}
	
	public String getDomNodeIdName() {	
		return externalAccountType.getDomNodeIdName();
	}
	
	public String getSiteUserInfoType() {
		return externalAccountType.getSiteUserInfoType();
	}

	public boolean isInfoTypeProvidedBySite() {
		return externalAccountType.isInfoTypeProvidedBySite();
	}
	
	public String getIconName() {
		return externalAccountType.getIconName();
	}
	
	public String getSentiment() {
		if (externalAccount != null)
		     return externalAccount.getSentiment().name().toLowerCase();
	
		return Sentiment.INDIFFERENT.name().toLowerCase();
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
				"type", getExternalAccount().getAccountType().name(),
				"sentiment", getSentiment(),
				"icon", "/images3/" + getExternalAccount().getIconName(),
				// The following will not be added unless the account is loved and enabled
				"link", getLink(),
				"linkText", getLinkText());
		if (getFeeds() != null) {
			builder.openElement("feeds");
			for (Feed f : getFeeds()) {
				builder.appendEmptyNode("feed", "src", f.getLink().getUrl());
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
