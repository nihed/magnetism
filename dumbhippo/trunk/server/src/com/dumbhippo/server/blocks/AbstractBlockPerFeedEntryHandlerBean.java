package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public abstract class AbstractBlockPerFeedEntryHandlerBean<ViewType extends AbstractFeedEntryBlockView> extends AbstractBlockHandlerBean<ViewType> implements BlockPerFeedEntryBlockHandler {
	
	static private final Logger logger = GlobalSetup.getLogger(AbstractBlockPerFeedEntryHandlerBean.class);
	
	protected AbstractBlockPerFeedEntryHandlerBean(Class<? extends ViewType> viewClass) {
		super(viewClass);
	}
	
	protected abstract BlockType getBlockType();
	
	protected abstract ExternalAccountType getAccountType();

	public BlockKey getKey(User user, FeedEntry feedEntry) {
		return new BlockKey(getBlockType(), user.getGuid(), null, feedEntry.getId());
	}
		
	@Override
	protected void populateBlockViewImpl(ViewType blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = getData1User(block);
		// no extras needed, we just need the username
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		
		FeedEntry entry = em.find(FeedEntry.class, block.getData3());
		
		List<ChatMessageView> messageViews = null;
		int messageCount = -1;

		if (block.getBlockType().isDirectlyChattable()) {
			messageViews = chatSystem.viewMessages(chatSystem.getNewestMessages(block, MusicChatBlockView.RECENT_MESSAGE_COUNT), viewpoint);
			
			if (messageViews.size() < BlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
				messageCount = messageViews.size();
			else
				messageCount = chatSystem.getMessageCount(block);
		}
			
	    blockView.populate(userView, entry, messageViews, messageCount);
	}

	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1UserAndExternalAccount(block, getAccountType());
	}
	
	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsInIfExternalAccount(block, getAccountType());
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// we do nothing, we don't know of feed entries yet
		//if (external.getAccountType() != getAccountType())
		//	return;
	}

	public void onNewBlockCreated(User user, ExternalAccount external) {
	    // implemented by some subclasses	
	}
	
	public void onExternalAccountFeedEntry(User user, ExternalAccount external, FeedEntry entry, int entryPosition) {
		
		if (!external.hasLovedAndEnabledType(getAccountType()))
			return;
		
		logger.debug("stacking new block for feed entry in {}", getClass().getName());
		
		// entry.getDate().getTime() creates a timestamp that is too old, at least with blogspot
		// so it is unreliable, because we update blocks based on timestamps
		long now = System.currentTimeMillis();
		Block block = stacker.createBlock(getKey(user, entry));
		onNewBlockCreated(user, external);
		stacker.stack(block, now, user, false, StackReason.NEW_BLOCK);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != getAccountType())
			return;
		
		for (Feed feed : external.getFeeds()) {
			Query q = em.createQuery("SELECT fe FROM FeedEntry fe WHERE fe.feed = :feed AND EXISTS (SELECT b FROM Block b WHERE b.blockType = " + 
					getBlockType().ordinal() + " AND b.data1 = :userId AND b.data3 = fe.id)");
			q.setParameter("feed", feed);
			q.setParameter("userId", user.getId());
			List<?> results = q.getResultList();
			for (FeedEntry e : TypeUtils.castList(FeedEntry.class, results)) {
				stacker.refreshDeletedFlags(getKey(user, e));
			}
		}
	}
}
