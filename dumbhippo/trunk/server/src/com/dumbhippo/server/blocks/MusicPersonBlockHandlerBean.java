package com.dumbhippo.server.blocks;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.BlockDMO;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
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
		
		// Naively, you might think that a MUSIC_PERSON block never has messages, because
		// we suppress MUSIC_PERSON blocks when there is a MUSIC_CHAT block for the same
		// track above it, but MUSIC_PERSON blocks with messages can appear in the
		// "X's Mugshot" (participated) part of the web, since MUSIC_CHAT blocks won't
		// appear there unless the chat messages are by the person playing the music.
		TrackHistory trackHistory = tracks.get(0).getTrackHistory();
		List<ChatMessageView> messageViews = chatSystem.viewMessages(chatSystem.getNewestTrackMessages(trackHistory, MusicChatBlockView.RECENT_MESSAGE_COUNT), viewpoint);
		
		int messageCount;
		if (messageViews.size() < MusicChatBlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
			messageCount = messageViews.size();
		else
			messageCount = chatSystem.getTrackMessageCount(trackHistory);
		
		blockView.populate(userView, messageViews, messageCount);
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
			publicBlock = musicSystem.hasTrackHistory(AnonymousViewpoint.getInstance(Site.NONE), user);
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
	
	public void onMusicSharingToggled(UserViewpoint viewpoint) {
		updatePublicFlag(viewpoint.getViewer().getAccount(), false);
	}

	public void onApplicationUsageToggled(UserViewpoint viewpoint) {
		// We aren't interested in this, just part of a listener iface we're using
	}

	public void onFacebookApplicationEnabled(UserViewpoint viewpoint) {
		// We aren't interested in this, just part of a listener iface we're using
	}
	
	public void onTrackPlayed(User user, Track track, Date when) {
		Block block = stacker.stack(getKey(user), when.getTime(), user, false, StackReason.BLOCK_UPDATE);
		
		DataService.currentSessionRW().feedChanged(BlockDMO.class, new BlockDMOKey(block), "tracks", when.getTime());

		// if we weren't public we might be now. Playing a track won't 
		// ever un-public us though.
		if (!block.isPublicBlock())
			updatePublicFlag(block, user, true);
	}
}
