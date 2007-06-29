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
		if (id.length() > FlickrWebServices.MAX_FLICKR_PHOTO_ID_LENGTH)
			throw new IllegalArgumentException("Flickr returned a longer photo id than we handle");		
		this.id = id;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		if (owner.length() > FlickrWebServices.MAX_FLICKR_USER_ID_LENGTH)
			throw new IllegalArgumentException("Flickr returned a longer user id than we handle");				
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
		if (secret.length() > FlickrWebServices.MAX_FLICKR_SECRET_LENGTH)
			throw new IllegalArgumentException("Flickr returned a longer secret than we handle");
		this.secret = secret;
	}
	public String getServer() {		
		return server;
	}
	public void setServer(String server) {
		if (server.length() > FlickrWebServices.MAX_FLICKR_SERVER_LENGTH)
			throw new IllegalArgumentException("Flickr returned a longer server than we handle");		
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
		if (server == null)
			throw new IllegalStateException("must have server set to get url from " + this);
		if (id == null)
			throw new IllegalStateException("must have id set to get url from " + this);
		if (secret == null)
			throw new IllegalStateException("must have secret set to get url from " + this);		
		
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
		return "{id=" + id + " title='" + title + "' server=" + server + " secret=" + secret + " owner=" + owner + "}";
	}
	
	public String getThumbnailSrc() {
		return getUrl(THUMBNAIL_SIZE);
	}
	
	public String getThumbnailHref() {
		if (owner == null)
			throw new IllegalStateException("owner field must be set to get thumbnail href from " + this);
		
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
