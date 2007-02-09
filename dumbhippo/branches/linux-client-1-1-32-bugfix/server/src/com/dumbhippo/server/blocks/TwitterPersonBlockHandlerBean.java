package com.dumbhippo.server.blocks;

import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;

@Stateless
public class TwitterPersonBlockHandlerBean extends AbstractSingleBlockForFeedBlockHandlerBean<TwitterPersonBlockView> implements TwitterPersonBlockHandler {
	
	static private final Logger logger = GlobalSetup.getLogger(TwitterPersonBlockHandler.class);
	
	public TwitterPersonBlockHandlerBean() {
		super(TwitterPersonBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.TWITTER;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.TWITTER_PERSON;
	}
	
	
	public void migrate(User user) {
		logger.debug("Migrating Twitter blocks for user {}", user);
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.TWITTER);
		if (external == null) {
			logger.debug("No Twitter account for this user");
			return;
		}
		stacker.getOrCreateBlock(getKey(user));
	}
}
