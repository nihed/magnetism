package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;

@Stateless
public class MySpacePersonBlockHandlerBean extends BlogLikeBlockHandlerBean implements MySpacePersonBlockHandler {
	public MySpacePersonBlockHandlerBean() {
		super(MySpacePersonBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.MYSPACE;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.MYSPACE_PERSON;
	}
}
