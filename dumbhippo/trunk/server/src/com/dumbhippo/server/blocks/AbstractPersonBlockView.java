package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Abstract base class for block views with a single user associated with them.
 *
 */
public abstract class AbstractPersonBlockView extends BlockView {
	
	private PersonView userView;

	protected AbstractPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	protected AbstractPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	/**
	 * "partially" because it doesn't setPopulated(true), the subclass must do that
	 * @param userView
	 */
	protected void partiallyPopulate(PersonView userView) {
		this.userView = userView;
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	// not public - use populate()
	protected void setUserView(PersonView userView) {
		this.userView = userView;
	}

	@Override
	public PersonView getPersonSource() {
		if (!isPopulated())
			throw new IllegalStateException("BlockView not populated yet, can't get source");
	    return userView;
	}

	public List<Object> getReferencedObjects() {
		return Collections.singletonList((Object)userView);
	}
}
