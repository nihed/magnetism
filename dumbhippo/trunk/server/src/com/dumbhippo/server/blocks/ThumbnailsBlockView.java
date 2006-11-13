package com.dumbhippo.server.blocks;

import java.util.List;

import com.dumbhippo.Thumbnail;

public interface ThumbnailsBlockView {
	public List<Thumbnail> getThumbnails();
	public int getThumbnailCount();
	/**
	 * Get the href for a "More..." link to see all the photos.
	 * (NOT XML-escaped!)
	 * @return
	 */
	public String getMoreThumbnailsLink();
	/** 
	 * Get the tooltip (title="" on the link element) for the "More..." link
	 * @return
	 */
	public String getMoreThumbnailsTitle();
}
