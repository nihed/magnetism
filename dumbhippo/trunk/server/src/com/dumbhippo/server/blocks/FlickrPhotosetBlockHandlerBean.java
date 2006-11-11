package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.services.FlickrPhotoView;

@Stateless
public class FlickrPhotosetBlockHandlerBean extends
		AbstractBlockHandlerBean<FlickrPhotosetBlockView> implements
		FlickrPhotosetBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(FlickrPhotosetBlockHandlerBean.class);	
	
	protected FlickrPhotosetBlockHandlerBean() {
		super(FlickrPhotosetBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(FlickrPhotosetBlockView blockView)
			throws BlockNotVisibleException {
		// FIXME
	}

	public BlockKey getKey(User user, FlickrPhotosetStatus photosetStatus) {
		// FIXME
		return null;
	}

	public Set<User> getInterestedUsers(Block block) {
		// FIXME
		return null;
	}

	public Set<Group> getInterestedGroups(Block block) {
		// FIXME
		return null;
	}

	public void onMostRecentFlickrPhotosChanged(String flickrId,
			List<FlickrPhotoView> recentPhotos) {
		logger.debug("most recent flickr photos changed for " + flickrId);

		// FIXME
	}

	public void onFlickrPhotosetCreated(FlickrPhotosetStatus photosetStatus) {
		// (remove this log message)
		logger.debug("new photoset status " + photosetStatus);
		
		// FIXME

	}

	public void onFlickrPhotosetChanged(FlickrPhotosetStatus photosetStatus) {
		// (remove this log message)
		logger.debug("changed photoset status " + photosetStatus);
		// FIXME
	}
}
