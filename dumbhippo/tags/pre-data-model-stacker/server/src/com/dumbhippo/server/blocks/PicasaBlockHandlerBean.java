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
import com.dumbhippo.server.PicasaUpdater;
import com.dumbhippo.services.PicasaAlbum;

@Stateless
public class PicasaBlockHandlerBean extends
		AbstractExternalThumbnailedPersonBlockHandlerBean<PicasaPersonBlockView> implements
		PicasaBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(PicasaBlockHandlerBean.class);	

	@EJB
	private PicasaUpdater picasaUpdater;
	
	protected PicasaBlockHandlerBean() {
		super(PicasaPersonBlockView.class, ExternalAccountType.PICASA, BlockType.PICASA_PERSON);
	}

	public void onPicasaRecentAlbumsChanged(String username, List<? extends PicasaAlbum> albums) {
		logger.debug("most recent Picasa albums changed for " + username);

		if (albums.size() == 0) {
			logger.debug("not restacking picasa person block since album count is 0");
			return;
		}
		
		long now = System.currentTimeMillis();
		Collection<User> users = picasaUpdater.getAccountLovers(username);
		for (User user : users) {
			stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
