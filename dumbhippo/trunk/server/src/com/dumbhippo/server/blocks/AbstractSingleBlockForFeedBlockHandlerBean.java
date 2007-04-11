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
			messageViews = chatSystem.viewMessages(chatSystem.getNewestMessages(block, MusicChatBlockView.RECENT_MESSAGE_COUNT), viewpoint);
			
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
		stacker.createBlock(getKey(user));
	}

	public void onExternalAccountFeedEntry(User user, ExternalAccount external, FeedEntry entry, int entryPosition) {
		if (!external.hasLovedAndEnabledType(getAccountType()))
			return;
		// entry.getDate().getTime() creates a timestamp that is too old, at least with blogspot
		// so it is unreliable, because we update blocks based on timestamps
		long now = System.currentTimeMillis();
		stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != getAccountType())
			return;
		stacker.refreshDeletedFlags(getKey(user));
	}
}
