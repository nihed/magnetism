package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.blocks.FlickrPhotosetBlockView;
import com.dumbhippo.services.FlickrPhotoView;

@DMO(classId="http://mugshot.org/p/o/flickrPhotosetBlock")
public abstract class FlickrPhotosetBlockDMO extends ThumbnailsBlockDMO {
	protected FlickrPhotosetBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public List<ThumbnailDMO> getThumbnails() {
		FlickrPhotosetBlockView photosetView = (FlickrPhotosetBlockView)blockView;
		
		Thumbnails thumbnails = photosetView.getThumbnails();
		User user = photosetView.getPersonSource().getUser(); 
		
		List<ThumbnailDMO> result = new ArrayList<ThumbnailDMO>();
		for (Thumbnail thumbnail : thumbnails.getThumbnails())
			result.add(session.findUnchecked(FlickrPhotoThumbnailDMO.class, FlickrPhotoThumbnailDMO.getKey(user, (FlickrPhotoView)thumbnail)));
		
		return result;
	}
}
