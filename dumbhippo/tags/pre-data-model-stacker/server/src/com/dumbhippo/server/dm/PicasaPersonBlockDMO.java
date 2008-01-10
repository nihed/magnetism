package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.blocks.PicasaPersonBlockView;
import com.dumbhippo.services.PicasaAlbum;

@DMO(classId="http://mugshot.org/p/o/picasaPersonBlock")
public abstract class PicasaPersonBlockDMO extends ThumbnailsBlockDMO {
	protected PicasaPersonBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public List<ThumbnailDMO> getThumbnails() {
		PicasaPersonBlockView picasaView = (PicasaPersonBlockView)blockView;
		
		Thumbnails thumbnails = picasaView.getThumbnails();
		User user = picasaView.getPersonSource().getUser(); 
		
		List<ThumbnailDMO> result = new ArrayList<ThumbnailDMO>();
		for (Thumbnail thumbnail : thumbnails.getThumbnails())
			result.add(session.findUnchecked(PicasaAlbumThumbnailDMO.class, PicasaAlbumThumbnailDMO.getKey(user, (PicasaAlbum)thumbnail)));
		
		return result;
	}
}
