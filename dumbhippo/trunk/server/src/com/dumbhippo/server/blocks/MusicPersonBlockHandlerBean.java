package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class MusicPersonBlockHandlerBean extends AbstractBlockHandlerBean<MusicPersonBlockView> implements
		MusicPersonBlockHandler {

	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private PersonViewer personViewer;
	
	public MusicPersonBlockHandlerBean() {
		super(MusicPersonBlockView.class);
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
}
