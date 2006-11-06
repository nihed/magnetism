package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackInclusion;
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

	public BlockKey getKey(User user, StackInclusion inclusion) {
		return getKey(user.getGuid(), inclusion);
	}

	public BlockKey getKey(Guid userId, StackInclusion inclusion) {
		return new BlockKey(BlockType.FACEBOOK_PERSON, userId, inclusion);
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
	
	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsIn(block);
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		stacker.createBlock(getKey(user, StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS));
		stacker.createBlock(getKey(user, StackInclusion.ONLY_WHEN_VIEWING_SELF));
	}

	private void stackSelfOnly(User user, long activity) {
		stacker.stack(getKey(user, StackInclusion.ONLY_WHEN_VIEWING_SELF), activity);
	}
	
	private void stackSelfAndOthers(User user, long activity) {
		stackSelfOnly(user, activity);
		stacker.stack(getKey(user, StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS), activity);
	}
	
	public void onFacebookSignedIn(User user, FacebookAccount facebookAccount, long activity) {
		// we want to stack an update regardless of whether they have new messages, so that they know
		// they logged in successfully		
		stackSelfOnly(user, activity);
	}

	public void onFacebookSignedOut(User user, FacebookAccount facebookAccount) {
		stackSelfOnly(user, System.currentTimeMillis());
	}

	public void onFacebookEvent(User user, FacebookEventType eventType, FacebookAccount facebookAccount, long updateTime) {
		if (eventType.getDisplayToOthers())
			stackSelfAndOthers(user, updateTime);
		else
			stackSelfOnly(user, updateTime);
	}
}
