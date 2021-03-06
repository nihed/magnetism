package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.FeedSystem.NoFeedEntryException;
import com.dumbhippo.server.dm.BlockDMO;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public abstract class AbstractSingleBlockForFeedBlockHandlerBean<ViewType extends AbstractFeedEntryBlockView> extends AbstractBlockHandlerBean<ViewType> implements SingleBlockForFeedBlockHandler {
	
	@EJB
	protected FeedSystem feedSystem;
	
	public AbstractSingleBlockForFeedBlockHandlerBean(Class<? extends ViewType> blockViewClass) {
		super(blockViewClass);
	}
	
	protected abstract BlockType getBlockType();
	
	protected abstract ExternalAccountType getAccountType();

	public BlockKey getKey(User user) {
		return new BlockKey(getBlockType(), user.getGuid());
	}	
	
	public BlockKey getKey(Guid userId) {
		return new BlockKey(getBlockType(), userId);
	}	
	
	@Override
	protected void populateBlockViewImpl(ViewType blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = getData1User(block);
		// no extras needed, we just need the username
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		ExternalAccount blogAccount;
		try {
			blogAccount = externalAccountSystem.lookupExternalAccount(viewpoint, user, getAccountType());
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external blog account for block not visible", e);
		}  
	    FeedEntry lastEntry;
		try {
			lastEntry = feedSystem.getLastEntry(blogAccount.getFeed());
		} catch (NoFeedEntryException e) {
			throw new BlockNotVisibleException("Can't view block with no feed entry", e);
		}

		List<ChatMessageView> messageViews = null;
		int messageCount = -1;

		if (block.getBlockType().isDirectlyChattable()) {
			messageViews = chatSystem.viewMessages(chatSystem.getNewestMessages(block, BlockView.RECENT_MESSAGE_COUNT), viewpoint);
			
			if (messageViews.size() < BlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
				messageCount = messageViews.size();
			else
				messageCount = chatSystem.getMessageCount(block);
		}
			
	    blockView.populate(userView, lastEntry, messageViews, messageCount);
	}

	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1UserAndExternalAccount(block, getAccountType());
	}
	
	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsInIfExternalAccount(block, getAccountType());
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// Note that we create the block even if the new account is not loved-and-enabled
		if (external.getAccountType() != getAccountType())
			return;
		
		try {
		    stacker.queryBlock(getKey(user));
		} catch (NotFoundException e) {    
			// Make sure we only try to create a block once for all the external accounts of a given type.
			// Will need to create multiple blocks and change what we use as a key if we decide to support
			// multiple accounts of the same type per user for Mugshot. The key can probably be externalAccount,
			// or a combination of user and externalAccount if we want to include the user.
		    stacker.createBlock(getKey(user));
		}
	}

	public void onExternalAccountFeedEntry(User user, ExternalAccount external, FeedEntry entry, int entryPosition) {
		// the newest entry will have entryPosition 0; since we only have a single block for this feed,
		// we want to restack it only once for each batch of new feed entries 
		if (!external.hasLovedAndEnabledType(getAccountType()) || entryPosition != 0)
			return;
		// entry.getDate().getTime() creates a timestamp that is too old, at least with blogspot
		// so it is unreliable, because we update blocks based on timestamps
		long now = System.currentTimeMillis();
		Block block = stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		BlockDMOKey key = new BlockDMOKey(block);
		DataService.currentSessionRW().changed(BlockDMO.class, key, "title");
		DataService.currentSessionRW().changed(BlockDMO.class, key, "titleLink");
		DataService.currentSessionRW().changed(BlockDMO.class, key, "description");
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != getAccountType())
			return;
		stacker.refreshDeletedFlags(getKey(user));
	}
}
