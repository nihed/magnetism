package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class DiggBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<DiggBlockView> implements DiggBlockHandler {
	public DiggBlockHandlerBean() {
		super(DiggBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.DIGG;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.DIGG_DUGG_ENTRY;
	}
}
