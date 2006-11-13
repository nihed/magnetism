package com.dumbhippo;

import java.util.List;


public interface Thumbnails {

	public List<Thumbnail> getThumbnails();

	public int getThumbnailCount();

	public int getThumbnailWidth();

	public int getThumbnailHeight();

	public int getTotalThumbnailItems();

	public String getTotalThumbnailItemsString();

}