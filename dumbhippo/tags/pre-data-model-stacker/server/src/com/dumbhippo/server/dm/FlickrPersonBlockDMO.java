package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.blocks.FlickrPersonBlockView;
import com.dumbhippo.services.FlickrPhotoView;

@DMO(classId="http://mugshot.org/p/o/flickrPhotosetBlock")
public abstract class FlickrPersonBlockDMO extends ThumbnailsBlockDMO {
	protected FlickrPersonBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public List<ThumbnailDMO> getThumbnails() {
		FlickrPersonBlockView photosetView = (FlickrPersonBlockView)blockView;
		
		Thumbnails thumbnails = photosetView.getThumbnails();
		User user = photosetView.getPersonSource().getUser(); 
		
		List<ThumbnailDMO> result = new ArrayList<ThumbnailDMO>();
		for (Thumbnail thumbnail : thumbnails.getThumbnails())
			result.add(session.findUnchecked(FlickrPhotoThumbnailDMO.class, FlickrPhotoThumbnailDMO.getKey(user, (FlickrPhotoView)thumbnail)));
		
		return result;
	}
}
