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
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.services.YouTubeVideo;

@Stateless
public class YouTubeBlockHandlerBean extends
		AbstractBlockHandlerBean<YouTubePersonBlockView> implements
		YouTubeBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(YouTubeBlockHandlerBean.class);	

	@EJB
	private YouTubeUpdater youTubeUpdater;
	
	protected YouTubeBlockHandlerBean() {
		super(YouTubePersonBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(YouTubePersonBlockView blockView)
			throws BlockNotVisibleException {
		User user = getData1User(blockView.getBlock());
		ExternalAccountView externalAccountView;
		try {
			externalAccountView = externalAccountSystem.getExternalAccountView(blockView.getViewpoint(),
					user, ExternalAccountType.YOUTUBE);
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external account not visible");
		}
		PersonView userView = personViewer.getPersonView(blockView.getViewpoint(), user);
		blockView.populate(userView, externalAccountView);
	}

	public BlockKey getKey(User user) {
		return new BlockKey(BlockType.YOUTUBE_PERSON, user.getGuid());
	}

	public Set<User> getInterestedUsers(Block block) {
		return super.getUsersWhoCareAboutData1UserAndExternalAccount(block, ExternalAccountType.YOUTUBE);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return super.getGroupsData1UserIsInIfExternalAccount(block, ExternalAccountType.YOUTUBE);
	}

	public void onYouTubeRecentVideosChanged(String username, List<? extends YouTubeVideo> videos) {
		logger.debug("most recent YouTube videos changed for " + username);

		long now = System.currentTimeMillis();
		Collection<User> users = youTubeUpdater.getAccountLovers(username);
		for (User user : users) {
			stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}

	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// Note that we create the block even if the new account is not loved-and-enabled
		if (external.getAccountType() != ExternalAccountType.YOUTUBE)
			return;
		Block block = stacker.createBlock(getKey(user));
		stacker.stack(block, System.currentTimeMillis(), user, false, StackReason.NEW_BLOCK);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.YOUTUBE)
			return;
		stacker.refreshDeletedFlags(getKey(user));
	}
	
	public void migrate(User user) {
		logger.debug("Migrating YouTube blocks for user {}", user);
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.YOUTUBE);
		if (external == null) {
			logger.debug("No YouTube account for this user");
			return;
		}
		Block block = stacker.getOrCreateBlock(getKey(user));
		if (block.getTimestampAsLong() <= 0) {
			logger.debug("Setting block {} timestamp", block);			
			stacker.stack(block, System.currentTimeMillis(), user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
