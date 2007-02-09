package com.dumbhippo;
import java.util.List;


public class BasicThumbnails implements Thumbnails {

	private List<? extends Thumbnail> thumbnails;
	private int thumbnailTotalItems;
	private int thumbnailWidth;
	private int thumbnailHeight;
	
	public BasicThumbnails(List<? extends Thumbnail> thumbnails, int totalItems, int width, int height) {
		this.thumbnails = thumbnails;
		this.thumbnailWidth = width;
		this.thumbnailHeight = height;
		this.thumbnailTotalItems = totalItems;
	}
	
	public List<? extends Thumbnail> getThumbnails() {
		return thumbnails;
	}
	
	public int getThumbnailCount() {
		if (thumbnails != null)
			return thumbnails.size();
		else
			return 0;
	}
	
	public int getThumbnailWidth() {
		return thumbnailWidth;
	}
	
	public int getThumbnailHeight() {
		return thumbnailHeight;
	}
	
	public int getTotalThumbnailItems() {
		return thumbnailTotalItems; 
	}
	
	// can be overridden such that it has more point
	public String getTotalThumbnailItemsString() {
		return Integer.toString(thumbnailTotalItems); 
	}
}
