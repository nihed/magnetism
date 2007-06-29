package com.dumbhippo.dm.dm;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/test/user", resourceBase="/o/test/user")
public abstract class TestUserDMO extends DMObject<Guid> {
	@Inject
	EntityManager em;
	
	@Inject
	DMSession session;

	TestUser user;
	
	protected TestUserDMO(Guid key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		user = em.find(TestUser.class, getKey().toString());
		if (user == null)
			throw new NotFoundException("No such user");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return user.getName();
	}
	
	@DMProperty
	@DMFilter("!viewer.isEnemy(this)")
	public Set<TestGroupDMO> getGroups() {
		Set<TestGroupDMO> result = new HashSet<TestGroupDMO>();
		
		for (TestGroupMember groupMember : user.getGroupMembers()) {
			if (!groupMember.isRemoved())
				result.add(session.findUnchecked(TestGroupDMO.class, groupMember.getGroup().getGuid()));
		}
		
		return result;
	}
}
