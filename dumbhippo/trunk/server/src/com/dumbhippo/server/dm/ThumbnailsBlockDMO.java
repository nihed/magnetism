package com.dumbhippo.server.dm;

import java.util.List;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;

@DMO(classId="http://mugshot.org/p/o/thumbnailsBlock")
public abstract class ThumbnailsBlockDMO extends BlockDMO {
	protected ThumbnailsBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@DMProperty(defaultInclude=true, defaultChildren="+")
	public abstract List<ThumbnailDMO> getThumbnails();
}
