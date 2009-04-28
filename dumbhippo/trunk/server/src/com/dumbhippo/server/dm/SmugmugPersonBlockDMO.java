package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.blocks.SmugmugPersonBlockView;
import com.dumbhippo.services.smugmug.rest.bind.Image;

@DMO(classId="http://mugshot.org/p/o/smugmugPersonBlock")
public abstract class SmugmugPersonBlockDMO extends ThumbnailsBlockDMO {
	protected SmugmugPersonBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public List<ThumbnailDMO> getThumbnails() {
		SmugmugPersonBlockView smugmugView = (SmugmugPersonBlockView)blockView;
		
		Thumbnails thumbnails = smugmugView.getThumbnails();
		User user = smugmugView.getPersonSource().getUser(); 
		
		List<ThumbnailDMO> result = new ArrayList<ThumbnailDMO>();
		for (Thumbnail thumbnail : thumbnails.getThumbnails())
			result.add(session.findUnchecked(SmugmugAlbumThumbnailDMO.class, SmugmugAlbumThumbnailDMO.getKey(user, (Image)thumbnail)));
		
		return result;
	}
}
