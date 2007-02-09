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
public abstract class AbstractPersonBlockView extends BlockView implements PersonSourceBlockView {
	
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
		if (userView == null)
			throw new IllegalArgumentException("populating block view with null user view");
		this.userView = userView;
	}
		
	// not public - use populate()
	protected void setPersonSource(PersonView userView) {
		this.userView = userView;
	}

	public PersonView getEntitySource() {
		return getPersonSource();
	}
	
	public PersonView getPersonSource() {
		if (!isPopulated())
			throw new IllegalStateException("BlockView not populated yet, can't get source");
	    return userView;
	}

	public List<Object> getReferencedObjects() {
		if (!isPopulated())
			throw new IllegalStateException("BlockView not populated yet, can't get referenced objects");		
		return Collections.singletonList((Object)userView);
	}
}
