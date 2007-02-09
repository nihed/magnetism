package com.dumbhippo.server.blocks;

import java.util.Date;
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
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class MusicPersonBlockHandlerBean extends AbstractBlockHandlerBean<MusicPersonBlockView> implements
		MusicPersonBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(MusicPersonBlockHandlerBean.class);	
	
	@EJB
	private MusicSystem musicSystem;
	
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
		
		User user = getData1User(block);
		// no resource needed just to display user.getName()
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		List<TrackView> tracks = musicSystem.getLatestTrackViews(viewpoint, user, 5);
		if (tracks.isEmpty()) {
			throw new BlockNotVisibleException("No tracks for this person are visible");
		}
		
		userView.setTrackHistory(tracks);
		
		blockView.populate(userView);
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
	private void updatePublicFlag(Block block, User user, boolean knownToHaveTracks) {
		boolean publicBlock = identitySpider.getMusicSharingEnabled(user, Enabled.AND_ACCOUNT_IS_ACTIVE);
		if (publicBlock && !knownToHaveTracks) {
			// maybe disable public block flag due to no visible tracks, this 
			// happens if someone has never played anything
			publicBlock = musicSystem.hasTrackHistory(AnonymousViewpoint.getInstance(), user);
		}
		block.setPublicBlock(publicBlock);
	}

	private void updatePublicFlag(Account account, boolean knownToHaveTracks) {
		Block block;
		try {
			block = stacker.queryBlock(getKey(account.getOwner()));
		} catch (NotFoundException e) {
			logger.warn("Account has no music person block, need migration? {}", account);
			return;
		}		
		updatePublicFlag(block, account.getOwner(), knownToHaveTracks);
	}
	
	public void onAccountDisabledToggled(Account account) {
		updatePublicFlag(account, false);
	}
	
	public void onAccountAdminDisabledToggled(Account account) {
		updatePublicFlag(account, false);
	}
	
	public void onMusicSharingToggled(Account account) {
		updatePublicFlag(account, false);
	}

	public void onTrackPlayed(User user, Track track, Date when) {
		Block block = stacker.stack(getKey(user), when.getTime(), user, false, StackReason.BLOCK_UPDATE);

		// if we weren't public we might be now. Playing a track won't 
		// ever un-public us though.
		if (!block.isPublicBlock())
			updatePublicFlag(block, user, true);
	}
}
