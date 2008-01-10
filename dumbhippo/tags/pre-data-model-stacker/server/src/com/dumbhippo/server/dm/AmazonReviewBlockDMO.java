package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.server.blocks.AmazonActivityBlockView;

@DMO(classId="http://mugshot.org/p/o/amazonReviewBlock")
public abstract class AmazonReviewBlockDMO extends BlockDMO {
	protected AmazonReviewBlockDMO(BlockDMOKey key) {
		super(key);
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getImageUrl() {
		return ((AmazonActivityBlockView)blockView).getImageUrl();
	}

	@DMProperty(defaultInclude=true)
	public int getImageWidth() {
		return ((AmazonActivityBlockView)blockView).getImageWidth();
	}

	@DMProperty(defaultInclude=true)
	public int getImageHeight() {
		return ((AmazonActivityBlockView)blockView).getImageHeight();
	}

	@DMProperty(defaultInclude=true)
	public String getReviewTitle() {
		return ((AmazonActivityBlockView)blockView).getReviewTitle();
	}

	@DMProperty(defaultInclude=true)
	public int getReviewRating() {
		return ((AmazonActivityBlockView)blockView).getReviewRating();
	}
}
