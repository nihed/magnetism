package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class FacebookBlockHandlerBean extends AbstractBlockHandlerBean<FacebookBlockView> implements
		FacebookBlockHandler {
	
	static private final Logger logger = GlobalSetup.getLogger(FacebookBlockHandlerBean.class);

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
	
	public BlockKey getKey(User user, FacebookEvent event, StackInclusion inclusion) {
		return getKey(user.getGuid(), event.getId(), inclusion);
	}

	public BlockKey getKey(Guid userId, long eventId, StackInclusion inclusion) {
		return new BlockKey(BlockType.FACEBOOK_EVENT, userId, null, eventId, inclusion);
	}	
	
	@Override
	protected void populateBlockViewImpl(FacebookBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = getData1User(block);
		// no extras needed, we just need user.getName
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		
		if (block.getBlockType() == BlockType.FACEBOOK_EVENT) {	
			FacebookEvent facebookEvent;
			try {
			    facebookEvent = facebookSystem.lookupFacebookEvent(viewpoint, block.getData3());
			} catch (NotFoundException e) {
				throw new BlockNotVisibleException("external facebook account for block not visible", e);
			}							
		    List<FacebookEvent> facebookEvents = new ArrayList<FacebookEvent>();
		    facebookEvents.add(facebookEvent);
		    
		    blockView.populate(userView, facebookEvents, facebookSystem.getEventLink(facebookEvent));
		} else if (block.getBlockType() == BlockType.FACEBOOK_PERSON) {
		    logger.error("Will not populate a block view for BlockType.FACEBOOK_PERSON, " +
		    		     "we should not have blocks with that type around anymore!");		
		} else {
			logger.error("Unexpected block type in populateBlockViewImpl(): {}", block);
		}
	}
	
	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1UserAndExternalAccount(block, ExternalAccountType.FACEBOOK);
	}	
	
	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsInIfExternalAccount(block, ExternalAccountType.FACEBOOK);
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// we do not have per-facebook-account blocks anymore
	}
	
	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.FACEBOOK)
			return;
		// FIXME need to do stacker.refreshDeletedFlags on all keys that might exist
	}
	
	public void onFacebookEventCreated(User user, FacebookEvent event) {
		
		// FIXME check ExternalAccountSystem.getExternalAccountExistsLovedAndEnabled or is that already guaranteed?
		
		if (event.getEventType().getDisplayToOthers()) {
			Block block = stacker.createBlock(getKey(user, event, StackInclusion.IN_ALL_STACKS));	
			// TODO: adjust publicity if an account is disabled per bug 929
			block.setPublicBlock(true);
		} else {
		    stacker.createBlock(getKey(user, event, StackInclusion.ONLY_WHEN_VIEWING_SELF));
		    // the block was created with publicBlock flag set to false by default
		    // TODO: consider making StackInclusion.ONLY_WHEN_VIEWING_SELF have publicBlock
		    // set to true, because they should always be "retrieved" only for the self,
		    // and therefore should always be "public" if they were retrieved
		}
	}
	
	public void onFacebookEvent(User user, FacebookEvent event) {
		
		// FIXME check ExternalAccountSystem.getExternalAccountExistsLovedAndEnabled or is that already guaranteed?
		
		if (event.getEventType().getDisplayToOthers()) {
			stacker.stack(getKey(user, event, StackInclusion.IN_ALL_STACKS), 
				          event.getEventTimestampAsLong(), user, false, StackReason.BLOCK_UPDATE);
		} else {
			stacker.stack(getKey(user, event, StackInclusion.ONLY_WHEN_VIEWING_SELF), 
				      event.getEventTimestampAsLong(), user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
