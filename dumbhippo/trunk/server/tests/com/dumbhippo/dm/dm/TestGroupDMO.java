package com.dumbhippo.dm.dm;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public abstract class TestGroupDMO extends DMObject<Guid> {
	@Inject
	EntityManager em;
	
	@Inject
	DMSession session;
	
	TestGroup group;
	
	protected TestGroupDMO(Guid key) {
		super(key);
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
	
	@DMProperty
	public List<TestGroupMemberDMO> getMembers() {
		List<TestGroupMemberDMO> result = new ArrayList<TestGroupMemberDMO>();
		for (TestGroupMember member : group.getMembers()) {
			result.add(session.findMustExist(TestGroupMemberDMO.class, new TestGroupMemberKey(member)));
		}
		
		return result;
	}
}
