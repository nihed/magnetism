package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class RedditBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<RedditBlockView> implements RedditBlockHandler {
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
}
