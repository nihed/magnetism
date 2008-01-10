package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.server.blocks.AmazonActivityBlockView;

@DMO(classId="http://mugshot.org/p/o/amazonReviewBlock")
public abstract class AmazonWishListItemBlockDMO extends BlockDMO {
	protected AmazonWishListItemBlockDMO(BlockDMOKey key) {
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
	public String getListName() {
		return ((AmazonActivityBlockView)blockView).getListName();
	}

	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getListLink() {
		return ((AmazonActivityBlockView)blockView).getListLink();
	}

	@DMProperty(defaultInclude=true)
	public String getComment() {
		return ((AmazonActivityBlockView)blockView).getListItemComment();
	}
}
