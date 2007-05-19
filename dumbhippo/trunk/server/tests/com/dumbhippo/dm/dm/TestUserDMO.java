package com.dumbhippo.dm.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/test/user", resourceBase="/o/test/user")
public abstract class TestUserDMO extends DMObject<Guid> {
	@Inject
	EntityManager em;
	
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
}
