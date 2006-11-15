package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

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
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.PersonView;
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
		
		User user = getData1User(blockView.getBlock());
		PersonView userView = personViewer.getPersonView(blockView.getViewpoint(), user);
		
		FlickrPhotosetStatus photosetStatus = em.find(FlickrPhotosetStatus.class, blockView.getBlock().getData2AsGuid().toString());
		
		blockView.populate(userView, photosetStatus.toPhotoset(), photosetStatus.getOwnerId());
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
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// nothing to do, just wait for a photoset to appear (?)
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.FLICKR)
			return;
		// FIXME we need to query our photosets and stacker.refreshDeletedFlags on all of their blocks
	}
}
