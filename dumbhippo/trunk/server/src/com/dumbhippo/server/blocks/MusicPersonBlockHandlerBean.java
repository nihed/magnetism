package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class MusicPersonBlockHandlerBean extends AbstractBlockHandlerBean<MusicPersonBlockView> implements
		MusicPersonBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(MusicPersonBlockHandlerBean.class);	
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private PersonViewer personViewer;
	
	public MusicPersonBlockHandlerBean() {
		super(MusicPersonBlockView.class);
	}

	public BlockKey getKey(User user) {
		return getKey(user.getGuid());
	}

	public BlockKey getKey(Guid userId) {
		return new BlockKey(BlockType.MUSIC_PERSON, userId);
	}	
	
	@Override
	protected void populateBlockViewImpl(MusicPersonBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = identitySpider.lookupUser(block.getData1AsGuid());
		PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.PRIMARY_RESOURCE);
		List<TrackView> tracks = musicSystem.getLatestTrackViews(viewpoint, user, 5);
		if (tracks.isEmpty()) {
			throw new BlockNotVisibleException("No tracks for this person are visible");
		}
		
		userView.setTrackHistory(tracks);
		
		blockView.setUserView(userView);
		blockView.setPopulated(true);
	}
	
	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1User(block);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsIn(block);
	}

	public void onUserCreated(User user) {
		// we leave the default publicBlock=false until a track 
		// gets played
		stacker.createBlock(getKey(user));
	}
	
	// FIXME some cut-and-paste from StackerBean here for now
	private void updatePublicFlag(Account account) {
		User user = account.getOwner();
		Block block;
		try {
			block = stacker.queryBlock(getKey(user));
		} catch (NotFoundException e) {
			logger.warn("Account has no music person block, need migration? {}", account);
			return;
		}
		boolean publicBlock = identitySpider.getMusicSharingEnabled(user, Enabled.AND_ACCOUNT_IS_ACTIVE);
		block.setPublicBlock(publicBlock);
	}
	
	public void onAccountDisabledToggled(Account account) {
		updatePublicFlag(account);
	}
	
	public void onAccountAdminDisabledToggled(Account account) {
		updatePublicFlag(account);
	}
	
	public void onMusicSharingToggled(Account account) {
		updatePublicFlag(account);
	}
}
