package com.dumbhippo;

/**
 * Interface for a photo thumbnail we might display in an HTML page.
 * The method names all contain "Thumbnail" to avoid confusion with other
 * methods in a photo object that might implement this.
 * 
 * @author Havoc Pennington
 *
 */
public interface Thumbnail {
	/** 
	 * Get the url of the image itself
	 * @return
	 */
	public String getThumbnailSrc();
	
	/**
	 * Get what the thumbnail should link to
	 * @return
	 */
	public String getThumbnailHref();
	
	/**
	 * Get the tooltip or caption
	 * @return
	 */
	public String getThumbnailTitle();
	
	public int getThumbnailWidth();
	
	public int getThumbnailHeight();
}
