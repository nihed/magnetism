package com.dumbhippo.server.blocks;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public abstract class BlogLikeBlockHandlerBean extends AbstractBlockHandlerBean<BlogLikeBlockView> implements BlogLikeBlockHandler {
	
	@EJB
	protected FeedSystem feedSystem;
	
	public BlogLikeBlockHandlerBean(Class<? extends BlogLikeBlockView> blockViewClass) {
		super(blockViewClass);
	}
	
	protected abstract BlockType getBlockType();
	
	protected abstract ExternalAccountType getAccountType();
	
	public BlockKey getKey(User user, StackInclusion inclusion) {
		return getKey(user.getGuid(), inclusion);
	}

	public BlockKey getKey(Guid userId, StackInclusion inclusion) {
		return new BlockKey(getBlockType(), userId, inclusion);
	}	
	
	@Override
	protected void populateBlockViewImpl(BlogLikeBlockView blockView) throws BlockNotVisibleException {
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
	    FeedEntry lastEntry = feedSystem.getLastEntry(blogAccount.getFeed());

	    blockView.populate(userView, lastEntry);
	}

	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1UserAndExternalAccount(block, ExternalAccountType.BLOG);
	}
	
	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsInIfExternalAccount(block, ExternalAccountType.BLOG);
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// Note that we create the block even if the new account is not loved-and-enabled
		if (external.getAccountType() != getAccountType())
			return;
		stacker.createBlock(getKey(user, StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS));
		stacker.createBlock(getKey(user, StackInclusion.ONLY_WHEN_VIEWING_SELF));
	}

	public void onExternalAccountFeedEntry(User user, ExternalAccount external, FeedEntry entry, int entryPosition) {
		if (!external.hasLovedAndEnabledType(getAccountType()))
			return;
		// entry.getDate().getTime() creates a timestamp that is too old, at least with blogspot
		// so it is unreliable, because we update blocks based on timestamps
		long now = System.currentTimeMillis();
		stacker.stack(getKey(user, StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS), now, user, false, StackReason.BLOCK_UPDATE);
		stacker.stack(getKey(user, StackInclusion.ONLY_WHEN_VIEWING_SELF), now, user, false, StackReason.BLOCK_UPDATE);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != getAccountType())
			return;
		stacker.refreshDeletedFlags(getKey(user, StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS));
		stacker.refreshDeletedFlags(getKey(user, StackInclusion.ONLY_WHEN_VIEWING_SELF));
	}
}