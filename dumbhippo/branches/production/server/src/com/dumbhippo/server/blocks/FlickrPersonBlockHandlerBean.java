package com.dumbhippo.server.blocks;

import java.util.Collection;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FlickrUpdater;
import com.dumbhippo.services.FlickrPhotosView;

@Stateless
public class FlickrPersonBlockHandlerBean extends
		AbstractExternalThumbnailedPersonBlockHandlerBean<FlickrPersonBlockView> implements
		FlickrPersonBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(FlickrPersonBlockHandlerBean.class);	

	@EJB
	private FlickrUpdater flickrUpdater;
	
	protected FlickrPersonBlockHandlerBean() {
		super(FlickrPersonBlockView.class, ExternalAccountType.FLICKR, BlockType.FLICKR_PERSON);
	}

	public void onMostRecentFlickrPhotosChanged(String flickrId, FlickrPhotosView photosView) {
		logger.debug("most recent flickr photos changed for " + flickrId);

		if (photosView.getPhotos().size() == 0) {
			logger.debug("not restacking flickr person block since photo count is 0");
			return;
		}	
		
		long now = System.currentTimeMillis();
		Collection<User> users = flickrUpdater.getAccountLovers(flickrId);
		for (User user : users) {
			stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}

	public void onFlickrPhotosetCreated(FlickrPhotosetStatus photosetStatus) {
		// we don't care about this, the photoset block does though
	}

	public void onFlickrPhotosetChanged(FlickrPhotosetStatus photosetStatus) {
		// we don't care about this, the photoset block does though
	}
}
