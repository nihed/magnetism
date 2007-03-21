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
import com.dumbhippo.server.PicasaUpdater;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.services.PicasaAlbum;

@Stateless
public class PicasaBlockHandlerBean extends
		AbstractBlockHandlerBean<PicasaPersonBlockView> implements
		PicasaBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(PicasaBlockHandlerBean.class);	

	@EJB
	private PicasaUpdater picasaUpdater;
	
	protected PicasaBlockHandlerBean() {
		super(PicasaPersonBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(PicasaPersonBlockView blockView)
			throws BlockNotVisibleException {
		User user = getData1User(blockView.getBlock());
		ExternalAccountView externalAccountView;
		try {
			externalAccountView = externalAccountSystem.getExternalAccountView(blockView.getViewpoint(),
					user, ExternalAccountType.PICASA);
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external account not visible");
		}
		PersonView userView = personViewer.getPersonView(blockView.getViewpoint(), user);
		blockView.populate(userView, externalAccountView);
	}

	public BlockKey getKey(User user) {
		return new BlockKey(BlockType.PICASA_PERSON, user.getGuid());
	}

	public Set<User> getInterestedUsers(Block block) {
		return super.getUsersWhoCareAboutData1UserAndExternalAccount(block, ExternalAccountType.PICASA);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return super.getGroupsData1UserIsInIfExternalAccount(block, ExternalAccountType.PICASA);
	}

	public void onPicasaRecentAlbumsChanged(String username, List<? extends PicasaAlbum> albums) {
		logger.debug("most recent Picasa albums changed for " + username);

		long now = System.currentTimeMillis();
		Collection<User> users = picasaUpdater.getAccountLovers(username);
		for (User user : users) {
			stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}

	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// Note that we create the block even if the new account is not loved-and-enabled
		if (external.getAccountType() != ExternalAccountType.PICASA)
			return;
		Block block = stacker.createBlock(getKey(user));
		stacker.stack(block, System.currentTimeMillis(), user, false, StackReason.NEW_BLOCK);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.PICASA)
			return;
		stacker.refreshDeletedFlags(getKey(user));
	}

	public void migrate(User user) {
		logger.debug("Migrating Picasa blocks for user {}", user);
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.PICASA);
		if (external == null) {
			logger.debug("No Picasa account for this user");
			return;
		}
		Block block = stacker.getOrCreateBlock(getKey(user));
		if (block.getTimestampAsLong() <= 0) {
			logger.debug("Setting block {} timestamp", block);			
			stacker.stack(block, System.currentTimeMillis(), user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
