package com.dumbhippo.server.blocks;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FlickrUpdater;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.services.FlickrPhoto;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.caches.FlickrPhotosetPhotosCache;

@Stateless
public class FlickrPhotosetBlockHandlerBean extends
		AbstractBlockHandlerBean<FlickrPhotosetBlockView> implements
		FlickrPhotosetBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(FlickrPhotosetBlockHandlerBean.class);	

	@EJB
	private FlickrUpdater flickrUpdater;	
	
	@EJB
	private FlickrPhotosetPhotosCache photosetPhotosCache;
	
	protected FlickrPhotosetBlockHandlerBean() {
		super(FlickrPhotosetBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(FlickrPhotosetBlockView blockView)
			throws BlockNotVisibleException {
		
		User user = getData1User(blockView.getBlock());
		PersonView userView = personViewer.getPersonView(blockView.getViewpoint(), user);
		
		FlickrPhotosetStatus photosetStatus = em.find(FlickrPhotosetStatus.class, blockView.getBlock().getData2AsGuid().toString());
		
		// This is all a screwy workaround for not having a "get photoset by ID" cached web service bean
		FlickrPhotos photos = new FlickrPhotos();		
		List<FlickrPhotoView> photoViews = photosetPhotosCache.getSync(photosetStatus.getFlickrId());
		
		photos.setPage(1);
		photos.setPerPage(photoViews.size());
		photos.setTotal(photoViews.size());

		for (FlickrPhotoView photoView : photoViews) {
			if (!(photoView instanceof FlickrPhoto))
				throw new RuntimeException("our lame hack broke, just add the get-photoset-by-id web service cache bean");
			FlickrPhoto photo = (FlickrPhoto) photoView;
			photo.setOwner(photosetStatus.getOwnerId());
			photos.addPhoto(photo);
		}
		
		FlickrPhotosetView photosetView = photosetStatus.toPhotoset(photos);
		
		blockView.populate(userView, photosetView, photosetStatus.getOwnerId());
	}

	public BlockKey getKey(User user, FlickrPhotosetStatus photosetStatus) {
		return new BlockKey(BlockType.FLICKR_PHOTOSET, user.getGuid(), photosetStatus.getGuid());
	}

	public Set<User> getInterestedUsers(Block block) {
		return super.getUsersWhoCareAboutData1UserAndExternalAccount(block, ExternalAccountType.FLICKR);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return super.getGroupsData1UserIsInIfExternalAccount(block, ExternalAccountType.FLICKR);
	}

	public void onMostRecentFlickrPhotosChanged(String flickrId,
			List<FlickrPhotoView> recentPhotos) {
		// we don't care about this here, only in FlickrPersonBlockHandlerBean
	}

	public void onFlickrPhotosetCreated(FlickrPhotosetStatus photosetStatus) {
		logger.debug("new photoset status to stack: " + photosetStatus);
		long now = System.currentTimeMillis();
		Collection<User> users = flickrUpdater.getAccountLovers(photosetStatus.getOwnerId());
		for (User user : users) {
			Block block = stacker.createBlock(getKey(user, photosetStatus));
			stacker.stack(block, now, user, false, StackReason.NEW_BLOCK);
		}
	}

	public void onFlickrPhotosetChanged(FlickrPhotosetStatus photosetStatus) {
		if (!photosetStatus.isActive()) {
			logger.debug("Not restacking inactive photoset status {}", photosetStatus);
			return;
		}
		logger.debug("photoset status changed, restacking {}", photosetStatus);
		long now = System.currentTimeMillis();
		Collection<User> users = flickrUpdater.getAccountLovers(photosetStatus.getOwnerId());
		for (User user : users) {
			stacker.stack(getKey(user, photosetStatus), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// nothing to do, just wait for a photoset to appear in periodic job updater
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.FLICKR)
			return;
		if (external.getHandle() == null)
			return;
		Collection<FlickrPhotosetStatus> statuses = flickrUpdater.getPhotosetStatusesForFlickrAccount(external.getHandle());
		for (FlickrPhotosetStatus status : statuses) {
			stacker.refreshDeletedFlags(getKey(user, status));
		}
	}
}
