package com.dumbhippo.server.blocks;

import java.util.Collection;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.SmugmugUpdater;
import com.dumbhippo.services.smugmug.rest.bind.Image;

@Stateless
public class SmugmugBlockHandlerBean extends
		AbstractExternalThumbnailedPersonBlockHandlerBean<SmugmugPersonBlockView> implements
		SmugmugBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(SmugmugBlockHandlerBean.class);	

	@EJB
	private SmugmugUpdater smugmugUpdater;
	
	protected SmugmugBlockHandlerBean() {
		super(SmugmugPersonBlockView.class, ExternalAccountType.SMUGMUG, BlockType.SMUGMUG_PERSON);
	}

	public void onSmugmugRecentAlbumsChanged(String username, List<? extends Image> albums) {
		logger.debug("most recent Smugmug albums changed for " + username);

		if (albums.size() == 0) {
			logger.debug("not restacking Smugmug person block since album count is 0");
			return;
		}
		else
			logger.debug("albums count is " + albums.size());
		
		long now = System.currentTimeMillis();
		Collection<User> users = smugmugUpdater.getAccountLovers(username);
		for (User user : users) {
			stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
