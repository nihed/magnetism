package com.dumbhippo;

import java.util.List;


public interface Thumbnails {

	public List<? extends Thumbnail> getThumbnails();

	public int getThumbnailCount();

	/** This is the max of the width of any thumbnail; FIXME rename to getThumbnailMaxWidth() and fix in jsps */
	public int getThumbnailWidth();

	/** This is the max of the width of any thumbnail; FIXME rename to getThumbnailMaxHeight() and fix in jsps */
	public int getThumbnailHeight();

	public int getTotalThumbnailItems();

	public String getTotalThumbnailItemsString();

}