package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class NetflixBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<NetflixBlockView> implements NetflixBlockHandler {
	public NetflixBlockHandlerBean() {
		super(NetflixBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.NETFLIX;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.NETFLIX_MOVIE;
	}
}
