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
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.services.FlickrPhotoView;

@Stateless
public class FlickrPersonBlockHandlerBean extends
		AbstractBlockHandlerBean<FlickrPersonBlockView> implements
		FlickrPersonBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(FlickrPersonBlockHandlerBean.class);	

	@EJB
	private FlickrUpdater flickrUpdater;
	
	protected FlickrPersonBlockHandlerBean() {
		super(FlickrPersonBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(FlickrPersonBlockView blockView)
			throws BlockNotVisibleException {
		try {
			blockView.populate(externalAccountSystem.getExternalAccountView(blockView.getViewpoint(),
					blockView.getPersonSource().getUser(), ExternalAccountType.FLICKR));
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external account not visible");
		}
	}

	public BlockKey getKey(User user) {
		return new BlockKey(BlockType.FLICKR_PERSON, user.getGuid());
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

		long now = System.currentTimeMillis();
		Collection<User> users = flickrUpdater.getUsersWhoLoveFlickrAccount(flickrId);
		for (User user : users) {
			stacker.stack(getKey(user), now, StackReason.BLOCK_UPDATE);
		}
	}

	public void onFlickrPhotosetCreated(FlickrPhotosetStatus photosetStatus) {
		// we don't care about this, the photoset block does though
	}

	public void onFlickrPhotosetChanged(FlickrPhotosetStatus photosetStatus) {
		// we don't care about this, the photoset block does though
	}

	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// Note that we create the block even if the new account is not loved-and-enabled
		if (external.getAccountType() != ExternalAccountType.FLICKR)
			return;
		stacker.createBlock(getKey(user));
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.FLICKR)
			return;
		stacker.refreshDeletedFlags(getKey(user));
	}
	
	public void migrate(User user) {
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.FLICKR);
		if (external == null)
			return;
		Block block = stacker.getOrCreateBlock(getKey(user));
		if (block.getTimestampAsLong() <= 0)
			stacker.stack(block, System.currentTimeMillis(), StackReason.BLOCK_UPDATE);
	}
}
