package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;

@Stateless
public class RedditBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<RedditBlockView> implements RedditBlockHandler {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(RedditBlockHandlerBean.class);
	
	public RedditBlockHandlerBean() {
		super(RedditBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.REDDIT;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.REDDIT_ACTIVITY_ENTRY;
	}	
	
	@Override
	public void onExternalAccountFeedEntry(User user, ExternalAccount external, FeedEntry entry, int entryPosition) {
		
		if (!external.hasLovedAndEnabledType(getAccountType()))
			return;
		
		logger.debug("stacking new block for feed entry in {}", getClass().getName());
		
		// when we get a block from a Reddit overview feed, we need to find out if we are getting
		// updates through likes and dislikes feeds, and if we are, we should ignore what we get
		// through an overview feed to avoid duplicates
		if (!entry.getFeed().getSource().getUrl().contains("liked") && 
			!entry.getFeed().getSource().getUrl().contains("disliked")) {
			for (Feed feed : external.getFeeds()) {
				if (feed.getSource().getUrl().contains("liked")) {
					if (feed.getLastFetchSucceeded())
						return;
					else 
						break;						
				}
			}
		}
		
		// entry.getDate().getTime() creates a timestamp that is too old, at least with blogspot
		// so it is unreliable, because we update blocks based on timestamps
		long now = System.currentTimeMillis();
		Block block = stacker.createBlock(getKey(user, entry));
		onNewBlockCreated(user, external);
		stacker.stack(block, now, user, false, StackReason.NEW_BLOCK);
	}
	
	@Override
	protected void populateBlockViewImpl(RedditBlockView blockView) throws BlockNotVisibleException {
	    super.populateBlockViewImpl(blockView);
	    FeedEntry feedEntry = blockView.getEntry();

		if (feedEntry.getFeed().getSource().getUrl().contains("liked")) {
			blockView.setTypeTitle("Liked on Reddit");			
		} else if (feedEntry.getFeed().getSource().getUrl().contains("disliked")) {
			blockView.setTypeTitle("Disliked on Reddit");
		} else {
			blockView.setTypeTitle(RedditBlockView.DEFAULT_TYPE_TITLE);
		}	
	}
}
