package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class BlogBlockHandlerBean extends BlogLikeBlockHandlerBean implements BlogBlockHandler {
	public BlogBlockHandlerBean() {
		super(BlogBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.BLOG;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.BLOG_PERSON;
	}
}
