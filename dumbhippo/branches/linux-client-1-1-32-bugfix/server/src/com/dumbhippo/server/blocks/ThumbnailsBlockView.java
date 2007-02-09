package com.dumbhippo.server.blocks;

import com.dumbhippo.Thumbnails;

public interface ThumbnailsBlockView {
	public Thumbnails getThumbnails();

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
