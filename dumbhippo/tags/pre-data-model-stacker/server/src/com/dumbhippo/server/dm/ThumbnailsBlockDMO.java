package com.dumbhippo.server.dm;

import java.util.List;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.server.blocks.ThumbnailsBlockView;

@DMO(classId="http://mugshot.org/p/o/thumbnailsBlock")
public abstract class ThumbnailsBlockDMO extends BlockDMO {
	protected ThumbnailsBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@DMProperty(defaultInclude=true, defaultChildren="+")
	public abstract List<ThumbnailDMO> getThumbnails();
	
	@DMProperty(defaultInclude=true)
	public String getMoreThumbnailsTitle() {
		return ((ThumbnailsBlockView)blockView).getMoreThumbnailsTitle();
	}

	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getMoreThumbnailsLink() {
		return ((ThumbnailsBlockView)blockView).getMoreThumbnailsLink();
	}
}
