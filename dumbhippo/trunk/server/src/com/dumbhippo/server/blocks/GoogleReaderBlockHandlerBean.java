package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class GoogleReaderBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<GoogleReaderBlockView> implements GoogleReaderBlockHandler {
	public GoogleReaderBlockHandlerBean() {
		super(GoogleReaderBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.GOOGLE_READER;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.GOOGLE_READER_SHARED_ITEM;
	}
}
