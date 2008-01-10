package com.dumbhippo.server.blocks;

/** Interface for block views that just have a simple link as the block title, 
 * allows jsp tags to treat these block views generically
 */
public interface TitleBlockView {
	/** Gets the title link for the block when the block is displayed on your home page,
	 * some block views override this differently from regular getTitle()
	 */
	public String getTitleForHome();
	
	/** Gets the regular title for the block */
	public String getTitle();
	
	/** Gets the href for the title link */
	public String getLink();
}
