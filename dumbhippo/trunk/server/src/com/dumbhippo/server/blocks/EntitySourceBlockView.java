package com.dumbhippo.server.blocks;

import java.util.List;

import com.dumbhippo.server.views.EntityView;

public interface EntitySourceBlockView {
	
	public List<Object> getReferencedObjects();
	
	public EntityView getEntitySource();
}
