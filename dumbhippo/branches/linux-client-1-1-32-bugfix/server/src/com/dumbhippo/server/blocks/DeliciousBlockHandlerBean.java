package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class DeliciousBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<DeliciousBlockView> implements DeliciousBlockHandler {
	public DeliciousBlockHandlerBean() {
		super(DeliciousBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.DELICIOUS;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.DELICIOUS_PUBLIC_BOOKMARK;
	}
}
