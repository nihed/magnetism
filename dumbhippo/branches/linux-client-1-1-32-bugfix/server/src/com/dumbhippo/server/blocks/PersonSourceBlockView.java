package com.dumbhippo.server.blocks;

import com.dumbhippo.server.views.PersonView;

public interface PersonSourceBlockView extends EntitySourceBlockView {
	// these two methods should return the same thing
	public PersonView getEntitySource();
	public PersonView getPersonSource();
}
