package com.dumbhippo.server.views;

import java.util.List;

import com.dumbhippo.BasicThumbnails;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
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
	
	public String getSiteBaseName() {		
		String name = externalAccountType.getSiteName();
		return name.substring(0, 1).toUpperCase() +
		       name.substring(1, name.length()).replaceAll("\\.", "");
	}
	
	public String getSiteUserInfoType() {
		return externalAccountType.getSiteUserInfoType();
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
		
		return externalAccount.getLink();
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
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("externalAccount",
				"type", getExternalAccount().getAccountType().name(),
				"link", getLink(),
				"linkText", getExternalAccount().getLinkText(),
				"icon", "/images3/" + getExternalAccount().getIconName());
	}
}
