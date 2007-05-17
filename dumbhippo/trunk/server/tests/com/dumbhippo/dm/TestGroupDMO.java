package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public class TestGroupDMO extends DMObject<Guid> {
	@Inject
	EntityManager em;
	
	TestGroup group;
	
	public TestGroupDMO(Guid resource) {
		super(resource);
	}

	@Override
	protected void init() throws NotFoundException {
		group = em.find(TestGroup.class, getKey().toString());
		if (group == null)
			throw new NotFoundException("No such group");
	}
	
	@DMProperty
	public String getName() {
		return group.getName();
	}
}
