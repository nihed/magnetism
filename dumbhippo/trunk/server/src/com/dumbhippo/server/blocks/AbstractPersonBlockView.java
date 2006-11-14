package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Abstract base class for block views with a single user associated with them.
 *
 */
public abstract class AbstractPersonBlockView extends BlockView {
	
	private PersonView userView;

	protected AbstractPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView) {
		super(viewpoint, block, ubd);
		partiallyPopulate(userView);
	}
	
	protected AbstractPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
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
	    return userView;	
	}

	public List<Object> getReferencedObjects() {
		return Collections.singletonList((Object)userView);
	}
}
