package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.blocks.FacebookBlockView;
import com.dumbhippo.services.FacebookPhotoDataView;

@DMO(classId="http://mugshot.org/p/o/facebookEventBlock")
public abstract class FacebookEventBlockDMO extends ThumbnailsBlockDMO {
	protected FacebookEventBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public List<ThumbnailDMO> getThumbnails() {
		FacebookBlockView facebookView = (FacebookBlockView)blockView;
		
		Thumbnails thumbnails = facebookView.getThumbnails();
		User user = facebookView.getPersonSource().getUser(); 
		
		List<ThumbnailDMO> result = new ArrayList<ThumbnailDMO>();
		for (Thumbnail thumbnail : thumbnails.getThumbnails())
			result.add(session.findUnchecked(FacebookPhotoThumbnailDMO.class, FacebookPhotoThumbnailDMO.getKey(user, (FacebookPhotoDataView)thumbnail)));
		
		return result;
	}
}
