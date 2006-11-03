package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class FacebookBlockHandlerBean extends AbstractBlockHandlerBean<FacebookBlockView> implements
		FacebookBlockHandler {

	@EJB
	private PersonViewer personViewer;	

	@EJB
	private FacebookSystem facebookSystem;
	
	public FacebookBlockHandlerBean() {
		super(FacebookBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(FacebookBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = identitySpider.lookupUser(block.getData1AsGuid());
		// TODO: check what extras we need to request here
		PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.ALL_RESOURCES);
		FacebookAccount facebookAccount;
		try {
			facebookAccount = facebookSystem.lookupFacebookAccount(viewpoint, user);
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external facebook account for block not visible", e);
		}
		int eventsToRequestCount = 3;
		if (!facebookAccount.isSessionKeyValid() && viewpoint.isOfUser(facebookAccount.getExternalAccount().getAccount().getOwner())) {
		    eventsToRequestCount = 2;
		}
		List<FacebookEvent> facebookEvents = facebookSystem.getLatestEvents(viewpoint, facebookAccount, eventsToRequestCount);
		blockView.setUserView(userView);
		blockView.setFacebookEvents(facebookEvents);
		blockView.setPopulated(true);
	}
	
	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1User(block);
	}	
}
