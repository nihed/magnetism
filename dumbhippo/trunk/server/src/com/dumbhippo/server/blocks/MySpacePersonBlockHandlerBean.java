package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.User;

@Stateless
public class MySpacePersonBlockHandlerBean extends BlogLikeBlockHandlerBean implements MySpacePersonBlockHandler {
	
	static private final Logger logger = GlobalSetup.getLogger(MySpacePersonBlockHandler.class);
	
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
	
	
	public void migrate(User user) {
		logger.debug("Migrating MySpace blocks for user {}", user);
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.MYSPACE);
		if (external == null) {
			logger.debug("No MySpace account for this user");
			return;
		}
		stacker.getOrCreateBlock(getKey(user, StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS));
		stacker.getOrCreateBlock(getKey(user, StackInclusion.ONLY_WHEN_VIEWING_SELF));		
	}	
}
