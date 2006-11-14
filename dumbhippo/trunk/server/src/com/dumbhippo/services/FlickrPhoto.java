package com.dumbhippo.services;


public final class FlickrPhoto implements FlickrPhotoView {
	private String id;
	private String owner;
	private String secret;
	private String server;
	private String title;
	private boolean public_;
	private boolean friend;
	private boolean family;
	 // is primary in a photoset; the photoset should be apparent from context
	private boolean primary;
	
	static public final FlickrPhotoSize THUMBNAIL_SIZE = FlickrPhotoSize.SMALL_SQUARE;
	
	public boolean isFamily() {
		return family;
	}
	public void setFamily(boolean family) {
		this.family = family;
	}
	public boolean isFriend() {
		return friend;
	}
	public void setFriend(boolean friend) {
		this.friend = friend;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public boolean isPublic() {
		return public_;
	}
	public void setPublic(boolean public_) {
		this.public_ = public_;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	// See http://www.flickr.com/services/api/misc.urls.html 
	// for info on urls
	public String getUrl(FlickrPhotoSize size) {
		StringBuilder sb = new StringBuilder("http://static.flickr.com/");
		sb.append(server);
		sb.append("/");
		sb.append(id);
		sb.append("_");
		sb.append(secret);
		
		char code = size.getUrlCode();
		if (code != '\0') {
			sb.append("_");
			sb.append(code);
		}
		sb.append(".jpg");
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "{id=" + id + " title='" + title + "' url=" +
		getUrl(FlickrPhotoSize.SMALL_SQUARE) + "}";
	}
	
	public String getThumbnailSrc() {
		return getUrl(THUMBNAIL_SIZE);
	}
	public String getThumbnailHref() {
		StringBuilder sb = new StringBuilder("http://www.flickr.com/photos/");
		sb.append(owner);
		sb.append("/");
		sb.append(id);
		return sb.toString();
	}
	public String getThumbnailTitle() {
		return getTitle();
	}
	public int getThumbnailWidth() {
		return THUMBNAIL_SIZE.getPixels();
	}
	public int getThumbnailHeight() {
		return THUMBNAIL_SIZE.getPixels();
	}

	public boolean isPrimary() {
		return primary;
	}
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
}
