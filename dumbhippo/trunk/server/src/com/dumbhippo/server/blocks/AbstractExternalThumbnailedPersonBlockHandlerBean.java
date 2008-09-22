package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

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
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

public abstract class AbstractExternalThumbnailedPersonBlockHandlerBean<BlockViewSubType extends ExternalThumbnailedPersonBlockView> extends AbstractBlockHandlerBean<BlockViewSubType> implements ExternalThumbnailedPersonBlockHandler {
	static private final Logger logger = GlobalSetup.getLogger(AbstractExternalThumbnailedPersonBlockHandlerBean.class);	
	
	private BlockType blockType;
	private ExternalAccountType accountType;
	
	protected AbstractExternalThumbnailedPersonBlockHandlerBean(Class<? extends BlockViewSubType> viewClass, ExternalAccountType accountType, BlockType blockType) {
		super(viewClass);
		this.blockType = blockType;
		this.accountType = accountType;
	}
	
	@Override
	protected void populateBlockViewImpl(BlockViewSubType blockView)
			throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = getData1User(block);
		ExternalAccountView externalAccountView;
		try {
			externalAccountView = externalAccountSystem.getExternalAccountView(viewpoint,
																				user,
																				accountType);
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external account not visible");
		}
		
		List<ChatMessageView> messageViews = chatSystem.viewMessages(chatSystem.getNewestMessages(block, BlockView.RECENT_MESSAGE_COUNT), viewpoint);
		
		int messageCount;
		if (messageViews.size() < BlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
			messageCount = messageViews.size();
		else
			messageCount = chatSystem.getMessageCount(block);
		
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		blockView.populate(userView, externalAccountView, messageViews, messageCount);
	}
	
	public BlockKey getKey(User user) {
		return new BlockKey(blockType, user.getGuid());
	}

	public Set<User> getInterestedUsers(Block block) {
		return super.getUsersWhoCareAboutData1UserAndExternalAccount(block, accountType);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return super.getGroupsData1UserIsInIfExternalAccount(block, accountType);
	}

	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// Note that we create the block even if the new account is not loved-and-enabled
		if (external.getAccountType() != accountType)
			return;
		
		try {
		    stacker.queryBlock(getKey(user));
		} catch (NotFoundException e) {    
			Block block = stacker.createBlock(getKey(user));
			stacker.stack(block, System.currentTimeMillis(), user, false, StackReason.NEW_BLOCK);
		}
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != accountType)
			return;
		stacker.refreshDeletedFlags(getKey(user));
	}

	public void migrate(User user) throws RetryException {
		logger.debug("Migrating {} blocks for user {}", blockType.name(), user);
		ExternalAccount external = user.getAccount().getExternalAccount(accountType);
		if (external == null) {
			logger.debug("No {} account for this user", accountType.name());
			return;
		}
		Block block = stacker.getOrCreateBlock(getKey(user));
		if (block.getTimestampAsLong() <= 0) {
			logger.debug("Setting block {} timestamp", block);			
			stacker.stack(block, System.currentTimeMillis(), user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
